import json, os

path = r'C:\Users\developer\.gemini\antigravity\brain\b68190cc-4231-4f36-9ad7-70107fe5cc36\.system_generated\logs\transcript_full.jsonl'

diffs = []
with open(path, 'r', encoding='utf-8') as f:
    for line in f:
        try:
            obj = json.loads(line)
            for tc in obj.get('tool_calls', []):
                if tc.get('name') == 'replace_file_content':
                    args = tc.get('args', {})
                    tf = args.get('TargetFile')
                    target = args.get('TargetContent')
                    repl = args.get('ReplacementContent')
                    instr = args.get('Instruction')
                    if tf and target and repl:
                        diffs.append((tf, target, repl, instr))
        except:
            pass

applied = 0
for tf, target, repl, instr in diffs:
    if not os.path.exists(tf):
        continue
    with open(tf, 'r', encoding='utf-8') as f:
        content = f.read()
    if target in content:
        content = content.replace(target, repl, 1)
        with open(tf, 'w', encoding='utf-8') as f:
            f.write(content)
        applied += 1
        print(f"Applied diff to {os.path.basename(tf)}: {instr}")

print(f"Successfully applied {applied} diffs to codebase!")
