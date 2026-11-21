# Cloud GPU server (optional)

Pairs with the app's **Model → Cloud GPU** toggle. The app uploads a photo to
this server, it runs Real-ESRGAN on a real GPU, and sends the upscaled photo
back. Nothing to do with Google/Replicate accounts — it's just your box.

## 1. Rent a GPU
Any of these work fine for occasional use (spin up only when you want to enhance,
shut down after): RunPod (pods or serverless), Vast.ai, Lambda Labs, or a cloud
GPU VM (AWS/GCP/Azure/Paperspace). Pick a small/cheap GPU (a T4 or similar is
plenty for this model) with CUDA drivers preinstalled.

## 2. Deploy
```
pip install -r requirements.txt
python app.py --model realesrgan.onnx --port 8000 --api-key mysecret
```
Use the same `realesrgan.onnx` that ships in the app
(`app/src/main/assets/realesrgan.onnx`), or any other Real-ESRGAN ONNX model.
Open the port you chose in the provider's firewall/network settings.

## 3. Point the app at it
In the app: **Model → Cloud GPU → On**, then:
- Server URL: `http://<box address>:8000/enhance`
- API key: whatever you passed to `--api-key` (leave blank if you didn't set one)
- Tap **Test connection**

## Notes
- This is plain HTTP by default — fine for testing, but it means the API key and
  the photo travel unencrypted. For real use, put a reverse proxy (e.g. Caddy,
  one line of config) in front with HTTPS and point the app at `https://...`.
- `--api-key` is a shared-secret header check, not real auth — don't rely on it
  alone for anything you care about being private.
- The server loads the model once at startup; the first request after that is
  fast. Shut the box down when you're not using it to avoid paying for idle GPU time.
- Photos only for now — video goes through the on-device path.
