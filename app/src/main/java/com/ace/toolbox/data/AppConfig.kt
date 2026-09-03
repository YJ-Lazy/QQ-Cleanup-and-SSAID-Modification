package com.ace.toolbox.data

import android.content.Context

class AppConfig(context: Context) {
    private val p = context.getSharedPreferences("ace_config", 0)

    var cleanEnabled: Boolean
        get() = p.getBoolean("clean_enabled", true)
        set(v) { p.edit().putBoolean("clean_enabled", v).apply() }

    /**
     * Compatibility mode avoids mutating QQ/WeChat's internal Preference model.
     * It is enabled by default because multiple modules may hook the same settings screen.
     */
    var compatibilityMode: Boolean
        get() = p.getBoolean("compat_mode", true)
        set(v) { p.edit().putBoolean("compat_mode", v).apply() }

    var ruleBaseUrl: String
        get() = p.getString("rule_base_url", "") ?: ""
        set(v) { p.edit().putString("rule_base_url", v.trim()).apply() }

    var ssaidEnabled: Boolean
        get() = p.getBoolean("ssaid_enabled", false)
        set(v) { p.edit().putBoolean("ssaid_enabled", v).apply() }

    var ssaidValue: String
        get() = p.getString("ssaid_value", "") ?: ""
        set(v) { p.edit().putString("ssaid_value", v.trim().lowercase()).apply() }

var javaScriptEnabled: Boolean
    get() = p.getBoolean("java_script_enabled", false)
    set(v) {
        val e = p.edit().putBoolean("java_script_enabled", v)
        if (v && !p.contains("java_script_source")) {
            e.putString("java_script_source", DEFAULT_JAVA_SCRIPT)
        }
        e.apply()
    }

var javaScriptSource: String
    get() = p.getString("java_script_source", DEFAULT_JAVA_SCRIPT) ?: DEFAULT_JAVA_SCRIPT
    set(v) { p.edit().putString("java_script_source", v.take(32768)).apply() }

var javaScriptAutoRun: Boolean
    get() = p.getBoolean("java_script_auto_run", false)
    set(v) { p.edit().putBoolean("java_script_auto_run", v).apply() }

var javaScriptReceiveCallback: Boolean
    get() = p.getBoolean("java_script_receive_callback", false)
    set(v) { p.edit().putBoolean("java_script_receive_callback", v).apply() }

var javaScriptSendCallback: Boolean
    get() = p.getBoolean("java_script_send_callback", false)
    set(v) { p.edit().putBoolean("java_script_send_callback", v).apply() }

var javaScriptGroupCallback: Boolean
    get() = p.getBoolean("java_script_group_callback", false)
    set(v) { p.edit().putBoolean("java_script_group_callback", v).apply() }

companion object {
    const val DEFAULT_JAVA_SCRIPT =
        "ace.log(\"ACE Java runtime ready\");\n" +
        "\n" +
        "void onMsg(Object msgData) {\n" +
        "    ace.log(\"收到消息: \" + msgData.text);\n" +
        "}\n" +
        "\n" +
        "void onSendMsg(Object msgData) {\n" +
        "    ace.log(\"发送消息: \" + msgData.text);\n" +
        "}\n" +
        "\n" +
        "void onGroupEvent(Object event) {\n" +
        "    ace.log(\"群事件: \" + event.type);\n" +
        "}\n" +
        "\n" +
        "void joinGroup(String group, String member) { ace.log(\"入群: \" + group + \" / \" + member); }\n" +
        "void quitGroup(String group, String member) { ace.log(\"退群: \" + group + \" / \" + member); }\n" +
        "void shutUpGroup(String group, String member, Long seconds, String operator) {\n" +
        "    ace.log(\"禁言: \" + group + \" / \" + member + \" / \" + seconds);\n" +
        "}\n"
}
}
