# Hook rule server layout

```text
ace-rules/
├── qq/
│   ├── default.json
│   └── 9.1.85.json
└── wechat/
    ├── default.json
    └── 8.0.60.json
```

Rules are data only. v0.1 rejects classes outside `com.tencent.*` and rejects hook methods other than `onCreate`.
