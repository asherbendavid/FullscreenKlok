# Fullscreen Klok

`cvc.dashingdog.fullscreenklok` — a deliberately minimal, single-purpose
fullscreen clock. No settings screen, no themes, no customization. It does
one thing: shows the time, date, and battery level, and cannot be touched
by accident.

## Design decisions

- **No touch targets, anywhere.** Every view has `clickable="false"` /
  `focusable="false"`, and on top of that `MainActivity.dispatchTouchEvent`
  unconditionally returns `true`, swallowing every touch before it reaches
  any view at all. Belt and braces — the whole point of the app.
- **System buttons (back/home/recents) behave normally** — only touches on
  the clock face itself are inert.
- **Screen stays on** via `FLAG_KEEP_SCREEN_ON` on the window, for as long
  as the activity is in the foreground. If you lock the phone or leave the
  app, normal screen timeout resumes — this is intentionally the simple
  "foreground keep-awake" approach, not a lock-screen overlay.
- **Orientation handled manually.** The manifest declares
  `configChanges="orientation|screenSize"` so the activity isn't destroyed
  on rotation (avoids losing the tick loop / receiver); `onConfigurationChanged`
  just re-inflates the right layout (`layout/` vs `layout-land/`).
- **Clock loop** uses a self-correcting `Handler.postDelayed`, aligning each
  tick to the next whole second (rather than a naive fixed 1000ms interval,
  which drifts).
- **Battery** read via a sticky-broadcast `BroadcastReceiver` on
  `ACTION_BATTERY_CHANGED` — no permission required, registered/unregistered
  in `onResume`/`onPause`.
- **Battery icon** is a custom `View` (`BatteryView.kt`) drawn with `Canvas`,
  matching Vaart's status-icon language: outline + fill, green >50%, amber
  20–50%, red <20%, with a gentle alpha pulse while charging. In landscape
  it's rotated -90° (wrapped in a fixed-size `FrameLayout` sized to its
  post-rotation footprint so it isn't clipped by neighbouring views).

## Layouts

- **Portrait** (`res/layout/activity_main.xml`): vertical block — time,
  date, battery icon below — centered both axes.
- **Landscape** (`res/layout-land/activity_main.xml`): horizontal block —
  time+date on the left, battery icon (rotated upright) to the right —
  centered both axes.

## Not included (by design)

No layout switcher, no font picker, no background/theme options. If you
ever want a variant, easiest path is duplicating this project rather than
adding settings — keeps it single-purpose per your "arrived at what I want"
philosophy.

### Burn-in prevention
- Screen saver mode always shifts the display slightly (±24px, every
  5 minutes) to reduce AMOLED burn-in risk during long overnight
  sessions.
- The same shift can optionally be enabled on the normal display too,
  via Settings > "Shift display to prevent burn-in". When enabled,
  the Settings screen shows an orange guide border indicating the
  margin to leave around your content.
