import re

with open('ClientHandler.java', 'r', encoding='utf-8') as f:
    vine = f.read()

# Extract AUTH cases
# The switch is: switch (e) { \n case "XXX": ... }
switch_start = vine.find('switch (e) {')
switch_end = vine.find('      } catch (Exception', switch_start)
if switch_start != -1 and switch_end != -1:
    switch_content = vine[switch_start:switch_end]
    # Extract only cases not in git checkout
    git_cases = set()
    with open(r'src\main\java\com\mycompany\tutorhub_enterprise\server\ClientHandler.java', 'r', encoding='utf-8') as f:
        for line in f:
            m = re.search(r'case\s+"([^"]+)"', line)
            if m:
                git_cases.add(m.group(1))

    # We will just parse the cases out of vine
    cases_code = []
    current_case = None
    current_code = []
    lines = switch_content.split('\n')
    for line in lines:
        m = re.search(r'case\s+"([^"]+)":', line)
        if m:
            if current_case and current_case not in git_cases:
                cases_code.append('\n'.join(current_code))
            current_case = m.group(1)
            current_code = [line]
        elif current_case:
            current_code.append(line)
    
    if current_case and current_case not in git_cases:
        cases_code.append('\n'.join(current_code))

    # Extract helper methods
    # Everything after "private void closeConnections() {" in Vineflower is wrong, but wait, the helper methods are before it.
    # Actually, let's just find "private boolean isCurrentUserConversationMember"
    helper_start = vine.find('   private boolean isCurrentUserConversationMember')
    helper_end = vine.find('   private int parseIntOrDefault', helper_start)
    helpers = ""
    if helper_start != -1 and helper_end != -1:
        helpers = vine[helper_start:helper_end]

    with open('extracted_patches.txt', 'w', encoding='utf-8') as out:
        out.write('// CASES\n')
        out.write('\n'.join(cases_code))
        out.write('\n// HELPERS\n')
        out.write(helpers)
