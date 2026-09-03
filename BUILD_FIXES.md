# Build fixes

## 0.1.2

- Replaced `Icons.Rounded.Database` with `Icons.Rounded.Storage`.
  `Database` is not available in the Compose Material Icons set resolved by this project
  (Compose 1.6.8 / Material Icons Extended from BOM 2024.06.00).
- Kept `androidx.annotation:annotation` aligned at 1.8.0 to avoid the prior dependency conflict.

## 0.1.3

- Fixed `HostCleanerDialog.java` Java compilation failure.
- `AlertDialog.setButton(..., DialogInterface.OnClickListener)` requires a two-parameter listener:
  `(dialog, which) -> ...`.
- Replaced the invalid one-parameter lambda `v -> ...` with
  `(d, which) -> confirmClean(...)`.

## 0.1.4

- Added default compatibility-mode cleanup entry and Activity lifecycle fallback.
- SSAID disabled state no longer installs the ANDROID_ID hook.

## 0.1.5

- Added SecureRandom-backed 16-character hexadecimal SSAID generation.


## 0.1.6

- Fixed host cleanup dialog where the scan completed but the `开始清理` button did not appear under some QQ themes.
- The positive button is now created before `AlertDialog.show()`, disabled while scanning, then enabled and relabeled after scan completes.
- If no disposable files are found, the button displays `没有可清理项` and stays disabled.

## 0.1.8

- Reverted the 0.1.7 experiment that moved the ACE cleanup overlay to QQ's top "+" button area.
- Restored the conflict-safe bottom-right floating `ACE 清理` entry used by 0.1.6.
- Kept the 0.1.6 AlertDialog cleanup-button fix.
- Kept the 0.1.5 SecureRandom SSAID generator.

## 0.1.9

- Replaced the default bottom-right floating cleanup button with a rendered settings-menu row.
- QQ `QPublicFragmentActivity` is no longer treated as a settings screen by class name alone.
- Added settings-page text fingerprinting to prevent the ACE entry from appearing on QQ's main screen.
- Compatibility mode inserts a normal View row into a safe vertical settings container instead of mutating the host Preference model.
- Kept the v0.1.6 cleanup dialog button fix and v0.1.5 SecureRandom SSAID generator.

## 0.2.0

- Removed the debug `applicationIdSuffix`; debug/release now share `com.ace.toolbox`.
- Added framework-level `XposedInterface.log()` lifecycle markers.
- Added `onPackageLoaded()` early Activity lifecycle watcher, with `onPackageReady()` retained.
- Relaxed QQ settings fingerprint to handle versions whose large "设置" title is not a normal TextView.
- Kept 0.1.9 in-menu rendering, 0.1.6 cleanup-button fix, and random SSAID support.


## 0.2.1

- Added QQ NT native settings-config-provider injection for MainSettingConfigProvider,
  NewSettingConfigProvider and the known obfuscated provider alias.
- Dynamically discovers a compatible SimpleItemProcessor by constructor/interface shape.
- Adds framework-level diagnostics for provider hooks, processor discovery, Activity resumes,
  page recognition and rendered fallback injection.
- Keeps the rendered-view path only as a fallback when native model injection has not succeeded.

## 0.2.2

- Reworked the in-QQ ACE toolbox into a rounded card-based interface.
- Added a large cleaner status card and cleaner progress styling.
- Added an SSAID status/value card below the cleaner section.
- Improved confirmation, progress, and completion dialogs.
- Kept QQ native settings-provider injection from 0.2.1.


## 0.2.3

- Expanded safe cache discovery under the host app's private data and app-specific external storage.
- Added WebView/Chromium Cache, Code Cache, GPUCache, CacheStorage, temp, logs, crash logs,
  thumbnails, HTTP/image/video/download cache directory-name discovery.
- Added stronger hard protection for databases, shared preferences, accounts/auth/session,
  messages, contacts, favorites, payment/wallet and backup paths.
- Added failed-delete accounting so occupied/inaccessible cache files are visible after cleanup.
- Kept QQ native settings-provider entry injection and SSAID status display.


## 0.2.4

- Added a QQ-specific selectable cleanup catalog based on publicly visible QFun cleanup-path knowledge.
- Added categories and per-item sizes/check boxes.
- Deep media cleanup is opt-in; sensitive user/account/payment/database paths remain excluded.


## 0.2.5

- Added experimental QFun-inspired Java-like scripting with BeanShell.
- Added `main.java` editor and enable switch in the ACE app.
- Added a Java Script card and explicit "运行脚本" action inside QQ's ACE toolbox.
- Scripts are disabled by default and never auto-run in this version.
- Exposed a small `ace` helper API: log, toast, package name, QQ version, SSAID state/value.
- No message/send/group-event callbacks are registered in v0.2.5.


## 0.2.6

- Added optional `随 QQ 自动执行` for Java scripts.
- Disabled by default.
- When enabled, `main.java` runs once per QQ process after the first Activity resumes.
- Manual execution remains available.
- Auto-run is guarded by an AtomicBoolean so Activity switching cannot repeatedly execute the script.


## 0.2.7

- Added optional BeanShell event callbacks for QQ messages and group changes.
- Receive callback: `onMsg(Object msgData)`.
- Send callback: `onSendMsg(Object msgData)` with `onSend(Object)` fallback.
- Group callback: `onGroupEvent(Object event)`, plus best-effort QFun-style
  `joinGroup`, `quitGroup`, and `shutUpGroup` aliases when IDs can be extracted.
- Message discovery follows QFun's public QQ NT hook observation (`onRecvMsg` /
  `onAddSendMsg`) but uses ACE's independent filtered class-load discovery instead of DexKit.
- Callbacks run on a bounded single worker queue (128 pending events) and never execute BeanShell
  inline on QQ's message hook thread.
- All callback toggles are disabled by default and can be enabled independently.


## 0.2.8

- Merged the former third-page cleanup reports into page 2 (`清理`).
- Page 2 now contains usage, host launch shortcuts, safety policy, and recent cleanup reports.
- Moved all Java scripting controls from Settings to a dedicated page 3 (`脚本`).
- Page 3 now owns main.java editing, auto-run, receive/send/group callbacks, API notes, and script safety notes.
- Settings is again focused on cleanup injection, SSAID, compatibility rules, and module information.


## 0.2.9

- Reworked navigation for future feature growth: `首页 / 工具 / 脚本 / 设置`.
- Renamed page 2 from a cleanup-only concept to a general `工具中心`.
- Added dashboard hero and feature tiles on the home page.
- Added reusable rounded icon containers and larger card radii across MIUI-style rows.
- Kept cleanup reports inside the Tools page.
- Kept Java scripting on its dedicated page and Settings focused on configuration.
- Layout now leaves a clear expansion path for additional independent feature pages/tools.


## 0.3.0

- Replaced the QQ-internal main ACE `AlertDialog` shell with a plain transparent `Dialog`.
  This removes the host theme's rectangular/black AlertDialog panel visible outside ACE's rounded UI.
- Rebuilt the cleanup confirmation dialog with ACE-native rounded cards, estimated-size emphasis,
  safety-protection card, and custom action buttons.
- Rebuilt cleanup progress into a compact rounded progress card.
- Rebuilt the cleanup-complete dialog with a success indicator, released-space emphasis,
  deleted/failed/remaining statistics, and one primary completion button.
- All cleanup dialogs now use the same transparent-window + white rounded-surface styling.


## 0.3.1

- Fixed WeChat settings-entry injection.
- Added both modern `setting_new.MainSettingsUI` and legacy `setting.SettingsUI` candidates.
- WeChat now prefers its native MMPreference model even when ACE compatibility mode is enabled;
  adapter-backed WeChat settings are no longer forced through the rendered LinearLayout fallback.
- Expanded current WeChat settings text fingerprints.
- Added best-effort Preference-screen refresh calls after insertion.
- Added LSPosed framework diagnostics for resumed WeChat Activities and native Preference injection.


## 0.3.2

- Added a WeChat-specific MMPreference/ListView fallback.
- If native WeChat Preference reflection is unavailable, ACE now adds a clickable footer row
  directly to the settings ListView instead of attempting to mutate an adapter-managed layout.
- Added an Instrumentation.callActivityOnResume watcher for WeChat so settings injection does not
  depend on the Activity overriding/calling Activity.onResume.
- Added LSPosed diagnostics for WeChat Instrumentation resume and ListView-footer injection.


## 0.3.3

- WeChat logs proved MainSettingsUI is detected correctly, but the current build exposes neither
  a ListView nor the old stable getPreferenceScreen path.
- Native WeChat Preference injection now discovers PreferenceScreen-like objects through:
  getPreferenceScreen(), zero-argument Activity methods, and Activity instance fields.
- Added stronger diagnostics for each native Preference failure stage.
- Added a one-time WeChat settings view-hierarchy dump when all injection paths fail.
- Kept the v0.3.2 Instrumentation resume watcher and ListView fallback.


## 0.3.4

- Fixed `HostSettingInjector.java` compilation failure caused by a missing
  `java.util.Collections` import in the v0.3.3 one-time WeChat hierarchy-dump set.
- No functional WeChat injection logic was removed; v0.3.3 diagnostics remain intact.


## 0.3.5

- Detailed LSPosed logs identified current WeChat MainSettingsUI as using
  `com.tencent.mm.view.recyclerview.WxRecyclerView`.
- Added a WeChat RecyclerView-aware settings-entry fallback.
- Preferred path inserts ACE after the RecyclerView branch inside the nearest vertical
  LinearLayout so it remains part of the settings scroll hierarchy.
- Secondary path adds a bottom entry to the nearest FrameLayout and reserves RecyclerView
  bottom padding so existing settings are not covered.
- Restricted WeChat settings detection to dedicated SettingsUI/MainSettingsUI classes, preventing
  the previous false-positive injection into LauncherUI/ConversationListView.
- Added RecyclerView parent-chain diagnostics.


## 0.3.6

- Fixed the v0.3.5 Java compile error in the WeChat RecyclerView fallback.
- The discovered RecyclerView remains typed as `View`; `setClipToPadding(false)` now uses a guarded
  `ViewGroup` cast.
- No WeChat injection behavior from v0.3.5 was removed.
