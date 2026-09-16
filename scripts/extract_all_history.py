import os, json

brain_dir = r'C:\Users\developer\.gemini\antigravity\brain'
conv_ids = [
    '34b2d8c5-1583-4abb-8a78-ca5d3a40d2a7',
    'dbeae72f-5e33-44f4-b661-13b31602c0fb',
    'cb34bd8d-ea1e-41de-acef-46dc9ca2c080',
    '93cab158-f605-4ebd-b279-5077340b7292',
    '50ab233d-3ffe-4ec5-8ecd-e9c315853445',
    'd880ef6d-f98d-4432-9b25-4ed670375dc9',
    'efae502b-a3ac-4dc9-aeca-672d6cb42271',
    'd495594d-b82c-4d38-a1b9-877509940493',
    '9f9b9657-92bf-4a58-8aa0-09c91ae9c593',
    'db8ea7cf-48a6-408f-aed3-470b6346fbef',
    '58d3f46a-d542-4076-bd7c-8199ab8b113e',
    'e513e36f-440c-40f9-b2ce-ddf45ddfd899',
    'da2e11d7-fe82-4b07-9621-f87f50a966aa',
    '2d772d22-32e3-4c7a-8e31-efa9f4120afb',
    'daf06196-db16-41df-8c3e-bb1c72c49c85',
    '5a4fa776-4e4a-40d1-8367-20cfdacff03e',
    '487e8d48-7bbf-4558-986a-352f0ad4c829',
    'e0e88e5a-7b57-41e1-b288-15bc03d06208',
    'c6783f64-dedc-4ab7-a6bf-6332bcab86ff',
    'b68190cc-4231-4f36-9ad7-70107fe5cc36'
]

# We will collect all files created or viewed completely
full_files = {}

for cid in conv_ids:
    p = os.path.join(brain_dir, cid, '.system_generated', 'logs', 'transcript_full.jsonl')
    if not os.path.exists(p): continue
    with open(p, 'r', encoding='utf-8', errors='ignore') as f:
        for line in f:
            try:
                obj = json.loads(line)
                # Check view_file output in type: PLANNER_RESPONSE or MODEL or type: tool_result
                content = obj.get('content', '')
                if 'File Path: `file:///' in content:
                    # Parse view_file output
                    # Format:
                    # File Path: `file:///c:/Users/developer/Dropbox/FoodLab/app/src/main/...`
                    # Total Lines: ...
                    # <line_number>: <original_line>
                    idx = content.find('File Path: `file:///')
                    end_path = content.find('`', idx + len('File Path: `file:///'))
                    fpath = content[idx + len('File Path: `file:///'):end_path].replace('/', '\\')
                    
                    # Check if whole file was shown
                    if 'Showing lines 1 to ' in content and 'The above content shows the entire, complete file contents' in content:
                        lines_block = content[content.find('The following code has been modified'):]
                        extracted_lines = []
                        for l in lines_block.splitlines():
                            m = l.split(': ', 1)
                            if len(m) == 2 and m[0].strip().isdigit():
                                extracted_lines.append(m[1])
                        code = '\n'.join(extracted_lines)
                        if len(code) > 100:
                            full_files[fpath] = code

                for tc in obj.get('tool_calls', []):
                    if tc.get('name') == 'write_to_file':
                        tf = tc['args'].get('TargetFile')
                        cc = tc['args'].get('CodeContent')
                        if tf and cc:
                            full_files[tf] = cc
            except:
                pass

print(f"Discovered full contents for {len(full_files)} files!")
for fp in sorted(full_files.keys()):
    if 'Dropbox\\FoodLab' in fp:
        print(f"  {fp} ({len(full_files[fp].splitlines())} lines)")
