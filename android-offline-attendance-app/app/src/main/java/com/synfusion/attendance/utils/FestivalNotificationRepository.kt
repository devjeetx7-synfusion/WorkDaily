package com.synfusion.attendance.utils

import android.content.Context
import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

data class FestivalNotificationTemplate(
    val id: String,
    val title: String,
    val body: String,
    val isUpcoming: Boolean = false
)

object FestivalNotificationRepository {

    private const val TAG = "WorkDailyFestivalNotif"
    private const val PREF_HISTORY = "festival_notification_history"

    fun normalizeFestivalId(rawId: String): String {
        return rawId.lowercase().replace(Regex("_[0-9]{4}$"), "")
    }

    private val festivalTemplates: Map<String, List<FestivalNotificationTemplate>> = mapOf(
        // ==========================================
        // 1. DIWALI (🪔 ✨ 🌟 🏮 🎇)
        // ==========================================
        "diwali" to listOf(
            // Upcoming
            FestivalNotificationTemplate("diwali_up_1", "🪔✨ Diwali Aa Rahi Hai!", "Diwali ki roshni aur tayyariyan shuru! WorkDaily ki taraf se advance Happy Diwali ✨", true),
            FestivalNotificationTemplate("diwali_up_2", "🌟🪔 Roshni Ka Tyohar Kal!", "Kal Diwali hai bhai! Ghar me roshni aur dil me khushiyan bhar lo 🎇", true),
            FestivalNotificationTemplate("diwali_up_3", "🏮✨ Festive Vibes Ahead!", "Diwali vibes ON! Tayyar ho jao kal ki grand celebration ke liye 🪔", true),

            // Festival Day
            FestivalNotificationTemplate("diwali_day_1", "🪔 Happy Diwali!", "Roshni, khushiyan aur positive vibes — WorkDaily ki taraf se Diwali ki hardik shubhkamnayein ✨"),
            FestivalNotificationTemplate("diwali_day_2", "🪔✨ Diwali Ki Roshni!", "Diwali ki roshni WorkDaily tak bhi aa gayi! Happy Diwali! 🎇"),
            FestivalNotificationTemplate("diwali_day_3", "✨🪔 Extra Bright Day!", "Aaj ka din thoda extra bright hai {userName}! Happy Diwali 🌟"),
            FestivalNotificationTemplate("diwali_day_4", "🏮🪔 Diwali Vibes ON!", "Diwali vibes ON! Roshni, khushiyan aur thoda WorkDaily magic ✨ Happy Diwali!"),
            FestivalNotificationTemplate("diwali_day_5", "🎇✨ Diwali Mubarak!", "Diwali Mubarak! Aaj WorkDaily bhi celebration mode mein hai 🪔"),
            FestivalNotificationTemplate("diwali_day_6", "🪔 Roshni Baanto!", "Roshni baanto, khushiyan manao aur din ko special banao! ✨ Happy Diwali {userName}!"),
            FestivalNotificationTemplate("diwali_day_7", "🌟🪔 Diya & Delight!", "Aaj ghar aur dil dono me roshni bhar do! Happy Diwali ✨"),
            FestivalNotificationTemplate("diwali_day_8", "🪔 Subh Diwali!", "Diye ki roshni aapke jeevan me nayi umang laaye {userName}! 🎇"),
            FestivalNotificationTemplate("diwali_day_9", "✨🪔 Festive Joy!", "Festive look ke saath aaj khushiyan bhi manao! Happy Diwali 🪔"),
            FestivalNotificationTemplate("diwali_day_10", "🪔 Lakshmi Pujan Vibes", "Dhan aur samriddhi aapke ghar aaye! Happy Diwali ✨"),
            FestivalNotificationTemplate("diwali_day_11", "🎇🪔 Sweets & Lights!", "Mithaai khao, diye jalao aur Diwali enjoy karo {userName}! 🌟"),
            FestivalNotificationTemplate("diwali_day_12", "🪔✨ Roshni Ka Parv", "Aaj har kona roshan hai! Happy Diwali WorkDaily family 🏮"),
            FestivalNotificationTemplate("diwali_day_13", "🌟 Bright Diwali Wishes", "Chamakte diye aur muskurate chehre! Happy Diwali {userName} 🪔"),
            FestivalNotificationTemplate("diwali_day_14", "🪔 Positive Energy", "Iss Diwali saare purane tension bhool ke celebrate karo ✨"),
            FestivalNotificationTemplate("diwali_day_15", "🏮🎇 Dher Sari Shubhkamnayein", "Aapko aur aapke pariwar ko Diwali ki dher sari shubhkamnayein 🪔")
        ),

        // ==========================================
        // 2. CHHATH PUJA (🌅 ☀️ OCEAN/RIVER: 🌊 🙏 ✨) - NO FIREWORKS
        // ==========================================
        "chhath_puja" to listOf(
            // Upcoming
            FestivalNotificationTemplate("chhath_up_1", "🌅✨ Chhath Puja Aagman", "Chhath Maiya ki bhakti aur arghya ka pavitra parv aa gaya hai. 🙏✨", true),
            FestivalNotificationTemplate("chhath_up_2", "☀️🌊 Chhath Parv Ki Tayyari", "Pavitra ghat, thekua aur arghya ki tayyariyaan shuru! Subh Chhath Puja ✨", true),
            FestivalNotificationTemplate("chhath_up_3", "🌅🙏 Pavitra Parv Kal!", "Kal Chhath Puja hai! Suraj Dev aur Chhath Maiya ka dhyan karein 🙏", true),

            // Festival Day
            FestivalNotificationTemplate("chhath_day_1", "🌅 शुभ Chhath Puja", "Suraj Dev ki roshni aur Chhath Maiya ka aashirwad aapke din ko khushiyon se bhar de 🙏✨"),
            FestivalNotificationTemplate("chhath_day_2", "🌅🙏 Pavitra Chhath Puja", "Chhath Puja ki pavitra roshni aapke din ko khushiyon se bhar de. शुभ Chhath! 🙏✨"),
            FestivalNotificationTemplate("chhath_day_3", "☀️🌊 Arghya & Aastha", "Arghya, aastha aur roshni — Chhath Puja ki hardik shubhkamnayein 🙏"),
            FestivalNotificationTemplate("chhath_day_4", "🙏🌅 Morning Chhath Vibes", "Aaj WorkDaily bhi Chhath ki subah wali positive vibes ke saath! शुभ Chhath Puja ✨"),
            FestivalNotificationTemplate("chhath_day_5", "🌅✨ Suraj Dev Ki Kripa", "Suraj Dev aur Chhath Maiya ka aashirwad hamesha aapke saath rahe {userName} 🙏"),
            FestivalNotificationTemplate("chhath_day_6", "🌊🌅 River Ghat Blessings", "Pavitra jal, arghya aur bhakti ka pyara din! Happy Chhath Puja ☀️"),
            FestivalNotificationTemplate("chhath_day_7", "🌅🙏 Chhath Parv Shubhkamnayein", "Shuddhata, aastha aur bhakti ka yeh maha-parv aapke jeevan me sukh laaye ✨"),
            FestivalNotificationTemplate("chhath_day_8", "☀️🌊 Surya Arghya Sandhya", "Sandhya Arghya aur Usha Arghya ka pavitra samay! Happy Chhath Puja 🙏"),
            FestivalNotificationTemplate("chhath_day_9", "🌅✨ Sacred Chhath Morning", "Subah ki pehli kiran ke saath Chhath Maiya ki kripa aap par bane rahe ☀️"),
            FestivalNotificationTemplate("chhath_day_10", "🙏🌅 Bhakti & Peace", "Aastha se bhara har pal! Aapko Chhath Puja ki shubhkamnayein {userName} ✨"),
            FestivalNotificationTemplate("chhath_day_11", "🌊☀️ Thekua & Arghya Prasad", "Pavitra prasad aur bhakti ka yeh sundar din! Happy Chhath Puja 🙏"),
            FestivalNotificationTemplate("chhath_day_12", "🌅🙏 Chhath Maiya Kripa", "Sabhi sankat door hon aur sukh-samriddhi aaye! Happy Chhath Puja ✨"),
            FestivalNotificationTemplate("chhath_day_13", "☀️🌅 Sunrise Blessings", "Suraj Dev ki kiranon se aapka din roshan ho {userName} 🙏"),
            FestivalNotificationTemplate("chhath_day_14", "🌊✨ Positive Chhath Energy", "Pavitra man aur shuddh bhav ke saath Chhath Puja ki hardik shubhkamnayein!"),
            FestivalNotificationTemplate("chhath_day_15", "🌅🙏 Subh Chhath Parv", "Chhath Maiya aapko swasth aur khush rakhein {userName} ✨")
        ),

        // ==========================================
        // 3. HOLI (🌈 🎨 🥳 💥 😉 ✨)
        // ==========================================
        "holi" to listOf(
            // Upcoming
            FestivalNotificationTemplate("holi_up_1", "🌈✨ Holi Aa Rahi Hai!", "Holi aa rahi hai! Ready for some colors {userName}? 🎨", true),
            FestivalNotificationTemplate("holi_up_2", "🎨💥 Rang Tayyar Rakho!", "Kal Holi hai bhai! Color aur pichkari nikallo 🌈", true),
            FestivalNotificationTemplate("holi_up_3", "🌈🥳 Color Mode Tomorrow!", "Kal WorkDaily me bhi rangon ki fuhar hone wali hai! Happy Holi in advance ✨", true),

            // Festival Day
            FestivalNotificationTemplate("holi_day_1", "🌈 Happy Holi!", "Rang ready hain? Happy Holi! Aaj WorkDaily mein bhi thoda color add ho gaya 😄"),
            FestivalNotificationTemplate("holi_day_2", "🎨💥 Holi Hai Bhai!", "Holi hai bhai! 🌈 Aaj ka din colorful rakho, baaki kaam WorkDaily sambhal lega 😉"),
            FestivalNotificationTemplate("holi_day_3", "🌈😄 Masti & Colors", "Thoda rang, thodi masti aur full positive vibes! Happy Holi 🎨✨"),
            FestivalNotificationTemplate("holi_day_4", "🎨🌈 Attendance Of Colors", "Aaj attendance se pehle colors ki attendance laga lo 😄 Happy Holi!"),
            FestivalNotificationTemplate("holi_day_5", "🥳🎨 Rangon Ka Tyohar", "Gulal aur pichkari taiyaar hai {userName}? Happy Holi 🌈!"),
            FestivalNotificationTemplate("holi_day_6", "🌈✨ Bura Na Mano Holi Hai", "Bura na mano Holi hai! Aaj din bhar khushiyon ke rang udao 😄"),
            FestivalNotificationTemplate("holi_day_7", "🎨😉 Gujhia & Gulal", "Rang laga lo, gujhia khao aur masti karo {userName}! Happy Holi 🌈"),
            FestivalNotificationTemplate("holi_day_8", "🌈🎨 Full Color Mode", "Aaj ki life ko ekdum colorful bana lo! Happy Holi 🥳"),
            FestivalNotificationTemplate("holi_day_9", "🎨💥 Happy Holi {userName}!", "Rangon ki fuhaar aur doston ka pyar! Happy Holi 🌈"),
            FestivalNotificationTemplate("holi_day_10", "🌈🥳 Festive Color Blast", "Aaj har gham bhool ke rangon me kho jao! Happy Holi ✨"),
            FestivalNotificationTemplate("holi_day_11", "🎨🌈 Herbal Color Fun", "Herbal rangon ke saath safe aur energetic Holi manao {userName}!"),
            FestivalNotificationTemplate("holi_day_12", "🥳🎨 Sweets & Joy", "Meethi gujhia aur gulal ka rang! Happy Holi 🌈"),
            FestivalNotificationTemplate("holi_day_13", "🌈😉 Color Day!", "Aaj koi tension nahi, bas rang aur dosti! Happy Holi 🎨"),
            FestivalNotificationTemplate("holi_day_14", "🎨✨ Dosti Ka Rang", "Pyar aur dosti ke rang har jagah failao! Happy Holi {userName} 🌈"),
            FestivalNotificationTemplate("holi_day_15", "🌈🥳 Chill & Celebrate", "Aaj kaam ki chinta nahi, bas Holi celebrate karo! Happy Holi 🎨")
        ),

        // ==========================================
        // 4. TEACHERS' DAY (🎓 📚 ✏️ 🙏 ✨)
        // ==========================================
        "teachers_day" to listOf(
            FestivalNotificationTemplate("teachers_up_1", "🎓📚 Teachers' Day Tomorrow", "Kal Teachers' Day hai! Apne mentors aur gurus ko dhanyawad bolne ka din 🙏", true),
            FestivalNotificationTemplate("teachers_day_1", "🎓 Happy Teachers' Day!", "Happy Teachers' Day! Jo sikhaya, wahi aage kaam aata hai. 🙏✨"),
            FestivalNotificationTemplate("teachers_day_2", "📚✨ Respect & Gratitude", "Aaj un sab teachers ko thank you bolne ka din hai jinhone humein better banaya. Happy Teachers' Day 🎓"),
            FestivalNotificationTemplate("teachers_day_3", "🎓❤️ Strong Basics!", "Teachers ne basics strong kiye, ab WorkDaily mein daily progress strong karo 😄"),
            FestivalNotificationTemplate("teachers_day_4", "✏️📚 Guru Pranam", "Guru bina gyan nahi! Aaj apne mentors ko yaad karne ka din hai 🙏✨"),
            FestivalNotificationTemplate("teachers_day_5", "🎓✨ Guidance & Mentorship", "Aapke मार्गदर्शन ne humein sahi raasta dikhaya. Happy Teachers' Day 📚"),
            FestivalNotificationTemplate("teachers_day_6", "📚🎓 Value Of Education", "Har sikhaane wale guru ko WorkDaily ka pranam 🙏 Happy Teachers' Day!"),
            FestivalNotificationTemplate("teachers_day_7", "✏️✨ Respect To Educators", "Sikhaana ek kala hai! Aaj sabhi teachers ko pranam 🎓"),
            FestivalNotificationTemplate("teachers_day_8", "🎓📚 Wisdom & Growth", "Sahi direction dikhane wale teachers ko dil se dhanyawad 🙏✨"),
            FestivalNotificationTemplate("teachers_day_9", "📚❤️ Lessons For Life", "Zindagi me har teacher ne kuch achha sikhaya hai {userName}! Respect 🙏"),
            FestivalNotificationTemplate("teachers_day_10", "🎓✨ Salute To Teachers", "Shiksha hi aage badhne ka raasta hai. Happy Teachers' Day! 📚")
        ),

        // ==========================================
        // 5. EID-UL-FITR (🌙 ✨ 🤲 🤍)
        // ==========================================
        "eid_ul_fitr" to listOf(
            FestivalNotificationTemplate("eid_up_1", "🌙✨ Chand Ka Intezaar", "Eid ka pyara tyohar aa raha hai! Advance Eid Mubarak ✨", true),
            FestivalNotificationTemplate("eid_day_1", "🌙✨ Eid Mubarak!", "Eid Mubarak! Khushiyan, sukoon aur barkat aapke ghar hamesha rahe. 🤲"),
            FestivalNotificationTemplate("eid_day_2", "🌙🤍 Eid Ki Chandni", "Eid ki chandni ke saath aapke din mein bhi khushiyon ki roshni aaye. Eid Mubarak ✨"),
            FestivalNotificationTemplate("eid_day_3", "✨🌙 Celebration Mode!", "Chand nazar aa gaya, celebration mode ON! Eid Mubarak {userName} 🤍"),
            FestivalNotificationTemplate("eid_day_4", "🤲🌙 Dua & Blessings", "Iss Eid par aapke aur aapke family ke liye dher sari duaayein 🤲✨ Eid Mubarak!"),
            FestivalNotificationTemplate("eid_day_5", "🌙✨ Sevaiyan & Joy", "Meethi sevaiyan aur meethi baatein! Eid Mubarak WorkDaily family 🤍"),
            FestivalNotificationTemplate("eid_day_6", "🤍🌙 Peace & Harmony", "Sukoon, pyar aur bhaichare ka yeh pyara tyohar! Eid Mubarak ✨"),
            FestivalNotificationTemplate("eid_day_7", "🌙🤲 Eid Mubarak {userName}!", "Aapki har dua qabool ho aur ghar me khushiyan aayein 🌙✨"),
            FestivalNotificationTemplate("eid_day_8", "✨🤍 Divine Eid Light", "Eid ka chand aapke jeevan me roshni laaye! Eid Mubarak 🤲"),
            FestivalNotificationTemplate("eid_day_9", "🌙✨ Festive Mubarakbaad", "Doston aur family ke saath Eid ki khushiyan manao {userName}! 🤍"),
            FestivalNotificationTemplate("eid_day_10", "🤲🌙 Happy Eid-ul-Fitr", "Dil me pyar aur chehre par muskaan! Eid Mubarak ✨"),
            FestivalNotificationTemplate("eid_day_11", "🌙🤍 Warmest Eid Wishes", "Barkat aur kamyabi aapke qadam choomein. Eid Mubarak 🤲"),
            FestivalNotificationTemplate("eid_day_12", "✨🌙 Crescent Blessings", "Chand ki roshni aapke har din ko roshan kare! Eid Mubarak 🤍"),
            FestivalNotificationTemplate("eid_day_13", "🌙🤲 Joyful Celebration", "Aaj gale milo aur khushiyan baanto! Eid Mubarak {userName} ✨"),
            FestivalNotificationTemplate("eid_day_14", "🤍✨ Eid Positive Vibes", "Ek doosre ko duayein do aur khush raho! Eid Mubarak 🌙"),
            FestivalNotificationTemplate("eid_day_15", "🌙🤲 Subh Eid Mubarak", "Aapko aur aapke apnon ko Eid-ul-Fitr ki dher sari mubarakbaad 🤍")
        ),

        // ==========================================
        // 6. MAHA SHIVRATRI (🕉️ 🔱 🌙 🙏 ✨)
        // ==========================================
        "maha_shivratri" to listOf(
            FestivalNotificationTemplate("shivratri_up_1", "🕉️✨ Maha Shivratri Ahead", "Mahadev ki bhakti ka pavitra parv aa raha hai! Om Namah Shivaya 🙏", true),
            FestivalNotificationTemplate("shivratri_day_1", "🕉️✨ Har Har Mahadev!", "Har Har Mahadev! Maha Shivratri ki hardik shubhkamnayein 🙏"),
            FestivalNotificationTemplate("shivratri_day_2", "🔱🌙 Mahadev Kripa", "Mahadev ki kripa aur shanti aapke saath rahe. Happy Maha Shivratri 🙏"),
            FestivalNotificationTemplate("shivratri_day_3", "🌙🔱 Devotion & Energy", "Aaj ka din devotion aur positive energy ke naam. Har Har Mahadev! ✨"),
            FestivalNotificationTemplate("shivratri_day_4", "🕉️🙏 Subh Shivratri", "Shivji ka aashirwad aapke pariwar me shanti laaye. Om Namah Shivaya ✨"),
            FestivalNotificationTemplate("shivratri_day_5", "🔱✨ Bholenath Blessings", "Bholenath aapke sabhi sankat door karein {userName}! Har Har Mahadev 🙏"),
            FestivalNotificationTemplate("shivratri_day_6", "🕉️🌙 Divine Shivratri", "Shiv bhakti me leen ho kar positive vibes feel karo ✨ Om Namah Shivaya!"),
            FestivalNotificationTemplate("shivratri_day_7", "🔱🙏 Spiritual Peace", "Mahadev ki shakti aur shanti aapke dil me rahe. Happy Maha Shivratri 🌙"),
            FestivalNotificationTemplate("shivratri_day_8", "🕉️✨ Om Namah Shivaya", "Har Har Mahadev! Shivratri ki pavitra sandhya par pranam 🙏"),
            FestivalNotificationTemplate("shivratri_day_9", "🌙🔱 Faith & Calm", "Mahadev ke aashirwad se har raasta aasan ho jaye {userName} ✨"),
            FestivalNotificationTemplate("shivratri_day_10", "🕉️🙏 Sacred Bhakti", "Bhakti, shanti aur pavitrata ka parv! Har Har Mahadev 🔱"),
            FestivalNotificationTemplate("shivratri_day_11", "🔱✨ Bhole Ki Kripa", "Aaj Bholenath ke darbar me shanti aur aashirwad mile 🙏"),
            FestivalNotificationTemplate("shivratri_day_12", "🕉️🌙 Shiva Energy", "Aapke har karya me safalta mile. Om Namah Shivaya ✨"),
            FestivalNotificationTemplate("shivratri_day_13", "🔱🙏 Har Har Mahadev!", "Maha Shivratri ke iss pavitra din par sabhi ko pranam 🕉️"),
            FestivalNotificationTemplate("shivratri_day_14", "🌙✨ Spiritual Shivratri", "Shivji ke mantra se din ko energize karo! Om Namah Shivaya 🙏"),
            FestivalNotificationTemplate("shivratri_day_15", "🕉️🔱 Subh Maha Shivratri", "Bholenath ki kripa se aapka jeevan sukhmay ho {userName} ✨")
        ),

        // ==========================================
        // 7. BUDDHA PURNIMA (🪷 ✨ 🌕 🙏)
        // ==========================================
        "buddha_purnima" to listOf(
            FestivalNotificationTemplate("buddha_up_1", "🪷✨ Buddha Purnima Tomorrow", "Kal Buddha Purnima hai! Mindful shanti aur positivity feel karo 🙏", true),
            FestivalNotificationTemplate("buddha_day_1", "🪷✨ Buddha Purnima", "Shanti, positivity aur inner peace — Buddha Purnima ki shubhkamnayein 🙏"),
            FestivalNotificationTemplate("buddha_day_2", "🌕🪷 Mindful Day", "Aaj thoda slow down, breathe aur positive vibes feel karo. Happy Buddha Purnima ✨"),
            FestivalNotificationTemplate("buddha_day_3", "🪷🙏 Peaceful Mind", "Ahimsa, karuna aur shanti ka sandesh! Happy Buddha Purnima {userName} 🌕"),
            FestivalNotificationTemplate("buddha_day_4", "🌕✨ Serene Peace", "Buddha Purnima par aapko aur aapke pariwar ko shanti mile 🙏"),
            FestivalNotificationTemplate("buddha_day_5", "🪷🌕 Calm Within", "Dil me shanti aur chehre par muskaan rakhein. Happy Buddha Purnima ✨"),
            FestivalNotificationTemplate("buddha_day_6", "🙏🪷 Truth & Peace", "Lord Buddha ke vichar aapko sahi marg dikhayein 🌕"),
            FestivalNotificationTemplate("buddha_day_7", "🌕✨ Enlightened Path", "Buddha Purnima wishes! Inner peace aur mindfulness ke saath din beete {userName} 🙏"),
            FestivalNotificationTemplate("buddha_day_8", "🪷🌕 Harmony & Kindness", "Karuna aur daya se bhara din! Happy Buddha Purnima ✨"),
            FestivalNotificationTemplate("buddha_day_9", "🌕🙏 Peace Is Power", "Shanti hi sabse bada dhan hai. Buddha Purnima ki shubhkamnayein 🪷"),
            FestivalNotificationTemplate("buddha_day_10", "🪷✨ Peaceful Mindset", "Aaj thoda shanti se apna kaam karo aur calm raho 🙏")
        ),

        // ==========================================
        // 8. MAKAR SANKRANTI (🪁 ☀️ 🧡 ✨)
        // ==========================================
        "makar_sankranti" to listOf(
            FestivalNotificationTemplate("sankranti_up_1", "🪁☀️ Makar Sankranti Ahead", "Patang aur til-gud ka parv aa gaya! Happy Makar Sankranti in advance 🧡", true),
            FestivalNotificationTemplate("sankranti_day_1", "🪁☀️ Makar Sankranti!", "Patang udao, til-gud khao aur positive vibes failao! Happy Makar Sankranti 🧡"),
            FestivalNotificationTemplate("sankranti_day_2", "☀️🪁 Sky Full Of Kites", "Sky full of kites, heart full of happiness! Happy Makar Sankranti ✨"),
            FestivalNotificationTemplate("sankranti_day_3", "🪁✨ Fly High {userName}!", "Peet ladao, mast raho! Happy Makar Sankranti ☀️"),
            FestivalNotificationTemplate("sankranti_day_4", "🧡🪁 Til-Gud Sweets", "Til-gud ghya, god-god bola! Happy Makar Sankranti WorkDaily family 🪁"),
            FestivalNotificationTemplate("sankranti_day_5", "☀️✨ Surya Dev Blessings", "Uttarayan ki nayi kiran aapke jeevan me khushiyan laaye 🧡"),
            FestivalNotificationTemplate("sankranti_day_6", "🪁☀️ Kite Flying Time", "Pech ladao, masti karo aur til-gud ki mithaai khao! Happy Sankranti ✨"),
            FestivalNotificationTemplate("sankranti_day_7", "🧡✨ Harvest Vibes", "Naye parv ke saath nayi shuruaat {userName}! Happy Makar Sankranti 🪁"),
            FestivalNotificationTemplate("sankranti_day_8", "☀️🪁 Bright Uttarayan", "Dhoop me betho, patang udao aur khush raho! Happy Makar Sankranti 🧡"),
            FestivalNotificationTemplate("sankranti_day_9", "🪁🧡 Colorful Sky", "Sky me rang-birangi patangon ka mela! Happy Makar Sankranti ✨"),
            FestivalNotificationTemplate("sankranti_day_10", "☀️✨ Sun God Energy", "Surya Dev ki aarti aur til-gud ka swad! Happy Makar Sankranti 🪁"),
            FestivalNotificationTemplate("sankranti_day_11", "🪁☀️ Fly To Dreams", "Aage badho aur unchaiyon ko chhuyo {userName}! Happy Sankranti 🧡"),
            FestivalNotificationTemplate("sankranti_day_12", "🧡🪁 Harvest Joy", "Khushiyan aur samriddhi aapke ghar aaye! Happy Makar Sankranti ✨"),
            FestivalNotificationTemplate("sankranti_day_13", "☀️🪁 Sunny Celebrations", "Aasmaan me patang aur dil me khushi! Happy Makar Sankranti 🧡"),
            FestivalNotificationTemplate("sankranti_day_14", "🪁✨ Til-Gud Mithaas", "Meethe til-gud ke saath meethi baatein! Happy Makar Sankranti ☀️"),
            FestivalNotificationTemplate("sankranti_day_15", "🧡☀️ Happy Uttarayan!", "Nayi umang aur nayi energy ke saath aage badho {userName}! 🪁")
        ),

        // ==========================================
        // 9. REPUBLIC DAY (🇮🇳 ✨ ❤️)
        // ==========================================
        "republic_day" to listOf(
            FestivalNotificationTemplate("republic_up_1", "🇮🇳✨ Republic Day Tomorrow", "Kal 26th January hai! Desh ke Samvidhan aur Shaan ko salam 🇮🇳", true),
            FestivalNotificationTemplate("republic_day_1", "🇮🇳✨ Happy Republic Day!", "Happy Republic Day! Proud to be part of this incredible journey. Jai Hind! ❤️"),
            FestivalNotificationTemplate("republic_day_2", "🇮🇳 Daily Progress", "Aaj WorkDaily ke saath ek aur reminder — progress bhi daily honi chahiye. Happy Republic Day! ✨"),
            FestivalNotificationTemplate("republic_day_3", "🇮🇳❤️ Proud Indian", "Samvidhan aur desh ke samman me pranam! Happy Republic Day {userName} ✨"),
            FestivalNotificationTemplate("republic_day_4", "✨🇮🇳 Freedom & Duty", "Apne kartavya aur adhikar dono ka samman karein. Jai Hind! 🇮🇳"),
            FestivalNotificationTemplate("republic_day_5", "🇮🇳✨ Republic Wishes", "Desh ki unnati me sabhi ka yogdan mahatvapurna hai ❤️ Happy Republic Day!"),
            FestivalNotificationTemplate("republic_day_6", "❤️🇮🇳 Unity In Nation", "Ekta aur bhaichare ke saath aage badhein. Jai Hind! ✨"),
            FestivalNotificationTemplate("republic_day_7", "🇮🇳✨ Salute To Constitution", "Aaj hamare samvidhan ki shaan ko pranam! Happy Republic Day 🇮🇳"),
            FestivalNotificationTemplate("republic_day_8", "✨🇮🇳 Progress & Honor", "Mehnat aur imandari se desh ko mazboot banayein {userName} ❤️"),
            FestivalNotificationTemplate("republic_day_9", "🇮🇳❤️ Jai Hind!", "Desh prem aur mehnat hamesha saath rahe! Happy Republic Day ✨"),
            FestivalNotificationTemplate("republic_day_10", "🇮🇳✨ Proud Nation", "Aaj ke din desh ke sabhi nagrikon ko pranam! ❤️")
        ),

        // ==========================================
        // 10. INDEPENDENCE DAY (🇮🇳 ✨ ❤️ 🧡🤍💚)
        // ==========================================
        "independence_day" to listOf(
            FestivalNotificationTemplate("ind_up_1", "🇮🇳✨ 15th August Ahead", "Kal Independence Day hai! Freedom celebrate karne ke liye tayyar ho jao 🇮🇳", true),
            FestivalNotificationTemplate("ind_day_1", "🇮🇳✨ Freedom Vibes!", "Freedom ki feeling hi alag hai! Happy Independence Day! Jai Hind ❤️"),
            FestivalNotificationTemplate("ind_day_2", "🇮🇳🧡🤍💚 Celebrate Azaadi", "Azaadi celebrate karo, dreams chase karo. Happy Independence Day! ✨"),
            FestivalNotificationTemplate("ind_day_3", "🇮🇳✨ Jai Hind {userName}!", "Azaad desh ke azaad nagrik! Happy Independence Day ❤️"),
            FestivalNotificationTemplate("ind_day_4", "❤️🇮🇳 Proud To Be Indian", "Desh ki azaadi aur shaan ko hamesha barkarar rakhein ✨"),
            FestivalNotificationTemplate("ind_day_5", "🇮🇳✨ Independence Wishes", "Hamare veeron ke balidan ko salam! Jai Hind 🇮🇳"),
            FestivalNotificationTemplate("ind_day_6", "🧡🤍💚 Tricolor Pride", "Tiranga lehraaye aur desh aage badhe! Happy Independence Day ✨"),
            FestivalNotificationTemplate("ind_day_7", "🇮🇳❤️ Freedom & Dreams", "Azaad vichar aur mehnat se safalta paayein {userName}! Jai Hind!"),
            FestivalNotificationTemplate("ind_day_8", "✨🇮🇳 Salute To Nation", "Aaj har dil me desh prem ki roshni hai ❤️ Happy 15th August!"),
            FestivalNotificationTemplate("ind_day_9", "🇮🇳🧡 Freedom & Progress", "Mehnat se desh aur apna naam roshan karo! Happy Independence Day ✨"),
            FestivalNotificationTemplate("ind_day_10", "🇮🇳✨ Azaadi Ka Jashn", "Tricolor ki shaan ke saath aage badhein! Jai Hind ❤️")
        ),

        // ==========================================
        // 11. JANMASTHAMI (🦚 🪈 💙 ✨ 🙏)
        // ==========================================
        "janmashtami" to listOf(
            FestivalNotificationTemplate("janmashtami_up_1", "🦚💙 Janmashtami Ahead", "Kanha ke aagman ka pawan parv aa raha hai! Radhe Radhe 🙏", true),
            FestivalNotificationTemplate("janmashtami_day_1", "🦚💙 Radhe Radhe!", "Radhe Radhe! Janmashtami ki hardik shubhkamnayein ✨🙏"),
            FestivalNotificationTemplate("janmashtami_day_2", "🪈💙 Kanha Ki Muskaan", "Kanha ki muskaan aur positivity aapke din ko special bana de. Happy Janmashtami 🙏"),
            FestivalNotificationTemplate("janmashtami_day_3", "✨🦚 Subh Janmashtami", "Natkhat Nandalal aapke ghar khushiyan aur mithaai laayein 💙"),
            FestivalNotificationTemplate("janmashtami_day_4", "🪈✨ Divine Flute Aura", "Bansuri ki dhun aur Krishna ki bhakti! Happy Janmashtami {userName} 🙏"),
            FestivalNotificationTemplate("janmashtami_day_5", "🦚💙 Jai Shree Krishna!", "Makhanchor aapke sabhi dukh door karein! Happy Janmashtami ✨"),
            FestivalNotificationTemplate("janmashtami_day_6", "🙏🪈 Divine Blessings", "Krishna Bhagwan ka aashirwad aap par hamesha rahe 💙"),
            FestivalNotificationTemplate("janmashtami_day_7", "🦚✨ Dahi Handi Masti", "Dahi handi aur energetic celebration! Happy Janmashtami 🙏"),
            FestivalNotificationTemplate("janmashtami_day_8", "💙🪈 Radhe Krishna Joy", "Pyar, shanti aur bhakti se bhara parv! Happy Janmashtami ✨"),
            FestivalNotificationTemplate("janmashtami_day_9", "🦚🙏 Little Kanha Blessings", "Gokul ke Gopal aapke parivar me samriddhi laayein 💙"),
            FestivalNotificationTemplate("janmashtami_day_10", "🪈✨ Krishna Janm Parv", "Aaj Nandalal ke aagman ka pawan din! Happy Janmashtami 🙏"),
            FestivalNotificationTemplate("janmashtami_day_11", "🦚💙 Joyful Krishna Day", "Kanha ke updesh aur bhakti se jeevan roshan ho ✨"),
            FestivalNotificationTemplate("janmashtami_day_12", "🙏🪈 Radhe Radhe {userName}!", "Makhan, misri aur Krishna bhakti! Happy Janmashtami 💙"),
            FestivalNotificationTemplate("janmashtami_day_13", "🦚✨ Divine Grace", "Krishna bhagwan ki kripa se har kaam safal ho! 🙏"),
            FestivalNotificationTemplate("janmashtami_day_14", "💙🪈 Shri Krishna Govind", "Hare Krishna Hare Rama! Subh Janmashtami 🙏"),
            FestivalNotificationTemplate("janmashtami_day_15", "🦚🙏 Festive Janmashtami", "Kanha ki lila aur bhakti ka anand lo {userName} ✨")
        ),

        // ==========================================
        // 12. RAKSHA BANDHAN (🧵 ❤️ ✨)
        // ==========================================
        "raksha_bandhan" to listOf(
            FestivalNotificationTemplate("rakhi_up_1", "🧵❤️ Raksha Bandhan Ahead", "Rakhi ka pyara tyohar aa raha hai! Sibling love celebrate karo ✨", true),
            FestivalNotificationTemplate("rakhi_day_1", "🧵❤️ Raksha Bandhan!", "Rakhi sirf thread nahi, ek beautiful bond hai. Happy Raksha Bandhan! ✨"),
            FestivalNotificationTemplate("rakhi_day_2", "❤️🧵 Sibling Attention", "Aaj siblings ko thoda extra attention dena banta hai 😄 Happy Raksha Bandhan!"),
            FestivalNotificationTemplate("rakhi_day_3", "🧵✨ Thread Of Love", "Bhai-behan ke pyare rishte ko pranam! Happy Raksha Bandhan {userName} ❤️"),
            FestivalNotificationTemplate("rakhi_day_4", "❤️✨ Rakhi & Sweets", "Mithaai khao, gift do aur pyari baatein karo! Happy Raksha Bandhan 🧵"),
            FestivalNotificationTemplate("rakhi_day_5", "🧵❤️ Sibling Bond", "Kitna bhi lad lo, rishta hamesha special rehta hai 😄 Happy Rakhi!"),
            FestivalNotificationTemplate("rakhi_day_6", "✨🧵 Happy Rakhi {userName}!", "Pyar, suraksha aur dher sari khushiyan! Happy Raksha Bandhan ❤️"),
            FestivalNotificationTemplate("rakhi_day_7", "❤️🧵 Sweet Childhood Memories", "Bachpan ki yaadein aur aaj ka pyara bond! Happy Raksha Bandhan ✨"),
            FestivalNotificationTemplate("rakhi_day_8", "🧵✨ Brother Sister Love", "Suraksha ka vada aur rishte ki mithaas! Happy Raksha Bandhan ❤️"),
            FestivalNotificationTemplate("rakhi_day_9", "❤️🧵 Gift Day!", "Aaj gift dene me kanjoosi mat karna bhai 😄 Happy Raksha Bandhan!"),
            FestivalNotificationTemplate("rakhi_day_10", "🧵✨ Rakhi Celebration", "Resham ke dhaage me chupa dher sara pyar ❤️ Happy Rakhi!"),
            FestivalNotificationTemplate("rakhi_day_11", "❤️🧵 Pure Sister Bond", "Behan ki dua aur bhai ka rakshak roop! Happy Raksha Bandhan ✨"),
            FestivalNotificationTemplate("rakhi_day_12", "🧵❤️ Special Day", "Aaj ke din dher sari mithaai aur smiles! Happy Raksha Bandhan {userName}"),
            FestivalNotificationTemplate("rakhi_day_13", "✨🧵 Rakhi Vibe", "Pyara sa rishta, pyari si rakhi! Happy Raksha Bandhan ❤️"),
            FestivalNotificationTemplate("rakhi_day_14", "❤️🧵 Playful Teasing", "Ladai-jhagda side me, aaj sirf pyar! Happy Raksha Bandhan 🧵"),
            FestivalNotificationTemplate("rakhi_day_15", "🧵✨ Subh Raksha Bandhan", "Sabhi bhai-behanon ko Raksha Bandhan ki hardik shubhkamnayein ❤️")
        ),

        // ==========================================
        // 13. GANESH CHATURTHI (🐘 🌺 🙏 ✨)
        // ==========================================
        "ganesh_chaturthi" to listOf(
            FestivalNotificationTemplate("ganesh_up_1", "🐘✨ Bappa Aagman Tomorrow", "Kal Bappa ka swagat karne ka pawan din hai! Ganpati Bappa Morya 🙏", true),
            FestivalNotificationTemplate("ganesh_day_1", "🐘✨ Ganpati Bappa Morya!", "Ganpati Bappa Morya! Aapke din mein happiness aur success bani rahe 🙏"),
            FestivalNotificationTemplate("ganesh_day_2", "🐘🌺 Bappa Aa Gaye!", "Bappa aa gaye! Positive vibes ON. Ganesh Chaturthi ki shubhkamnayein ✨"),
            FestivalNotificationTemplate("ganesh_day_3", "🙏🐘 Modak & Blessings", "Modak ki mithaas aur Bappa ka aashirwad! Happy Ganesh Chaturthi {userName} 🌺"),
            FestivalNotificationTemplate("ganesh_day_4", "✨🐘 Obstacle Remover", "Vighnaharta aapke sabhi vighna door karein! Ganpati Bappa Morya 🙏"),
            FestivalNotificationTemplate("ganesh_day_5", "🌺🐘 Welcome Bappa!", "Ghar-ghar me Bappa ka swagat aur dher sari khushiyan ✨"),
            FestivalNotificationTemplate("ganesh_day_6", "🐘🙏 Subh Ganesh Chaturthi", "Bappa ki kripa se har kaam me safalta mile! Ganpati Bappa Morya 🌺"),
            FestivalNotificationTemplate("ganesh_day_7", "✨🐘 Dhol Tasha Vibe", "Dhol, tasha aur Bappa ki bhakti! Happy Ganesh Chaturthi {userName} 🙏"),
            FestivalNotificationTemplate("ganesh_day_8", "🌺✨ Riddhi Siddhi Blessings", "Samriddhi, buddhi aur gyan ka aashirwad mile! Happy Ganesh Chaturthi 🐘"),
            FestivalNotificationTemplate("ganesh_day_9", "🐘🌺 Lord Ganesha Welcome", "Riddhi-Siddhi ke data aapke ghar padharein 🙏✨"),
            FestivalNotificationTemplate("ganesh_day_10", "🙏🐘 Mangal Murti Morya", "Aaj se nayi energy aur positive vibes start! Happy Ganesh Chaturthi ✨"),
            FestivalNotificationTemplate("ganesh_day_11", "🐘✨ Modak Time", "Bappa ko modak bhog lagao aur khushiyan manao 🌺"),
            FestivalNotificationTemplate("ganesh_day_12", "🌺🐘 Ganpati Morya {userName}!", "Vighnaharta Ganesha aapko hamesha khush rakhein 🙏"),
            FestivalNotificationTemplate("ganesh_day_13", "✨🐘 Divine Arrival", "Bappa ke aagman se sabhi tension door! Happy Ganesh Chaturthi 🌺"),
            FestivalNotificationTemplate("ganesh_day_14", "🐘🙏 Subh Chaturthi", "Siddhivinayak ka aashirwad aapke saath rahe ✨"),
            FestivalNotificationTemplate("ganesh_day_15", "🌺✨ Bappa Ki Jai Ho!", "Sabhi ko Ganesh Chaturthi ki dher sari shubhkamnayein 🐘")
        ),

        // ==========================================
        // 14. ONAM (🌼 🌸 🌾 ✨ 🙏)
        // ==========================================
        "onam" to listOf(
            FestivalNotificationTemplate("onam_up_1", "🌼✨ Onam Celebrations Ahead", "Harvest festival Onam aa gaya hai! Happy Onam in advance 🙏", true),
            FestivalNotificationTemplate("onam_day_1", "🌼✨ Happy Onam!", "Happy Onam! Khushiyan, prosperity aur beautiful vibes aapke ghar aaye 🙏"),
            FestivalNotificationTemplate("onam_day_2", "🌸🌼 Pookalam Color Day", "Pookalam jaisi colorful ho aaj ki day! Happy Onam ✨"),
            FestivalNotificationTemplate("onam_day_3", "🌾🌼 Harvest Prosperity", "King Mahabali's spirit & blessings for peace and prosperity! Happy Onam 🙏"),
            FestivalNotificationTemplate("onam_day_4", "🌼✨ Subh Onam {userName}!", "Phoolon ki khushbu aur Sadya ka swad! Happy Onam 🌸"),
            FestivalNotificationTemplate("onam_day_5", "🌸🌾 Festive Harvest", "Samriddhi aur khushiyon se bhara harvest festival! Happy Onam ✨"),
            FestivalNotificationTemplate("onam_day_6", "🌼🙏 Golden Onam", "Aapke parivar me hamesha sukh aur shanti bani rahe 🌸"),
            FestivalNotificationTemplate("onam_day_7", "🌾✨ Onam Joy", "Tradition, culture aur dher sari happiness! Happy Onam {userName} 🌼"),
            FestivalNotificationTemplate("onam_day_8", "🌸🌼 Onam Sadya Vibe", "Mithaas aur khushiyan baanto! Happy Onam 🙏"),
            FestivalNotificationTemplate("onam_day_9", "🌼🌾 Happy Harvest Festival", "Nayi umang aur nayi safalta ke saath Onam celebrate karo ✨"),
            FestivalNotificationTemplate("onam_day_10", "🌸✨ Pookalam Magic", "Phoolon ke rangon jaisa pyara din beete! Happy Onam 🌼")
        ),

        // ==========================================
        // 15. GANDHI JAYANTI (🕊️ 👓 ✨ 🙏)
        // ==========================================
        "gandhi_jayanti" to listOf(
            FestivalNotificationTemplate("gandhi_day_1", "🕊️✨ Gandhi Jayanti", "Gandhi Jayanti ki hardik shubhkamnayein. Peace aur positivity ke saath aage badhein. 🙏"),
            FestivalNotificationTemplate("gandhi_day_2", "👓🕊️ Truth & Non-Violence", "Satya aur ahimsa ke marg par chalna hi sachha samman hai. Happy Gandhi Jayanti ✨"),
            FestivalNotificationTemplate("gandhi_day_3", "🕊️🙏 Simple & Honest", "Sahi raaste par chalne se hi sachhi khushi milti hai {userName}. Respect 👓"),
            FestivalNotificationTemplate("gandhi_day_4", "✨🕊️ Peace First", "Aaj thoda shanti, daya aur sachhai ke sath din beete 🙏"),
            FestivalNotificationTemplate("gandhi_day_5", "👓✨ Inspiring Thoughts", "Be the change you wish to see in the world. Happy Gandhi Jayanti 🕊️"),
            FestivalNotificationTemplate("gandhi_day_6", "🕊️🙏 Respect To Bapu", "Mahatma Gandhi ke vicharon ko apne jeevan me utarein ✨"),
            FestivalNotificationTemplate("gandhi_day_7", "👓🕊️ Cleanliness & Peace", "Swachhata aur shanti ka pyara sandesh! Happy Gandhi Jayanti 🙏"),
            FestivalNotificationTemplate("gandhi_day_8", "✨🕊️ Ahimsa Power", "Ahimsa ki shakti sabse badi shakti hai. Gandhi Jayanti ki shubhkamnayein 👓"),
            FestivalNotificationTemplate("gandhi_day_9", "🕊️🙏 Truthful Living", "Imandari aur mehnat se hi desh mazboot banta hai {userName} ✨"),
            FestivalNotificationTemplate("gandhi_day_10", "👓✨ Peaceful Work", "Shanti aur mehnat ke saath aage badhte rahein 🙏")
        ),

        // ==========================================
        // 16. DUSSEHRA (🏹 ✨ 🙏 😄)
        // ==========================================
        "dussehra" to listOf(
            FestivalNotificationTemplate("dussehra_up_1", "🏹✨ Vijayadashami Ahead", "Burai par achchai ki jeet ka din aa gaya! Happy Dussehra in advance 🙏", true),
            FestivalNotificationTemplate("dussehra_day_1", "🏹✨ Happy Dussehra!", "Burai par achchai ki jeet — Happy Dussehra! 🙏"),
            FestivalNotificationTemplate("dussehra_day_2", "✨🏹 Positive Energy ON", "Positive vibes rakho, negativity ko bye-bye! Happy Dussehra 😄"),
            FestivalNotificationTemplate("dussehra_day_3", "🏹🙏 Triumph Of Good", "Shri Ram ke aashirwad se sabhi buraiyon ka nash ho! Happy Dussehra ✨"),
            FestivalNotificationTemplate("dussehra_day_4", "😄🏹 Victory Day", "Aaj apne andhar ke saare doubts aur negativity ko khatam karo {userName}! Happy Dussehra ✨"),
            FestivalNotificationTemplate("dussehra_day_5", "🏹✨ Subh Vijayadashami", "Achhai, satya aur dharama ki jeet ka pyara parv! Happy Dussehra 🙏"),
            FestivalNotificationTemplate("dussehra_day_6", "🙏🏹 Goodness Prevaileth", "Hamesha sahi raaste par chalne ki prerna mile! Happy Dussehra ✨"),
            FestivalNotificationTemplate("dussehra_day_7", "🏹😄 Ravan Dahan Vibe", "Ravan Dahan aur positive shuruaat! Happy Dussehra {userName} 🏹"),
            FestivalNotificationTemplate("dussehra_day_8", "✨🙏 Divine Dussehra", "Aapke jeevan me hamesha safalta aur khushi bani rahe 🏹"),
            FestivalNotificationTemplate("dussehra_day_9", "🏹✨ Victory & Prosperity", "Nayi umang aur sachhai ke saath aage badho! Happy Dussehra 🙏"),
            FestivalNotificationTemplate("dussehra_day_10", "😄🏹 Happy Dussehra {userName}!", "Negativity out, positivity in! Vijayadashami ki shubhkamnayein ✨"),
            FestivalNotificationTemplate("dussehra_day_11", "🏹🙏 Shri Ram Blessings", "Bhagwan Ram aapko har kathinai me vijay dein ✨"),
            FestivalNotificationTemplate("dussehra_day_12", "✨🏹 Focus & Victory", "Apne lakshya par focus karo aur jeet hasil karo! Happy Dussehra 😄"),
            FestivalNotificationTemplate("dussehra_day_13", "🏹✨ Jalebi & Joy", "Family ke saath khushiyan manao aur jalebi khao! Happy Dussehra 🙏"),
            FestivalNotificationTemplate("dussehra_day_14", "🙏😄 Truth Wins", "Imandari ki hamesha jeet hoti hai! Happy Dussehra ✨"),
            FestivalNotificationTemplate("dussehra_day_15", "🏹✨ Subh Dussehra", "Aapko aur aapke pariwar ko Vijayadashami ki dher sari shubhkamnayein 🏹")
        ),

        // ==========================================
        // 17. CHILDREN'S DAY (🎈 🧸 😄 ❤️ ✨)
        // ==========================================
        "childrens_day" to listOf(
            FestivalNotificationTemplate("children_day_1", "🎈😄 Childhood Mode ON!", "Aaj thoda childhood mode ON kar lo! Happy Children's Day! ✨"),
            FestivalNotificationTemplate("children_day_2", "🧸🎈 Keep Smiling", "Bachpan ki smile ko kabhi retire mat hone dena {userName} ❤️ Happy Children's Day!"),
            FestivalNotificationTemplate("children_day_3", "🎈✨ Inner Child Joy", "Har dil me ek baccha chupa rehta hai! Happy Children's Day 😄"),
            FestivalNotificationTemplate("children_day_4", "❤️🧸 Nostalgic Smiles", "Bachpan ki wo befikar yaadein aur pyari muskaan! Happy Children's Day 🎈"),
            FestivalNotificationTemplate("children_day_5", "🎈😄 Innocent Joy", "Haste raho, muskuraate raho aur tension free raho! Happy Children's Day ✨"),
            FestivalNotificationTemplate("children_day_6", "🧸✨ Chacha Nehru Remembrance", "Bachon ki pyari muskan hi asli khushi hai ❤️ Happy Children's Day!"),
            FestivalNotificationTemplate("children_day_7", "🎈❤️ Happy Children's Day {userName}!", "Aaj thodi masti aur thoda fun banta hai 😄"),
            FestivalNotificationTemplate("children_day_8", "😄🧸 Smile Free", "Befikar hokar muskurane ka din! Happy Children's Day ✨"),
            FestivalNotificationTemplate("children_day_9", "🎈✨ Colorful Balloons", "Khushiyon ke gubbare udao aur celebrate karo 🎈"),
            FestivalNotificationTemplate("children_day_10", "🧸❤️ Playful Vibes", "Life me thodi innocent joy hamesha barkarar rakhein! Happy Children's Day 😄")
        ),

        // ==========================================
        // 18. GURU NANAK JAYANTI (🪯 ✨ 🙏)
        // ==========================================
        "guru_nanak_jayanti" to listOf(
            FestivalNotificationTemplate("gurpurab_up_1", "🪯✨ Gurpurab Ahead", "Guru Nanak Dev Ji ke Prakash Parv ki tayyariyaan! Subh Gurpurab in advance 🙏", true),
            FestivalNotificationTemplate("gurpurab_day_1", "🪯✨ Happy Gurpurab!", "Guru Nanak Dev Ji ke teachings se peace, kindness aur positivity mile. Happy Gurpurab 🙏"),
            FestivalNotificationTemplate("gurpurab_day_2", "✨🪯 Peace & Kindness", "Peace, kindness aur positivity — Happy Guru Nanak Jayanti 🙏"),
            FestivalNotificationTemplate("gurpurab_day_3", "🪯🙏 Waheguru Blessings", "Waheguru Ji Ka Khalsa, Waheguru Ji Ki Fateh! Subh Gurpurab {userName} ✨"),
            FestivalNotificationTemplate("gurpurab_day_4", "✨🪯 Divine Prakash Parv", "Guru Nanak Dev Ji ki kripa se aapka jeevan roshan ho 🙏"),
            FestivalNotificationTemplate("gurpurab_day_5", "🪯✨ Kirat Karo Naam Japo", "Vand Chhako, Naam Japo, Kirat Karo! Happy Guru Nanak Jayanti 🙏"),
            FestivalNotificationTemplate("gurpurab_day_6", "🙏🪯 Sacred Teachings", "Dil me daya aur chehre par shanti! Happy Guru Nanak Jayanti ✨"),
            FestivalNotificationTemplate("gurpurab_day_7", "🪯✨ Prakash Purab Joy", "Guru Nanak Dev Ji ke Prakash Purab ki dher sari shubhkamnayein 🙏"),
            FestivalNotificationTemplate("gurpurab_day_8", "✨🙏 Waheguru Mehr Karein", "Aapke ghar me sukh, shanti aur barkat rahe {userName} 🪯"),
            FestivalNotificationTemplate("gurpurab_day_9", "🪯✨ Truthful Path", "Sachha marg dikhane wale Satguru ko pranam! Happy Gurpurab 🙏"),
            FestivalNotificationTemplate("gurpurab_day_10", "🙏🪯 Subh Gurpurab Wishes", "Gurpurab ki pavitra sanjhya par sabhi ko prem aur pranam ✨")
        ),

        // ==========================================
        // 19. CHRISTMAS (🎄 🎅 ⭐ ❤️ ✨)
        // ==========================================
        "christmas" to listOf(
            FestivalNotificationTemplate("xmas_up_1", "🎄✨ Christmas Is Coming!", "Christmas vibes are in the air! Get ready for joy and smiles 🎅", true),
            FestivalNotificationTemplate("xmas_day_1", "🎄✨ Merry Christmas!", "Christmas vibes are here! Khushiyan, peace aur lots of smiles. Merry Christmas ❤️"),
            FestivalNotificationTemplate("xmas_day_2", "🎅🎄 Festive Mood!", "Aaj WorkDaily bhi festive mood mein hai! Merry Christmas {userName} ✨"),
            FestivalNotificationTemplate("xmas_day_3", "⭐🎄 Lights & Happiness", "Thodi lights, thodi happiness aur full festive vibes! Merry Christmas ❤️"),
            FestivalNotificationTemplate("xmas_day_4", "🎄🎅 Santa Gifts", "Santa Claus aapke liye dher sari khushiyan aur success laayein! Merry Christmas ✨"),
            FestivalNotificationTemplate("xmas_day_5", "❤️🎄 Warm Joy", "Family aur doston ke saath pyara din beete! Merry Christmas 🎅"),
            FestivalNotificationTemplate("xmas_day_6", "⭐✨ Merry Christmas {userName}!", "Tree sajao, cake khao aur khush raho! Merry Christmas 🎄"),
            FestivalNotificationTemplate("xmas_day_7", "🎄❤️ Peace & Good Cheer", "Shanti aur prem ka pyara tyohar! Merry Christmas WorkDaily family ✨"),
            FestivalNotificationTemplate("xmas_day_8", "🎅⭐ Secret Santa Joy", "Aaj kisi ke chehre par muskaan laayein! Merry Christmas 🎄"),
            FestivalNotificationTemplate("xmas_day_9", "🎄✨ Holiday Spirit", "Festive lights aur joyful spirit! Merry Christmas {userName} ❤️"),
            FestivalNotificationTemplate("xmas_day_10", "🎅🎄 Christmas Magic", "Dil me umang aur ghar me roshni! Merry Christmas ✨"),
            FestivalNotificationTemplate("xmas_day_11", "⭐❤️ Joy & Peace", "Nayi umang ke saath Naye Saal ki taraf badho! Merry Christmas 🎄"),
            FestivalNotificationTemplate("xmas_day_12", "🎄🎅 Plum Cake & Cheerful Beats", "Sweet cake aur sweet baatein! Merry Christmas ✨"),
            FestivalNotificationTemplate("xmas_day_13", "❤️⭐ Christmas Evening", "Shaam ko lights ke saath celebrate karo! Merry Christmas {userName} 🎄"),
            FestivalNotificationTemplate("xmas_day_14", "🎄✨ Warm Christmas Wishes", "Aapko aur aapke apnon ko Christmas ki dher sari badhaiyan 🎅"),
            FestivalNotificationTemplate("xmas_day_15", "🎅❤️ Jolly Merry Christmas", "Ho Ho Ho! Khushiyaan baanto aur safe raho! Merry Christmas 🎄")
        ),

        // ==========================================
        // 20. MAHAVIR JAYANTI (☸️ ✨ 🙏)
        // ==========================================
        "mahavir_jayanti" to listOf(
            FestivalNotificationTemplate("mahavir_up_1", "☸️✨ Mahavir Jayanti Ahead", "Lord Mahavira ke teachings aur ahimsa ka parv aa gaya hai. 🙏", true),
            FestivalNotificationTemplate("mahavir_day_1", "☸️✨ Mahavir Jayanti", "Lord Mahavira ke teachings se ahimsa, satya aur shanti mile. Happy Mahavir Jayanti 🙏"),
            FestivalNotificationTemplate("mahavir_day_2", "🙏☸️ Live And Let Live", "Live and let live — Mahavir Jayanti ki hardik shubhkamnayein ✨"),
            FestivalNotificationTemplate("mahavir_day_3", "☸️✨ Sacred Path", "Ahimsa aur daya ka marg apnayein {userName}. Happy Mahavir Jayanti 🙏"),
            FestivalNotificationTemplate("mahavir_day_4", "✨☸️ Truth & Peace", "Bhagwan Mahavir ka aashirwad aapke jeevan me shanti laaye 🙏"),
            FestivalNotificationTemplate("mahavir_day_5", "☸️🙏 Subh Mahavir Jayanti", "Pavitra vichar aur shuddh man ke saath din beete ✨"),
            FestivalNotificationTemplate("mahavir_day_6", "🙏✨ Compassionate Heart", "Daya aur prem se bhara har pal! Happy Mahavir Jayanti ☸️"),
            FestivalNotificationTemplate("mahavir_day_7", "☸️✨ Mahavir Jayanti Wishes", "Inner peace aur truthfulness ke saath aage badhein {userName} 🙏"),
            FestivalNotificationTemplate("mahavir_day_8", "✨☸️ Ahimsa Paramo Dharma", "Ahimsa Paramo Dharma! Happy Mahavir Jayanti 🙏"),
            FestivalNotificationTemplate("mahavir_day_9", "☸️🙏 Peace Within", "Dil me shanti aur sabhi ke prati prem rakhein ✨"),
            FestivalNotificationTemplate("mahavir_day_10", "🙏☸️ Happy Mahavir Jayanti", "Bhagwan Mahavir ke vichar aapko sahi raasta dikhayein ✨")
        ),

        // ==========================================
        // 21. RAM NAVAMI (🏹 ✨ 🙏)
        // ==========================================
        "ram_navami" to listOf(
            FestivalNotificationTemplate("ramnavami_up_1", "🏹✨ Ram Navami Tomorrow", "Kal Ram Navami hai! Maryada Purushottam Ram Lalla ka janmotsav 🙏", true),
            FestivalNotificationTemplate("ramnavami_day_1", "🏹✨ Jai Shree Ram!", "Ram Navami ki hardik shubhkamnayein 🙏 Bhagwan Ram kripa karein!"),
            FestivalNotificationTemplate("ramnavami_day_2", "🙏🏹 Shri Ram Birth Parv", "Maryada Purushottam Bhagwan Ram ka aashirwad aap par rahe. Happy Ram Navami ✨"),
            FestivalNotificationTemplate("ramnavami_day_3", "🏹✨ Auspicious Ram Navami", "Satya, maryada aur dharama ke marg par chalne ki prerna mile {userName} 🙏"),
            FestivalNotificationTemplate("ramnavami_day_4", "✨🏹 Ram Navami Wishes", "Bhagwan Ram aapke jeevan me sukh aur samriddhi laayein! Jai Shree Ram 🙏"),
            FestivalNotificationTemplate("ramnavami_day_5", "🏹🙏 Divine Ram Lalla", "Aaj Ram Lalla ke aagman ka pawan din! Happy Ram Navami ✨"),
            FestivalNotificationTemplate("ramnavami_day_6", "🙏✨ Subh Ram Navami", "Bhagwan Ram ki kripa se har mushkil aasan ho! Jai Shree Ram 🏹"),
            FestivalNotificationTemplate("ramnavami_day_7", "🏹✨ Righteousness & Virtue", "Imandari aur maryada se bhara jeevan! Happy Ram Navami {userName} 🙏"),
            FestivalNotificationTemplate("ramnavami_day_8", "✨🏹 Ram Rajya Blessings", "Shanti, samriddhi aur khushiyon ka aashirwad! Jai Shree Ram 🙏"),
            FestivalNotificationTemplate("ramnavami_day_9", "🏹🙏 Sacred Pranam", "Maryada Purushottam Ram ke charnon me pranam ✨"),
            FestivalNotificationTemplate("ramnavami_day_10", "🙏🏹 Happy Ram Navami", "Aapko aur aapke pariwar ko Ram Navami ki dher sari shubhkamnayein 🏹")
        )
    )

    fun getTemplatesForFestival(festivalId: String, isUpcoming: Boolean = false): List<FestivalNotificationTemplate> {
        val normId = normalizeFestivalId(festivalId)
        val pool = festivalTemplates[normId] ?: emptyList()
        val filtered = pool.filter { it.isUpcoming == isUpcoming }
        return if (filtered.isNotEmpty()) filtered else pool
    }

    fun selectTemplateForFestival(
        context: Context,
        festivalId: String,
        isUpcoming: Boolean = false,
        placeholders: Map<String, String> = emptyMap()
    ): Pair<FestivalNotificationTemplate, String>? {
        val pool = getTemplatesForFestival(festivalId, isUpcoming)
        if (pool.isEmpty()) {
            Log.w(TAG, "No festival templates found for festivalId: $festivalId, isUpcoming: $isUpcoming")
            return null
        }

        val normId = normalizeFestivalId(festivalId)
        val historyKey = "last_used_festival_${normId}_${if (isUpcoming) "up" else "day"}"

        val prefs = context.getSharedPreferences(PREF_HISTORY, Context.MODE_PRIVATE)
        val lastUsedId = prefs.getString(historyKey, null)

        val available = if (pool.size > 1 && lastUsedId != null) {
            pool.filter { it.id != lastUsedId }
        } else {
            pool
        }

        val selected = available[Random.nextInt(available.size)]

        prefs.edit().putString(historyKey, selected.id).apply()

        val formattedBody = evaluateTemplate(selected.body, placeholders)
        val formattedTitle = evaluateTemplate(selected.title, placeholders)

        return Pair(selected.copy(title = formattedTitle), formattedBody)
    }

    private fun evaluateTemplate(template: String, placeholders: Map<String, String>): String {
        var result = template
        placeholders.forEach { (key, value) ->
            val safeValue = if (value.isBlank() || value == "null") "" else value
            result = result.replace("{$key}", safeValue)
        }

        // Clean unpopulated placeholders safely
        val placeholderRegex = "\\{[a-zA-Z0-9_]+\\}".toRegex()
        result = result.replace(placeholderRegex, "")
            .replace("\\s+".toRegex(), " ")
            .replace("\\s+,".toRegex(), ",")
            .replace("^\\s*,\\s*".toRegex(), "")
            .replace("^,\\s*".toRegex(), "")
            .trim()

        return result
    }

    fun getAllSupportedFestivalIds(): List<String> {
        return festivalTemplates.keys.toList()
    }
}
