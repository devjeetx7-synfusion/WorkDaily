import xml.etree.ElementTree as ET

def add_strings(file_path, new_strings):
    tree = ET.parse(file_path)
    root = tree.getroot()

    # Check what already exists to avoid duplicates
    existing_names = set(elem.attrib.get('name') for elem in root.findall('string'))

    changed = False
    for name, value in new_strings.items():
        if name not in existing_names:
            elem = ET.Element('string', name=name)
            elem.text = value
            # Formatting to match standard xml
            elem.tail = '\n    '
            root.append(elem)
            changed = True

    if changed:
        # Prettify closing tag
        if len(root) > 0:
            root[-1].tail = '\n'
        tree.write(file_path, encoding='utf-8', xml_declaration=True)

english_strings = {
    "htu_getting_started_title": "1. Getting Started",
    "htu_getting_started_desc": "Open the application, complete your initial setup, configure your user profile, and set basic preferences like your daily wage and working hours.",
    "htu_attendance_title": "2. Attendance",
    "htu_attendance_desc": "Mark your attendance daily as Present, Half Day, or Absent. You can view, edit, and keep track of your date-based attendance records easily.",
    "htu_earnings_title": "3. Earnings",
    "htu_earnings_desc": "Calculates your earnings based on your daily wage configuration, base salary, overtime hours, advances, and overall attendance.",
    "htu_calendar_title": "4. Calendar",
    "htu_calendar_desc": "Open the Calendar to select specific dates and view your attendance records with color-coded indicators.",
    "htu_passbook_title": "5. Passbook",
    "htu_passbook_desc": "Review your daily records including earnings, overtime, advance amounts, and personal notes. You can search, sort, and view your complete monthly summary.",
    "htu_all_time_passbook_title": "6. All Time Passbook",
    "htu_all_time_passbook_desc": "Your full historical records organized by year and month. Expand or collapse monthly blocks to view detailed attendance and financial summaries.",
    "htu_statistics_title": "7. Statistics",
    "htu_statistics_desc": "Visualize your overall earnings and attendance statistics. Dive deep into your financial summaries and drill-down into specific records.",
    "htu_backup_restore_title": "8. Backup & Restore",
    "htu_backup_restore_desc": "Keep your data safe by creating backups. You can share or save these files, and restore them later to recover your records safely.",
    "htu_pdf_export_title": "9. PDF Export",
    "htu_pdf_export_desc": "Generate professional PDF reports for your Monthly Passbook or All Time Passbook and share them instantly via WhatsApp or other apps.",
    "htu_language_title": "10. Language",
    "htu_language_desc": "Switch between English and Hindi languages for a fully localized user interface.",
    "htu_settings_title": "11. Settings",
    "htu_settings_desc": "Manage your app preferences, contact the developer, read the Privacy Policy and Terms & Conditions, and explore more help topics.",

    "privacy_intro": "This Privacy Policy explains how WorkDaily collects, uses, and protects your information.",
    "privacy_local_data_title": "1. Local Data Storage",
    "privacy_local_data_desc": "WorkDaily is designed as an offline-first application. All your attendance records, earnings data, notes, and user profile information are securely stored locally on your device via Room database and DataStore preferences. We do not transmit your local data to any external server.",
    "privacy_user_sharing_title": "2. User-Initiated Sharing",
    "privacy_user_sharing_desc": "Your data only leaves your device when you explicitly choose to share it. For instance, when you create and share a backup file, or export and share your Passbook via PDF.",
    "privacy_third_party_title": "3. Third-Party Apps",
    "privacy_third_party_desc": "When you choose to share your backups or PDFs using third-party apps like WhatsApp, Email, or File Managers, those apps are governed by their own privacy policies.",
    "privacy_contact_title": "4. Contact Us",
    "privacy_contact_desc": "If you have questions about this Privacy Policy, please contact SynFusion at workdaily.synfusion@gmail.com.",

    "terms_acceptance_title": "1. Acceptance of Terms",
    "terms_acceptance_desc": "By accessing and using WorkDaily, you accept and agree to be bound by the terms and provisions of this agreement.",
    "terms_use_title": "2. Use of WorkDaily",
    "terms_use_desc": "WorkDaily provides tools for tracking offline attendance and earnings. It is intended for personal, offline use.",
    "terms_responsibilities_title": "3. User Responsibilities",
    "terms_responsibilities_desc": "You are fully responsible for the accuracy of the attendance records, advance entries, and wage configurations you enter into the application.",
    "terms_financial_title": "4. Earnings and Calculation Disclaimer",
    "terms_financial_desc": "WorkDaily calculations are provided for convenience based on user-entered values. They should not be considered guaranteed accounting or payroll results. The user remains responsible for verifying final amounts.",
    "terms_backup_title": "5. Backup & Restore Responsibility",
    "terms_backup_desc": "Since all data is stored offline, SynFusion is not responsible for data loss due to device failure, app deletion, or user error. You are responsible for routinely creating and securing your backup files.",
    "terms_branding_title": "6. Intellectual Property & Branding",
    "terms_branding_desc": "WorkDaily is a product developed by SynFusion. The WorkDaily branding and all associated assets remain the intellectual property of SynFusion.",
    "terms_contact_title": "7. Contact Information",
    "terms_contact_desc": "For support or inquiries regarding these Terms & Conditions, contact SynFusion at workdaily.synfusion@gmail.com."
}

hindi_strings = {
    "htu_getting_started_title": "1. शुरू करना",
    "htu_getting_started_desc": "एप्लिकेशन खोलें, अपना प्रारंभिक सेटअप पूरा करें, अपनी उपयोगकर्ता प्रोफ़ाइल कॉन्फ़िगर करें, और अपनी दैनिक मजदूरी और काम के घंटे जैसी बुनियादी प्राथमिकताएँ सेट करें।",
    "htu_attendance_title": "2. उपस्थिति (Attendance)",
    "htu_attendance_desc": "अपनी उपस्थिति को प्रतिदिन उपस्थित (Present), आधा दिन (Half Day), या अनुपस्थित (Absent) के रूप में दर्ज करें। आप अपने दिनांक-आधारित उपस्थिति रिकॉर्ड आसानी से देख और संपादित कर सकते हैं।",
    "htu_earnings_title": "3. कमाई (Earnings)",
    "htu_earnings_desc": "यह आपकी दैनिक मजदूरी कॉन्फ़िगरेशन, मूल वेतन, ओवरटाइम घंटे, अग्रिम (advances) और समग्र उपस्थिति के आधार पर आपकी कमाई की गणना करता है।",
    "htu_calendar_title": "4. कैलेंडर (Calendar)",
    "htu_calendar_desc": "विशिष्ट तिथियों का चयन करने और रंग-कोडित संकेतकों के साथ अपने उपस्थिति रिकॉर्ड देखने के लिए कैलेंडर खोलें।",
    "htu_passbook_title": "5. पासबुक (Passbook)",
    "htu_passbook_desc": "कमाई, ओवरटाइम, अग्रिम राशि और व्यक्तिगत नोट्स सहित अपने दैनिक रिकॉर्ड की समीक्षा करें। आप खोज सकते हैं, सॉर्ट कर सकते हैं और अपना पूरा मासिक सारांश देख सकते हैं।",
    "htu_all_time_passbook_title": "6. ऑल टाइम पासबुक (All Time Passbook)",
    "htu_all_time_passbook_desc": "आपके पूर्ण ऐतिहासिक रिकॉर्ड वर्ष और माह के अनुसार व्यवस्थित। विस्तृत उपस्थिति और वित्तीय सारांश देखने के लिए मासिक ब्लॉक का विस्तार करें या सिकोड़ें।",
    "htu_statistics_title": "7. सांख्यिकी (Statistics)",
    "htu_statistics_desc": "अपनी समग्र कमाई और उपस्थिति के आँकड़े देखें। अपने वित्तीय सारांशों में गहराई से जाएँ और विशिष्ट रिकॉर्ड की जाँच करें।",
    "htu_backup_restore_title": "8. बैकअप और रिस्टोर",
    "htu_backup_restore_desc": "बैकअप बनाकर अपने डेटा को सुरक्षित रखें। आप इन फ़ाइलों को साझा या सहेज सकते हैं, और अपने रिकॉर्ड को सुरक्षित रूप से पुनर्प्राप्त करने के लिए उन्हें बाद में पुनर्स्थापित कर सकते हैं।",
    "htu_pdf_export_title": "9. पीडीएफ एक्सपोर्ट (PDF Export)",
    "htu_pdf_export_desc": "अपनी मासिक पासबुक या ऑल टाइम पासबुक के लिए पेशेवर पीडीएफ रिपोर्ट तैयार करें और उन्हें तुरंत व्हाट्सएप या अन्य ऐप के माध्यम से साझा करें।",
    "htu_language_title": "10. भाषा (Language)",
    "htu_language_desc": "पूरी तरह से स्थानीयकृत यूज़र इंटरफ़ेस के लिए अंग्रेजी और हिंदी भाषाओं के बीच स्विच करें।",
    "htu_settings_title": "11. सेटिंग्स (Settings)",
    "htu_settings_desc": "अपनी ऐप प्राथमिकताओं को प्रबंधित करें, डेवलपर से संपर्क करें, गोपनीयता नीति (Privacy Policy) और नियम व शर्तें (Terms & Conditions) पढ़ें, और अधिक सहायता विषय एक्सप्लोर करें।",

    "privacy_intro": "यह गोपनीयता नीति बताती है कि WorkDaily आपकी जानकारी कैसे एकत्र, उपयोग और सुरक्षित करता है।",
    "privacy_local_data_title": "1. स्थानीय डेटा स्टोरेज (Local Data Storage)",
    "privacy_local_data_desc": "WorkDaily को एक ऑफ़लाइन-प्रथम एप्लिकेशन के रूप में डिज़ाइन किया गया है। आपके सभी उपस्थिति रिकॉर्ड, कमाई का डेटा, नोट्स और उपयोगकर्ता प्रोफ़ाइल जानकारी आपके डिवाइस पर रूम डेटाबेस और डेटास्टोर प्राथमिकताओं के माध्यम से सुरक्षित रूप से संग्रहीत की जाती है। हम आपके स्थानीय डेटा को किसी बाहरी सर्वर पर प्रसारित नहीं करते हैं।",
    "privacy_user_sharing_title": "2. उपयोगकर्ता द्वारा साझाकरण (User-Initiated Sharing)",
    "privacy_user_sharing_desc": "आपका डेटा आपके डिवाइस से तभी बाहर जाता है जब आप स्पष्ट रूप से इसे साझा करना चुनते हैं। उदाहरण के लिए, जब आप कोई बैकअप फ़ाइल बनाते और साझा करते हैं, या पीडीएफ के माध्यम से अपनी पासबुक एक्सपोर्ट और साझा करते हैं।",
    "privacy_third_party_title": "3. थर्ड-पार्टी ऐप्स (Third-Party Apps)",
    "privacy_third_party_desc": "जब आप व्हाट्सएप, ईमेल या फ़ाइल मैनेजर जैसे थर्ड-पार्टी ऐप का उपयोग करके अपने बैकअप या पीडीएफ साझा करना चुनते हैं, तो वे ऐप अपनी गोपनीयता नीतियों द्वारा शासित होते हैं।",
    "privacy_contact_title": "4. हमसे संपर्क करें",
    "privacy_contact_desc": "यदि इस गोपनीयता नीति के बारे में आपके कोई प्रश्न हैं, तो कृपया workdaily.synfusion@gmail.com पर SynFusion से संपर्क करें।",

    "terms_acceptance_title": "1. शर्तों की स्वीकृति (Acceptance of Terms)",
    "terms_acceptance_desc": "WorkDaily तक पहुँच कर और उसका उपयोग करके, आप इस समझौते के नियमों और प्रावधानों से बंधे होने के लिए सहमत हैं।",
    "terms_use_title": "2. WorkDaily का उपयोग (Use of WorkDaily)",
    "terms_use_desc": "WorkDaily ऑफ़लाइन उपस्थिति और कमाई पर नज़र रखने के लिए उपकरण प्रदान करता है। यह व्यक्तिगत, ऑफ़लाइन उपयोग के लिए है।",
    "terms_responsibilities_title": "3. उपयोगकर्ता की जिम्मेदारियाँ (User Responsibilities)",
    "terms_responsibilities_desc": "एप्लिकेशन में आपके द्वारा दर्ज किए गए उपस्थिति रिकॉर्ड, अग्रिम प्रविष्टियों और मजदूरी कॉन्फ़िगरेशन की सटीकता के लिए आप पूरी तरह से जिम्मेदार हैं।",
    "terms_financial_title": "4. कमाई और गणना अस्वीकरण (Earnings and Calculation Disclaimer)",
    "terms_financial_desc": "WorkDaily की गणना उपयोगकर्ता द्वारा दर्ज किए गए मानों के आधार पर सुविधा के लिए प्रदान की जाती है। इन्हें गारंटीकृत लेखांकन या पेरोल परिणाम नहीं माना जाना चाहिए। अंतिम राशि को सत्यापित करने के लिए उपयोगकर्ता स्वयं जिम्मेदार रहता है।",
    "terms_backup_title": "5. बैकअप और रिस्टोर की जिम्मेदारी",
    "terms_backup_desc": "चूंकि सभी डेटा ऑफ़लाइन संग्रहीत होता है, इसलिए डिवाइस की विफलता, ऐप हटाने या उपयोगकर्ता की त्रुटि के कारण डेटा हानि के लिए SynFusion जिम्मेदार नहीं है। आप नियमित रूप से अपनी बैकअप फ़ाइलें बनाने और सुरक्षित करने के लिए जिम्मेदार हैं।",
    "terms_branding_title": "6. बौद्धिक संपदा और ब्रांडिंग (Intellectual Property & Branding)",
    "terms_branding_desc": "WorkDaily SynFusion द्वारा विकसित एक उत्पाद है। WorkDaily ब्रांडिंग और उससे जुड़ी सभी संपत्तियां SynFusion की बौद्धिक संपदा हैं।",
    "terms_contact_title": "7. संपर्क जानकारी (Contact Information)",
    "terms_contact_desc": "इन नियमों और शर्तों के संबंध में समर्थन या पूछताछ के लिए, workdaily.synfusion@gmail.com पर SynFusion से संपर्क करें।"
}

add_strings('./android-offline-attendance-app/app/src/main/res/values/strings.xml', english_strings)
add_strings('./android-offline-attendance-app/app/src/main/res/values-hi/strings.xml', hindi_strings)
