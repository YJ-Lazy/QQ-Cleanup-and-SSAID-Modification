# QFun reference

Public project reviewed while implementing ACE's QQ-specific features:

- Project: `oneQAQone/QFun`
- Repository: https://github.com/oneQAQone/QFun
- License observed in the repository: GNU GPL v3

## Cleanup-path reference

ACE 0.2.4/0.2.5 expands QQ cleanup coverage using the high-level idea of an explicit,
user-selectable QQ cache-path catalog. The ACE implementation is independently written and keeps
stronger deny rules for databases, account/auth/session data, messages, contacts, favorites,
wallet/payment, backups, and shared preferences.

Potentially user-visible media caches such as chat images, video, voice/PTT and thumbnails are
marked as deep-clean items and are not selected by default.

## Java-script architecture reference

QFun's public plugin implementation uses BeanShell `Interpreter`, loads a `main.java`, exposes
host variables/APIs, and can register callbacks such as receive/send message and group events.

ACE 0.2.5 implements only the first, safer subset:

- BeanShell Java-like `main.java` source;
- feature disabled by default;
- manual execution only from the QQ-internal ACE toolbox;
- no automatic startup execution;
- no receive/send-message, group-event, or menu callbacks;
- a small helper object `ace` for log/toast/basic host information.

No QFun source file is copied into ACE.
