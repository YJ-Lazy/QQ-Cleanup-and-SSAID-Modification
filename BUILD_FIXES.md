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

- Added compatibility mode, enabled by default.
- Compatibility mode no longer mutates QQ/WeChat's internal `PreferenceScreen`; it uses an isolated `Activity.addContentView()` cleanup entry instead.
- Added an `Activity.onResume` fallback watcher with delayed page checks so the cleanup entry can return after QQ/WeChat fragment redraws.
- Added page/class recognition fallback for settings screens.
- SSAID now installs the `Settings.Secure.ANDROID_ID` hook only when the SSAID feature is explicitly enabled.
- Added a Settings toggle for compatibility mode; native Preference injection remains available as an opt-in path.
