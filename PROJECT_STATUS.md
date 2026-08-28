# Project status

Current source version: 0.1.3

Static checks completed:
- Required-file layout: PASS
- XML parse: PASS
- Placeholder/TODO scan: PASS at initial handoff
- Modern Xposed metadata: API 102 / static scope QQ + WeChat

Build-fix history from user-side Gradle compilation:
- 0.1.1: resolved `androidx.annotation` version conflict by aligning to 1.8.0.
- 0.1.2: replaced unavailable `Icons.Rounded.Database` with `Icons.Rounded.Storage`.
- 0.1.3: fixed `AlertDialog.setButton` listener lambda signature in `HostCleanerDialog.java`.

The latest project should be validated with:

```bash
./gradlew clean assembleDebug
```

or on Windows:

```bat
gradlew.bat clean assembleDebug
```
