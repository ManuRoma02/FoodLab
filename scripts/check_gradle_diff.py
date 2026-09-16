import json

path = r'C:\Users\developer\.gemini\antigravity\brain\b68190cc-4231-4f36-9ad7-70107fe5cc36\.system_generated\logs\transcript_full.jsonl'

with open(path, 'r', encoding='utf-8') as f:
    for line in f:
        if 'app\\build.gradle.kts' in line or 'app/build.gradle.kts' in line:
            obj = json.loads(line)
            for tc in obj.get('tool_calls', []):
                args = tc.get('args', {})
                if 'build.gradle.kts' in args.get('TargetFile', ''):
                    print("Tool:", tc.get('name'))
                    print("Diff instruction:", args.get('Instruction'))
                    print("Target:", args.get('TargetContent'))
                    print("Replacement:", args.get('ReplacementContent'))
