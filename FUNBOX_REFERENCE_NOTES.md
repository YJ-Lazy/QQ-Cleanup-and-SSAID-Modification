# FunBox reference notes

Reference APK: `FunBox_v2184.apk`

Observed package characteristics used for UI/architecture reference:
- Xposed entry asset points to `fun.box001.loader.XPEntry`.
- Resource table contains FunBox-specific setting/UI identifiers such as:
  `FunBoxCheckBoxStyle`, `FunBoxContextTheme`, `FunBoxContextThemeOverlay`,
  `group_qq`, `main_info_icon_qq`, `btn_open_settings`, `iv_settings`,
  and `ll_group_float_icon_setting`.
- The APK contains native loader/hook libraries, so its host-injection implementation is not
  reproduced here.

ACE v0.1.9 only borrows the high-level interaction idea: present the module as a normal option
inside the host's settings menu. It does not copy FunBox private code, resources, or native binaries.
