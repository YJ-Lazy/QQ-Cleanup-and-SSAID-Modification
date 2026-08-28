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
