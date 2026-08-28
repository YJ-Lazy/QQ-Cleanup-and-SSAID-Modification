package com.ace.toolbox.data

import android.content.Context

class AppConfig(context: Context) {
    private val p = context.getSharedPreferences("ace_config", 0)
    var cleanEnabled: Boolean
        get() = p.getBoolean("clean_enabled", true)
        set(v) { p.edit().putBoolean("clean_enabled", v).apply() }
    var ruleBaseUrl: String
        get() = p.getString("rule_base_url", "") ?: ""
        set(v) { p.edit().putString("rule_base_url", v.trim()).apply() }
    var ssaidEnabled: Boolean
        get() = p.getBoolean("ssaid_enabled", false)
        set(v) { p.edit().putBoolean("ssaid_enabled", v).apply() }
    var ssaidValue: String
        get() = p.getString("ssaid_value", "") ?: ""
        set(v) { p.edit().putString("ssaid_value", v.trim().lowercase()).apply() }
}
