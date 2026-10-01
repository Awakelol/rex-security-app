# Rex

A small Android app that keeps an eye on a relative's phone. It's for people who keep getting
tricked into installing junk: fake "your phone has a virus" alerts, fake download buttons, and
"cleaner" apps that turn out to be malware.

Rex remembers which apps were on the phone when you set it up. When anything new gets installed it:

- scores how risky the app looks (where it came from, what it asks for, whether its name copies an existing app)
- hides that app's notifications
- puts up a big, simple warning with a **Remove it** button
- sends you a message on Discord

You approve or remove new apps from a PIN-protected area. Rex also deletes scareware
notifications that websites push through the browser ("Your phone is infected! Clean now").

Rex never uninstalls or kills anything by itself. It warns, hides notifications, and tells you.

## Building

You need JDK 17 or newer and the Android SDK (platform 37, build-tools 36). Create
`local.properties` in the project root pointing at your SDK (this file is git-ignored):

```
sdk.dir=C\:\\Users\\you\\AppData\\Local\\Android\\Sdk
```

Then build:

```
gradlew.bat assembleDebug      # Windows
./gradlew assembleDebug        # macOS / Linux
```

The APK ends up at `app/build/outputs/apk/debug/app-debug.apk`.

Run the unit tests with `gradlew test`.

## Installing on a Samsung phone

1. **Turn on Developer options.** Settings → About phone → Software information → tap
   **Build number** seven times. Enter the phone's PIN if asked.
2. **Turn on USB debugging.** Settings → Developer options → USB debugging.
3. **Turn off Auto Blocker for now** (One UI 6 and later). Settings → Security and privacy →
   Auto Blocker. It blocks USB commands, so `adb` won't work while it's on.
4. Plug the phone into your computer. Tap **Allow** on the "Allow USB debugging?" prompt
   (tick "Always allow from this computer").
5. Check the phone shows up, then install:

   ```
   adb devices
   adb install -r app\build\outputs\apk\debug\app-debug.apk
   ```

   `-r` lets you install over an older copy without losing Rex's data.
6. When you're done, it's worth turning **Auto Blocker back on**. It stops apps being installed
   from outside the Play Store and Galaxy Store, which covers a lot of the damage on its own.
   You'll need to switch it off again whenever you update Rex.

### "Restricted setting"

On Android 13 and later, apps that weren't installed from a store aren't allowed to turn on some
sensitive permissions (notification access is one). If you see a **Restricted setting** message:

1. Try to turn the setting on once, so Android registers the attempt.
2. Go to Settings → Apps → Rex, tap the **⋮** menu in the top right and choose
   **Allow restricted settings**.
3. Go back and turn the setting on again.

## Setting it up

Open Rex on the phone. The first launch saves every app that's already installed as the approved
list, so make sure nothing nasty is on there before you start (or remove it first).

Tap the small button at the bottom of the screen, create a PIN, then work through the
**Setup** tab from top to bottom:

1. **Allow notifications.** Without this the warning can't pop up.
2. **Notification access.** Lets Rex hide notifications (see "Restricted setting" above).
3. **Ignore battery optimisation.** Stops Android from putting Rex to sleep.
4. **Full-screen notifications** (Android 14+). Lets the warning take over the screen.
5. **Display over other apps.** Optional, but the warning opens straight away instead of
   only showing as a notification.
6. **Samsung: never sleeping apps.** Settings → Battery → Background usage limits →
   Never sleeping apps → add Rex. Rex can't check this one for you.

Then on the **Settings** tab:

- Enter your name and phone number. They're used for the "Call" button on the warning and home screens.
- Paste a Discord webhook URL (in Discord: channel settings → Integrations → Webhooks →
  New Webhook → Copy Webhook URL) and press **Send test alert**.

The home screen should now be green and say "This phone is protected". If it's amber,
something on the Setup tab still needs doing.

Treat the webhook URL like a password. Anyone who has it can post in your channel. It lives only
on the phone and isn't backed up anywhere.

## Testing it

### Play Store app (scores LOW)

1. On the protected phone, install something harmless from the Play Store, ideally one that
   sends notifications (a to-do or reminder app works well).
2. Check that:
   - a "New app installed" notification appears (LOW-risk apps don't get the full warning screen)
   - you get a Discord message with the app name, package, source, risk level and reasons
   - the app is listed under **Pending** in the helper area
   - the new app's notifications don't show up (set a reminder in it and wait for it)
3. Approve it from the **Pending** tab. Its notifications should start showing again.

### Sideloaded app (scores HIGH, shows the warning screen)

4. From your computer, `adb install` any harmless APK you trust, for example the F-Droid app
   from f-droid.org. Anything that didn't come from a store counts as HIGH risk.
5. The full warning screen should appear (with "Display over other apps" on, straight away;
   otherwise as a notification or full-screen alert). Check the Discord message says HIGH.
6. Press **Back** to dismiss it, lock the phone, wait an hour and unlock. The warning should come
   back (at most once an hour while the app is still installed and pending).
7. Press **Remove it**. The system uninstall dialog should appear. After removing, the warning
   says it's gone and you get a "Removed" message on Discord.

### Restart

8. Reboot the phone and check the home screen is still green.

## What Rex can't do

Be honest with yourself about these:

- **It can't stop an install.** Rex only finds out after an app is on the phone. The warning
  depends on someone pressing **Remove it**.
- **It can't remove apps by itself, or stop a nasty one from running.** If something grabs
  device-admin or accessibility rights before anyone removes it, it may fight the uninstall.
  Booting into Safe Mode usually helps: hold the power button, then press and hold
  **Power off** until **Safe mode** appears. Uninstall from there.
- **It doesn't scan for malware.** The risk score is a set of simple rules. A clean-looking app from
  the Play Store will score LOW even if it turns bad later, and an app that's already approved
  is never re-checked after updates.
- **It doesn't see web pages.** Fake download buttons and pop-ups inside the browser are invisible
  to it. It only filters browser *notifications* that match scareware phrases.
- **It can be switched off.** Anyone holding the phone can force-stop or uninstall Rex, revoke its
  permissions, or turn off notification access. The home screen goes amber when that happens,
  but nothing stops it.
- **Phones kill background apps.** Samsung is aggressive about this. With the setup steps done
  it should stay up, and a check every 15 minutes catches anything installed while it was down,
  but delays are possible.
- **Discord needs internet.** Alerts queue up while the phone is offline and are sent later.

## How it's put together

```
app/src/main/java/io/github/awakelol/rex/
  core/      plain Kotlin: risk rules and scoring, notification filter, PIN hashing (unit tested)
  data/      Room database (event log, pending apps) and DataStore settings
  watch/     foreground service, install receiver, periodic check, PackageManager lookups
  notify/    notification channels and the notification listener
  alert/     Discord webhook sender
  warning/   full-screen warning activity
  ui/        Compose screens: home, PIN, helper area
```

Keyword lists, trusted stores and signal weights are all in `core/Rules.kt` if you want to tune them.

Everything a response to a new app does goes through the `Enforcer` interface. The only
implementation today is `NotifyOnlyEnforcer`; a device-owner version that can actually suspend
apps could slot in there later.
