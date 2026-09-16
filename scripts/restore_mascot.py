import json, os

path = r'C:\Users\developer\.gemini\antigravity\brain\b68190cc-4231-4f36-9ad7-70107fe5cc36\.system_generated\logs\transcript_full.jsonl'

latest_mascot = None
latest_test = None

with open(path, 'r', encoding='utf-8') as f:
    for line in f:
        try:
            obj = json.loads(line)
            for tc in obj.get('tool_calls', []):
                args = tc.get('args', {})
                tf = args.get('TargetFile', '')
                rc = args.get('ReplacementContent', '')
                if 'MascotComponents.kt' in tf and rc:
                    latest_mascot = rc
                elif 'MascotComponentsTest.kt' in tf and rc:
                    latest_test = rc
                elif tc.get('name') == 'write_to_file' and 'MascotComponentsTest.kt' in tf:
                    latest_test = args.get('CodeContent')
        except:
            pass

if latest_mascot:
    p = r'c:\Users\developer\Dropbox\FoodLab\app\src\main\java\com\example\ui\screens\MascotComponents.kt'
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, 'w', encoding='utf-8') as out:
        out.write(latest_mascot)
    print("Restored MascotComponents.kt:", len(latest_mascot), "chars")

if latest_test:
    p = r'c:\Users\developer\Dropbox\FoodLab\app\src\test\java\com\example\ui\MascotComponentsTest.kt'
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, 'w', encoding='utf-8') as out:
        out.write(latest_test)
    print("Restored MascotComponentsTest.kt:", len(latest_test), "chars")
