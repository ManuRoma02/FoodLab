import os
import sys
import requests

def _load_env():
    env_path = os.path.join(os.path.dirname(__file__), "..", ".env")
    if os.path.exists(env_path):
        with open(env_path, "r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if line and not line.startswith("#") and "=" in line:
                    k, v = line.split("=", 1)
                    k = k.strip()
                    v = v.strip().strip("\"'")
                    if k not in os.environ:
                        os.environ[k] = v

_load_env()
TOKEN = os.environ.get("TELEGRAM_BOT_TOKEN", "")
CHAT_ID = os.environ.get("TELEGRAM_CHAT_ID", "")

def main():
    apk_path = sys.argv[1] if len(sys.argv) > 1 else "FoodLab_Latest.apk"
    if not os.path.exists(apk_path):
        candidate = os.path.join("app", "build", "outputs", "apk", "debug", "app-debug.apk")
        if os.path.exists(candidate):
            apk_path = candidate
        else:
            print(f"Error: APK not found at {apk_path}", file=sys.stderr)
            sys.exit(1)

    file_size_mb = os.path.getsize(apk_path) / (1024 * 1024)
    file_name = os.path.basename(apk_path)
    caption = sys.argv[2] if len(sys.argv) > 2 else "FoodLab Latest APK Build"

    print(f"Uploading {file_name} ({file_size_mb:.1f} MB) to Telegram chat {CHAT_ID}...")
    url = f"https://api.telegram.org/bot{TOKEN}/sendDocument"
    with open(apk_path, "rb") as f:
        files = {"document": (file_name, f, "application/vnd.android.package-archive")}
        data = {"chat_id": CHAT_ID, "caption": f"{caption}\nFile: {file_name} ({file_size_mb:.1f} MB)"}
        resp = requests.post(url, data=data, files=files, timeout=300)

    res = resp.json()
    if res.get("ok"):
        print("Success! APK sent to Telegram.")
    else:
        print(f"Telegram error: {res.get('description')}", file=sys.stderr)
        sys.exit(1)

if __name__ == "__main__":
    main()
