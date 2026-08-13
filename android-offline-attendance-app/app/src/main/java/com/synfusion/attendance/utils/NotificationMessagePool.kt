package com.synfusion.attendance.utils

object NotificationMessagePool {

    val dailyAttendanceMessages = listOf(
        "{userName}, आज की attendance अभी तक नहीं लगी है, जल्दी से mark कर लो! 😊",
        "क्या आप आज काम पर गए थे? {userName}, अपनी attendance लगाना मत भूलना!",
        "शाम हो गई है भाई, आज का full day या half day mark कर लो! ⏰",
        "Dost, today\'s attendance is pending. Please check and mark it now!",
        "आज की दिहाड़ी छूट न जाए! {userName}, WorkDaily पर attendance लगाओ! 💰",
        "अरे {userName}! आज का काम दर्ज किया क्या? जल्दी से उपस्थिति दर्ज करें!",
        "समय पर attendance लगाने से record सही रहता है। आज की attendance लगाएं! 👍",
        "दिन खत्म होने वाला है, आज का attendance status update कर लो भाई!",
        "क्या आज छुट्टी पर थे? जो भी हो, present या absent दर्ज करें {userName}!",
        "मजदूरी का हिसाब सही रखने के लिए आज की attendance तुरंत mark करें!",
        "हेलो {userName}, आज काम किया या आराम? अपनी attendance दर्ज करें! 🌟",
        "आज की मेहनत का हिसाब लिख लें। attendance mark करने का समय हो गया है!",
        "एक click में अपनी आज की उपस्थिति दर्ज करें और हिसाब clear रखें! 👉",
        "WorkDaily reminder: {userName}, आज का attendance register खाली है, इसे भरें!",
        "भाई, आज की दिहाड़ी का record update कर लो, बाद में भूल जाओगे! 😉",
        "अपनी रोजाना की उपस्थिति को दर्ज करना आपकी अच्छी आदत है। अभी mark करें!",
        "हिसाब-किताब में गड़बड़ी न हो, इसलिए आज की attendance अभी mark करें {userName}!",
        "आज की मेहनत बेकार न जाए, उपस्थिति और ओवरटाइम दर्ज कर लें! 💪",
        "सुनो भाई! आज की attendance पेंडिंग है। जल्दी से register update करो!",
        "रोज का काम, रोज का हिसाब! {userName}, आज की उपस्थिति अभी mark करें!"
    )

    val missedTwoDaysMessages = listOf(
        "अरे भाई! लगातार {missedDays} दिन से कोई attendance नहीं लगी है। सब ठीक तो है? 🤔",
        "लगातार दो दिन हो गए {userName}, आपने अपनी हाजिरी दर्ज नहीं की!",
        "क्या आप दो दिन से छुट्टी पर हैं? कृपया अपना record update करें! 📅",
        "Two days missed! {userName}, attendance register में अपनी उपस्थिति दर्ज करें!",
        "भाई, 2 दिन का record missing है। जल्दी से calendar में जाकर update कर लो!",
        "क्या बात है भाई, 2 दिन से WorkDaily को भूल गए क्या? attendance लगा लो!",
        "लगातार दो दिन से attendance खाली है। अपना हिसाब सही रखने के लिए अभी mark करें!",
        "Two days consecutive absence? {userName}, check and log your days!",
        "भाई, दो दिन की अनुपस्थिति दर्ज करनी है या उपस्थिति? Register update करो!",
        "हिसाब अधूरा लग रहा है, पिछले 2 दिनों की attendance update करें!",
        "काम पर नहीं गए क्या दो दिन से? {userName}, attendance register आपका इंतजार कर रहा है!",
        "लगातार दो दिनों की दिहाड़ी का हिसाब दर्ज करें, record complete रखें!",
        "Hey {userName}, you missed logging attendance for 2 days. Register it now!",
        "दो दिन से हाजिरी गायब है! कहीं भूल तो नहीं गए? अभी दर्ज करें! 🙌",
        "दो दिन की मेहनत का हिसाब लिख लो भाई, बाद में याद नहीं रहेगा!",
        "दो दिन का gap हो गया है! WorkDaily में पिछले दिनों की attendance update करें!",
        "Attendance update alert: लगातार 2 दिनों से कोई entry नहीं हुई है!"
    )

    val missedThreeDaysMessages = listOf(
        "लगातार 3 दिन से हाजिरी गायब है! {userName}, क्या छुट्टी मना रहे हो? 🏖️",
        "Three days missed in a row! अपना attendance record अभी दुरुस्त करें!",
        "भाई, लगातार 3 दिनों से attendance register खाली पड़ा है। जल्दी भरो!",
        "{userName}, 3 दिन से कोई entry नहीं हुई। कहीं घूमने गए हो क्या? 😄",
        "लगातार 3 दिनों की दिहाड़ी का हिसाब छूट गया है। कृपया record update करें!",
        "Three days without attendance! {userName}, register details inside WorkDaily!",
        "अरे भाई, 3 दिन से attendance नहीं लगी! कहीं register खो तो नहीं गया? 😉",
        "3 days missed! अपनी पुरानी उपस्थिति दर्ज करने के लिए calendar का उपयोग करें!",
        "काम और दाम का हिसाब रखने के लिए लगातार 3 दिनों की attendance दर्ज करें!",
        "Hey {userName}, 3 days gap detected in your work ledger. Fill it today!",
        "लगातार 3 दिन की अनुपस्थिति? या बस attendance दर्ज करना भूल गए? अभी update करें!",
        "तीन दिन से कोई record नहीं मिला। कृपया आज ही अपना details update करें!",
        "दिहाड़ी का हिसाब बिगड़ने न दें, पिछले 3 दिनों की attendance दर्ज करें {userName}!",
        "लगातार 3 दिनों का gap! WorkDaily app खोलें और attendance check करें!",
        "ओए भाई, 3 दिन से attendance लगानी थी, गायब होने का रिकॉर्ड नहीं बनाना था 😂",
        "तीन दिन की मेहनत का ledger update करें, हिसाब सुरक्षित रखें!",
        "Record checklist: पिछले 3 दिनों की attendance पेंडिंग है। अभी दर्ज करें!"
    )

    val missedFourToSixDaysMessages = listOf(
        "भाई, पूरे {missedDays} दिन हो गए और कोई हाजिरी नहीं! कहाँ गायब हो? 🕵️‍♂️",
        "{missedDays} दिनों से attendance register सूना पड़ा है। {userName}, कृपया update करें!",
        "लगभग एक हफ्ता होने को आया, {missedDays} दिनों की attendance पेंडिंग है भाई!",
        "क्या बात है, लगातार {missedDays} दिन का gap? WorkDaily पर अपनी स्थिति दर्ज करें!",
        "Hey {userName}, you missed logging attendance for {missedDays} days. Log them now!",
        "इतनी लंबी छुट्टी? {missedDays} दिन से कोई attendance entry नहीं मिली!",
        "दिहाड़ी और एडवांस का हिसाब बिगड़ने से बचाएं, {missedDays} दिनों का record दर्ज करें!",
        "लगातार {missedDays} दिनों से attendance missing! Calendar खोलें और update करें!",
        "क्या आप छुट्टी पर थे या हाजिरी लगाना भूल गए? {missedDays} दिन का हिसाब बाकी है!",
        "{userName}, {missedDays} days consecutive gap in work diary. Keep ledger updated!",
        "रिकॉर्ड्स की जांच करें: पिछले {missedDays} दिनों से कोई entry नहीं मिली है।",
        "भाई, {missedDays} दिनों की हाजिरी दर्ज कर लो, वरना अंत में हिसाब नहीं मिलेगा! 📉",
        "WorkDaily reminders: You have {missedDays} days of pending attendance. Update now!",
        "क्या आपने {missedDays} दिन से काम नहीं किया? Attendance update करें {userName}!",
        "इतनी छुट्टी क्यों मार रहा है भाई? WorkDaily भी सोच रहा है तू काम पर जाता है या नहीं 😭",
        "भाई attendance लगा दे, कल फिर मत बोलना record missing है 😂",
        "पूरे {missedDays} दिन का ledger खाली है। कृपया अपनी attendance status दर्ज करें!"
    )

    val missedSevenPlusDaysMessages = listOf(
        "सावधान! लगातार {missedDays} दिनों से कोई attendance नहीं लगी है। 🚨",
        "पूरा हफ्ता निकल गया {userName}, पर आपने 7+ दिनों से कोई entry नहीं की!",
        "7+ days missed! क्या आपने नौकरी बदल ली या बस छुट्टी पर हो? 😅",
        "लगातार {missedDays} दिनों से register खाली। अपना पुराना record दुरुस्त करें!",
        "Hey {userName}, a full week of attendance is pending. Check calendar now!",
        "महीने का हिसाब गड़बड़ा जाएगा! पिछले {missedDays} दिनों की attendance दर्ज करें!",
        "लगातार {missedDays} दिनों की दिहाड़ी पेंडिंग! WorkDaily पर अभी status update करें!",
        "Weekly gap detected: {missedDays} days missing records. Open app and update!",
        "भाई, एक हफ्ते से ज्यादा का हिसाब बाकी है। कृपया calendar में जाकर mark करें!",
        "क्या आप बड़ी छुट्टी पर हैं? {userName}, {missedDays} दिनों का attendance update करें!",
        "7+ consecutive days gap! Your professional passbook needs attention.",
        "दिहाड़ी और एडवांस का तालमेल बैठाएं, पिछले {missedDays} दिनों का log update करें!",
        "Please update: {missedDays} days of missing records found in your WorkDaily app!",
        "एक हफ्ते से हाजिरी गायब! कहीं भूल तो नहीं गए? अभी दर्ज करें, {userName}!",
        "भाई, {missedDays} दिन का record missing है, जल्दी से calendar check करो!",
        "लगातार {missedDays} दिनों की मेहनत का हिसाब लिख लें, record complete रखें!",
        "Attendance alert: 7+ days missed. Please confirm your attendance in WorkDaily!"
    )

    val missedTenPlusDaysMessages = listOf(
        "गंभीर चेतावनी: लगातार {missedDays} दिनों से attendance register खाली पड़ा है! ⚠️",
        "पूरे {missedDays} दिन का gap हो गया है {userName}! अपनी attendance check करें।",
        "10+ days without any activity! क्या आप सच में छुट्टी पर थे? 😲",
        "हिसाब-किताब बंद होने की कगार पर है! पिछले {missedDays} दिनों का record दर्ज करें!",
        "Hey {userName}, 10+ consecutive days missing in your personal ledger!",
        "दस दिन से ज्यादा का gap! WorkDaily app खोलें और पुराना record भरें!",
        "भाई, इतने दिनों की हाजिरी नहीं लगाओगे तो payment कैसे बनेगी? 📉",
        "{missedDays} दिनों की attendance missing है। कृपया calendar का उपयोग कर update करें!",
        "क्या आप ठीक हैं? लगातार {missedDays} दिन से कोई entry दर्ज नहीं हुई है।",
        "10+ days missed list: {userName}, please update your daily logs immediately!",
        "दशकों बाद (मजाक में!) याद आया, आपने {missedDays} दिनों से attendance दर्ज नहीं की!",
        "लगातार {missedDays} दिनों की दिहाड़ी और एडवांस का हिसाब अधूरा है, इसे पूरा करें!",
        "WorkDaily reminder: {missedDays} days gap in ledger. Maintain your transparency!",
        "भाई, 10 दिन से ज्यादा हो गए, अपनी attendance status update करो जल्दी!",
        "Record update request: {missedDays} days pending attendance for {userName}!",
        "क्या आपने काम छोड़ दिया? {missedDays} दिनों से कोई entry नहीं मिली है भाई!",
        "attendance register पेंडिंग alert: {missedDays} days missing for {userName}!"
    )

    val longAbsenceMessages = listOf(
        "लंबे समय से कोई गतिविधि नहीं मिली! {userName}, क्या आप वापस काम पर लौट आए हैं? 👋",
        "लंबे अंतराल के बाद वापसी? अपने पुराने रिकॉर्ड दर्ज करें और हिसाब बराबर करें!",
        "Long absence detected! {userName}, please catch up with your pending logs.",
        "काफी दिनों से हाजिरी पेंडिंग है। WorkDaily पर अपना हिसाब दुरुस्त करें!",
        "Comeback time! अपनी पुरानी attendance और दिहाड़ी का विवरण अभी update करें!",
        "लंबे समय से गायब होने के बाद, आज ही अपनी हाजिरी update करें {userName}!",
        "हिसाब-किताब का सिलसिला फिर शुरू करें! अपने सभी पेंडिंग रिकॉर्ड दर्ज करें।",
        "Welcome back! चलिए पिछले छूटे हुए दिनों की attendance update कर लेते हैं! 👍",
        "क्या आपने लंबे समय बाद काम शुरू किया? WorkDaily पर अपनी उपस्थिति दर्ज करें!",
        "लंबे अंतराल का हिसाब दर्ज करें, ताकि आपकी कमाई का लेखा-जोखा सही रहे!",
        "Hey {userName}, let\'s restore your work records. Fill in the pending days now!",
        "लंबे अवकाश के बाद वापसी! अपनी आज की उपस्थिति और दिहाड़ी दर्ज करें!",
        "काम पर वापसी मुबारक हो! चलिए छूटे हुए दिनों का हिसाब calendar में दर्ज करते हैं!",
        "लंबे समय बाद हाजिरी लगाने का समय! अपनी status update करें, {userName}!",
        "काफी छुट्टियां हो गईं भाई, अब काम पर ध्यान दें और attendance update करें! 😄",
        "Long gap resolved? Update your personal ledger with your actual work details!",
        "लंबे समय से रुकी हाजिरी को आज ही पूरा करें। WorkDaily हमेशा आपके साथ है!"
    )

    val weeklyReportMessages = listOf(
        "इस हफ्ते का report तैयार है! 📊 Present: {presentDays}, Half Day: {halfDays}, Net: ₹{netPayable}",
        "Weekly progress report checklist: कमाई: ₹{totalEarnings}, Advance: ₹{advance}!",
        "इस हफ्ते का लेखा-जोखा: {presentDays} दिन काम किया, कुल कमाई ₹{totalEarnings} है! 💪",
        "Weekly performance: {userName}, इस हफ्ते आपने {workingDays} कार्य दिवस पूरे किए!",
        "हफ्ते भर की मेहनत का फल: Net Payable ₹{netPayable}. अपना passbook चेक करें!",
        "Weekly report card: Present: {presentDays}, Absent: {absentDays}, Earnings: ₹{totalEarnings}!",
        "इस सप्ताह का summary: कुल दिहाड़ी ₹{totalEarnings}, जिसमें से ₹{advance} advance है।",
        "Hey {userName}, your weekly work ledger status is ready. Present: {presentDays} days!",
        "हफ़्ता खत्म, हिसाब शुरू! इस हफ्ते का net balance ₹{netPayable} है। Check inside!",
        "Weekly analysis: {attendancePercentage}% attendance rate was achieved this week! 📈",
        "इस हफ्ते की कमाई का summary: Total ₹{totalEarnings} earned, Net Payable ₹{netPayable}!",
        "सप्ताह भर का हिसाब: {workingDays} days worked, details inside WorkDaily!",
        "Weekly update: {userName}, you made ₹{totalEarnings} this week. Tap to view!",
        "इस हफ्ते का पूरा हिसाब तैयार है भाई, अपनी passbook entry check कर लो! 😊",
        "Weekly breakdown: Present: {presentDays}, Half: {halfDays}, Absent: {absentDays}!",
        "हफ्ते भर की कमाई और अग्रिम का हिसाब: Total advance deducted is ₹{advance}.",
        "Weekly performance update: {userName}, your attendance rate was {attendancePercentage}%!"
    )

    val monthlyReportMessages = listOf(
        "महीना खत्म! {userName}, इस महीने की कुल कमाई ₹{totalEarnings} दर्ज की गई है। 💰",
        "Monthly performance report: कुल उपस्थित दिन: {presentDays}, Half Days: {halfDays}!",
        "इस महीने का पूरा हिसाब: Total: ₹{totalEarnings}, Advance: ₹{advance}, Net: ₹{netPayable}!",
        "Monthly ledger checklist: {userName}, check your full monthly passbook now!",
        "इस महीने की मेहनत रंग लाई! कुल कमाई ₹{totalEarnings} हुई है। 🌟",
        "Monthly attendance rate: {attendancePercentage}% this month. Keep up the consistency!",
        "महीने का हिसाब तैयार है: Net Payable ₹{netPayable} after ₹{advance} advance deduction.",
        "Hey {userName}, your monthly work sheet is ready. Total working days: {workingDays}!",
        "महीने भर की दिहाड़ी का हिसाब: Total overtime earned is ₹{overtime}. Tap to view!",
        "Monthly wrap-up: {presentDays} present, {absentDays} absent. Net payout is ₹{netPayable}!",
        "इस महीने का balance sheet: ₹{totalEarnings} total earnings for {userName}!",
        "Monthly overview: You achieved an attendance rate of {attendancePercentage}% this month!",
        "महीने भर की हाजिरी और एडवांस का विवरण: Total advance: ₹{advance}. check ledger!",
        "Hey {userName}, your monthly earnings of ₹{totalEarnings} are securely logged!",
        "इस महीने का digital passbook तैयार है। तुरंत PDF export करें और share करें! 📄",
        "Monthly record summary: {workingDays} days on duty. Net balance to take home is ₹{netPayable}!",
        "महीने भर का हिसाब-किताब: ₹{totalEarnings} earned, check your detailed monthly ledger!"
    )

    val yearlyReportMessages = listOf(
        "सर्वकालिक लेखा-जोखा! इस साल आपने कुल ₹{totalEarnings} की कमाई की है! 👑",
        "Yearly performance report: {userName}, total working days: {workingDays} days!",
        "सालाना रिपोर्ट तैयार है: Present: {presentDays}, Half: {halfDays}, Absent: {absentDays}!",
        "इस साल की कुल बचत और कमाई: Net Payable stands at ₹{netPayable}! Check now!",
        "Yearly summary: Your average attendance rate for the year is {attendancePercentage}%! 📈",
        "पूरे साल की मेहनत का लेखा-जोखा: Total earnings: ₹{totalEarnings}, Advance: ₹{advance}!",
        "Hey {userName}, your all-time passbook has been updated with {workingDays} working days!",
        "वार्षिक प्रदर्शन विश्लेषण: कुल overtime कमाई ₹{overtime} दर्ज की गई है!",
        "सालाना दिहाड़ी का हिसाब: Total gross earnings ₹{totalEarnings} for {userName}!",
        "Yearly ledger details: Present days: {presentDays} days. Logged inside WorkDaily safely!",
        "पूरे साल का हिसाब-किताब चुस्त-दुरुस्त! Net Payable amount is ₹{netPayable}.",
        "Yearly milestone: {userName}, you have completed {workingDays} working days this year!",
        "इस साल की कुल हाजिरी का breakdown: {attendancePercentage}% attendance record! 👍",
        "सालाना अग्रिम कटौती: Total advance recorded this year is ₹{advance}.",
        "All-time report summary: Gross: ₹{totalEarnings}, Net: ₹{netPayable} for {userName}!",
        "इस साल का ऑल-टाइम पीडीएफ पासबुक तैयार है। तुरंत डाउनलोड कर सुरक्षित रखें!",
        "सालाना रिकॉर्ड्स की जांच करें: {presentDays} days present out of total passed days!"
    )

    val earningsMessages = listOf(
        "कमाई का नया अपडेट! {userName}, आपकी आज की कमाई ₹{totalEarnings} दर्ज हुई है। 💵",
        "Earnings alert: इस महीने आपकी कुल कमाई ₹{totalEarnings} तक पहुँच गई है! 💰",
        "ओवरटाइम धमाका! आज आपने {overtime} घंटे overtime किया, अच्छी कमाई हुई! 💪",
        "एडवांस अपडेट: ₹{advance} advance दर्ज किया गया है। शेष देय: ₹{netPayable}।",
        "Net Payable status: आपकी बची हुई शुद्ध राशि ₹{netPayable} है। Check passbook!",
        "{userName}, इस महीने आपने overtime से ₹{overtime} अतिरिक्त कमाए हैं! 👍",
        "हिसाब-किताब अपडेट: कुल कमाई ₹{totalEarnings}, अग्रिम ₹{advance}, शुद्ध देय ₹{netPayable}!",
        "Hey {userName}, new advance entry of ₹{advance} has been securely logged.",
        "दिहाड़ी अपडेट: आज की दिहाड़ी ₹{totalEarnings} आपके ledger में जोड़ दी गई है!",
        "Overtime logging: {overtime} hours of overtime recorded for {userName} today!",
        "कमाई का विवरण: ₹{totalEarnings} total earnings in this cycle. Keep earning!",
        "हिसाब की बात: ₹{advance} advance is deducted from your total wages of ₹{totalEarnings}.",
        "Earnings summary: Today\'s net payout calculation is completed. Tap to view!",
        "Hey {userName}, your overtime of {overtime} hrs has been added to your wages!",
        "एडवांस का हिसाब: ₹{advance} advance recorded today. check your net balance!",
        "Wages updated: Your active daily wage rate is configured at ₹{totalEarnings}/day.",
        "कमाई और ओवरटाइम: इस महीने कुल overtime hours: {overtime} hrs!",
        "Net balance checklist: {userName}, your take-home amount is ₹{netPayable}.",
        "दिहाड़ी और एडवांस का तालमेल: Net Payable update is ₹{netPayable} after deductions.",
        "Wages statement: {userName}, your financial records are 100% accurate!"
    )

    val funnyRoastMessages = listOf(
        "भाई आज की attendance लगा दे, क्या attendance register से रिश्ता तोड़ लिया है? 😂",
        "ओए भाई, attendance लगानी थी, गायब होने का रिकॉर्ड नहीं बनाना था 😂",
        "इतनी छुट्टी क्यों मार रहा है भाई? WorkDaily भी सोच रहा है तू काम पर जाता है या नहीं 😭",
        "भाई attendance लगा दे, कल फिर मत बोलना record missing है 😂",
        "अरे भाई! क्या पैसे कमाना छोड़ दिया? attendance लगाने में इतनी कंजूसी क्यों? 😜",
        "सुनो शहंशाह! आज काम पर गए थे या सिर्फ सोने का इरादा था? attendance लगा लो! 😴",
        "लगता है सरकारी नौकरी मिल गई है भाई को, हाजिरी लगाने का मन ही नहीं करता! 🏛️",
        "ओए! बिना attendance के दिहाड़ी कैसे बनेगी? क्या हवा से पैसे आएंगे? 😂",
        "attendance register रो रहा है भाई, बोल रहा है आज तो चेहरा दिखा दो! 😭",
        "क्या आज भी छुट्टी मना ली? भाई, अमीर बनने का कोई नया तरीका मिला हो तो हमें भी बताओ! 🤑",
        "लगता है आज फिर आलस जीत गया और तुम हार गए! attendance लगा लो भाई!",
        "attendance लगाना भूल जाते हो, खाना खाना भी भूल जाते हो क्या? 🍔😂",
        "भाई, attendance लगाने में सिर्फ 2 सेकंड लगते हैं, 2 घंटे का प्रवचन मत सुनो, अभी लगाओ!",
        "क्या आज भी बिना हाजिरी के दिहाड़ी चाहिए? ऐसी स्कीम सिर्फ सपनों में होती है! 😂",
        "ओए हीरो! आज की attendance पेंडिंग है। जल्दी लगाओ, वरना हिसाब गोल हो जाएगा!",
        "attendance register में आपका नाम ढूंढने के लिए दूरबीन लगानी पड़ रही है! 🔭😂",
        "भाई, अगर ऐसे ही छुट्टी मारते रहे, तो WorkDaily app भी uninstall होना पसंद करेगा! 😉",
        "काम चोरी की भी एक सीमा होती है भाई! पिछले दिनों की attendance कब लगाओगे? 😜",
        "चलो भाई, आलस छोड़ो और attendance लगाओ, खाली बैठ कर जेब नहीं भरती! 💰😂",
        "श्रीमती जी के डर से ही सही, काम का हिसाब तो दर्ज कर लो! attendance लगाओ! 😂"
    )

    val motivationalMessages = listOf(
        "मेहनत का कोई विकल्प नहीं है। आज की attendance दर्ज करें और आगे बढ़ें! 💪",
        "सफलता रोज की छोटी-छोटी कोशिशों का नतीजा है। आज की हाजिरी दर्ज करें!",
        "Keep grinding! {userName}, आपकी हर एक दिहाड़ी आपकी मेहनत का सबूत है! 🌟",
        "अनुशासन ही आपको सफलता की ओर ले जाता है। अपनी रोजाना हाजिरी दर्ज करें!",
        "मेहनत जारी रखें भाई, आपका भविष्य आपके हाथों में है। Attendance mark करें!",
        "Every day counts! {userName}, make today another productive day!",
        "अपनी कमाई का सही हिसाब रखना भी एक जिम्मेदारी है। हाजिरी अभी दर्ज करें!",
        "मेहनत करो, आगे बढ़ो! WorkDaily आपके हर एक कदम को रिकॉर्ड कर रहा है। 👍",
        "आपकी आज की मेहनत कल का सुखद भविष्य बनेगी। उपस्थिति दर्ज करना न भूलें!",
        "Consistency is key! {userName}, keep logging and tracking your success!",
        "आज फिर एक नई शुरुआत! अपनी आज की उपस्थिति दर्ज करें और हिसाब साफ रखें!",
        "मेहनती लोगों के सपने जरूर सच होते हैं। अपनी दिहाड़ी और उपस्थिति को ट्रैक करें!",
        "हिसाब साफ, मन साफ! WorkDaily के साथ अपनी मेहनत का पूरा सम्मान करें! 😊",
        "Success doesn\'t stop! Keep tracking your progress daily with WorkDaily!",
        "अपनी आज की उपलब्धियों को दर्ज करें। हाजिरी लगाना आपकी सफलता का पहला कदम है!",
        "आगे बढ़ते रहो, रुकना मना है! आज की उपस्थिति तुरंत दर्ज करें {userName}!",
        "मेहनत का हिसाब हमेशा सुरक्षित रखें। WorkDaily के साथ कदम से कदम मिलाएं! 🚀"
    )

    val goodMorningMessages = listOf(
        "🌅 Subah ho gayi! Aaj duty pe jaana hai kya? 😄",
        "☀️ Good Morning! Utho bhai, duty tumhara intezaar kar rahi hai 😂",
        "🌞 Morning! Aaj kaam pe ja rahe ho ya aaj chhutti ka plan hai? 😄",
        "☕ Good Morning! Chai ho gayi? Ab duty ka number hai 😜",
        "😴 Utho! Suraj bhi duty pe aa gaya 😂",
        "🌻 Khus raho, happy raho! Ab batao aaj duty jaana hai kya? 😄"
    )

    val goodEveningMessages = listOf(
        "🌆 Good Evening! Kaise ho? Aaj ki duty kaisi rahi? 😄",
        "😄 Kaise {userName}, aaj kaisa raha? Duty se wapas aa gaye?",
        "☕ Good Evening! Duty khatam? Ab thoda aaram bhi kar lo 😌",
        "😂 Aaj duty ne zyada kaam karwaya ya tumne duty se?",
        "🌇 Khus raho, happy raho! Aaj ka din kaisa gaya? ❤️"
    )

    val goodNightMessages = listOf(
        "🌙 Good Night! Aaj ki duty ka hisaab ho gaya? Ab aaram karo 😴",
        "😴 Din khatam, duty khatam… ab phone rakho aur so jao 😂",
        "🌙 Good Night! Kal phir duty se mulaqat hogi 😜",
        "⭐ Khus raho, happy raho. Kal ka din aur achha ho ❤️",
        "😂 Ab bas bhi karo! Duty kal bhi hai, sone ka time ho gaya 😴",
        "🌃 Good Night! Aaj ka attendance lag gaya, ab neend lagao 😂"
    )
}
