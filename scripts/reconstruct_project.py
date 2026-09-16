import json, os

cursor_log = r'C:\Users\developer\.cursor\projects\c-Users-developer-Dropbox-FoodLab\agent-transcripts\36716cbc-03ca-44c2-b12e-3c4acf1f635b\36716cbc-03ca-44c2-b12e-3c4acf1f635b.jsonl'

tool_uses = {}
files_state = {}

with open(cursor_log, 'r', encoding='utf-8') as f:
    for line in f:
        try:
            obj = json.loads(line)
            msg = obj.get('message', {})
            content = msg.get('content', [])
            if isinstance(content, list):
                for part in content:
                    if isinstance(part, dict):
                        ptype = part.get('type')
                        if ptype == 'tool_use':
                            t_id = part.get('id')
                            t_name = part.get('name')
                            t_input = part.get('input', {})
                            tool_uses[t_id] = (t_name, t_input)
                            
                            if t_name == 'Write':
                                fp = t_input.get('file_path') or t_input.get('path')
                                fc = t_input.get('content') or t_input.get('file_content')
                                if fp and fc:
                                    files_state[fp] = fc
                            elif t_name == 'StrReplace':
                                fp = t_input.get('file_path') or t_input.get('path')
                                old_str = t_input.get('old_str') or t_input.get('old_string')
                                new_str = t_input.get('new_str') or t_input.get('new_string')
                                if fp in files_state and old_str and new_str:
                                    files_state[fp] = files_state[fp].replace(old_str, new_str, 1)

                        elif ptype == 'tool_result':
                            t_id = part.get('tool_use_id')
                            res_content = part.get('content')
                            if t_id in tool_uses:
                                t_name, t_input = tool_uses[t_id]
                                if t_name == 'Read':
                                    fp = t_input.get('file_path') or t_input.get('path')
                                    # res_content can be a string or list of dicts
                                    if isinstance(res_content, list):
                                        res_str = "".join(x.get('text', '') if isinstance(x, dict) else str(x) for x in res_content)
                                    else:
                                        res_str = str(res_content)
                                    
                                    # Only set if not already modified or as initial state
                                    if fp and fp not in files_state and len(res_str) > 50:
                                        files_state[fp] = res_str
        except Exception as e:
            pass

print(f"Reconstructed {len(files_state)} files from cursor log!")

base_prefix = r"C:\Users\developer\Dropbox\FoodLab"

written_count = 0
for fp, content in files_state.items():
    if not fp.startswith(base_prefix):
        continue
    # write to disk
    os.makedirs(os.path.dirname(fp), exist_ok=True)
    with open(fp, 'w', encoding='utf-8') as out:
        out.write(content)
    written_count += 1
    print(f"Restored: {fp.replace(base_prefix, '')} ({len(content.splitlines())} lines)")

print(f"Total project files restored to {base_prefix}: {written_count}")
