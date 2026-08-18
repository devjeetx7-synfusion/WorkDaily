const { chromium } = require('playwright');
const path = require('path');

(async () => {
  const browser = await chromium.launch();
  const context = await browser.newContext({
    viewport: { width: 375, height: 812 },
    isMobile: true,
    hasTouch: true
  });
  const page = await context.newPage();

  const filePath = `file://${path.resolve('test_index.html')}`;
  await page.goto(filePath, { waitUntil: 'load' });

  console.log('--- AUDITING MOBILE DRAWER & INTERACTION ---');

  // Check initial drawer state
  const drawer = page.locator('#menuDrawer');
  const isDrawerVisibleBefore = await drawer.evaluate(el => {
    const style = window.getComputedStyle(el);
    return style.visibility !== 'hidden' && style.opacity !== '0';
  });
  console.log('Drawer visible before click:', isDrawerVisibleBefore);

  // Click menu toggle
  const toggle = page.locator('#menuToggle');
  await toggle.click();
  await page.waitForTimeout(300);

  const isDrawerVisibleAfter = await drawer.evaluate(el => {
    const style = window.getComputedStyle(el);
    return style.visibility === 'visible';
  });
  console.log('Drawer visible after toggle click:', isDrawerVisibleAfter);

  // Check body scroll lock
  const bodyOverflow = await page.evaluate(() => document.body.style.overflow);
  console.log('Body overflow after drawer open:', bodyOverflow);

  // Press Escape key
  await page.keyboard.press('Escape');
  await page.waitForTimeout(300);

  const isDrawerVisibleAfterEsc = await drawer.evaluate(el => {
    const style = window.getComputedStyle(el);
    return style.visibility !== 'hidden' && style.opacity !== '0';
  });
  console.log('Drawer visible after ESC key:', isDrawerVisibleAfterEsc);

  console.log('--- AUDITING DARK MODE ---');
  const darkToggle = page.locator('#themeToggle');
  if (await darkToggle.count() > 0) {
    await darkToggle.click();
    await page.waitForTimeout(200);
    const themeAttr = await page.getAttribute('html', 'data-theme');
    console.log('HTML data-theme attribute after toggle click:', themeAttr);
  }

  await browser.close();
  console.log('Component audit finished successfully!');
})();
