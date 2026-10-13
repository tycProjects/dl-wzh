/*
 * mem_engine - "mesin" akses memori proses lain, jalan sebagai root (lewat su).
 *
 * Dieksekusi lewat "su -c <path>" dari MemEngine.kt, bicara lewat stdin/stdout
 * baris per baris (protokol teks sederhana). Semua perintah scan bekerja pada
 * proses yang sedang di-ATTACH (lihat cmd_attach).
 *
 * Perintah dasar:
 *   PING                        -> PONG
 *   ATTACH <pid>                -> OK | ERR <pesan>
 *   DETACH                      -> OK
 *   PROCS                       -> baris "P <pid> <nama>" ... lalu "END"
 *   MAPS <pid>                  -> baris "M <start> <end> <perm> <path>" ... lalu "END"
 *   READ <addr_hex> <len>       -> OK <hex> | ERR <pesan>          (maks 64KB)
 *   WRITE <addr_hex> <hex>      -> OK <jumlah_byte> | ERR <pesan>  (maks 64KB)
 *   EXIT                        -> (proses berhenti)
 *
 * Perintah scan nilai (modul 2+3), tipe: i16 | i32 | i64 | f32
 *   SCANNEW <tipe> <nilai>              -> OK <jumlah_kandidat>
 *   SCANFILTER <tipe> <op> [nilai]      -> OK <jumlah_kandidat>
 *       op = eq | changed | unchanged | inc | dec   (nilai wajib untuk "eq")
 *   SCANCOUNT                            -> OK <jumlah_kandidat>
 *   SCANPAGE <offset> <jumlah>           -> baris "A <addr_hex> <nilai>" ... lalu "END"
 *   SCANRESET                            -> OK
 *   WRITEVAL <addr_hex> <tipe> <nilai>   -> OK | ERR <pesan>
 *
 * Batasan sengaja dipasang:
 *   - READ/WRITE manual: maksimal 64 KB per perintah.
 *   - SCANNEW: skip region memori yang lebih besar dari 200 MB, dan berhenti
 *     kalau kandidat sudah mencapai 200.000 (supaya cepat & aman dipakai).
 */

#define _XOPEN_SOURCE 700
#define _GNU_SOURCE

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <errno.h>
#include <fcntl.h>
#include <math.h>
#include <dirent.h>
#include <ctype.h>
#include <stdint.h>
#include <signal.h>
#include <sys/ptrace.h>
#include <sys/wait.h>
#include <sys/types.h>

#define MAX_LINE 131200          /* cukup untuk 64KB data dalam bentuk hex + margin */
#define MAX_CHUNK 65536           /* batas baca/tulis manual per perintah: 64 KB */
#define SCAN_CHUNK (1 << 20)      /* baca 1MB sekaligus saat scan penuh */
#define MAX_REGION_SCAN (200ULL * 1024 * 1024) /* lewati region > 200MB */
#define MAX_CANDIDATES 200000     /* batas jumlah kandidat supaya tetap cepat & hemat memori */
#define FLOAT_EPS 0.0001f

static pid_t g_pid = -1;
static int g_memfd = -1;

/* ---- status scan nilai ---- */
static char g_type[4] = "i32";
static int g_type_size = 4;
static int g_is_float = 0;

static uint64_t *g_addrs = NULL;
static unsigned char (*g_raws)[8] = NULL;
static size_t g_cand_count = 0;
static size_t g_cand_cap = 0;

/* ============================================================ util dasar */

static void detach_current(void) {
    if (g_pid > 0) {
        /* proses dibiarkan tetap jalan (bukan freeze) selama "diawasi", supaya
         * nilai di dalamnya bisa berubah wajar antar sesi scan. Supaya
         * PTRACE_DETACH valid, proses perlu dihentikan sesaat dulu, lalu
         * detach otomatis melanjutkannya lagi. */
        kill(g_pid, SIGSTOP);
        int status = 0;
        waitpid(g_pid, &status, 0);
        ptrace(PTRACE_DETACH, g_pid, NULL, NULL);
    }
    if (g_memfd >= 0) { close(g_memfd); g_memfd = -1; }
    g_pid = -1;
}

static void scan_reset(void) {
    free(g_addrs); g_addrs = NULL;
    free(g_raws); g_raws = NULL;
    g_cand_count = 0;
    g_cand_cap = 0;
}

static int hex_val(char c) {
    if (c >= '0' && c <= '9') return c - '0';
    if (c >= 'a' && c <= 'f') return c - 'a' + 10;
    if (c >= 'A' && c <= 'F') return c - 'A' + 10;
    return -1;
}

static int hex_decode(const char *hex, unsigned char *out, size_t max_out) {
    size_t n = strlen(hex);
    if (n % 2 != 0) return -1;
    size_t bytes = n / 2;
    if (bytes > max_out) return -1;
    for (size_t i = 0; i < bytes; i++) {
        int hi = hex_val(hex[i * 2]);
        int lo = hex_val(hex[i * 2 + 1]);
        if (hi < 0 || lo < 0) return -1;
        out[i] = (unsigned char)((hi << 4) | lo);
    }
    return (int)bytes;
}

static void hex_encode(const unsigned char *data, int len, char *out) {
    static const char *H = "0123456789abcdef";
    for (int i = 0; i < len; i++) {
        out[i * 2] = H[(data[i] >> 4) & 0xF];
        out[i * 2 + 1] = H[data[i] & 0xF];
    }
    out[len * 2] = '\0';
}

/* ============================================================ perintah dasar */

static void cmd_attach(const char *arg) {
    pid_t pid = (pid_t)strtol(arg, NULL, 10);
    if (pid <= 0) { printf("ERR pid_invalid\n"); fflush(stdout); return; }

    if (g_pid > 0 && g_pid != pid) { detach_current(); scan_reset(); }

    if (g_pid != pid) {
        if (ptrace(PTRACE_ATTACH, pid, NULL, NULL) == -1) {
            printf("ERR attach_gagal:%s\n", strerror(errno)); fflush(stdout); return;
        }
        int status = 0;
        if (waitpid(pid, &status, 0) == -1) {
            printf("ERR waitpid_gagal:%s\n", strerror(errno)); fflush(stdout);
            ptrace(PTRACE_DETACH, pid, NULL, NULL); return;
        }
        char path[64]; snprintf(path, sizeof(path), "/proc/%d/mem", pid);
        int fd = open(path, O_RDWR);
        if (fd < 0) {
            printf("ERR buka_mem_gagal:%s\n", strerror(errno)); fflush(stdout);
            ptrace(PTRACE_DETACH, pid, NULL, NULL); return;
        }
        g_pid = pid; g_memfd = fd;
        scan_reset();

        /* Lepas lagi supaya proses lanjut berjalan normal. Hak baca/tulis ke
         * /proc/pid/mem tetap ada selama hubungan ptrace ini belum di-DETACH,
         * walau prosesnya sendiri sedang aktif jalan (tidak di-freeze). */
        ptrace(PTRACE_CONT, pid, NULL, NULL);
    }
    printf("OK\n"); fflush(stdout);
}

static void cmd_detach(void) {
    detach_current(); scan_reset();
    printf("OK\n"); fflush(stdout);
}

static void cmd_procs(void) {
    DIR *d = opendir("/proc");
    if (!d) { printf("ERR buka_proc_gagal:%s\n", strerror(errno)); fflush(stdout); return; }
    struct dirent *e;
    while ((e = readdir(d)) != NULL) {
        const char *name = e->d_name;
        int is_pid = name[0] != '\0';
        for (const char *p = name; *p; p++) if (!isdigit((unsigned char)*p)) { is_pid = 0; break; }
        if (!is_pid) continue;

        char path[280]; snprintf(path, sizeof(path), "/proc/%s/cmdline", name);
        FILE *f = fopen(path, "rb");
        if (!f) continue;
        char buf[256]; size_t n = fread(buf, 1, sizeof(buf) - 1, f);
        fclose(f);
        if (n == 0) continue;
        buf[n] = '\0';
        /* cmdline dipisah byte NUL; ambil token pertama saja */
        for (size_t i = 0; i < n; i++) if (buf[i] == '\0') { buf[i] = '\0'; break; }
        if (buf[0] == '\0') continue;
        printf("P %s %s\n", name, buf);
    }
    closedir(d);
    printf("END\n"); fflush(stdout);
}

static void cmd_maps(const char *arg) {
    pid_t pid = (pid_t)strtol(arg, NULL, 10);
    char path[64]; snprintf(path, sizeof(path), "/proc/%d/maps", pid);
    FILE *f = fopen(path, "r");
    if (!f) { printf("ERR buka_maps_gagal:%s\n", strerror(errno)); fflush(stdout); return; }
    char line[512];
    while (fgets(line, sizeof(line), f)) {
        size_t n = strlen(line);
        if (n && line[n - 1] == '\n') line[n - 1] = '\0';
        printf("M %s\n", line);
    }
    fclose(f);
    printf("END\n"); fflush(stdout);
}

static void cmd_read(const char *addr_hex, const char *len_str) {
    if (g_memfd < 0) { printf("ERR belum_attach\n"); fflush(stdout); return; }
    unsigned long long addr = strtoull(addr_hex, NULL, 16);
    long len = strtol(len_str, NULL, 10);
    if (len <= 0 || len > MAX_CHUNK) { printf("ERR panjang_tidak_valid\n"); fflush(stdout); return; }

    unsigned char *buf = (unsigned char *)malloc((size_t)len);
    if (!buf) { printf("ERR memori_habis\n"); fflush(stdout); return; }

    ssize_t got = pread(g_memfd, buf, (size_t)len, (off_t)addr);
    if (got < 0) { printf("ERR baca_gagal:%s\n", strerror(errno)); fflush(stdout); free(buf); return; }

    char *hex = (char *)malloc((size_t)got * 2 + 1);
    if (!hex) { printf("ERR memori_habis\n"); fflush(stdout); free(buf); return; }
    hex_encode(buf, (int)got, hex);
    printf("OK %s\n", hex); fflush(stdout);
    free(buf); free(hex);
}

static void cmd_write(const char *addr_hex, const char *data_hex) {
    if (g_memfd < 0) { printf("ERR belum_attach\n"); fflush(stdout); return; }
    unsigned long long addr = strtoull(addr_hex, NULL, 16);
    unsigned char buf[MAX_CHUNK];
    int n = hex_decode(data_hex, buf, sizeof(buf));
    if (n < 0) { printf("ERR data_tidak_valid\n"); fflush(stdout); return; }
    ssize_t put = pwrite(g_memfd, buf, (size_t)n, (off_t)addr);
    if (put < 0) { printf("ERR tulis_gagal:%s\n", strerror(errno)); fflush(stdout); return; }
    printf("OK %zd\n", put); fflush(stdout);
}

/* ============================================================ tipe nilai scan */

static int set_type(const char *t) {
    if (strcmp(t, "i16") == 0) { g_type_size = 2; g_is_float = 0; }
    else if (strcmp(t, "i32") == 0) { g_type_size = 4; g_is_float = 0; }
    else if (strcmp(t, "i64") == 0) { g_type_size = 8; g_is_float = 0; }
    else if (strcmp(t, "f32") == 0) { g_type_size = 4; g_is_float = 1; }
    else return 0;
    strncpy(g_type, t, sizeof(g_type) - 1);
    g_type[sizeof(g_type) - 1] = '\0';
    return 1;
}

static int parse_value(const char *s, unsigned char *out8) {
    memset(out8, 0, 8);
    errno = 0;
    char *end = NULL;
    if (g_is_float) {
        float v = strtof(s, &end);
        if (end == s || errno == ERANGE) return 0;
        memcpy(out8, &v, 4);
    } else {
        long long v = strtoll(s, &end, 10);
        if (end == s) return 0;
        memcpy(out8, &v, (size_t)g_type_size);
    }
    return 1;
}

static int64_t raw_to_i64(const unsigned char *b) {
    if (g_type_size == 2) { int16_t v; memcpy(&v, b, 2); return v; }
    if (g_type_size == 4) { int32_t v; memcpy(&v, b, 4); return v; }
    int64_t v; memcpy(&v, b, 8); return v;
}

static float raw_to_f32(const unsigned char *b) {
    float v; memcpy(&v, b, 4); return v;
}

static void raw_to_text(const unsigned char *b, char *out, size_t outlen) {
    if (g_is_float) snprintf(out, outlen, "%g", raw_to_f32(b));
    else snprintf(out, outlen, "%lld", (long long)raw_to_i64(b));
}

/* bandingkan nilai "cur" (sekarang) terhadap "prev" (sebelumnya) / "target" (untuk op eq) */
static int value_matches(const unsigned char *cur, const unsigned char *prev,
                          const char *op, const unsigned char *target) {
    if (g_is_float) {
        float c = raw_to_f32(cur), p = raw_to_f32(prev);
        float t = target ? raw_to_f32(target) : 0.0f;
        if (strcmp(op, "eq") == 0) return fabsf(c - t) < FLOAT_EPS;
        if (strcmp(op, "changed") == 0) return fabsf(c - p) > FLOAT_EPS;
        if (strcmp(op, "unchanged") == 0) return fabsf(c - p) <= FLOAT_EPS;
        if (strcmp(op, "inc") == 0) return c > p + FLOAT_EPS;
        if (strcmp(op, "dec") == 0) return c < p - FLOAT_EPS;
        return 0;
    } else {
        int64_t c = raw_to_i64(cur), p = raw_to_i64(prev);
        int64_t t = target ? raw_to_i64(target) : 0;
        if (strcmp(op, "eq") == 0) return c == t;
        if (strcmp(op, "changed") == 0) return c != p;
        if (strcmp(op, "unchanged") == 0) return c == p;
        if (strcmp(op, "inc") == 0) return c > p;
        if (strcmp(op, "dec") == 0) return c < p;
        return 0;
    }
}

static int add_candidate(uint64_t addr, const unsigned char *raw) {
    if (g_cand_count >= g_cand_cap) {
        size_t newcap = g_cand_cap == 0 ? 4096 : g_cand_cap * 2;
        uint64_t *na = (uint64_t *)realloc(g_addrs, newcap * sizeof(uint64_t));
        unsigned char (*nr)[8] = (unsigned char (*)[8])realloc(g_raws, newcap * 8);
        if (!na || !nr) return 0;
        g_addrs = na; g_raws = nr; g_cand_cap = newcap;
    }
    g_addrs[g_cand_count] = addr;
    memset(g_raws[g_cand_count], 0, 8);
    memcpy(g_raws[g_cand_count], raw, (size_t)g_type_size);
    g_cand_count++;
    return 1;
}

/* ============================================================ perintah scan */

static void cmd_scannew(const char *type, const char *valstr) {
    if (g_memfd < 0) { printf("ERR belum_attach\n"); fflush(stdout); return; }
    if (!set_type(type)) { printf("ERR tipe_tidak_valid\n"); fflush(stdout); return; }
    unsigned char target[8];
    if (!parse_value(valstr, target)) { printf("ERR nilai_tidak_valid\n"); fflush(stdout); return; }

    scan_reset();

    char path[64]; snprintf(path, sizeof(path), "/proc/%d/maps", g_pid);
    FILE *f = fopen(path, "r");
    if (!f) { printf("ERR buka_maps_gagal:%s\n", strerror(errno)); fflush(stdout); return; }

    unsigned char *buf = (unsigned char *)malloc(SCAN_CHUNK);
    if (!buf) { fclose(f); printf("ERR memori_habis\n"); fflush(stdout); return; }

    char line[512];
    while (fgets(line, sizeof(line), f)) {
        unsigned long long start = 0, end = 0;
        char perm[8] = {0};
        if (sscanf(line, "%llx-%llx %7s", &start, &end, perm) != 3) continue;
        if (perm[0] != 'r' || perm[1] != 'w') continue; /* hanya region readable+writable */
        unsigned long long region_len = end - start;
        if (region_len == 0 || region_len > MAX_REGION_SCAN) continue;

        unsigned long long off = 0;
        while (off < region_len && g_cand_count < MAX_CANDIDATES) {
            size_t want = (size_t)(region_len - off);
            if (want > SCAN_CHUNK) want = SCAN_CHUNK;
            ssize_t got = pread(g_memfd, buf, want, (off_t)(start + off));
            if (got <= 0) break;
            for (size_t i = 0; i + (size_t)g_type_size <= (size_t)got; i += (size_t)g_type_size) {
                if (memcmp(buf + i, target, (size_t)g_type_size) == 0) {
                    if (!add_candidate(start + off + i, buf + i)) goto done;
                    if (g_cand_count >= MAX_CANDIDATES) goto done;
                }
            }
            off += (unsigned long long)got;
        }
    }
done:
    fclose(f); free(buf);
    printf("OK %zu\n", g_cand_count); fflush(stdout);
}

static void cmd_scanfilter(const char *type, const char *op, const char *valstr_or_null) {
    if (g_memfd < 0) { printf("ERR belum_attach\n"); fflush(stdout); return; }
    if (g_cand_count == 0) { printf("ERR tidak_ada_kandidat\n"); fflush(stdout); return; }
    if (!set_type(type)) { printf("ERR tipe_tidak_valid\n"); fflush(stdout); return; }

    unsigned char target[8];
    int have_target = 0;
    if (strcmp(op, "eq") == 0) {
        if (!valstr_or_null || !parse_value(valstr_or_null, target)) {
            printf("ERR nilai_tidak_valid\n"); fflush(stdout); return;
        }
        have_target = 1;
    }

    size_t kept = 0;
    for (size_t i = 0; i < g_cand_count; i++) {
        unsigned char cur[8];
        ssize_t got = pread(g_memfd, cur, (size_t)g_type_size, (off_t)g_addrs[i]);
        if (got != g_type_size) continue; /* alamat sudah tidak valid -> buang */
        if (value_matches(cur, g_raws[i], op, have_target ? target : NULL)) {
            g_addrs[kept] = g_addrs[i];
            memset(g_raws[kept], 0, 8);
            memcpy(g_raws[kept], cur, (size_t)g_type_size);
            kept++;
        }
    }
    g_cand_count = kept;
    printf("OK %zu\n", g_cand_count); fflush(stdout);
}

static void cmd_scancount(void) {
    printf("OK %zu\n", g_cand_count); fflush(stdout);
}

static void cmd_scanpage(const char *offset_str, const char *count_str) {
    long offset = strtol(offset_str, NULL, 10);
    long count = strtol(count_str, NULL, 10);
    if (offset < 0) offset = 0;
    if (count < 0) count = 0;
    size_t start = (size_t)offset;
    size_t end = start + (size_t)count;
    if (end > g_cand_count) end = g_cand_count;
    for (size_t i = start; i < end; i++) {
        char valtxt[32];
        raw_to_text(g_raws[i], valtxt, sizeof(valtxt));
        printf("A %llx %s\n", (unsigned long long)g_addrs[i], valtxt);
    }
    printf("END\n"); fflush(stdout);
}

static void cmd_scanreset(void) {
    scan_reset();
    printf("OK\n"); fflush(stdout);
}

static void cmd_writeval(const char *addr_hex, const char *type, const char *valstr) {
    if (g_memfd < 0) { printf("ERR belum_attach\n"); fflush(stdout); return; }
    if (!set_type(type)) { printf("ERR tipe_tidak_valid\n"); fflush(stdout); return; }
    unsigned char val[8];
    if (!parse_value(valstr, val)) { printf("ERR nilai_tidak_valid\n"); fflush(stdout); return; }
    unsigned long long addr = strtoull(addr_hex, NULL, 16);
    ssize_t put = pwrite(g_memfd, val, (size_t)g_type_size, (off_t)addr);
    if (put != g_type_size) { printf("ERR tulis_gagal:%s\n", strerror(errno)); fflush(stdout); return; }
    printf("OK\n"); fflush(stdout);
}

/* ============================================================ loop utama */

int main(void) {
    setvbuf(stdout, NULL, _IONBF, 0);
    char line[MAX_LINE];

    while (fgets(line, sizeof(line), stdin)) {
        size_t n = strlen(line);
        if (n && line[n - 1] == '\n') line[--n] = '\0';
        if (n && line[n - 1] == '\r') line[--n] = '\0';

        if (strcmp(line, "PING") == 0) {
            printf("PONG\n"); fflush(stdout);
        } else if (strncmp(line, "ATTACH ", 7) == 0) {
            cmd_attach(line + 7);
        } else if (strcmp(line, "DETACH") == 0) {
            cmd_detach();
        } else if (strcmp(line, "PROCS") == 0) {
            cmd_procs();
        } else if (strncmp(line, "MAPS ", 5) == 0) {
            cmd_maps(line + 5);
        } else if (strncmp(line, "READ ", 5) == 0) {
            char *addr = line + 5; char *sp = strchr(addr, ' ');
            if (!sp) { printf("ERR format_salah\n"); fflush(stdout); continue; }
            *sp = '\0'; cmd_read(addr, sp + 1);
        } else if (strncmp(line, "WRITE ", 6) == 0) {
            char *addr = line + 6; char *sp = strchr(addr, ' ');
            if (!sp) { printf("ERR format_salah\n"); fflush(stdout); continue; }
            *sp = '\0'; cmd_write(addr, sp + 1);
        } else if (strncmp(line, "SCANNEW ", 8) == 0) {
            char *type = line + 8; char *sp = strchr(type, ' ');
            if (!sp) { printf("ERR format_salah\n"); fflush(stdout); continue; }
            *sp = '\0'; cmd_scannew(type, sp + 1);
        } else if (strncmp(line, "SCANFILTER ", 11) == 0) {
            char *type = line + 11;
            char *sp1 = strchr(type, ' ');
            if (!sp1) { printf("ERR format_salah\n"); fflush(stdout); continue; }
            *sp1 = '\0';
            char *op = sp1 + 1;
            char *sp2 = strchr(op, ' ');
            if (sp2) { *sp2 = '\0'; cmd_scanfilter(type, op, sp2 + 1); }
            else { cmd_scanfilter(type, op, NULL); }
        } else if (strcmp(line, "SCANCOUNT") == 0) {
            cmd_scancount();
        } else if (strncmp(line, "SCANPAGE ", 9) == 0) {
            char *off = line + 9; char *sp = strchr(off, ' ');
            if (!sp) { printf("ERR format_salah\n"); fflush(stdout); continue; }
            *sp = '\0'; cmd_scanpage(off, sp + 1);
        } else if (strcmp(line, "SCANRESET") == 0) {
            cmd_scanreset();
        } else if (strncmp(line, "WRITEVAL ", 9) == 0) {
            char *addr = line + 9;
            char *sp1 = strchr(addr, ' ');
            if (!sp1) { printf("ERR format_salah\n"); fflush(stdout); continue; }
            *sp1 = '\0';
            char *type = sp1 + 1;
            char *sp2 = strchr(type, ' ');
            if (!sp2) { printf("ERR format_salah\n"); fflush(stdout); continue; }
            *sp2 = '\0';
            cmd_writeval(addr, type, sp2 + 1);
        } else if (strcmp(line, "EXIT") == 0) {
            break;
        } else if (n == 0) {
            /* baris kosong, abaikan */
        } else {
            printf("ERR perintah_tidak_dikenal\n"); fflush(stdout);
        }
    }

    detach_current();
    scan_reset();
    return 0;
}
