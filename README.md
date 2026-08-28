# QQ Cleanup and SSAID Modification

一个面向 QQ / 微信的 Modern LSPosed 工具箱初版，当前版本 **0.1.3**。项目参考 FunBox 的模块组织、宿主注入和功能分组思路重新实现，不包含或分发 FunBox 的私有源码、资源或 native `.so`。

## 当前功能

- Modern LSPosed / libxposed **API 102**：`XposedModule`、`META-INF/xposed/java_init.list`、`module.prop`、`scope.list`。
- 静态作用域：`com.tencent.mobileqq`、`com.tencent.mm`。
- QQ 设置页候选：`com.tencent.mobileqq.activity.QPublicFragmentActivity`。
- 微信设置页候选：`com.tencent.mm.plugin.setting.ui.setting_new.MainSettingsUI`。
- 原生 Preference 反射注入：优先复用宿主 PreferenceScreen / Preference；失败时使用 View 行兜底。
- Cleaner：扫描大小、文件数、QQ/微信用户目录识别、清理进度、二次确认、删除统计。
- 防误删：数据库扩展名和 message/contact/favorite/account/auth 路径硬保护。
- 清理报告：宿主进程通过受 UID 白名单保护的 ContentProvider 回传到模块 App，可查看最近 30 条。
- Hook 规则：内置 JSON + 按目标版本缓存 + 可配置 HTTPS 远程规则。
- QQ SSAID：可选且默认关闭；仅在 QQ scope 内替换 `Settings.Secure.ANDROID_ID`。
- Material You + MIUI/HyperOS 风格 Compose UI：首页 / 清理 / 报告 / 设置。
- ACE 自适应图标。

## SSAID 来源与许可声明

本仓库中的 **QQ SSAID 功能部分基于 / 改编自 `YJ-Lazy/ssaid-qq` 的公开实现思路**：

- 上游仓库：<https://github.com/YJ-Lazy/ssaid-qq>
- 上游作者：**YJ-Lazy**
- 上游许可：**MIT License**
- 上游版权：`Copyright (c) 2026 YJ-Lazy`

当前项目保留了对应来源说明，并在 `third_party/ssaid-qq-LICENSE.txt` 中附带上游 MIT License 全文。上游 `ssaid-qq` 自身的 NOTICE 还说明其基于 / 受 `YJ-Lazy/SSaidHook` 启发；本仓库在 `NOTICE.md` 中保留了这一来源链。

除 SSAID 部分的上述来源外，本项目的 Cleaner、宿主设置注入、规则管理和 Compose UI 为本工程独立组织实现。

## API 102 元数据

`app/src/main/resources/META-INF/xposed/module.prop`

```properties
minApiVersion=102
targetApiVersion=102
staticScope=true
exceptionMode=protective
autoHotReload=false
```

目标 API 102，不混用 legacy `de.robv.android.xposed.*`。

## 在线规则格式

设置页可以填写 HTTPS 基础 URL，例如：

```text
https://example.com/ace-rules
```

模块依次尝试：

```text
{base}/qq/{QQ版本}.json
{base}/qq/default.json
{base}/wechat/{微信版本}.json
{base}/wechat/default.json
```

示例：

```json
{
  "schema": 1,
  "package": "com.tencent.mobileqq",
  "version": "9.x",
  "settings": ["com.tencent.mobileqq.activity.QPublicFragmentActivity"],
  "method": "onCreate"
}
```

远程规则做最小能力限制：仅允许 `com.tencent.*` 类名和受支持方法，避免规则源获得任意 Hook 能力。

## 编译

要求：

- JDK 17
- Android SDK 35
- 可访问 Google / Maven Central

Windows：

```bat
gradlew.bat clean assembleDebug
```

Linux / macOS：

```bash
./gradlew clean assembleDebug
```

Debug APK：

```text
app/build/outputs/apk/debug/app-debug.apk
```

仓库自带 GitHub Actions：`.github/workflows/build.yml`。

## 使用

1. 编译并安装 APK。
2. 在支持 Modern API 102 的 LSPosed 中启用模块，作用域保持 QQ / 微信。
3. 强制停止 QQ / 微信并重新打开。
4. 进入 QQ 设置或微信设置，寻找「ACE 工具箱」。
5. 先扫描，确认清理项后执行安全清理。
6. SSAID 功能默认关闭；需要时在模块 App 中单独开启并设置 16 位十六进制值。

## 当前兼容边界

QQ / 微信内部设置实现经常变化。当前采用：

1. 内置候选类；
2. 远程版本规则；
3. 原生 Preference 反射；
4. View 兜底。

当前不是 DexKit 自动反混淆器。如果目标版本彻底更换设置实现，需要补充对应规则。

## 构建修复记录

- **0.1.1**：将 `androidx.annotation` 与当前依赖图对齐到 `1.8.0`。
- **0.1.2**：将不可用的 `Icons.Rounded.Database` 替换为 `Icons.Rounded.Storage`。
- **0.1.3**：修复 `AlertDialog.setButton` 的 Java Lambda 参数签名。

## 第三方参考

- `FunBox_v2184.apk`：仅用于模块组织、UI/交互方向参考，不复制其代码、资源或 native 实现。
- `YJ-Lazy/ssaid-qq`：SSAID 功能来源/改编参考，MIT License。详见 `NOTICE.md` 和 `third_party/ssaid-qq-LICENSE.txt`。
