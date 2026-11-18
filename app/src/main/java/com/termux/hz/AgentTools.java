package com.termux.hz;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Tools the AI agent may call: read / edit files, search, run commands, build. */
public final class AgentTools {

    public interface Host {
        File project();
        /** Blocks the worker thread until the user answers (or auto-approves). */
        boolean approve(String title, String detail, boolean isCommand);
        void fileChanged(File f);
        String buildBlocking();
        Sh.Result runCommand(String cmd, Sh.Handle handle);
    }

    private final Context ctx;
    private final Host host;
    /** Original contents of files the agent touched (null = file did not exist). */
    private final Map<String, String> backups = new LinkedHashMap<>();

    public AgentTools(Context c, Host h) { ctx = c; host = h; }

    public int changeCount() { return backups.size(); }

    public String revertAll() {
        int n = 0;
        for (Map.Entry<String, String> e : backups.entrySet()) {
            File f = new File(e.getKey());
            try {
                if (e.getValue() == null) f.delete(); else FileOps.writeText(f, e.getValue());
                host.fileChanged(f);
                n++;
            } catch (Exception ignored) {}
        }
        backups.clear();
        return "Reverted " + n + " file(s).";
    }

    private void backup(File f) {
        String k = f.getAbsolutePath();
        if (backups.containsKey(k)) return;
        try { backups.put(k, f.exists() ? FileOps.readText(f, 3_000_000) : null); } catch (Exception e) { backups.put(k, null); }
    }

    public static JSONArray schema() {
        JSONArray a = new JSONArray();
        try {
            a.put(tool("list_files", "List files and folders of the project as a tree.",
                    props("path", "string", "Folder relative to project root (default '.')", "depth", "integer", "Max depth (default 3)"), ""));
            a.put(tool("read_file", "Read a text file. Optionally only a line range (1-based, inclusive).",
                    props("path", "string", "File path relative to project root", "start_line", "integer", "First line", "end_line", "integer", "Last line"), "path"));
            a.put(tool("write_file", "Create or overwrite a file with the full content. Use edit_file for small changes.",
                    props("path", "string", "File path relative to project root", "content", "string", "Complete new file content"), "path,content"));
            a.put(tool("edit_file", "Replace exact text in a file. old_text must match exactly once unless replace_all is true.",
                    props("path", "string", "File path", "old_text", "string", "Exact existing text (include enough context to be unique)",
                            "new_text", "string", "Replacement text", "replace_all", "boolean", "Replace every occurrence"), "path,old_text,new_text"));
            a.put(tool("delete_file", "Delete a file or folder.", props("path", "string", "Path to delete"), "path"));
            a.put(tool("search_files", "Search text across project files (like grep). Returns path:line: text.",
                    props("query", "string", "Text or regex", "regex", "boolean", "Treat query as regex"), "query"));
            a.put(tool("run_command", "Run a shell command in the project folder (Termux bash). Use for git, ls, unzip, etc.",
                    props("command", "string", "Shell command"), "command"));
            a.put(tool("build_project", "Build the mod with Gradle/Maven using the correct JDK. Returns success/failure and the relevant log.",
                    props(), ""));
        } catch (JSONException ignored) {}
        return a;
    }

    private static JSONObject props(String... kv) throws JSONException {
        JSONObject p = new JSONObject();
        for (int i = 0; i + 2 < kv.length + 0; i += 3) {
            p.put(kv[i], new JSONObject().put("type", kv[i + 1]).put("description", kv[i + 2]));
        }
        return p;
    }

    private static JSONObject tool(String name, String desc, JSONObject props, String required) throws JSONException {
        JSONArray req = new JSONArray();
        for (String s : required.split(",")) if (!s.isEmpty()) req.put(s);
        JSONObject params = new JSONObject().put("type", "object").put("properties", props).put("required", req);
        return new JSONObject().put("type", "function").put("function",
                new JSONObject().put("name", name).put("description", desc).put("parameters", params));
    }

    /** Human readable one-liner for the UI. */
    public static String describe(String name, JSONObject a) {
        String p = a.optString("path", a.optString("query", a.optString("command", "")));
        return name + (p.isEmpty() ? "" : "  " + (p.length() > 60 ? p.substring(0, 60) + "…" : p));
    }

    public String execute(String name, String argsJson, Sh.Handle handle) {
        JSONObject a;
        try { a = new JSONObject(argsJson); }
        catch (JSONException e) { return "ERROR: arguments are not valid JSON: " + e.getMessage(); }
        File root = host.project();
        if (root == null) return "ERROR: no project is open.";
        try {
            switch (name) {
                case "list_files": {
                    File d = FileOps.safe(root, a.optString("path", "."));
                    if (!d.isDirectory()) return "ERROR: not a directory: " + a.optString("path");
                    return FileOps.tree(d, Math.max(0, Math.min(a.optInt("depth", 3), 8)), 400);
                }
                case "read_file": {
                    File f = FileOps.safe(root, a.getString("path"));
                    if (!f.isFile()) return "ERROR: file not found: " + a.getString("path");
                    if (FileOps.isBinary(f)) return "ERROR: binary file (" + f.length() + " bytes)";
                    String t = FileOps.readText(f, 400_000);
                    if (a.has("start_line") || a.has("end_line")) {
                        String[] ls = t.split("\n", -1);
                        int s = Math.max(1, a.optInt("start_line", 1)), e = Math.min(ls.length, a.optInt("end_line", ls.length));
                        StringBuilder sb = new StringBuilder();
                        for (int i = s; i <= e; i++) sb.append(i).append(": ").append(ls[i - 1]).append('\n');
                        return sb.toString();
                    }
                    if (t.length() > 60_000) return t.substring(0, 60_000) + "\n…[truncated, use start_line/end_line]";
                    return t;
                }
                case "write_file": {
                    File f = FileOps.safe(root, a.getString("path"));
                    String content = a.getString("content");
                    if (!host.approve((f.exists() ? "Overwrite " : "Create ") + FileOps.rel(root, f), preview(content), false)) return "DENIED by user.";
                    backup(f);
                    FileOps.writeText(f, content);
                    host.fileChanged(f);
                    return "OK: wrote " + content.length() + " chars to " + FileOps.rel(root, f);
                }
                case "edit_file": {
                    File f = FileOps.safe(root, a.getString("path"));
                    if (!f.isFile()) return "ERROR: file not found: " + a.getString("path");
                    String t = FileOps.readText(f, 3_000_000);
                    String oldT = a.getString("old_text"), newT = a.getString("new_text");
                    if (oldT.isEmpty()) return "ERROR: old_text is empty";
                    int first = t.indexOf(oldT);
                    if (first < 0) {
                        // tolerate CRLF / trailing whitespace differences
                        String t2 = t.replace("\r\n", "\n");
                        if (t2.contains(oldT)) { t = t2; first = t.indexOf(oldT); }
                    }
                    if (first < 0) return "ERROR: old_text not found. Read the file again and copy the text exactly.";
                    boolean all = a.optBoolean("replace_all", false);
                    if (!all && t.indexOf(oldT, first + 1) >= 0) return "ERROR: old_text matches multiple places. Add more context or set replace_all.";
                    if (!host.approve("Edit " + FileOps.rel(root, f), "- " + preview(oldT) + "\n+ " + preview(newT), false)) return "DENIED by user.";
                    backup(f);
                    String out = all ? t.replace(oldT, newT) : t.substring(0, first) + newT + t.substring(first + oldT.length());
                    FileOps.writeText(f, out);
                    host.fileChanged(f);
                    return "OK: edited " + FileOps.rel(root, f);
                }
                case "delete_file": {
                    File f = FileOps.safe(root, a.getString("path"));
                    if (f.equals(root)) return "ERROR: refusing to delete the project root.";
                    if (!f.exists()) return "ERROR: not found";
                    if (!host.approve("Delete " + FileOps.rel(root, f), "", false)) return "DENIED by user.";
                    if (f.isFile()) backup(f);
                    FileOps.deleteTree(f);
                    host.fileChanged(f);
                    return "OK: deleted " + FileOps.rel(root, f);
                }
                case "search_files": {
                    List<FileOps.Hit> hits = FileOps.grep(root, a.getString("query"), a.optBoolean("regex", false), false, 80);
                    if (hits.isEmpty()) return "No matches.";
                    StringBuilder sb = new StringBuilder();
                    for (FileOps.Hit h : hits) sb.append(FileOps.rel(root, h.file)).append(':').append(h.line).append(": ").append(h.text.trim()).append('\n');
                    return sb.toString();
                }
                case "run_command": {
                    String cmd = a.getString("command");
                    if (!host.approve("Run command", cmd, true)) return "DENIED by user.";
                    Sh.Result r = host.runCommand(cmd, handle);
                    return "exit code " + r.code + "\n" + Sh.tail(r.output, 8000);
                }
                case "build_project": {
                    if (!host.approve("Build project", "Run the Gradle/Maven build", true)) return "DENIED by user.";
                    return host.buildBlocking();
                }
                default: return "ERROR: unknown tool " + name;
            }
        } catch (JSONException e) {
            return "ERROR: missing or invalid argument: " + e.getMessage();
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    private static String preview(String s) {
        s = s.trim();
        return s.length() > 600 ? s.substring(0, 600) + "\n…" : s;
    }
}
