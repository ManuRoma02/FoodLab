import json, os

cursor_log = r'C:\Users\developer\.cursor\projects\c-Users-developer-Dropbox-FoodLab\agent-transcripts\36716cbc-03ca-44c2-b12e-3c4acf1f635b\36716cbc-03ca-44c2-b12e-3c4acf1f635b.jsonl'

# Dictionary mapping relative file path to its latest text content
file_map = {}

with open(cursor_log, 'r', encoding='utf-8') as f:
    for line in f:
        try:
            obj = json.loads(line)
            # 1. Check if tool result of "Read"
            # In Anthropic / Cursor JSONL format:
            # { "role": "user", "message": { "content": [ { "type": "tool_result", "tool_use_id": "...", "content": "FILE CONTENT" } ] } }
            # And matching tool_use in previous assistant message
            msg = obj.get('message', {})
            content = msg.get('content', [])
            if isinstance(content, list):
                for part in content:
                    if isinstance(part, dict) and part.get('type') == 'tool_result':
                        tool_res = part.get('content')
                        # Sometimes content is string or list
                        if isinstance(tool_res, str) and ('package com.example' in tool_res or '<?xml' in tool_res or 'plugins {' in tool_res):
                            # find tool_use_id
                            pass
        except:
            pass

print("Parsed successfully.")
