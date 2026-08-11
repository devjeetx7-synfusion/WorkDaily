import xml.etree.ElementTree as ET

def add_strings(file_path, new_strings):
    tree = ET.parse(file_path)
    root = tree.getroot()
    existing_names = set(elem.attrib.get('name') for elem in root.findall('string'))

    changed = False
    for name, value in new_strings.items():
        if name not in existing_names:
            elem = ET.Element('string', name=name)
            elem.text = value
            elem.tail = '\n    '
            root.append(elem)
            changed = True

    if changed:
        if len(root) > 0:
            root[-1].tail = '\n'
        tree.write(file_path, encoding='utf-8', xml_declaration=True)

english_strings = {
    "how_to_use": "How to Use"
}

hindi_strings = {
    "how_to_use": "उपयोग कैसे करें"
}

add_strings('./android-offline-attendance-app/app/src/main/res/values/strings.xml', english_strings)
add_strings('./android-offline-attendance-app/app/src/main/res/values-hi/strings.xml', hindi_strings)
