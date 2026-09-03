# LSPosed diagnostics — 0.2.0

This build fixes a packaging identity problem from earlier debug builds:

- Before: debug APK used `com.ace.toolbox.debug`.
- Now: debug and release both use `com.ace.toolbox`.

Why it matters:
LSPosed enables modules by the installed module package. A debug `applicationIdSuffix`
creates a different module identity, so enabling `com.ace.toolbox` does not enable
`com.ace.toolbox.debug`.

Expected LSPosed module log after launching QQ:

```text
ACE: ACE 0.2.0 loaded; process=com.tencent.mobileqq; ...
ACE: onPackageLoaded pkg=com.tencent.mobileqq; ...
ACE-Hook: Activity.onResume watcher installed for com.tencent.mobileqq
ACE: onPackageReady pkg=com.tencent.mobileqq; ...
```

If these lines are absent, the problem is module activation/scope/install identity rather than
the settings-menu injection code.

For the first 0.2.0 install, remove any old `com.ace.toolbox.debug` build to avoid keeping two
different ACE apps installed at the same time, then install the newly built 0.2.0 APK and enable
ACE for QQ in LSPosed.


## 0.2.1 expected QQ-setting diagnostics

After opening QQ Settings, the framework log should now show one of these native-provider lines:

```text
ACE-QQSettings: Hooked provider method: ...
ACE-QQSettings: Simple item processor selected: ...
ACE-QQSettings: ACE native settings entry injected; ...
```

If the current QQ version is too new for the built-in processor-name discovery, the log will instead say:

```text
ACE-QQSettings: Could not locate QQ SimpleItemProcessor-compatible class
```

The rendered fallback is also logged through LSPosed now:

```text
ACE-Inject: Page probe: ...; settings=true
ACE-Inject: Rendered fallback menu row inserted ...
```

This makes the next compatibility failure actionable without relying on logcat.
