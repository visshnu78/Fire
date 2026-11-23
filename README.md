# Fire: a personal anti-doomscroll app

Fire is an Android app that blocks the endless-scroll parts of Instagram and YouTube while a lock timer runs. When the timer ends, everything unlocks on its own.

## What it blocks during a lock
- **Instagram Reels** (the Reels tab, and reels opened from DMs or profiles)
- **YouTube Shorts**
- **YouTube long-form videos** (the full watch page)
- **The whole YouTube app** (optional): opening YouTube sends you straight to the home screen

DMs, search and profiles keep working. Patched YouTube builds such as **ReVanced / RVX** are covered too.

## How it works
- Pick a lock length (**30m / 1h / 2h / 4h** or any number of minutes). **Add 30 minutes** extends a running lock.
- When you land on a blocked screen, Fire presses Back (or Home for a whole-app block) and shows a **"Not now."** screen with the time left.
- If YouTube slips into a floating picture-in-picture window, Fire pauses the video and drags the window away.
- To end a lock early, use **fingerprint or face unlock**. On phones without either, you type a sentence instead.
- You can't change what's blocked while a lock is running.
- Fire does nothing while the screen is off.
- **Inspector mode** lists the screen IDs on the current screen, so you can fix detection after an Instagram or YouTube update (see `Targets.java`).

It uses Android's Accessibility Service, acts only on Instagram and YouTube, and stores nothing.

## Build and install
1. Install [Android Studio](https://developer.android.com/studio).
2. **File → Open** this folder and wait for Gradle sync.
3. On the phone, turn on **Developer options → USB debugging** and plug it in.
4. Pick the phone in the device list and press ▶ **Run**.

## First run on the phone
1. In Fire, tap **Turn on in Accessibility settings** and switch on **Fire blocker**.
   If it's greyed out ("Restricted setting"): **Settings → Apps → Fire → ⋮ → Allow restricted settings**, then try again.
2. On Xiaomi / Oppo / Vivo / Realme / Samsung, set **Settings → Apps → Fire → Battery → Unrestricted** (and Autostart if offered), or the phone may switch the blocker off.

## Limits
- It covers the Instagram and YouTube **apps**, not instagram.com or youtube.com in a browser.
- Turning off the accessibility service or uninstalling Fire ends the block. It's a speed bump for your own habits, not a jail.

## Tech
Plain Java, no third-party libraries. Min Android 8.0 (API 26), target API 34.
