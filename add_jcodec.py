import re

file_path = r'd:\Ban_sao_du_an\pom.xml'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

jcodec_deps = '''        <!-- JCodec for video thumbnail extraction -->
        <dependency>
            <groupId>org.jcodec</groupId>
            <artifactId>jcodec</artifactId>
            <version>0.2.5</version>
        </dependency>
        <dependency>
            <groupId>org.jcodec</groupId>
            <artifactId>jcodec-javase</artifactId>
            <version>0.2.5</version>
        </dependency>
    </dependencies>'''

if 'org.jcodec' not in content:
    content = content.replace('    </dependencies>', jcodec_deps)
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)
    print("Added JCodec to pom.xml")
else:
    print("JCodec already in pom.xml")
