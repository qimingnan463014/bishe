import zipfile
import xml.etree.ElementTree as ET
import os

def extract_text(docx_path, out_path):
    print(f"Processing: {docx_path}")
    if not os.path.exists(docx_path):
        print(f"Error: File not found: {docx_path}")
        return
    
    try:
        with zipfile.ZipFile(docx_path, 'r') as z:
            if 'word/document.xml' not in z.namelist():
                print(f"Error: No word/document.xml in {docx_path}")
                return
            
            xml_content = z.read('word/document.xml')
            root = ET.fromstring(xml_content)
            ns = {'w': 'http://schemas.openxmlformats.org/wordprocessingml/2006/main'}
            
            lines = []
            for p in root.findall('.//w:p', ns):
                text = "".join([t.text for t in p.findall('.//w:t', ns) if t.text])
                if text:
                    lines.append(text)
            
            with open(out_path, 'w', encoding='utf-8') as f:
                f.write("\n".join(lines))
            print(f"Success: Saved to {out_path}")
    except Exception as e:
        print(f"Exception: {e}")

if __name__ == "__main__":
    t_in = r"e:\bishe\5.毕业设计终稿-软件工程（专升本）2205班-冯阳-220829110520.docx"
    d_in = r"e:\bishe\基于Java的员工薪资管理系统设计与实现 - 副本 (2).docx"
    
    extract_text(t_in, r"e:\bishe\template_content.txt")
    extract_text(d_in, r"e:\bishe\draft_content.txt")
