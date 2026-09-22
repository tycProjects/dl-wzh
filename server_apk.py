import http.server
import socketserver
import os
import mimetypes
import json

PORT = 3000
DIRECTORY = os.path.abspath("public")
STATS_FILE = os.path.abspath("download_stats.json")

mimetypes.add_type("application/vnd.android.package-archive", ".apk")

def get_stats():
    if not os.path.exists(STATS_FILE):
        data = {"downloads": 128}
        with open(STATS_FILE, "w") as f:
            json.dump(data, f)
        return data
    try:
        with open(STATS_FILE, "r") as f:
            return json.load(f)
    except Exception:
        return {"downloads": 128}

def increment_downloads():
    stats = get_stats()
    stats["downloads"] = stats.get("downloads", 0) + 1
    with open(STATS_FILE, "w") as f:
        json.dump(stats, f)
    return stats

class Handler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=DIRECTORY, **kwargs)

    def do_GET(self):
        # API endpoint to fetch download count
        if self.path == "/api/stats":
            stats = get_stats()
            body = json.dumps(stats).encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(body)))
            self.send_header("Access-Control-Allow-Origin", "*")
            self.send_header("Cache-Control", "no-cache, no-store, must-revalidate")
            self.end_headers()
            self.wfile.write(body)
            return

        # Track when user downloads APK
        if self.path.endswith(".apk"):
            increment_downloads()

        return super().do_GET()

    def do_POST(self):
        # API endpoint to explicitly increment count when clicked
        if self.path == "/api/increment":
            stats = increment_downloads()
            body = json.dumps(stats).encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(body)))
            self.send_header("Access-Control-Allow-Origin", "*")
            self.send_header("Cache-Control", "no-cache, no-store, must-revalidate")
            self.end_headers()
            self.wfile.write(body)
            return
        self.send_error(404, "Not found")

    def end_headers(self):
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Cache-Control", "no-cache")
        super().end_headers()

if __name__ == "__main__":
    os.chdir(DIRECTORY)
    with socketserver.TCPServer(("", PORT), Handler) as httpd:
        print(f"Serving HTTP on 0.0.0.0 port {PORT} from {DIRECTORY}...")
        httpd.serve_forever()
