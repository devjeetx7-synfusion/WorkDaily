# -*- coding: utf-8 -*-
import base64

def main():
    with open("theme.xml", "r", encoding="utf-8") as f:
        content = f.read()

    # Get logo base64
    with open("android-offline-attendance-app/app/src/main/res/drawable/logo_transparent.png", "rb") as lf:
        logo_b64 = base64.b64encode(lf.read()).decode("utf-8")

    # 1. Update :root variables
    root_old = """    --glass-bg: rgba(255, 255, 255, 0.85);
    --glass-border: rgba(255, 255, 255, 0.6);
  }"""
    root_new = """    --glass-bg: rgba(255, 255, 255, 0.85);
    --glass-border: rgba(255, 255, 255, 0.6);
    --phone-w: 300px;
    --phone-h: 600px;
  }

  @media (max-width: 480px) {
    :root {
      --phone-w: 280px;
      --phone-h: 560px;
    }
  }

  @media (max-width: 360px) {
    :root {
      --phone-w: 250px;
      --phone-h: 500px;
    }
  }"""
    content = content.replace(root_old, root_new)

    # 2. Section spacing and padding
    content = content.replace("  section {\n    padding: 100px 0;", "  section {\n    padding: clamp(40px, 6vw, 100px) 0;")
    content = content.replace("  .hero-section {\n    padding-top: 180px;\n    padding-bottom: 100px;", "  .hero-section {\n    padding-top: clamp(100px, 12vw, 180px);\n    padding-bottom: clamp(40px, 7vw, 100px);")
    content = content.replace("    padding: 64px;", "    padding: clamp(24px, 5vw, 64px);")
    content = content.replace("  .site-footer {\n    padding: 80px 0 40px 0;", "  .site-footer {\n    padding: clamp(40px, 6vw, 80px) 0 clamp(24px, 3vw, 40px) 0;")

    # 3. Phone mockup structures
    old_frame_1 = """  .phone-frame {
    width: 320px;
    height: 640px;
    background-color: #020617;
    border: 12px solid #1E293B;
    border-radius: 40px;
    box-shadow: 0 25px 50px -12px rgba(15, 23, 42, 0.25);
    position: relative;
    overflow: hidden;
    display: flex;
    flex-direction: column;
  }"""
    new_frame_1 = """  .phone-frame {
    width: var(--phone-w) !important;
    height: var(--phone-h) !important;
    background-color: #020617 !important;
    border: 12px solid #1E293B !important;
    border-radius: 36px !important;
    box-shadow: 0 25px 50px -12px rgba(15, 23, 42, 0.25) !important;
    position: relative !important;
    overflow: hidden !important;
    display: flex !important;
    flex-direction: column !important;
    box-sizing: border-box !important;
  }"""
    content = content.replace(old_frame_1, new_frame_1)

    old_frame_2 = """  /* Phone Mockup Design */
  .phone-frame {
    width: 310px;
    height: 620px;
    background-color: #020617;
    border: 12px solid #1E293B;
    border-radius: 40px;
    box-shadow: 0 25px 50px -12px rgba(15, 23, 42, 0.25);
    position: relative;
    overflow: hidden;
    display: flex;
    flex-direction: column;
    transition: transform 0.3s cubic-bezier(0.16, 1, 0.3, 1), box-shadow 0.3s ease;
  }"""
    new_frame_2 = """  /* Phone Mockup Design */
  .phone-frame {
    width: var(--phone-w) !important;
    height: var(--phone-h) !important;
    background-color: #020617 !important;
    border: 12px solid #1E293B !important;
    border-radius: 36px !important;
    box-shadow: 0 25px 50px -12px rgba(15, 23, 42, 0.25) !important;
    position: relative !important;
    overflow: hidden !important;
    display: flex !important;
    flex-direction: column !important;
    box-sizing: border-box !important;
    transition: transform 0.3s cubic-bezier(0.16, 1, 0.3, 1), box-shadow 0.3s ease !important;
  }"""
    content = content.replace(old_frame_2, new_frame_2)

    # Phone screens
    old_screen_1 = """  .phone-screen {
    flex: 1;
    background-color: var(--bg-light);
    padding: 30px 16px 16px 16px;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    font-size: 12px;
  }"""
    new_screen_1 = """  .phone-screen {
    flex: 1 !important;
    background-color: var(--bg-light) !important;
    padding: 24px 12px 12px 12px !important;
    display: flex !important;
    flex-direction: column !important;
    overflow: hidden !important;
    position: relative !important;
    border-radius: 26px !important;
    font-size: 12px !important;
    box-sizing: border-box !important;
  }"""
    content = content.replace(old_screen_1, new_screen_1)

    old_screen_2 = """  .phone-screen {
    flex: 1;
    background-color: var(--bg-light);
    padding: 30px 0 16px 0;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    position: relative;
  }"""
    new_screen_2 = """  .phone-screen {
    flex: 1 !important;
    background-color: var(--bg-light) !important;
    padding: 24px 0 12px 0 !important;
    display: flex !important;
    flex-direction: column !important;
    overflow: hidden !important;
    position: relative !important;
    border-radius: 26px !important;
    box-sizing: border-box !important;
  }"""
    content = content.replace(old_screen_2, new_screen_2)

    # 4. Carousel max-width
    content = content.replace("""  .wd-carousel {
    position: relative;
    width: 100%;
    max-width: 320px;
    margin: 0 auto;
  }""", """  .wd-carousel {
    position: relative !important;
    width: 100% !important;
    max-width: var(--phone-w) !important;
    margin: 0 auto !important;
  }""")

    # 5. Bullet / list normalization
    old_bullets = """  .showcase-bullets {
    list-style: none;
    margin-bottom: 32px;
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .showcase-bullet-item {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    font-size: 15px;
    color: var(--text-secondary);
    text-align: left;
  }

  .showcase-bullet-icon {
    flex-shrink: 0;
    width: 22px;
    height: 22px;
    border-radius: 50%;
    background-color: var(--mint-light);
    color: var(--mint-primary);
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: bold;
  }"""

    new_bullets = """  .showcase-bullets {
    list-style: none !important;
    list-style-type: none !important;
    margin: 0 0 32px 0 !important;
    padding: 0 !important;
    display: flex !important;
    flex-direction: column !important;
    gap: 16px !important;
  }

  .showcase-bullet-item {
    list-style: none !important;
    list-style-type: none !important;
    display: flex !important;
    flex-direction: row !important;
    align-items: flex-start !important;
    gap: 12px !important;
    font-size: 15px !important;
    color: var(--text-secondary) !important;
    text-align: left !important;
    margin: 0 !important;
    padding: 0 !important;
  }

  .showcase-bullet-icon {
    flex-shrink: 0 !important;
    width: 20px !important;
    height: 20px !important;
    border-radius: 50% !important;
    background-color: var(--mint-light) !important;
    color: var(--mint-primary) !important;
    display: inline-flex !important;
    align-items: center !important;
    justify-content: center !important;
    font-weight: 800 !important;
    font-size: 11px !important;
    line-height: 1 !important;
    margin-top: 2px !important;
  }"""
    content = content.replace(old_bullets, new_bullets)

    # 6. Ultra-narrow adaptation update
    content = content.replace("""  /* ULTRA-NARROW VIEWPORT ADAPTABILITY */
  @media (max-width: 360px) {
    .phone-frame {
      width: 270px !important;
      height: 540px !important;
      border-width: 8px !important;
      border-radius: 30px !important;
    }""", """  /* ULTRA-NARROW VIEWPORT ADAPTABILITY */
  @media (max-width: 360px) {
    .phone-frame {
      width: var(--phone-w) !important;
      height: var(--phone-h) !important;
      border-width: 10px !important;
      border-radius: 30px !important;
    }""")

    # 7. Move fc-1 and fc-2 to desktop min-width media query
    orig_fc = """  .fc-1 {
    bottom: 80px;
    left: -40px;
    animation: float1 4s ease-in-out infinite alternate;
  }

  .fc-2 {
    top: 80px;
    right: -40px;
    animation: float2 4.5s ease-in-out infinite alternate;
  }"""
    new_fc = """  @media (min-width: 1025px) {
    .fc-1 {
      bottom: 80px;
      left: -40px;
      animation: float1 4s ease-in-out infinite alternate;
    }

    .fc-2 {
      top: 80px;
      right: -40px;
      animation: float2 4.5s ease-in-out infinite alternate;
    }
  }"""
    content = content.replace(orig_fc, new_fc)

    # 8. Adaptive styling for floating cards under 1024px
    content = content.replace("@media (max-width: 1024px) {", """@media (max-width: 1024px) {
    .floating-card {
      position: static !important;
      transform: none !important;
      animation: none !important;
      width: 100% !important;
      max-width: 300px !important;
      margin: 12px auto !important;
      display: flex !important;
      box-shadow: 0 4px 12px rgba(15, 23, 42, 0.05) !important;
    }""")

    # 9. Clean up Blogger elements CSS
    content = content.replace("  ]]></b:skin>", """  /* NATIVE BLOGGER NOISE CLEANUP */
  .feed-links,
  .blog-feeds,
  .post-feeds,
  #blog-pager,
  .attribution,
  #Attribution1,
  .widget.Attribution,
  .item-control,
  .quickedit,
  .Navbar,
  #navbar-iframe {
    display: none !important;
    visibility: hidden !important;
    height: 0 !important;
    opacity: 0 !important;
    overflow: hidden !important;
    margin: 0 !important;
    padding: 0 !important;
  }
  ]]></b:skin>""")

    # 10. Native Blogger widget overrides
    content = content.replace("""      </b:includable>
    </b:widget>
  </b:section>""", """      </b:includable>
      <b:includable id='feedLinks'/>
    </b:widget>
  </b:section>""")

    # 11. REBRAND ALL COPY & Terminology
    # Replace Header and Footer Logo
    content = content.replace("""        <a class='logo' href='#'>
          <svg viewBox='0 0 24 24'>
            <path d='M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-9 14l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z'/>
          </svg>
          WorkDaily
        </a>""", f"""        <a class='logo' href='#'>
          <img src='data:image/png;base64,{logo_b64}' alt='WorkDaily App Icon' style='width:36px;height:36px;object-fit:contain;margin-right:2px;'/>
          WorkDaily
        </a>""")

    content = content.replace("""          <a class='logo' href='#'>
            <svg viewBox='0 0 24 24'>
              <path d='M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-9 14l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z'/>
            </svg>
            WorkDaily
          </a>""", f"""          <a class='logo' href='#'>
            <img src='data:image/png;base64,{logo_b64}' alt='WorkDaily App Icon' style='width:36px;height:36px;object-fit:contain;margin-right:2px;'/>
            WorkDaily
          </a>""")

    # Replace Hero
    content = content.replace("""            <span class='section-tag'>Official Release by SynFusion</span>
            <h1>WorkDaily — Your Daily <span class='text-gradient'>Productivity</span> Companion</h1>
            <p class='hero-description'>
              Manage tasks. Track time. Stay focused. Effortlessly coordinate contractor attendance, daily wage worker logs, and personal earnings through an elegant, offline-first native interface.
            </p>""", f"""            <div style='margin-bottom:20px;display:flex;align-items:center;gap:12px;'>
              <img src='data:image/png;base64,{logo_b64}' alt='WorkDaily App Icon' style='width:56px;height:56px;object-fit:contain;border-radius:14px;'/>
              <span class='section-tag' style='margin:0;'>Official Release by SynFusion</span>
            </div>
            <h1>WorkDaily — Offline <span class='text-gradient'>Personal Attendance</span></h1>
            <p class='hero-description'>
              Track your daily attendance, calculate wages, log overtime, and manage your advances. All your personal attendance records are stored securely inside your phone—completely offline.
            </p>""")

    # Hero Mockup User Role & Content
    content = content.replace("<div class='mockup-user-role'>Contractor Account</div>", "<div class='mockup-user-role'>Personal Account</div>")
    content = content.replace("<div class='mockup-card-title'>Recent Worker Updates</div>", "<div class='mockup-card-title'>Recent Attendance Logs</div>")
    content = content.replace("<span style='font-weight:600;color:var(--text-primary);'>Rajesh Kumar</span>\n                    <span style='color:#0F9F7A;font-weight:700;'>Present</span>", "<span style='font-weight:600;color:var(--text-primary);'>August 24, 2026</span>\n                    <span style='color:#0F9F7A;font-weight:700;'>Present</span>")
    content = content.replace("<span style='font-weight:600;color:var(--text-primary);'>Vikram Singh</span>\n                    <span style='color:#EF4444;font-weight:700;'>Absent</span>", "<span style='font-weight:600;color:var(--text-primary);'>August 23, 2026</span>\n                    <span style='color:#EF4444;font-weight:700;'>Absent</span>")

    # Trust Proof Section
    content = content.replace("<h2>Endorsed by Thousands of Contractors and Workers Worldwide</h2>", "<h2>Your Trustworthy, Secure and Offline Attendance Ledger</h2>")
    content = content.replace("Active Users", "Offline Users")
    content = content.replace("Productivity Boost", "Data Control")

    # Features Section Header
    content = content.replace("<h2 class='section-title'>Designed Specifically for Dynamic Modern Workforce Tracking</h2>", "<h2 class='section-title'>Designed Specifically for Personal Attendance &amp; Wage Tracking</h2>")
    content = content.replace("<p class='section-subtitle'>WorkDaily delivers robust offline attendance management features inspired by actual workflow requirements, avoiding complex online configurations.</p>", "<p class='section-subtitle'>WorkDaily delivers robust offline personal attendance tracking and ledger features, completely avoiding complex online accounts and data risks.</p>")

    # Feature Cards
    content = content.replace("<h3>Workforce Logs</h3>\n            <p>Seamlessly track and log attendances, salary balances, dynamic daily wages, and overtime hours for contract groups and teams.</p>", "<h3>Daily Attendance</h3>\n            <p>Easily log your daily attendance with statuses like Present, Absent, Half Day, or Holiday. Ideal for tracking your daily shifts and working days.</p>")
    content = content.replace("<h3>Calendar Grid</h3>\n            <p>Quickly view, edit, and update monthly attendances via a clean, color-coded interactive grid representation.</p>", "<h3>Calendar View</h3>\n            <p>A beautifully color-coded calendar grid lets you view, review, and edit your monthly records at a single glance.</p>")
    content = content.replace("<h3>Advances &amp; Overtime</h3>\n            <p>Track advanced wage drawings and calculate custom overtime ratios automatically to ensure complete payment transparency.</p>", "<h3>Wages &amp; Overtime</h3>\n            <p>Set your daily wage, record advance drawings, and calculate custom overtime earnings automatically to know exactly what you are owed.</p>")
    content = content.replace("<h3>High Fidelity PDF Export</h3>\n            <p>Generate highly professional, clear A4 Monthly Passbooks and All-Time reports. Supports full Devanagari Hindi Unicode rendering.</p>", "<h3>High Fidelity PDF Export</h3>\n            <p>Compile your earnings, attendances, and advances into clean A4 PDF Passbooks. Supports native Devanagari Hindi for perfect local reports.</p>")
    content = content.replace("<h3>100% Offline-First Privacy</h3>\n            <p>Keep your data strictly secured inside your phone's internal private storage directory. No cloud login leak risks.</p>", "<h3>100% Offline Privacy</h3>\n            <p>Your data stays completely inside your phone's secure private folder. No internet connection, no servers, and absolutely 100% private.</p>")
    content = content.replace("<h3>Localized Reminders</h3>\n            <p>Receive local, friendly conversational push notifications and attendance reminders in Hinglish and Hindi on daily basis.</p>", "<h3>Localized Reminders</h3>\n            <p>Get friendly daily reminders in conversational Hindi, Hinglish, or English, encouraging you to keep your logs updated.</p>")

    # Showcase Intro
    content = content.replace("<p class='section-subtitle'>Swipe through the phone mockups below to see exactly how our premium offline Android application tracks attendance and prints PDF reports.</p>", "<p class='section-subtitle'>Slide through the mockups below to see how our offline Android app makes attendance tracking, monthly logs, and PDF exports a breeze.</p>")

    # Showcase 1 Title Row & Bullets
    old_sh1_text = """        <!-- SHOWCASE 1: Contractor Dashboard -->
        <div class='showcase-item'>
          <div class='showcase-text-col'>
            <span class='section-tag'>Management Hub</span>
            <h3>Contractor Dashboard</h3>
            <p>Monitor your entire active labor force, check today's pending logs, and view overall cost indicators on a single screen. Our high-fidelity interface puts data at your fingertips.</p>
            <ul class='showcase-bullets'>
              <li class='showcase-bullet-item'>
                <span class='showcase-bullet-icon'>✓</span>
                <div><strong>Realtime labor indicators:</strong> Check active daily wages, total hours worked, and overtime rates instantly.</div>
              </li>
              <li class='showcase-bullet-item'>
                <span class='showcase-bullet-icon'>✓</span>
                <div><strong>Live progress tracking:</strong> Monitor completion percentages as workers check in and out.</div>
              </li>
              <li class='showcase-bullet-item'>
                <span class='showcase-bullet-icon'>✓</span>
                <div><strong>Actionable shortcuts:</strong> One-tap access to add new workers, record advances, or generate instant summaries.</div>
              </li>
            </ul>
            <a class='btn btn-primary' href='#download'>Get App on Google Play</a>
          </div>"""

    new_sh1_text = """        <!-- SHOWCASE 1: Personal Attendance Dashboard -->
        <div class='showcase-item'>
          <div class='showcase-text-col'>
            <span class='section-tag'>Main Dashboard</span>
            <h3>Personal Attendance Dashboard</h3>
            <p>Track your personal attendance, monthly wages, advances, and overtime shift statistics in one intuitive dashboard view. Keep a running tally of your days worked.</p>
            <ul class='showcase-bullets'>
              <li class='showcase-bullet-item'>
                <span class='showcase-bullet-icon'>✓</span>
                <div><strong>Real-time Wage Indicators:</strong> Keep a running total of your standard wage earnings, overtime hours, and outstanding balance.</div>
              </li>
              <li class='showcase-bullet-item'>
                <span class='showcase-bullet-icon'>✓</span>
                <div><strong>Work Ratio Metrics:</strong> Track your monthly worked ratio, present days, and absent days in a single, beautiful visual ring.</div>
              </li>
              <li class='showcase-bullet-item'>
                <span class='showcase-bullet-icon'>✓</span>
                <div><strong>Quick Action Buttons:</strong> Add daily attendance notes, log immediate advance draws, or open drill-down summaries in one tap.</div>
              </li>
            </ul>
            <a class='btn btn-primary' href='#download'>Get App on Google Play</a>
          </div>"""
    content = content.replace(old_sh1_text, new_sh1_text)

    # Carousel 1 Slide contents
    content = content.replace("mockup-card-title'>Quick Budget</div>", "mockup-card-title'>Attendance Overview</div>")
    content = content.replace("Active Workers:", "Total Present:")
    content = content.replace("18 Workers", "18 Days")
    content = content.replace("Today's Total Budget:", "Monthly Advances:")
    content = content.replace("₹8,500", "₹3,500")

    content = content.replace("class='slide-title'>Progress Tracker</span>", "class='slide-title'>Monthly Status</span>")
    content = content.replace("Attendance Progress", "Worked Ratio")
    content = content.replace("Completed logs (14/18)", "Worked Days Ratio")
    content = content.replace("Realtime Log Status", "Recent Log Status")
    content = content.replace("Rajesh Kumar", "August 24")
    content = content.replace("Vikram Singh", "August 23")

    # Slide 3 of Carousel 1
    content = content.replace("""                          <div class='mockup-card' style='text-align:center;padding:16px 8px;cursor:pointer;'>
                            <div style='font-size:24px;margin-bottom:6px;'>👥</div>
                            <div style='font-weight:700;font-size:12px;'>Add Worker</div>
                          </div>
                          <div class='mockup-card' style='text-align:center;padding:16px 8px;cursor:pointer;'>
                            <div style='font-size:24px;margin-bottom:6px;'>📊</div>
                            <div style='font-weight:700;font-size:12px;'>Passbook Report</div>
                          </div>
                          <div class='mockup-card' style='text-align:center;padding:16px 8px;cursor:pointer;grid-column: span 2;'>
                            <div style='font-size:20px;margin-bottom:4px;'>⚙️</div>
                            <div style='font-weight:700;font-size:12px;'>System Preferences</div>
                          </div>""", """                          <div class='mockup-card' style='text-align:center;padding:16px 8px;cursor:pointer;'>
                            <div style='font-size:24px;margin-bottom:6px;'>📝</div>
                            <div style='font-weight:700;font-size:12px;'>Add Note</div>
                          </div>
                          <div class='mockup-card' style='text-align:center;padding:16px 8px;cursor:pointer;'>
                            <div style='font-size:24px;margin-bottom:6px;'>💰</div>
                            <div style='font-weight:700;font-size:12px;'>Add Advance</div>
                          </div>
                          <div class='mockup-card' style='text-align:center;padding:16px 8px;cursor:pointer;grid-column: span 2;'>
                            <div style='font-size:20px;margin-bottom:4px;'>📅</div>
                            <div style='font-weight:700;font-size:12px;'>View Calendar</div>
                          </div>""")

    # Showcase 2 Text Column
    old_sh2_text = """        <!-- SHOWCASE 2: Attendance Calendar (Reverse Layout) -->
        <div class='showcase-item reverse'>
          <div class='showcase-text-col'>
            <span class='section-tag'>Visual Tracker</span>
            <h3>Dynamic Attendance Calendar</h3>
            <p>Manage worker registries and wage payments directly inside a color-coded Interactive Calendar Grid. Fast taps let you mark Holiday, Present, Absent, or Half-day statuses with full precision.</p>
            <ul class='showcase-bullets'>
              <li class='showcase-bullet-item'>
                <span class='showcase-bullet-icon'>✓</span>
                <div><strong>Visual color codes:</strong> Spot attendance states at a glance. Green for present, red for absent, orange for half-days.</div>
              </li>
              <li class='showcase-bullet-item'>
                <span class='showcase-bullet-icon'>✓</span>
                <div><strong>Drill-down details:</strong> Open deep-dive sheets to log detailed wages, overtime ratios, or advanced drawings per worker.</div>
              </li>
              <li class='showcase-bullet-item'>
                <span class='showcase-bullet-icon'>✓</span>
                <div><strong>Isolated local Storage:</strong> All calendar calculations and worker statistics run instantly offline, protected inside filesDir.</div>
              </li>
            </ul>
            <a class='btn btn-secondary' href='#features'>Explore Technical Details</a>
          </div>"""

    new_sh2_text = """        <!-- SHOWCASE 2: Color-Coded Attendance Calendar (Reverse Layout) -->
        <div class='showcase-item reverse'>
          <div class='showcase-text-col'>
            <span class='section-tag'>Calendar Logs</span>
            <h3>Color-Coded Attendance Calendar</h3>
            <p>Manage your records on a premium interactive monthly calendar. Fast, single-tap controls let you log or update your attendance as Present, Absent, Half Day, or Holiday with complete precision.</p>
            <ul class='showcase-bullets'>
              <li class='showcase-bullet-item'>
                <span class='showcase-bullet-icon'>✓</span>
                <div><strong>Visual Color Indicators:</strong> Instantly check your patterns. Green for present, red for absent, orange for half-days, and grey for holidays.</div>
              </li>
              <li class='showcase-bullet-item'>
                <span class='showcase-bullet-icon'>✓</span>
                <div><strong>Drill-down Records:</strong> Open full date sheets to add custom wages, specify overtime hours, subtract advances, or write persistent notes.</div>
              </li>
              <li class='showcase-bullet-item'>
                <span class='showcase-bullet-icon'>✓</span>
                <div><strong>100% Local Processing:</strong> All calendar tallies and wage estimates are processed on-device, keeping your files completely secure.</div>
              </li>
            </ul>
            <a class='btn btn-secondary' href='#features'>Explore Technical Details</a>
          </div>"""
    content = content.replace(old_sh2_text, new_sh2_text)

    # Carousel 2 slide contents
    content = content.replace("Standard Daily Wage:", "Daily Wage Rate:")
    content = content.replace("Overtime Multiplier:", "Overtime Multiplier:")
    content = content.replace("Worker: Rajesh Kumar", "Selected Date Details")
    content = content.replace("Completed site layout foundations phase with standard daily wage parameters.", "Finished work on shift A, recorded standard wage rate plus 2 hours overtime.")

    # Showcase 3 Text Column
    old_sh3_text = """        <!-- SHOWCASE 3: Passbook Export Engine -->
        <div class='showcase-item'>
          <div class='showcase-text-col'>
            <span class='section-tag'>Reports &amp; Sharing</span>
            <h3>High-Fidelity PDF Export</h3>
            <p>Compile complex financial statements, attendances, and outstanding payment ledgers directly into beautifully formatted A4 PDF reports. Supports full Devanagari Hindi Unicode text for perfect localization.</p>
            <ul class='showcase-bullets'>
              <li class='showcase-bullet-item'>
                <span class='showcase-bullet-icon'>✓</span>
                <div><strong>Devanagari text compliance:</strong> Ensure correct rendering of Hindi names and titles without clipping or blank boxes.</div>
              </li>
              <li class='showcase-bullet-item'>
                <span class='showcase-bullet-icon'>✓</span>
                <div><strong>Background thread execution:</strong> File preparation runs completely in the background, keeping the app smooth.</div>
              </li>
              <li class='showcase-bullet-item'>
                <span class='showcase-bullet-icon'>✓</span>
                <div><strong>Direct WhatsApp sharing:</strong> Exported documents can be instantly routed to worker groups with localized sharing messages.</div>
              </li>
            </ul>
            <a class='btn btn-primary' href='#download'>Generate Passbook Now</a>
          </div>"""

    new_sh3_text = """        <!-- SHOWCASE 3: PDF Passbook Export Engine -->
        <div class='showcase-item'>
          <div class='showcase-text-col'>
            <span class='section-tag'>Reports &amp; Sharing</span>
            <h3>PDF Passbook Export Engine</h3>
            <p>Compile all your daily shifts, overtime wages, and advance payouts into a sleek, professional A4 PDF Ledger. Native Devanagari Unicode support ensures error-free rendering in Hindi or Hinglish.</p>
            <ul class='showcase-bullets'>
              <li class='showcase-bullet-item'>
                <span class='showcase-bullet-icon'>✓</span>
                <div><strong>Devanagari Hindi Fonts:</strong> Generate your PDF Passbook in full Hindi with clean Unicode text, keeping records accessible.</div>
              </li>
              <li class='showcase-bullet-item'>
                <span class='showcase-bullet-icon'>✓</span>
                <div><strong>Background Thread Generation:</strong> Room database queries and PDF drawings execute asynchronously, ensuring zero UI stutter.</div>
              </li>
              <li class='showcase-bullet-item'>
                <span class='showcase-bullet-icon'>✓</span>
                <div><strong>Instant Sharing Options:</strong> Instantly share PDF files via WhatsApp or email using localized templates directly from the app.</div>
              </li>
            </ul>
            <a class='btn btn-primary' href='#download'>Generate Passbook Now</a>
          </div>"""
    content = content.replace(old_sh3_text, new_sh3_text)

    # Carousel 3 Slide Contents
    content = content.replace("August 2026 Monthly Passbook", "August 2026 PDF Passbook")
    content = content.replace("Layout Type: A4 Portrait Ledger", "Layout Type: A4 Portrait Passbook")
    content = content.replace("Compiling Financial Ledger", "Compiling PDF Passbook")
    content = content.replace("Aug_2026_Report.pdf", "Aug_2026_Passbook.pdf")
    content = content.replace("142 KB • Generated cleanly", "142 KB • PDF Generated Successfully")

    # Download Box Wrap copy
    download_old = """            <div>
              <h2>Download WorkDaily Today</h2>
              <p>Boost your labor workforce output, track your contractor accounts offline, and eliminate standard payment misalignments completely.</p>"""
    download_new = f"""            <div>
              <div style='display:flex;align-items:center;gap:12px;margin-bottom:12px;'>
                <img src='data:image/png;base64,{logo_b64}' alt='WorkDaily App Icon' style='width:48px;height:48px;object-fit:contain;border-radius:10px;'/>
                <h2 style='margin:0;'>Download WorkDaily Today</h2>
              </div>
              <p>Take full control of your shifts, wages, and records. Download the secure offline-first application and start tracking your attendance today.</p>"""
    content = content.replace(download_old, download_new)

    # Footer Desc
    content = content.replace("""          <p class='footer-desc'>A product of SynFusion. Designed to help contractors and workforce groups track attendance, calculate earnings, and generate dynamic PDF reports cleanly offline.</p>""", f"""          <p class='footer-desc'>A premium product of SynFusion. Designed to help individuals track attendance, calculate daily wages, and generate professional PDF passbooks cleanly and offline.</p>""")
    content = content.replace("Workforce Tracking", "Attendance Tracking")

    # Write final output
    with open("theme.xml", "w", encoding="utf-8") as f:
        f.write(content)

    print("Successfully built and applied changes!")

if __name__ == "__main__":
    main()
