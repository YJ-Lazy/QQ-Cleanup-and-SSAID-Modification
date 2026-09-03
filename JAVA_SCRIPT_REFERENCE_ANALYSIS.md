# Java script reference analysis

Reference APKs supplied for local static analysis:

- `QFun_1.3.3.apk.1`
- `FkWeChat_1.2.6.apk.1`

Observed from APK/Dex strings:

## QFun

- Contains BeanShell (`Interpreter`, `BeanshellExecutable`, BeanShell 3.x strings).
- Uses a `main.java` entry script.
- Has a Java plugin architecture with `PluginManager`, `PluginCompiler`, `PluginCallback`,
  `PluginMethod`, local/online plugin UI and reload/enable operations.
- Exposes host-oriented callback concepts including receive/send message and group/event APIs.
- Uses host `classLoader` plumbing and additional plugin loaders.

## FkWeChat

- Also bundles BeanShell classes.
- Contains `main.java`, `PluginManager`, `pluginSdk`, `pluginInfo`, `pluginPath`.
- Contains explicit `loadJava`/plugin loading strings and helper functions for loading
  Dex/Jar/AAR/class loaders.
- Includes event-dispatch related plugin strings.

## ACE 0.2.5 design

ACE uses those projects only as architectural reference and implements an independent first-stage
Java-like scripting feature:

- BeanShell Maven dependency (`org.apache-extras.beanshell:bsh:2.0b6`);
- feature disabled by default;
- editable `main.java` source stored in ACE configuration;
- manual execution from the QQ-internal ACE toolbox;
- exposed script variables: `ace`, `context`, `classLoader`, `packageName`, `hostVersion`, `ssaid`;
- `ace` helpers: `log`, `toast`, `getPackageName`, `getHostVersion`, `getSsaid`,
  `isSsaidEnabled`;
- 32 KiB script limit;
- no automatic startup execution;
- no receive/send-message or group-event callbacks yet.

This avoids silently executing third-party scripts while the basic interpreter/classloader path is
being validated on real QQ builds.


## ACE 0.2.7 callback layer

QFun's public callback layer maps its message listener to `onMsg(Object)` and group listeners to
`joinGroup`, `quitGroup`, and `shutUpGroup`. Its QQ NT receive hook observes the message-service
methods `onRecvMsg` and `onAddSendMsg`.

ACE 0.2.7 independently implements:
- filtered discovery of QQ NT message-service classes by those method names;
- receive and send observation callbacks;
- semantic discovery of QQ NT troop/group push processors;
- a stable `onGroupEvent(Object)` wrapper for version-dependent group push payloads;
- QFun-style group callback aliases only when IDs are available;
- bounded off-hook-thread callback execution.

ACE does not copy QFun's DexKit tasks, callback classes, or parser implementation.
