# ACE QQ/微信工具箱 v0.1

这是一个参考 FunBox **模块组织、宿主注入和功能分组思路**重新实现的初版 LSPosed 工程；不包含 FunBox 的私有源码、资源或 native `.so`。

## 已实现

- Modern LSPosed / libxposed **API 102**：`XposedModule`、`META-INF/xposed/java_init.list`、`module.prop`、`scope.list`。
- 静态作用域：`com.tencent.mobileqq`、`com.tencent.mm`。
- QQ 设置页候选：`com.tencent.mobileqq.activity.QPublicFragmentActivity`。
- 微信设置页候选：`com.tencent.mm.plugin.setting.ui.setting_new.MainSettingsUI`。
- 原生 Preference 反射注入：优先复用宿主 PreferenceScreen / Preference 类；失败时才使用 View 行兜底。
- 安全 Cleaner：扫描大小、文件数、识别 QQ/微信用户目录、进度回调、二次确认、删除统计。
- 防误删：数据库扩展名和 message/contact/favorite/account/auth 路径硬保护。
- 清理报告：宿主进程通过受 UID 白名单保护的 ContentProvider 回传到模块 App，首页可查看最近 30 条。
- Hook 规则：内置 JSON + 按目标版本缓存 + 可配置 HTTPS 远程规则。
- QQ SSAID：可选、默认关闭；仅在 QQ scope 内替换 `Settings.Secure.ANDROID_ID`，值通过 Provider 安全读取。
- Material You + MIUI/HyperOS 风格 Compose UI：首页 / 清理 / 报告 / 设置四页。
- ACE 自适应图标：使用项目中提供的 ACE 图标重新制作 foreground/background。

## Java 脚本（实验）

0.2.5 起加入 QFun 插件架构启发的 BeanShell Java-like 脚本功能：

- 在 ACE App「设置 → Java 脚本 · 实验」中启用并编辑 `main.java`；
- 在 QQ「设置 → ACE 工具箱」中手动点击「运行脚本」；
- 默认关闭；可选择“随 QQ 自动执行”，开启后每个 QQ 进程自动运行一次；
- 当前不注册消息接收/发送、群事件或菜单事件回调；
- 可使用 `ace.log(...)`、`ace.toast(...)`、`ace.getHostVersion()`、
  `ace.getPackageName()`、`ace.getSsaid()`、`ace.isSsaidEnabled()`。

示例：

```java
ace.log("hello");
ace.toast("ACE Java 脚本运行成功");
return ace.getHostVersion();
```

脚本运行在 QQ 进程中，只应执行自己编写或审计过的代码。

## API 102 元数据

`app/src/main/resources/META-INF/xposed/module.prop`

```properties
minApiVersion=102
targetApiVersion=102
staticScope=true
exceptionMode=protective
autoHotReload=false
```

Modern API 102 **不混用** legacy `de.robv.android.xposed.*`。

## 在线规则格式

设置页填写基础 URL，例如：

```text
https://example.com/ace-rules
```

模块会尝试：

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

v0.1 对远程规则做了最小能力限制：只允许 `com.tencent.*` 类名以及 `onCreate`，避免规则源获得任意 Hook 能力。

## 编译

要求：JDK 17、Android SDK 35、联网解析 Maven 依赖。

Linux/macOS：

```bash
./gradlew assembleDebug
```

Windows：

```bat
gradlew.bat assembleDebug
```

本仓库的 `gradlew` / `gradlew.bat` 是自举脚本，会下载 Gradle 8.7，因此不依赖缺失的 `gradle-wrapper.jar`。

APK 输出：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 使用

1. 编译并安装 APK。
2. 在支持 Modern API 102 的 LSPosed 中启用模块，作用域保持 QQ / 微信。
3. 强制停止 QQ/微信并重新打开。
4. 进入 QQ 设置或微信设置，寻找「ACE 工具箱」。
5. 先扫描，确认后执行安全清理。

## 当前兼容边界

QQ/微信内部设置实现经常变化。v0.1 采用“内置候选 + 远程版本规则 + 原生 Preference 反射 + View 兜底”四层策略。它不是 DexKit 自动反混淆器；如果目标版本彻底更换设置类，需要新增规则。此设计故意避免在未经验证的版本上扫描并 Hook 任意类。

## 第三方参考

- FunBox_v2184.apk：仅用于观察模块组织和 UI/交互方向，不复制代码或 native 实现。
- YJ-Lazy/ssaid-qq：公开 MIT 项目；本项目的 QQ-only SSAID 功能在设计上参考其公开实现思路。请保留 `NOTICE.md`。

## 0.1.5

- 新增基于 SecureRandom 的 16 位随机 SSAID 生成功能。
- 默认兼容模式，降低与 FunBox 等设置注入模块同时启用时的冲突概率。


## 0.1.8

- **0.1.8**：撤销 0.1.7 将 ACE 清理入口移动到 QQ 顶部“+”位置的实验，恢复兼容模式下右下角独立「ACE 清理」入口。保留 0.1.6 的扫描/清理按钮修复与 0.1.5 的随机 SSAID 功能。


## 0.1.9 设置菜单入口

根据 FunBox APK 的界面/资源结构进行重新参考后，本版本不再使用右下角悬浮清理按钮作为默认入口。

兼容模式下：
- QQ 的 `QPublicFragmentActivity` 只被当作通用容器，不再看到该 Activity 就直接注入；
- 必须同时识别「设置」标题及多个设置项文本，避免在 QQ 主界面误显示；
- 在已渲染的设置页中寻找垂直菜单容器，并把「ACE 工具箱」作为普通菜单行插入；
- 菜单行包含圆形 A 图标、标题、说明和右箭头，点击打开安全清理；
- 不直接调用宿主 `PreferenceScreen.addPreference()`，以降低与 FunBox 等模块共存时的冲突概率；
- 若关闭兼容模式，仍保留原生 Preference 反射注入路径。

FunBox 仅用于设置入口的布局/交互思路参考，不复制其私有代码、资源或 native 实现。


## Java 脚本事件回调

0.2.7 起可在 ACE App 中独立启用三类 QQ 回调：

```java
void onMsg(Object msgData) {
    ace.log("收到: " + msgData.text);
}

void onSendMsg(Object msgData) {
    ace.log("发送: " + msgData.text);
}

void onGroupEvent(Object event) {
    ace.log("群事件: " + event.type);
}
```

`msgData` 为 ACE 的稳定包装对象，包含 `direction`、`chatType`、`peerUid`、
`peerUin`、`senderUid`、`senderUin`、`msgId`、`time`、`text` 和 `raw`。

群事件提供 `type`（`join` / `quit` / `shutup`）、`troopUin`、`memberUin`、
`operatorUin`、`durationSeconds` 和 `rawArgs`。由于 QQ 推送处理类会随版本混淆，
`onGroupEvent` 是稳定入口；只有能可靠提取 ID 时才额外调用
`joinGroup` / `quitGroup` / `shutUpGroup`。

回调通过独立的有界单线程队列执行，不在 QQ Hook 线程内运行 BeanShell。
