# QQEnhancedBypass compatibility reference

ACE QQ/WeChat Toolbox uses `Xalsace/QQEnhancedBypass` and the user-maintained mirror `YJ-Lazy/QQEnhancedBypass-` as compatibility research references.

The upstream project is GPL-3.0 licensed. ACE does not copy the upstream bypass implementation into this compatibility layer. Instead, ACE independently implements read-only diagnostics and version-adaptation structure inspired by observable project organization and documented compatibility targets.

## Integrated ideas

- modular QQ compatibility probing
- centralized compatibility diagnostics
- version-sensitive class availability checks
- safe degradation when QQ implementation details change
- room for future dynamic target location for ACE-owned features

## Explicitly not integrated

ACE does not implement security-evasion behavior from the reference project, including hiding Root/LSPosed/Frida traces, filtering `/proc/self/maps`, spoofing debugger state, bypassing signature checks, or blocking security/risk telemetry.

## Current probe targets

The compatibility probe checks for selected classes documented by the QQEnhancedBypass 9.3.50 adaptation branch and ACE's own QQ settings integration. Probe results are informational only and never modify the target class or its return values.
