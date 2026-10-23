import os
import re
from datetime import date

def bump():
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    props_path = os.path.join(root, "version.properties")
    update_json_path = os.path.join(root, "app-update.json")

    code = 30
    name = "1.0.30"

    if os.path.exists(props_path):
        with open(props_path, "r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if line.startswith("VERSION_CODE="):
                    code = int(line.split("=")[1].strip())
                elif line.startswith("VERSION_NAME="):
                    name = line.split("=")[1].strip()

    new_code = code + 1
    parts = name.split(".")
    major = parts[0] if len(parts) > 0 else "1"
    minor = parts[1] if len(parts) > 1 else "0"
    new_name = f"{major}.{minor}.{new_code}"

    with open(props_path, "w", encoding="utf-8") as f:
        f.write(f"# FoodLab Version Configuration\nVERSION_CODE={new_code}\nVERSION_NAME={new_name}\n")

    if os.path.exists(update_json_path):
        with open(update_json_path, "r", encoding="utf-8") as f:
            content = f.read()
        content = re.sub(r'"versionCode":\s*\d+', f'"versionCode": {new_code}', content)
        content = re.sub(r'"versionName":\s*"[^"]+"', f'"versionName": "{new_name}"', content)
        content = re.sub(r'"releaseDate":\s*"[^"]+"', f'"releaseDate": "{date.today().isoformat()}"', content)
        content = re.sub(r'/v[^/]+/app-debug\.apk', f'/v{new_name}/app-debug.apk', content)
        with open(update_json_path, "w", encoding="utf-8") as f:
            f.write(content)

    print(f"Bumped FoodLab version to: versionCode={new_code}, versionName={new_name}")

if __name__ == "__main__":
    bump()
