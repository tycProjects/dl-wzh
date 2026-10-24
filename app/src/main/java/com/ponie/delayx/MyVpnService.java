package com.ponie.delayx;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import com.ponie.delayx.MyVpnService.TcpSession;

public class MyVpnService extends VpnService implements Runnable {

    private static final String VPN_CHANNEL_ID = "vpn_ch";
    private static final int VPN_NOTIFICATION_ID = 1001;

    public static final String A_SE      = "com.ponie.delayx.md.se";
    public static final String A_SX      = "com.ponie.delayx.md.sx";
    public static final String A_S1      = "com.ponie.delayx.md.s1";
    public static final String A_S0      = "com.ponie.delayx.md.s0";
    public static final String A_F1      = "com.ponie.delayx.md.f1";
    public static final String A_ST      = "com.ponie.delayx.md.st";
    public static final String A_G1      = "com.ponie.delayx.md.g1";
    public static final String A_TK      = "com.ponie.delayx.md.tk";
    public static final String ACT_JITTER = "com.ponie.delayx.md.jt";
    public static final String ACT_TELE_SETPOS = "com.ponie.delayx.md.tp";
    public static final String ACT_SET_AUTO_POS = "com.ponie.delayx.md.ap";
    public static final String BROADCAST_POS = "com.ponie.delayx.md.POS";
    public static final String ACTION_START = A_S1;
    public static final String ACTION_STOP = A_SX;
    public static final String ACTION_FREEZE = A_F1;
    public static final String ACTION_GHOST = A_G1;
    public static final String ACTION_STATUS = A_ST;
    public static final String BC_SYNC = "com.ponie.delayx.md.SYNC";

    private static final int BUF_SIZE = 65536;
    private static final int IPV4_HEADER_MIN = 20;

    private static final int TCP_MSS = 1460;
    private static final int UDP_PAYLOAD_MAX_HARD = 65507;
    private static final int UDP_PAYLOAD_MIN_HARD = 1;

    volatile long lastStatMaintenance = 0L;
    volatile long lastMatchPacketTime = 0L;

    volatile int _cachedRbuf = 8388608;
    volatile int _cachedSbuf = 4194304;
    volatile int _burstMax = 64;

    volatile int _sportRotateCnt = 0;
    volatile int _sportRotateThresh = 120; // Xoay source port thường xuyên hơn (antiban)
    volatile int _sportBase = 1024;

    // ===== Antiban nhẹ: spike tự nhiên + noise thưa =====
    volatile long lastPingSpikeTime = 0L;
    volatile int spikePacketCounter = 0;
    private volatile boolean noiseActive = false;
    private Thread noiseThread = null;
    private final int NOISE_INTERVAL_MS = 12000; // 12 giây / lần – thưa, ít ảnh hưởng ping

    private static final int[] _RBUF_POOL = {4194304, 8388608, 16777216};
    private static final int[] _SBUF_POOL = {2097152, 4194304, 8388608};
    private static final String[] _SESS_NAMES = {"VPN-C1", "VPN-C2"};
    volatile int _sessNameIdx = 0;
    private static final int[] _SO_TIMEOUT_POOL = {8000, 12000, 15000};
    volatile int _soTimeoutIdx = 0;

    public static boolean isVpnRunning = false;
    volatile boolean active = false;
    volatile boolean userStopped = false;
    volatile boolean freeze = false;
    volatile boolean ghost = false;
    volatile boolean a4 = false;

    static final int TELE_BUF_MAX = 5000;

    private static final int HEARTBEAT_PACKET_SIZE = 20; // không dùng nữa (đã xoá heartbeat)

    volatile long unfreeze = 0L;

    volatile boolean jitterMode = false;
    volatile boolean jitterInfinity = false;

    final ConcurrentHashMap<Integer, Boolean> blockedIpMap = new ConcurrentHashMap<Integer, Boolean>();
    final ConcurrentLinkedQueue<_TP> _teleBuf = new ConcurrentLinkedQueue<_TP>();
    final ConcurrentLinkedQueue<byte[]> _freezeBuf = new ConcurrentLinkedQueue<byte[]>();

    volatile boolean _teleInfinityActive = false;
    volatile long _teleLastEnd = 0L;
    volatile long _teleStartTime = 0L;
    volatile int teleMode = 9;
    volatile boolean teleSmoothInfinity = true;
    volatile boolean postTelePatchActive = false;

    volatile long myPosLastUpdate = 0L;
    volatile long _lastPosBroadcast = 0L;

    volatile int lastSrvIp = 0;
    volatile int lastSrvSp = 0;
    volatile int lastSrvDp = 0;

    volatile boolean hasTeleDest = false;
    final java.util.ArrayList<byte[]> _burstPackets = new java.util.ArrayList<byte[]>();

    volatile boolean teleFinished = false;
    volatile boolean forceBlockOriginalPos = false;
    volatile boolean teleActiveMode = false;
    volatile int posBlockCounter = 0;

    ExecutorService tcpExecutor;
    final ConcurrentHashMap<Long, TcpSession> tcpSessionMap = new ConcurrentHashMap<Long, TcpSession>();
    ExecutorService icmpExecutor;

    volatile int _knownSrvIp1 = 0;
    volatile int _knownSrvIp2 = 0;
    volatile int _knownSrvIp3 = 0;

    volatile boolean run = false;
    volatile int tip;

    final AtomicLong bi;
    final AtomicLong bo;
    final AtomicLong sent;
    final AtomicLong drop;
    final Random rng;

    String pkg = "";
    ExecutorService pool = Executors.newCachedThreadPool();
    Thread th;
    ParcelFileDescriptor tun;
    volatile FileChannel tunOutChannel;

    final ConcurrentHashMap<Long, SE> map = new ConcurrentHashMap<Long, SE>();

    volatile int lastGhostDropTime = (int) 0L;

    // ========== ANTI-DETECT CẢI TIẾN (tích hợp từ Antiban) ==========
    private final AtomicInteger fragId = new AtomicInteger(new Random().nextInt(65536));

    // IP-ID thông minh: seq ^ random → xóa chữ ký gói, khó fingerprint
    int nextIpId() {
        int seq = fragId.incrementAndGet() & 0x7FFF;
        int randId = rng.nextInt(0x7FFF);
        return (seq ^ randId) & 0xFFFF;
    }

    // TTL ổn định (64 ± 2) – tránh bị flag TTL bất thường như random 50-90
    private byte randomTTL() {
        return (byte) (64 + rng.nextInt(5) - 2); // 62-66
    }

    // Chặn port rác / telemetry + bổ sung từ Antiban (443, STUN, auth range…)
    private static final int[] BLOCKED_PORTS = {
        53, 80, 443, 123, 1900, 55, 5353, 67, 68,
        137, 138, 139, 445, 25, 110, 143, 993, 995,
        3478, 3479,           // STUN
        5000, 7004, 8080, 8443,
        10001, 10002, 10003
    };

    private boolean isGamePort(int port) {
        return (port >= 7000 && port <= 10000) || (port >= 20000 && port <= 20030);
    }

    private boolean isBlockedPort(int port) {
        if (isGamePort(port)) return false;
        // Auth range 9000-9999 (trừ game port đã loại)
        if (port >= 9000 && port <= 9999) return true;
        for (int p : BLOCKED_PORTS) {
            if (p == port) return true;
        }
        return false;
    }

    // Lọc gói sảnh bất thường (tránh Abnormal Data)
    private boolean isLobbyTraffic(int dstPort, int payloadLen) {
        return (dstPort >= 7000 && dstPort <= 8500 && payloadLen < 30);
    }

    public MyVpnService() {
        this.rng = new Random();
        this.bi = new AtomicLong();
        this.bo = new AtomicLong();
        this.sent = new AtomicLong();
        this.drop = new AtomicLong();
        this.tip = ip(10, 8, 0, 2);
        this._sportRotateThresh = 200 + this.rng.nextInt(300);
        this._sportBase = 10000 + this.rng.nextInt(50000);
        this.tcpExecutor = Executors.newCachedThreadPool();
        this.icmpExecutor = Executors.newFixedThreadPool(4);

        initAntiBanIps();
    }

    private void initAntiBanIps() {
        blockedIpMap.clear();
    }

    boolean shouldGhostDropPacket(int payloadBytes) {
        return payloadBytes >= 55 && payloadBytes <= 205;
    }

    private boolean isWhitelistedIp(int ip) {
        int net = 0x6794CA00;
        int mask = 0xFFFFFF00;
        return (ip & mask) == net;
    }

    static final class TcpSession {
        final int srcIp, dstIp, srcPort, dstPort;
        volatile long clientSeq;
        volatile long serverSeq;
        volatile SocketChannel channel;
        volatile boolean closed = false;
        volatile long lastActive;
        final java.util.concurrent.LinkedBlockingQueue<byte[]> outQueue = new java.util.concurrent.LinkedBlockingQueue<byte[]>(16384);

        TcpSession(int srcIp, int dstIp, int srcPort, int dstPort, long clientSeq) {
            this.srcIp = srcIp;
            this.dstIp = dstIp;
            this.srcPort = srcPort;
            this.dstPort = dstPort;
            this.clientSeq = clientSeq;
            this.serverSeq = ((long)(new Random().nextInt(Integer.MAX_VALUE))) & 0xFFFFFFFFL;
            this.lastActive = System.currentTimeMillis();
        }
        void touch() {
            this.lastActive = System.currentTimeMillis();
        }
    }

    private boolean isMovementPacket(byte[] data, int offset, int len) {
        int typeOffset = offset + 4;
        if (typeOffset >= offset + len) return false;
        int type = data[typeOffset] & 0xFF;
        return (type <= 0x0F);
    }

    void handleTcpProtocol(byte[] fullPkt, int fullLen, final FileChannel outChannel) {
        try {
            int ipHeaderLen = (fullPkt[0] & 0x0F) * 4;
            if (fullLen < ipHeaderLen + 20) return;

            int srcIp = ri(fullPkt, 12);
            int dstIp = ri(fullPkt, 16);

            if (isBlockedIp(srcIp) || isBlockedIp(dstIp)) {
                this.drop.incrementAndGet();
                return;
            }

            int tcpOff = ipHeaderLen;
            int srcPort = rs(fullPkt, tcpOff);
            int dstPort = rs(fullPkt, tcpOff + 2);
            long seqNum = ((long)ri(fullPkt, tcpOff + 4)) & 0xFFFFFFFFL;
            int dataOff = ((fullPkt[tcpOff + 12] & 0xF0) >> 4) * 4;
            if (dataOff < 20) dataOff = 20;
            int flags = fullPkt[tcpOff + 13] & 0xFF;

            boolean isSyn = (flags & 0x02) != 0;
            boolean isAck = (flags & 0x10) != 0;
            boolean isFin = (flags & 0x01) != 0;
            boolean isRst = (flags & 0x04) != 0;

            int payloadStart = tcpOff + dataOff;
            int payloadLen = fullLen - payloadStart;
            if (payloadLen < 0) payloadLen = 0;

            byte[] payload = null;
            if (payloadLen > 0) {
                payload = new byte[payloadLen];
                System.arraycopy(fullPkt, payloadStart, payload, 0, payloadLen);
                payload = filterTcpSensitiveData(payload);
            }

            if (payload != null && payload.length == 0 && !isSyn && !isFin) {
                this.drop.incrementAndGet();
                return;
            }

            long key = ((long)(srcPort & 0xFFFF) << 48) | ((long)(dstPort & 0xFFFF) << 32) | (dstIp & 0xFFFFFFFFL);
            TcpSession session = tcpSessionMap.get(key);

            if (isSyn && !isAck) {
                if (session != null) {
                    session.closed = true;
                    session.outQueue.offer(new byte[0]);
                    try { if (session.channel != null) session.channel.close(); } catch (Exception ignored) {}
                    tcpSessionMap.remove(key);
                }
                final TcpSession ns = new TcpSession(srcIp, dstIp, srcPort, dstPort, seqNum + 1);
                try {
                    SocketChannel channel = SocketChannel.open();
                    protect(channel.socket());
                    channel.connect(new InetSocketAddress(InetAddress.getByAddress(tb(dstIp)), dstPort));
                    channel.configureBlocking(true);
                    ns.channel = channel;
                } catch (Exception e) {
                    byte[] rst = buildTcpPacket(ns, null, (byte)0x04, ns.serverSeq, ns.clientSeq);
                    writeTunSafe(outChannel, rst, false);
                    return;
                }
                tcpSessionMap.put(key, ns);
                byte[] synAck = buildTcpSynAck(ns, ns.serverSeq, ns.clientSeq);
                writeTunSafe(outChannel, synAck, false);
                ns.serverSeq++;
                startTcpHandler(ns, key, outChannel);
                return;
            }

            if (session == null || session.closed) return;
            session.touch();

            if (isRst) {
                session.closed = true;
                session.outQueue.offer(new byte[0]);
                try { if (session.channel != null) session.channel.close(); } catch (Exception ignored) {}
                tcpSessionMap.remove(key);
                return;
            }

            if (isFin) {
                if (payload != null && payload.length > 0) {
                    session.outQueue.offer(payload);
                }
                session.closed = true;
                session.outQueue.offer(new byte[0]);
                closeSession(session, key, outChannel);
                return;
            }

            if (isAck && payload == null) {
                if (seqNum > session.clientSeq) {
                    session.clientSeq = seqNum;
                }
                return;
            }

            if (payload != null && payload.length > 0) {
                if (!session.outQueue.offer(payload)) {
                    session.outQueue.poll();
                    session.outQueue.offer(payload);
                }
                synchronized (session) {
                    session.clientSeq = seqNum + (long)payloadLen;
                    byte[] ack = buildTcpPacket(session, null, (byte)0x10, session.serverSeq, session.clientSeq);
                    writeTunSafe(outChannel, ack, false);
                }
            }
        } catch (Exception ignored) {}
    }

    private void startTcpHandler(final TcpSession session, final long key, final FileChannel tunOut) {
        tcpExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    while (!session.closed && session.channel != null && session.channel.isOpen()) {
                        byte[] data = session.outQueue.poll();
                        if (data == null) {
                            Thread.sleep(5);
                            continue;
                        }
                        if (data.length == 0) break;
                        ByteBuffer buf = ByteBuffer.wrap(data);
                        while (buf.hasRemaining()) {
                            session.channel.write(buf);
                        }
                    }
                } catch (Exception e) {
                } finally {
                    try { if (session.channel != null) session.channel.close(); } catch (Exception ignored) {}
                }
            }
        });

        tcpExecutor.execute(new Runnable() {
            @Override
            public void run() {
                ByteBuffer buf = ByteBuffer.allocate(BUF_SIZE);
                try {
                    while (!session.closed && session.channel != null && session.channel.isOpen()) {
                        buf.clear();
                        int len = session.channel.read(buf);
                        if (len < 0) break;
                        if (len == 0) {
                            Thread.sleep(5);
                            continue;
                        }
                        buf.flip();
                        byte[] data = new byte[len];
                        buf.get(data);
                        synchronized (session) {
                            byte[] pkt = buildTcpPacket(session, data, (byte)0x18, session.serverSeq, session.clientSeq);
                            writeTunSafe(tunOut, pkt, false);
                            session.serverSeq += len;
                        }
                    }
                } catch (Exception e) {
                } finally {
                    session.closed = true;
                    try { if (session.channel != null) session.channel.close(); } catch (Exception ignored) {}
                    if (!session.closed) {
                        synchronized (session) {
                            byte[] fin = buildTcpPacket(session, null, (byte)0x11, session.serverSeq, session.clientSeq);
                            writeTunSafe(tunOut, fin, false);
                        }
                    }
                    tcpSessionMap.remove(key);
                }
            }
        });
    }

    byte[] filterTcpSensitiveData(byte[] data) {
        if (data == null || data.length < 8) return data;
        if (data.length == 32 || data.length == 48 || data.length == 64 || data.length == 128 || data.length == 256) {
            return new byte[0];
        }
        return data;
    }

    byte[] buildTcpSynAck(TcpSession session, long seq, long ack) {
        byte[] options = {
            0x02, 0x04, (byte)(TCP_MSS >> 8), (byte)(TCP_MSS & 0xFF),
            0x01, 0x03, 0x03, 0x08, 0x01, 0x01, 0x04, 0x02, 0x00
        };
        int optLen = 12;
        int tcpHdrLen = 20 + optLen;
        int ipTotal = 20 + tcpHdrLen;
        byte[] pkt = new byte[ipTotal];

        pkt[0] = 0x45;
        pkt[1] = 0x10;
        pkt[2] = (byte)(ipTotal >> 8);
        pkt[3] = (byte)(ipTotal & 0xFF);
        int ipId = nextIpId();
        pkt[4] = (byte)(ipId >> 8);
        pkt[5] = (byte)(ipId & 0xFF);
        pkt[6] = 0x40;
        pkt[7] = 0x00;
        pkt[8] = randomTTL();
        pkt[9] = 6;
        pkt[12] = (byte)(session.dstIp >> 24);
        pkt[13] = (byte)(session.dstIp >> 16);
        pkt[14] = (byte)(session.dstIp >> 8);
        pkt[15] = (byte)(session.dstIp);
        pkt[16] = (byte)(session.srcIp >> 24);
        pkt[17] = (byte)(session.srcIp >> 16);
        pkt[18] = (byte)(session.srcIp >> 8);
        pkt[19] = (byte)(session.srcIp);
        pkt[10] = 0; pkt[11] = 0;
        short ipCk = checksumRfc791(pkt, 0, 20);
        pkt[10] = (byte)(ipCk >> 8);
        pkt[11] = (byte)(ipCk & 0xFF);

        int t = 20;
        pkt[t]   = (byte)(session.dstPort >> 8);
        pkt[t+1] = (byte)(session.dstPort & 0xFF);
        pkt[t+2] = (byte)(session.srcPort >> 8);
        pkt[t+3] = (byte)(session.srcPort & 0xFF);
        pkt[t+4] = (byte)(seq >> 24);
        pkt[t+5] = (byte)(seq >> 16);
        pkt[t+6] = (byte)(seq >> 8);
        pkt[t+7] = (byte)(seq);
        pkt[t+8]  = (byte)(ack >> 24);
        pkt[t+9]  = (byte)(ack >> 16);
        pkt[t+10] = (byte)(ack >> 8);
        pkt[t+11] = (byte)(ack);
        pkt[t+12] = (byte)((tcpHdrLen / 4) << 4);
        pkt[t+13] = 0x12;
        int winSize = 50000 + rng.nextInt(15536);
        pkt[t+14] = (byte)(winSize >> 8);
        pkt[t+15] = (byte)(winSize & 0xFF);
        pkt[t+16] = 0;
        pkt[t+17] = 0;
        pkt[t+18] = 0;
        pkt[t+19] = 0;
        System.arraycopy(options, 0, pkt, t + 20, optLen);
        short tcpCk = computeTcpChecksumRfc793(pkt, 20, tcpHdrLen, session.dstIp, session.srcIp);
        pkt[t+16] = (byte)(tcpCk >> 8);
        pkt[t+17] = (byte)(tcpCk & 0xFF);
        return pkt;
    }

    byte[] buildTcpPacket(TcpSession session, byte[] payload, byte flags, long seq, long ack) {
        int payloadLen = (payload != null) ? payload.length : 0;
        int tcpHdrLen = 20;
        int ipTotal = 20 + tcpHdrLen + payloadLen;
        byte[] pkt = new byte[ipTotal];

        pkt[0] = 0x45;
        pkt[1] = 0x00;
        pkt[2] = (byte)(ipTotal >> 8);
        pkt[3] = (byte)(ipTotal & 0xFF);
        int ipId = nextIpId();
        pkt[4] = (byte)(ipId >> 8);
        pkt[5] = (byte)(ipId & 0xFF);
        pkt[6] = 0x40;
        pkt[7] = 0x00;
        pkt[8] = randomTTL();
        pkt[9] = 6;
        pkt[12] = (byte)(session.dstIp >> 24);
        pkt[13] = (byte)(session.dstIp >> 16);
        pkt[14] = (byte)(session.dstIp >> 8);
        pkt[15] = (byte)(session.dstIp);
        pkt[16] = (byte)(session.srcIp >> 24);
        pkt[17] = (byte)(session.srcIp >> 16);
        pkt[18] = (byte)(session.srcIp >> 8);
        pkt[19] = (byte)(session.srcIp);
        pkt[10] = 0; pkt[11] = 0;
        short ipCk = checksumRfc791(pkt, 0, 20);
        pkt[10] = (byte)(ipCk >> 8);
        pkt[11] = (byte)(ipCk & 0xFF);

        int t = 20;
        pkt[t]   = (byte)(session.dstPort >> 8);
        pkt[t+1] = (byte)(session.dstPort & 0xFF);
        pkt[t+2] = (byte)(session.srcPort >> 8);
        pkt[t+3] = (byte)(session.srcPort & 0xFF);
        pkt[t+4] = (byte)(seq >> 24);
        pkt[t+5] = (byte)(seq >> 16);
        pkt[t+6] = (byte)(seq >> 8);
        pkt[t+7] = (byte)(seq);
        pkt[t+8]  = (byte)(ack >> 24);
        pkt[t+9]  = (byte)(ack >> 16);
        pkt[t+10] = (byte)(ack >> 8);
        pkt[t+11] = (byte)(ack);
        pkt[t+12] = (byte)((tcpHdrLen / 4) << 4);
        pkt[t+13] = flags;
        int winSize = 50000 + rng.nextInt(15536);
        pkt[t+14] = (byte)(winSize >> 8);
        pkt[t+15] = (byte)(winSize & 0xFF);
        pkt[t+16] = 0;
        pkt[t+17] = 0;
        pkt[t+18] = 0;
        pkt[t+19] = 0;
        if (payload != null) {
            System.arraycopy(payload, 0, pkt, t + tcpHdrLen, payloadLen);
        }
        short tcpCk = computeTcpChecksumRfc793(pkt, 20, tcpHdrLen + payloadLen, session.dstIp, session.srcIp);
        pkt[t+16] = (byte)(tcpCk >> 8);
        pkt[t+17] = (byte)(tcpCk & 0xFF);
        return pkt;
    }

    void closeSession(TcpSession session, long key, FileChannel tunOut) {
        if (session.closed) return;
        session.closed = true;
        session.outQueue.offer(new byte[0]);
        try { if (session.channel != null) session.channel.close(); } catch (Exception ignored) {}
        tcpSessionMap.remove(key);
        synchronized (session) {
            byte[] fin = buildTcpPacket(session, null, (byte)0x11, session.serverSeq, session.clientSeq);
            writeTunSafe(tunOut, fin, false);
        }
    }

    short computeTcpChecksumRfc793(byte[] packet, int ipHeaderLen, int tcpLen, int srcIp, int dstIp) {
        int sum = 0;
        sum += (srcIp >>> 16) & 0xFFFF;
        sum += srcIp & 0xFFFF;
        sum += (dstIp >>> 16) & 0xFFFF;
        sum += dstIp & 0xFFFF;
        sum += 6;
        sum += tcpLen;
        int end = ipHeaderLen + tcpLen;
        for (int i = ipHeaderLen; i + 1 < end; i += 2) {
            sum += ((packet[i] & 0xFF) << 8) | (packet[i + 1] & 0xFF);
        }
        if ((tcpLen & 1) != 0) {
            sum += (packet[end - 1] & 0xFF) << 8;
        }
        while ((sum >> 16) != 0) sum = (sum & 0xFFFF) + (sum >> 16);
        return (short)(~sum & 0xFFFF);
    }

    short checksumRfc791(byte[] buf, int offset, int length) {
        int sum = 0;
        for (int i = offset; i < offset + length - 1; i += 2) {
            sum += ((buf[i] & 0xFF) << 8) | (buf[i+1] & 0xFF);
        }
        if ((length & 1) != 0) {
            sum += (buf[offset + length - 1] & 0xFF) << 8;
        }
        while ((sum >> 16) != 0) sum = (sum & 0xFFFF) + (sum >> 16);
        return (short)(~sum & 0xFFFF);
    }

    void handleIcmpProtocol(final byte[] buf, final int n, final int ipHdrLen, final FileChannel tunOut) {
        if (n < ipHdrLen + 8) return;
        int icmpType = buf[ipHdrLen] & 0xFF;
        if (icmpType != 8) return;
        final int dstIp = ri(buf, 16);
        final int srcIp = ri(buf, 12);
        if (isBlockedIp(srcIp) || isBlockedIp(dstIp)) {
            this.drop.incrementAndGet();
            return;
        }
        final int icmpLen = n - ipHdrLen;
        final byte[] icmpData = new byte[icmpLen];
        System.arraycopy(buf, ipHdrLen, icmpData, 0, icmpLen);
        icmpExecutor.execute(new Runnable() {
            @Override
            public void run() {
                byte[] reply = buildIcmpReply(icmpData, icmpLen, dstIp, srcIp);
                if (reply != null) writeTunSafe(tunOut, reply, false);
            }
        });
    }

    byte[] buildIcmpReply(byte[] icmpData, int icmpLen, int serverIp, int clientIp) {
        try {
            int ipTotal = 20 + icmpLen;
            byte[] pkt = new byte[ipTotal];
            pkt[0] = 0x45;
            pkt[1] = 0x00;
            pkt[2] = (byte)(ipTotal >> 8);
            pkt[3] = (byte)(ipTotal & 0xFF);
            int ipId = nextIpId();
            pkt[4] = (byte)(ipId >> 8);
            pkt[5] = (byte)(ipId & 0xFF);
            pkt[6] = 0x00;
            pkt[7] = 0x00;
            pkt[8] = randomTTL();
            pkt[9] = 1;
            pkt[10] = 0; pkt[11] = 0;
            pkt[12] = (byte)(serverIp >> 24);
            pkt[13] = (byte)(serverIp >> 16);
            pkt[14] = (byte)(serverIp >> 8);
            pkt[15] = (byte)(serverIp);
            pkt[16] = (byte)(clientIp >> 24);
            pkt[17] = (byte)(clientIp >> 16);
            pkt[18] = (byte)(clientIp >> 8);
            pkt[19] = (byte)(clientIp);
            short ipCk = checksumRfc791(pkt, 0, 20);
            pkt[10] = (byte)(ipCk >> 8);
            pkt[11] = (byte)(ipCk & 0xFF);
            System.arraycopy(icmpData, 0, pkt, 20, icmpLen);
            pkt[20] = 0;
            pkt[21] = 0;
            pkt[22] = 0;
            pkt[23] = 0;
            int icmpCs = 0;
            for (int i = 20; i < ipTotal - 1; i += 2) {
                icmpCs += ((pkt[i] & 0xFF) << 8) | (pkt[i + 1] & 0xFF);
            }
            if ((icmpLen & 1) != 0) icmpCs += (pkt[ipTotal - 1] & 0xFF) << 8;
            while ((icmpCs >> 16) != 0) icmpCs = (icmpCs & 0xFFFF) + (icmpCs >> 16);
            short icmpCk = (short)(~icmpCs & 0xFFFF);
            pkt[22] = (byte)(icmpCk >> 8);
            pkt[23] = (byte)(icmpCk & 0xFF);
            return pkt;
        } catch (Exception e) { return null; }
    }

    // Antiban: jitter nhẹ + spike tự nhiên thỉnh thoảng (40-90ms, 2-3 gói / ~80s)
    void writeTunSafe(FileChannel tunOut, byte[] pkt, boolean applyJitter) {
        if (tunOut == null || !tunOut.isOpen() || pkt == null || pkt.length == 0) return;
        try {
            int delay = 0;
            long now = System.currentTimeMillis();

            // Spike nhẹ định kỳ – tạo độ trễ “tự nhiên”, khó fingerprint
            if (now - lastPingSpikeTime >= 80000L) {
                lastPingSpikeTime = now;
                spikePacketCounter = 2 + rng.nextInt(2); // 2-3 gói
            }
            if (spikePacketCounter > 0) {
                spikePacketCounter--;
                delay = 40 + rng.nextInt(51); // 40-90ms
            } else if (applyJitter) {
                delay = 1 + rng.nextInt(3); // 1-3ms
            }

            if (delay > 0) {
                Thread.sleep(delay);
            }
            ByteBuffer buffer = ByteBuffer.wrap(pkt);
            while (buffer.hasRemaining() && tunOut.isOpen()) {
                tunOut.write(buffer);
            }
        } catch (Exception ignored) {}
    }

    // Noise thưa: gửi gói UDP rác thỉnh thoảng → làm traffic không chỉ toàn game
    private void startNoiseThread() {
        if (noiseThread != null && noiseThread.isAlive()) return;
        noiseActive = true;
        noiseThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (noiseActive && run && active) {
                    try {
                        byte[] fake = new byte[8 + rng.nextInt(16)];
                        rng.nextBytes(fake);
                        int fakeIp = (8 << 24) | (8 << 16) | (8 << 8) | 8; // 8.8.8.8 fallback
                        if (rng.nextBoolean()) {
                            fakeIp = (rng.nextInt(200) + 20) << 24
                                    | (rng.nextInt(256) << 16)
                                    | (rng.nextInt(256) << 8)
                                    | rng.nextInt(256);
                        }
                        int fakePort = 10000 + rng.nextInt(50000);
                        DatagramChannel ch = DatagramChannel.open();
                        protect(ch.socket());
                        ch.send(ByteBuffer.wrap(fake),
                                new InetSocketAddress(InetAddress.getByAddress(tb(fakeIp)), fakePort));
                        ch.close();
                    } catch (Exception ignored) {}
                    try {
                        Thread.sleep(NOISE_INTERVAL_MS + rng.nextInt(4000));
                    } catch (InterruptedException e) { break; }
                }
            }
        }, "AntiBanNoise");
        noiseThread.setPriority(Thread.MIN_PRIORITY);
        noiseThread.start();
    }

    private void stopNoiseThread() {
        noiseActive = false;
        if (noiseThread != null) {
            noiseThread.interrupt();
            noiseThread = null;
        }
    }

    void ensureForegroundServiceStarted() {
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                if (nm != null) {
                    NotificationChannel channel = new NotificationChannel(VPN_CHANNEL_ID, "System", NotificationManager.IMPORTANCE_LOW);
                    channel.setDescription("Optimizing network");
                    nm.createNotificationChannel(channel);
                }
            }
            Notification.Builder builder = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, VPN_CHANNEL_ID) : new Notification.Builder(this);
            builder.setContentTitle("System").setContentText("Optimizing network").setSmallIcon(android.R.drawable.stat_sys_upload_done).setOngoing(true);
            startForeground(VPN_NOTIFICATION_ID, builder.build());
        } catch (Exception ignored) {}
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() { super.onCreate(); }

    @Override
    public void onDestroy() { this.doStop(); super.onDestroy(); }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) return START_NOT_STICKY;
        String action = intent.getAction();
        applyRuntimeOptimizations();

        if (A_SE.equals(action)) {
            String p = intent.getStringExtra("PKG");
            this.pkg = (p != null) ? p : "";
        } else if (A_SX.equals(action)) {
            this.freeze = false;
            this.ghost = false;
            this.userStopped = true;
            this.doStop();
            return START_NOT_STICKY;
        } else if (A_F1.equals(action)) {
            boolean nextFreeze = intent.hasExtra("enabled") ? intent.getBooleanExtra("enabled", false) : !this.freeze;
            this.freeze = nextFreeze;
            if (!this.freeze) {
                this.unfreeze = System.currentTimeMillis();
                _flushFreezeBuffer();
            } else {
                this._freezeBuf.clear();
            }
            m_bcs();
        } else if (A_S1.equals(action)) {
            ensureForegroundServiceStarted();
            this.userStopped = false;
            String newToken = Long.toHexString(new Random().nextLong()) + Long.toHexString(System.currentTimeMillis());
            getSharedPreferences("cfg", MODE_PRIVATE).edit().putString("_xt", newToken).apply();
        } else if (A_S0.equals(action)) {
            this.freeze = false;
            this.ghost = false;
            this.userStopped = true;
            getSharedPreferences("cfg", MODE_PRIVATE).edit().putString("_xt", "").apply();
            this.doStop();
            return START_NOT_STICKY;
        } else if (A_TK.equals(action)) {
            int mode = intent.getIntExtra("MODE", 1);
            boolean smooth = intent.getBooleanExtra("SMOOTH", true);
            _doTele(mode, smooth);
        } else if (ACT_TELE_SETPOS.equals(action)) {
            this.hasTeleDest = true;
        } else if (ACT_SET_AUTO_POS.equals(action)) {
            this.hasTeleDest = true;
        } else if (A_G1.equals(action)) {
            boolean nextGhost = intent.hasExtra("enabled") ? intent.getBooleanExtra("enabled", false) : !this.ghost;
            this.ghost = nextGhost;
            m_bcs();
        } else if (ACT_JITTER.equals(action)) {
            if (!jitterMode) {
                _teleBuf.clear();
                jitterMode = true;
                jitterInfinity = false;
                if (a4) _disableTele();
            } else {
                jitterMode = false;
                jitterInfinity = true;
                _startJitterInfinity();
            }
        } else if (A_ST.equals(action)) {
            applyRuntimeOptimizations();
            this.m_bcs();
            return START_NOT_STICKY;
        }

        if (!this.run && !this.userStopped) {
            this.run = true;
            this.active = false;
            this.th = new Thread(this, "VPN_Main");
            this.th.setPriority(Thread.MAX_PRIORITY);
            this.th.start();
            this.startStat();
        }
        return START_NOT_STICKY;
    }

    // ========== FREEZE FLUSH ĐÃ TĂNG TỐC (BỎ JITTER) ==========
    void _flushFreezeBuffer() {
        if (this._freezeBuf.isEmpty() || this.tunOutChannel == null || !this.tunOutChannel.isOpen()) return;
        final java.util.ArrayList<byte[]> all = new java.util.ArrayList<byte[]>(this._freezeBuf);
        this._freezeBuf.clear();
        Thread flushThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < all.size(); i++) {
                        byte[] pkt = all.get(i);
                        // Ghi không jitter để flush nhanh nhất có thể
                        writeTunSafe(tunOutChannel, pkt, false);
                    }
                } catch (Exception e) {}
            }
        }, "FreezeFlush");
        flushThread.setPriority(Thread.MAX_PRIORITY);
        flushThread.start();
    }

    void applyRuntimeOptimizations() {
        _cachedRbuf = 8388608;
        _cachedSbuf = 4194304;
        _burstMax = 64;
    }

    int parseIpv4(String ipText) {
        if (ipText == null) return Integer.MIN_VALUE;
        String s = ipText.trim();
        String[] arr = s.split("\\.");
        if (arr.length != 4) return Integer.MIN_VALUE;
        try {
            int a = Integer.parseInt(arr[0]);
            int b = Integer.parseInt(arr[1]);
            int c = Integer.parseInt(arr[2]);
            int d = Integer.parseInt(arr[3]);
            if (a < 0 || a > 255 || b < 0 || b > 255 || c < 0 || c > 255 || d < 0 || d > 255) return Integer.MIN_VALUE;
            return (a << 24) | (b << 16) | (c << 8) | d;
        } catch (Exception ignored) { return Integer.MIN_VALUE; }
    }

    boolean isBlockedIp(int ip) {
        return false;
    }

    private boolean shouldBlockDnsQuery(byte[] data, int offset, int length) {
        if (data == null || length < 12) return false;
        try {
            StringBuilder sb = new StringBuilder();
            for (int i = offset; i < offset + length; i++) {
                char c = (char) (data[i] & 0xFF);
                if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '-' || c == '.') {
                    sb.append(Character.toLowerCase(c));
                } else {
                    sb.append(' ');
                }
            }
            String domainContent = sb.toString();
            if (domainContent.contains("log") || domainContent.contains("report") ||
                domainContent.contains("anticheat") || domainContent.contains("crash") ||
                domainContent.contains("apm") || domainContent.contains("tds") ||
                domainContent.contains("stats") || domainContent.contains("sentry") ||
                domainContent.contains("garena.com/api") || domainContent.contains("garena.com/sdk")) {
                return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    boolean shouldFreezeDropPacket(int payloadBytes) {
        SharedPreferences p = getSharedPreferences("cfg", MODE_PRIVATE);
        int baseMin = p.getInt("fdm_n", 20);
        int baseMax = p.getInt("fdm_x", 450);
        int min = baseMin + rng.nextInt(10);
        int max = baseMax + rng.nextInt(50);
        if (min >= max) min = max - 1;
        return payloadBytes > min && payloadBytes < max;
    }

    @Override
    public void run() {
        try {
            Builder builder = new Builder();
            String sessionName = "tun_" + Long.toHexString(System.currentTimeMillis()) + "_" + rng.nextInt(10000);
            builder.setSession(sessionName);
            int subnet = 16 + rng.nextInt(16);
            int host = 2 + rng.nextInt(253);
            String vpnIp = "172." + subnet + ".0." + host;
            builder.addAddress(vpnIp, 24);
            this.tip = parseIpv4(vpnIp);

            builder.addRoute("0.0.0.0", 0);
            builder.setMtu(1400); // Cố định – giảm fragment & ping
            builder.setBlocking(true);
            try { builder.addAllowedApplication("com.dts.freefireth"); } catch (Exception ignored) {}
            try { builder.addAllowedApplication("com.dts.freefiremax"); } catch (Exception ignored) {}
            this.tun = builder.establish();
            if (this.tun == null) { this.doStop(); return; }
            this.active = true;
            isVpnRunning = true;
            startNoiseThread(); // Antiban noise thưa
            this.m_bcs();
            this.loop();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            this.active = false;
            this.tunOutChannel = null;
            stopNoiseThread();
            this.m_bcs();
            if (this.run) this.doStop();
        }
    }

    void loop() throws Exception {
        FileInputStream fileInputStream = new FileInputStream(this.tun.getFileDescriptor());
        FileOutputStream fileOutputStream = new FileOutputStream(this.tun.getFileDescriptor());
        FileChannel tunIn = fileInputStream.getChannel();
        FileChannel tunOut = fileOutputStream.getChannel();
        this.tunOutChannel = tunOut;

        ByteBuffer readBuffer = ByteBuffer.allocateDirect(BUF_SIZE);
        byte[] arrby = new byte[BUF_SIZE];
        applyRuntimeOptimizations();

        while (this.run && this.active) {
            readBuffer.clear();
            int n;
            try {
                n = tunIn.read(readBuffer);
            } catch (Exception e) {
                break;
            }
            if (n <= 0) continue;
            readBuffer.flip();
            readBuffer.get(arrby, 0, n);

            this.bi.addAndGet((long)n);
            int ipVersion = (arrby[0] >> 4) & 0x0F;
            if (ipVersion == 4) {
                if (n < IPV4_HEADER_MIN) continue;
                handleIPv4Packet(arrby, n, tunOut);
            }
        }
    }

    void handleIPv4Packet(byte[] arrby, int n, FileChannel tunOut) {
        int n3 = (arrby[0] & 0x0F) * 4;
        if (n < n3 + 8) return;
        int protocol = arrby[9] & 0xFF;
        int packetDstIp = ri(arrby, 16);
        int packetSrcIp = ri(arrby, 12);

        if (isBlockedIp(packetDstIp) || isBlockedIp(packetSrcIp)) {
            this.drop.incrementAndGet();
            return;
        }

        if (isWhitelistedIp(packetDstIp) || isWhitelistedIp(packetSrcIp)) {
            if (protocol == 17) {
                int srcPort = rs(arrby, n3);
                int dstPort = rs(arrby, n3 + 2);
                int udpLen = rs(arrby, n3 + 4);
                int payloadStart = n3 + 8;
                int payloadLen = udpLen - 8;
                if (payloadLen >= 0 && payloadStart + payloadLen <= n) {
                    forwardUdpPacketDirect(arrby, n3, packetDstIp, srcPort, dstPort, payloadStart, payloadLen, tunOut, true);
                }
            } else if (protocol == 1) {
                byte[] icmpPkt = new byte[n];
                System.arraycopy(arrby, 0, icmpPkt, 0, n);
                handleIcmpProtocol(icmpPkt, n, n3, tunOut);
            } else if (protocol == 6) {
                byte[] tcpPkt = new byte[n];
                System.arraycopy(arrby, 0, tcpPkt, 0, n);
                handleTcpProtocol(tcpPkt, n, tunOut);
            }
            return;
        }

        if (protocol == 6 || protocol == 17) {
            int srcPort = rs(arrby, n3);
            int dstPort = rs(arrby, n3 + 2);
            // Chặn port rác / telemetry mạnh hơn
            if (isBlockedPort(srcPort) || isBlockedPort(dstPort) ||
                srcPort == 55 || dstPort == 55 ||
                srcPort == 53 || dstPort == 53 ||
                srcPort == 80 || dstPort == 80 ||
                srcPort == 123 || dstPort == 123) {
                this.drop.incrementAndGet();
                return;
            }
        }

        if (protocol == 17) {
            int n4 = packetDstIp;
            int n5 = rs(arrby, n3);
            int n6 = rs(arrby, n3 + 2);
            int n7 = rs(arrby, n3 + 4);
            int n8 = n3 + 8;
            int n9 = n7 - 8;

            if (n9 < 0 || n8 + n9 > n) return;

            // Chặn DNS chứa log/anticheat/sentry...
            if (n6 == 53 || n5 == 53) {
                if (shouldBlockDnsQuery(arrby, n8, n9)) {
                    this.drop.incrementAndGet();
                    return;
                }
            }

            // Lọc gói sảnh bất thường
            if (isBlockedPort(n5) || isBlockedPort(n6) || isLobbyTraffic(n6, n9)) {
                this.drop.incrementAndGet();
                return;
            }

            boolean isLobbyOrMatchmaking = (n6 >= 7000 && n6 <= 10000) || n9 < 15;
            if (!isLobbyOrMatchmaking && n6 > 10000 && n6 != 53) {
                lastMatchPacketTime = System.currentTimeMillis();
            }

            if (n9 > 0 && n9 <= 45) {
                forwardUdpPacketDirect(arrby, n3, n4, n5, n6, n8, n9, tunOut, true);
                return;
            }

            boolean anyFeatureActive = this.freeze || this.ghost || this.a4 || this._teleInfinityActive || this.jitterMode || this.jitterInfinity;
            if (!anyFeatureActive) {
                forwardUdpPacketDirect(arrby, n3, n4, n5, n6, n8, n9, tunOut, true);
                return;
            }

            if (n4 != 0 && n6 > 1024 && n5 > 1024) {
                lastSrvIp = n4;
                lastSrvSp = n5;
                lastSrvDp = n6;
                if (n4 != _knownSrvIp1 && n4 != _knownSrvIp2 && n4 != _knownSrvIp3) {
                    _knownSrvIp3 = _knownSrvIp2;
                    _knownSrvIp2 = _knownSrvIp1;
                    _knownSrvIp1 = n4;
                }
            }

            if (isLobbyOrMatchmaking) {
                forwardUdpPacketDirect(arrby, n3, n4, n5, n6, n8, n9, tunOut, true);
                return;
            }

            if (this.ghost) {
                if (shouldGhostDropPacket(n9)) {
                    this.lastGhostDropTime = (int) System.currentTimeMillis();
                    this.drop.incrementAndGet();
                    return;
                }
                forwardUdpPacketDirect(arrby, n3, n4, n5, n6, n8, n9, tunOut, true);
                return;
            }

            long key = (long)(n5 & 65535) << 48 | (long)(n6 & 65535) << 32 | (0xFFFFFFFFL & (long)n4);

            if ((jitterMode || jitterInfinity) && n6 != 53 && n9 >= 0x37 && n9 <= 0xA0) {
                if (jitterMode && !jitterInfinity) {
                    if (this._teleBuf.size() < TELE_BUF_MAX) {
                        byte[] copy = new byte[n9];
                        System.arraycopy(arrby, n8, copy, 0, n9);
                        this._teleBuf.add(new _TP(copy, n4, n6, n5));
                    }
                    this.drop.incrementAndGet();
                    return;
                } else if (jitterInfinity) {
                    this.drop.incrementAndGet();
                    return;
                }
            }

            if (this.a4 && n6 != 53 && n9 > 20) {
                int dstPort = n6;
                if (!isMovementPacket(arrby, n8, n9)) {
                    if ((dstPort >= 10000 && dstPort <= 10030) || (dstPort >= 20000 && dstPort <= 20030)) {
                        if (n9 >= 25) {
                            byte[] pkt = new byte[n9];
                            System.arraycopy(arrby, n8, pkt, 0, n9);
                            while (this._teleBuf.size() >= TELE_BUF_MAX) {
                                this._teleBuf.poll();
                            }
                            this._teleBuf.add(new _TP(pkt, n4, n6, n5));
                        }
                    }
                }

                if (teleMode == 1 || teleMode == 9) {
                    if (n9 > 20) { this.drop.incrementAndGet(); return; }
                } else if (teleMode == 2) {
                    if (n9 > 20 && rng.nextInt(100) < 70) { this.drop.incrementAndGet(); return; }
                } else if (teleMode == 4) {
                    this.drop.incrementAndGet();
                    return;
                }
            }

            if (this._teleInfinityActive && n6 != 53 && n9 > 20) {
                this.drop.incrementAndGet();
                return;
            }

            forwardUdpPacketDirect(arrby, n3, n4, n5, n6, n8, n9, tunOut, true);

        } else if (protocol == 1) {
            byte[] icmpPkt = new byte[n];
            System.arraycopy(arrby, 0, icmpPkt, 0, n);
            handleIcmpProtocol(icmpPkt, n, n3, tunOut);
        } else if (protocol == 6) {
            byte[] tcpPkt = new byte[n];
            System.arraycopy(arrby, 0, tcpPkt, 0, n);
            handleTcpProtocol(tcpPkt, n, tunOut);
        }
    }

    void forwardUdpPacketDirect(byte[] arrby, int n3, int n4, int n5, int n6, int n8, int n9, FileChannel tunOut, boolean applyJitter) {
        long key = (long)(n5 & 65535) << 48 | (long)(n6 & 65535) << 32 | (0xFFFFFFFFL & (long)n4);
        SE sE = this.getSE(key, n5, n4, n6, tunOut);
        if (sE == null) return;
        try {
            if (sE.channel != null && sE.channel.isOpen()) {
                byte[] cleanData = filterCleanByte(java.util.Arrays.copyOfRange(arrby, n8, n8 + n9), n9);
                if (cleanData != null && cleanData.length > 0) {
                    sE.channel.send(ByteBuffer.wrap(cleanData), new InetSocketAddress(InetAddress.getByAddress(tb(n4)), n6));
                }
                sE.touch();
                this.sent.incrementAndGet();
                this.bo.addAndGet((long)n9);
            }
        } catch (Exception ignored) {}
    }

    void _startJitterInfinity() {
        if (_teleBuf.isEmpty()) {
            jitterInfinity = false;
            return;
        }
        final java.util.ArrayList<_TP> all = new java.util.ArrayList<_TP>(_teleBuf);
        _teleBuf.clear();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < all.size(); i++) {
                        _TP tp = all.get(i);
                        if (isMovementPacket(tp.data, 0, tp.data.length)) continue;
                        sendGhostPacket(tp.data, tp.dip, tp.dp, tp.sp);
                        try { Thread.sleep(8); } catch (Exception e) { e.printStackTrace(); }
                    }
                } finally {
                    jitterInfinity = false;
                    _teleBuf.clear();
                }
            }
        }, "JitterInfinity").start();
    }

    private void sendGhostPacket(byte[] data, int dip, int dp, int sp) {
        try {
            long k = (long)(sp & 65535) << 48 | (long)(dp & 65535) << 32 | (0xFFFFFFFFL & (long)dip);
            SE se = map.get(k);
            DatagramChannel sock;
            boolean closeAfter = false;
            if (se != null && se.channel != null && se.channel.isOpen()) {
                sock = se.channel;
            } else {
                sock = DatagramChannel.open();
                protect(sock.socket());
                closeAfter = true;
            }
            if (sock.isOpen()) {
                if (data != null && data.length > 0) {
                    sock.send(ByteBuffer.wrap(data), new InetSocketAddress(InetAddress.getByAddress(tb(dip)), dp));
                }
            }
            if (se != null) se.touch();
            if (closeAfter) sock.close();
        } catch (Exception ignored) {}
    }

    void broadcastPosThrottled() {
        long now = System.currentTimeMillis();
        if (now - _lastPosBroadcast < 33) return;
        _lastPosBroadcast = now;
    }

    void broadcastPos(float x, float y, float z) {
        try {
            Intent intent = new Intent(BROADCAST_POS);
            intent.putExtra("pos_x", x);
            intent.putExtra("pos_y", y);
            intent.putExtra("pos_z", z);
            sendBroadcast(intent);
        } catch (Exception ignored) {}
    }

    void doStop() {
        stopNoiseThread();
        try {
            Iterator<TcpSession> itTcp = tcpSessionMap.values().iterator();
            while (itTcp.hasNext()) {
                TcpSession s = itTcp.next();
                if (s != null) {
                    s.closed = true;
                    s.outQueue.offer(new byte[0]);
                    try { if (s.channel != null) s.channel.close(); } catch (Exception ignored) {}
                }
            }
            tcpSessionMap.clear();
        } catch (Exception ignored) {}
        this.run = false;
        this.active = false;
        isVpnRunning = false;
        this.tunOutChannel = null;
        try { if (this.tun != null) this.tun.close(); } catch (Exception ignored) {}
        this.tun = null;
        if (this.th != null) { this.th.interrupt(); this.th = null; }

        getSharedPreferences("cfg", MODE_PRIVATE).edit().remove("_xt").apply();

        if (this.pool != null) {
            this.pool.shutdownNow();
            this.pool = Executors.newCachedThreadPool();
        }
        if (this.tcpExecutor != null) {
            this.tcpExecutor.shutdownNow();
            this.tcpExecutor = Executors.newCachedThreadPool();
        }
        if (this.icmpExecutor != null) {
            this.icmpExecutor.shutdownNow();
            this.icmpExecutor = Executors.newFixedThreadPool(4);
        }
        this.closeSocks();
        try {
            if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE);
            else stopForeground(true);
        } catch (Exception ignored) {}
        this.stopSelf();
    }

    SE getSE(long l, int n, int n2, int n3, FileChannel tunOut) {
        SE sE = this.map.get(l);
        if (sE != null && sE.channel != null && sE.channel.isOpen()) return sE;
        try {
            int _sport = 1024 + rng.nextInt(64512);
            DatagramChannel datagramChannel = DatagramChannel.open();
            try {
                datagramChannel.socket().bind(new InetSocketAddress(_sport));
            } catch (Exception _sportEx) {
                datagramChannel.socket().bind(null);
            }
            this.protect(datagramChannel.socket());
            int _rbuf = _RBUF_POOL[_soTimeoutIdx % _RBUF_POOL.length];
            int _sbuf = _SBUF_POOL[_soTimeoutIdx % _SBUF_POOL.length];
            int _soTo = _SO_TIMEOUT_POOL[_soTimeoutIdx % _SO_TIMEOUT_POOL.length];
            _soTimeoutIdx = (_soTimeoutIdx + 1) % _SO_TIMEOUT_POOL.length;
            datagramChannel.socket().setSoTimeout(_soTo);
            datagramChannel.socket().setReceiveBufferSize(_rbuf);
            datagramChannel.socket().setSendBufferSize(_sbuf);
            try { datagramChannel.socket().setTrafficClass(0x18); } catch (Exception ignored) {}
            SE sE2 = new SE(datagramChannel);
            this.map.put(l, sE2);
            this.pool.execute(new RecvRunnable(this, sE2, n2, n3, n, tunOut, l));
            return sE2;
        } catch (Exception e) { return null; }
    }

    void closeSocks() {
        Iterator<SE> it = this.map.values().iterator();
        while (it.hasNext()) {
            try {
                SE se = it.next();
                if (se != null && se.channel != null && se.channel.isOpen()) se.channel.close();
            } catch (Exception ignored) {}
        }
        this.map.clear();
    }

    byte[] mkPkt(byte[] arrby, int n, int n2, int n3, int n4, int n5) {
        int n6 = 28 + n2;
        byte[] arrby2 = new byte[n6];
        arrby2[0] = 0x45;
        arrby2[1] = 0x00;
        arrby2[2] = (byte)(n6 >> 8);
        arrby2[3] = (byte)(n6 & 0xFF);
        arrby2[4] = arrby[4];
        arrby2[5] = arrby[5];
        arrby2[6] = 0x40;
        arrby2[7] = 0x00;
        arrby2[8] = arrby[8];
        arrby2[9] = 17;
        arrby2[10] = 0;
        arrby2[11] = 0;
        arrby2[12] = (byte)(n3 >> 24);
        arrby2[13] = (byte)(n3 >> 16);
        arrby2[14] = (byte)(n3 >> 8);
        arrby2[15] = (byte)n3;
        arrby2[16] = (byte)(this.tip >> 24);
        arrby2[17] = (byte)(this.tip >> 16);
        arrby2[18] = (byte)(this.tip >> 8);
        arrby2[19] = (byte)this.tip;
        short s = checksumRfc791(arrby2, 0, 20);
        arrby2[10] = (byte)(s >> 8);
        arrby2[11] = (byte)(s & 0xFF);
        arrby2[20] = (byte)(n4 >> 8);
        arrby2[21] = (byte)(n4 & 0xFF);
        arrby2[22] = (byte)(n5 >> 8);
        arrby2[23] = (byte)(n5 & 0xFF);
        int udpLen = 8 + n2;
        arrby2[24] = (byte)(udpLen >> 8);
        arrby2[25] = (byte)(udpLen & 0xFF);
        arrby2[26] = 0;
        arrby2[27] = 0;
        System.arraycopy(arrby, n, arrby2, 28, n2);
        short udpCk = computeUdpChecksumRfc768(arrby2, 20, udpLen, n3, this.tip);
        arrby2[26] = (byte)(udpCk >> 8);
        arrby2[27] = (byte)(udpCk & 0xFF);
        return arrby2;
    }

    short computeUdpChecksumRfc768(byte[] pkt, int udpOff, int udpLen, int srcIp, int dstIp) {
        int sum = 0;
        sum += (srcIp >>> 16) & 0xFFFF;
        sum += srcIp & 0xFFFF;
        sum += (dstIp >>> 16) & 0xFFFF;
        sum += dstIp & 0xFFFF;
        sum += 17;
        sum += udpLen;
        int end = udpOff + udpLen;
        for (int i = udpOff; i + 1 < end; i += 2) {
            sum += ((pkt[i] & 0xFF) << 8) | (pkt[i+1] & 0xFF);
        }
        if ((udpLen & 1) != 0) {
            sum += (pkt[end - 1] & 0xFF) << 8;
        }
        while ((sum >> 16) != 0) sum = (sum & 0xFFFF) + (sum >> 16);
        int result = ~sum & 0xFFFF;
        if (result == 0) result = 0xFFFF;
        return (short)result;
    }

    void startStat() { this.pool.execute(new StatRunnable(this)); }

    void m_bcs() {
        try {
            Intent intent = new Intent(BC_SYNC);
            SharedPreferences _sp = getSharedPreferences("cfg", MODE_PRIVATE);
            String _token = _sp.getString("_xt", "");
            if (!_token.isEmpty()) intent.putExtra("_t", _token);
            intent.putExtra("_r", this.active);
            intent.putExtra("status", this.active ? "RUNNING" : "OFF");
            intent.putExtra("_to", this.a4);
            intent.putExtra("_tm", this.teleMode);
            intent.putExtra("ghost", this.ghost);
            intent.putExtra("freeze", this.freeze);
            intent.putExtra("_bb", 0);
            intent.putExtra("_bs", 0);
            intent.putExtra("_ts", "OK");
            intent.putExtra("_nq", "GOOD");
            sendBroadcast(intent);
        } catch (Exception ignored) {}
    }

    void _doTele(int mode, boolean smooth) {
        if (this.a4) { _disableTele(); return; }
        if (this._teleInfinityActive) { this._teleInfinityActive = false; }

        this._teleBuf.clear();
        this._burstPackets.clear();

        this._teleStartTime = System.currentTimeMillis();
        this.teleMode = mode;
        this.teleSmoothInfinity = smooth;
        this.teleFinished = false;
        this.a4 = true;
        this.teleActiveMode = true;
        try {
            Intent on = new Intent(BC_SYNC);
            on.putExtra("_to", true);
            on.putExtra("_tm", mode);
            on.putExtra("_r", active);
            on.putExtra("ghost", ghost);
            on.putExtra("freeze", freeze);
            sendBroadcast(on);
        } catch (Exception ignored) {}
    }

    void _disableTele() {
        this.a4 = false;
        this.teleFinished = true;
        this.posBlockCounter = 0;
        this.teleActiveMode = false;
        this.forceBlockOriginalPos = false;
        this.postTelePatchActive = false;
        _startTeleInfinity(true);
        this.posBlockCounter = 0;
    }

    void disableForceBlock() {
        this.forceBlockOriginalPos = false;
        this.teleFinished = false;
    }

    // ========== TELEKILL DELAY CỐ ĐỊNH 9ms ==========
    void _startTeleInfinity(final boolean smooth) {
        if (this._teleBuf.isEmpty()) {
            this._teleInfinityActive = false;
            _broadcastTeleOff();
            return;
        }
        final java.util.ArrayList<_TP> all = new java.util.ArrayList<_TP>(this._teleBuf);
        this._teleBuf.clear();
        this._teleInfinityActive = true;
        final MyVpnService svc = this;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < all.size(); i++) {
                        if (!svc._teleInfinityActive) { cleanup(svc); return; }
                        _TP tp = all.get(i);

                        if (isMovementPacket(tp.data, 0, tp.data.length)) continue;

                        _send(svc, tp);

                        // Delay cố định 9ms sau mỗi gói
                        try { Thread.sleep(10); } catch (InterruptedException e) { break; }
                    }
                } catch (Throwable t) {}
                cleanup(svc);
            }

            private void cleanup(final MyVpnService svc) {
                svc._teleInfinityActive = false;
                svc._teleBuf.clear();
                svc.teleActiveMode = false;
                svc.disableForceBlock();
                svc._broadcastTeleOff();
            }

            private void _send(final MyVpnService svc, final _TP tp) {
                _sendRaw(svc, tp.data, tp.dip, tp.dp, tp.sp);
            }

            private void _sendRaw(final MyVpnService svc, final byte[] data, final int dip, final int dp, final int sp) {
                try {
                    long k = (long)(sp & 65535) << 48 | (long)(dp & 65535) << 32 | (0xFFFFFFFFL & (long)dip);
                    SE se = svc.map.get(k);
                    DatagramChannel sock;
                    boolean closeAfter = false;
                    if (se != null && se.channel != null && se.channel.isOpen()) {
                        sock = se.channel;
                    } else {
                        sock = DatagramChannel.open();
                        svc.protect(sock.socket());
                        closeAfter = true;
                    }
                    if (sock.isOpen()) {
                        if (data != null && data.length > 0) {
                            sock.send(ByteBuffer.wrap(data), new InetSocketAddress(InetAddress.getByAddress(tb(dip)), dp));
                        }
                    }
                    if (se != null) se.touch();
                    if (closeAfter) sock.close();
                } catch (Exception ignored) {}
            }
        }, "TeleInfinity").start();
    }

    void _broadcastTeleOff() {
        try {
            Intent done = new Intent(BC_SYNC);
            done.putExtra("_to", false);
            done.putExtra("ts_st", false);
            done.putExtra("_r", active);
            done.putExtra("status", active ? "RUNNING" : "OFF");
            done.putExtra("ghost", ghost);
            done.putExtra("freeze", freeze);
            done.putExtra("_tm", teleMode);
            sendBroadcast(done);
        } catch (Exception ignored) {}
    }

    private byte[] filterCleanByte(byte[] data, int len) {
        if (data == null) return null;
        return data;
    }

    static int ip(int n, int n2, int n3, int n4) {
        return n4 | (n << 24 | n2 << 16 | n3 << 8);
    }
    static int ri(byte[] b, int n) {
        return (255 & b[n]) << 24 | (255 & b[n+1]) << 16 | (255 & b[n+2]) << 8 | (255 & b[n+3]);
    }
    static int rs(byte[] b, int n) {
        return (255 & b[n]) << 8 | (255 & b[n+1]);
    }
    static byte[] tb(int n) {
        return new byte[]{(byte)(n >> 24), (byte)(n >> 16), (byte)(n >> 8), (byte)n};
    }

    static final class _TP {
        final byte[] data;
        final int dip, dp, sp;
        final long ts;
        _TP(byte[] data, int dip, int dp, int sp) {
            this.data = data;
            this.dip = dip;
            this.dp = dp;
            this.sp = sp;
            this.ts = System.currentTimeMillis();
        }
    }

    static final class SE {
        final DatagramChannel channel;
        volatile long t;
        SE(DatagramChannel channel) {
            this.channel = channel;
            this.t = System.currentTimeMillis();
        }
        void touch() { this.t = System.currentTimeMillis(); }
    }

    static final class RecvRunnable implements Runnable {
        private final MyVpnService svc;
        private final SE ne;
        private final int sip, dp, sp;
        private final FileChannel out;
        private final long key;

        RecvRunnable(MyVpnService svc, SE ne, int sip, int dp, int sp, FileChannel out, long key) {
            this.svc = svc;
            this.ne  = ne;
            this.sip = sip;
            this.dp  = dp;
            this.sp  = sp;
            this.out = out;
            this.key = key;
        }

        @Override
        public void run() {
            ByteBuffer buffer = ByteBuffer.allocateDirect(BUF_SIZE);
            byte[] arrby = new byte[BUF_SIZE];
            long _rlWindowStart = System.currentTimeMillis();
            int _rlCount = 0;
            final int INBOUND_RATE_MAX = 12000; // Tăng để tránh drop oan khi traffic cao
            final int _expectedSrvIp;
            {
                int _raw = 0;
                try {
                    InetAddress _ia = InetAddress.getByName(this.ne.channel.socket().getInetAddress() != null ? this.ne.channel.socket().getInetAddress().getHostAddress() : "0.0.0.0");
                    byte[] _b = _ia.getAddress();
                    if (_b != null && _b.length == 4)
                        _raw = ((_b[0] & 0xFF) << 24) | ((_b[1] & 0xFF) << 16) | ((_b[2] & 0xFF) << 8) | (_b[3] & 0xFF);
                } catch (Exception ignored) {}
                _expectedSrvIp = _raw;
            }
            while (this.svc.run && this.svc.active) {
                try {
                    if (!this.ne.channel.isOpen()) break;
                    buffer.clear();
                    SocketAddress sender = this.ne.channel.receive(buffer);
                    if (sender == null) continue;
                    buffer.flip();
                    int n = buffer.remaining();

                    int currentPacketSrvIp = 0;
                    if (sender instanceof InetSocketAddress) {
                        byte[] _sb = ((InetSocketAddress)sender).getAddress().getAddress();
                        if (_sb != null && _sb.length == 4) {
                            currentPacketSrvIp = ((_sb[0] & 0xFF) << 24) | ((_sb[1] & 0xFF) << 16) | ((_sb[2] & 0xFF) << 8) | (_sb[3] & 0xFF);
                        }
                    }
                    if (this.svc.isBlockedIp(currentPacketSrvIp)) {
                        this.svc.drop.incrementAndGet();
                        continue;
                    }
                    if (_expectedSrvIp != 0 && currentPacketSrvIp != 0) {
                        if (currentPacketSrvIp != _expectedSrvIp) {
                            this.svc.drop.incrementAndGet();
                            continue;
                        }
                    }
                    long _now = System.currentTimeMillis();
                    if (_now - _rlWindowStart >= 1000L) {
                        _rlWindowStart = _now;
                        _rlCount = 0;
                    }
                    _rlCount++;
                    boolean justUnfrozen = (System.currentTimeMillis() - this.svc.unfreeze < 5000);
                    if (!justUnfrozen && _rlCount > INBOUND_RATE_MAX) {
                        this.svc.drop.incrementAndGet();
                        continue;
                    }
                    if (n < 1 || n > BUF_SIZE) {
                        this.svc.drop.incrementAndGet();
                        continue;
                    }
                    this.svc.bi.addAndGet((long)n);
                    this.ne.touch();
                    buffer.get(arrby, 0, n);

                    if (this.svc.a4 && this.svc.isMovementPacket(arrby, 0, n)) {
                        this.svc.drop.incrementAndGet();
                        continue;
                    }

                    boolean skipFreeze = (this.dp >= 7000 && this.dp <= 10000);
                    if (!skipFreeze && this.svc.freeze && this.svc.shouldFreezeDropPacket(n)) {
                        while (this.svc._freezeBuf.size() >= 10000) {
                            this.svc._freezeBuf.poll();
                        }
                        byte[] arrby2 = this.svc.mkPkt(arrby, 0, n, this.sip, this.dp, this.sp);
                        this.svc._freezeBuf.add(arrby2);
                        this.svc.drop.incrementAndGet();
                        continue;
                    }
                    byte[] arrby2 = this.svc.mkPkt(arrby, 0, n, this.sip, this.dp, this.sp);
                    this.svc.writeTunSafe(this.out, arrby2, true);
                } catch (java.nio.channels.ClosedChannelException e) {
                    break;
                } catch (java.net.SocketTimeoutException e) {
                    if (!this.svc.run || !this.svc.active) break;
                } catch (Exception e) {
                    break;
                }
            }
            this.svc.map.remove(this.key);
            try { if (this.ne.channel.isOpen()) this.ne.channel.close(); } catch (Exception ignored) {}
        }
    }

    static final class StatRunnable implements Runnable {
        private final MyVpnService svc;
        StatRunnable(MyVpnService svc) { this.svc = svc; }

        @Override
        public void run() {
            while (this.svc.run) {
                try { Thread.sleep(500L); } catch (Exception ignored) {}
                Intent i1 = new Intent(A_ST);
                i1.putExtra("status", svc.active ? "RUNNING" : "OFF");
                svc.sendBroadcast(i1);
                Intent i2 = new Intent(BC_SYNC);
                i2.putExtra("_r", svc.active);
                i2.putExtra("status", svc.active ? "RUNNING" : "OFF");
                i2.putExtra("_to", svc.a4);
                i2.putExtra("_tm", svc.teleMode);
                i2.putExtra("ghost", svc.ghost);
                i2.putExtra("freeze", svc.freeze);
                i2.putExtra("_bb", 0);
                i2.putExtra("_bs", 0);
                i2.putExtra("_ts", "OK");
                i2.putExtra("_nq", "GOOD");
                i2.putExtra("th_a", false);
                i2.putExtra("th_t", "NONE");
                svc.sendBroadcast(i2);

                try {
                    Iterator<Map.Entry<Long, SE>> it = svc.map.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<Long, SE> entry = it.next();
                        SE se = entry.getValue();
                        if (se.channel == null || !se.channel.isOpen()) {
                            try { if (se.channel != null) se.channel.close(); } catch (Exception ignored) {}
                            it.remove();
                        }
                    }

                    Iterator<Map.Entry<Long, TcpSession>> tcpIt = svc.tcpSessionMap.entrySet().iterator();
                    while (tcpIt.hasNext()) {
                        Map.Entry<Long, TcpSession> entry = tcpIt.next();
                        TcpSession session = entry.getValue();
                        if (session.closed || (session.channel != null && !session.channel.isOpen())) {
                            session.closed = true;
                            try { if (session.channel != null) session.channel.close(); } catch (Exception ignored) {}
                            tcpIt.remove();
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
    }
}
