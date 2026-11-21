"""
Tyce Enhancer - GPU server

Runs the same Real-ESRGAN ONNX model as the Android app, but on a rented GPU
instead of the phone. Pairs with CloudEnhancer.kt / the app's "Cloud GPU" setting.

Contract (what the app sends/expects):
  POST /enhance            multipart/form-data
    image  = the photo, any common format
    scale  = "2" or "4"    (hint only; actual scale is whatever the model outputs)
  header X-API-Key: <key>  required only if --api-key is set
  response: 200, image/jpeg body  -- OR --  4xx/5xx, plain-text error message

Run it:
  pip install -r requirements.txt
  python app.py --model realesrgan.onnx --port 8000 --api-key mysecret
  (omit --api-key to leave it open -- fine for quick personal testing only)

Rent a GPU to run it on:
  Any box with an NVIDIA GPU + Python works (RunPod, Vast.ai, Lambda Labs, a cloud VM
  with a GPU, etc). Pick one with onnxruntime-gpu support (CUDA), open the port you
  choose, and point the app's "Server URL" at http://<that box's address>:<port>/enhance.
  Put a reverse proxy with HTTPS (e.g. Caddy) in front of it for anything beyond testing.
"""

import argparse
import io
import math

import numpy as np
import onnxruntime as ort
from flask import Flask, request, Response, abort
from PIL import Image

app = Flask(__name__)

SESSION: ort.InferenceSession | None = None
INPUT_NAME: str = ""
TILE: int = 192
PAD = 16
API_KEY: str | None = None


def load_model(path: str):
    global SESSION, INPUT_NAME, TILE
    providers = ["CUDAExecutionProvider", "CPUExecutionProvider"]
    opts = ort.SessionOptions()
    opts.graph_optimization_level = ort.GraphOptimizationLevel.ORT_ENABLE_ALL
    SESSION = ort.InferenceSession(path, sess_options=opts, providers=providers)
    print("Providers in use:", SESSION.get_providers())

    inp = SESSION.get_inputs()[0]
    INPUT_NAME = inp.name
    shape = inp.shape  # e.g. [1, 3, 'height', 'width'] or [1, 3, 192, 192]
    h, w = shape[2], shape[3]
    if isinstance(h, int) and isinstance(w, int) and h > 0 and w > 0:
        TILE = min(h, w)
    print(f"Model loaded. input={INPUT_NAME} tile={TILE}")


def enhance_tiled(img: Image.Image) -> Image.Image:
    """Same tile/pad/stitch approach as Enhancer.kt, so output matches the on-device path."""
    assert SESSION is not None
    src = np.asarray(img.convert("RGB"), dtype=np.float32) / 255.0  # H,W,3
    h, w, _ = src.shape

    t = TILE
    pad = PAD
    core = t - 2 * pad
    if core <= 0:
        raise ValueError("Tile size too small for this model")

    tiles_x = math.ceil(w / core)
    tiles_y = math.ceil(h / core)

    out = None
    scale = 0

    for ty in range(tiles_y):
        for tx in range(tiles_x):
            cx, cy = tx * core, ty * core
            cw = min(core, w - cx)
            ch = min(core, h - cy)

            tile = np.zeros((t, t, 3), dtype=np.float32)
            for y in range(t):
                sy = min(max(cy - pad + y, 0), h - 1)
                for x in range(t):
                    sx = min(max(cx - pad + x, 0), w - 1)
                    tile[y, x] = src[sy, sx]
            # ^ simple and correct; for real traffic, vectorize this with np.take if it's too slow

            inp = np.transpose(tile, (2, 0, 1))[None, ...].astype(np.float32)  # 1,3,t,t
            result = SESSION.run(None, {INPUT_NAME: inp})[0][0]  # 3,oh,ow
            oh, ow = result.shape[1], result.shape[2]

            if out is None:
                scale = ow // t
                out = np.zeros((h * scale, w * scale, 3), dtype=np.float32)

            off = pad * scale
            ocw, och = cw * scale, ch * scale
            block = np.transpose(result, (1, 2, 0))[off:off + och, off:off + ocw]
            block = np.clip(block, 0.0, 1.0)
            out[cy * scale:cy * scale + och, cx * scale:cx * scale + ocw] = block

    out_img = (out * 255.0 + 0.5).astype(np.uint8)
    return Image.fromarray(out_img, mode="RGB")


@app.before_request
def check_key():
    if request.path != "/enhance":
        return
    if API_KEY and request.headers.get("X-API-Key") != API_KEY:
        abort(401, "Bad or missing API key")


@app.route("/enhance", methods=["POST"])
def enhance():
    if "image" not in request.files:
        abort(400, "No 'image' field in the upload")
    try:
        img = Image.open(request.files["image"].stream)
        img.load()
    except Exception as e:
        abort(400, f"Could not read image: {e}")

    try:
        result = enhance_tiled(img)
    except Exception as e:
        abort(500, f"Enhance failed: {e}")

    buf = io.BytesIO()
    result.save(buf, format="JPEG", quality=95)
    return Response(buf.getvalue(), mimetype="image/jpeg")


@app.route("/health", methods=["GET"])
def health():
    return {"ok": SESSION is not None, "tile": TILE}


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--model", required=True, help="Path to realesrgan.onnx (or your own model)")
    parser.add_argument("--port", type=int, default=8000)
    parser.add_argument("--host", default="0.0.0.0")
    parser.add_argument("--api-key", default=None, help="If set, required as X-API-Key header")
    args = parser.parse_args()

    API_KEY = args.api_key
    load_model(args.model)
    app.run(host=args.host, port=args.port, threaded=True)
