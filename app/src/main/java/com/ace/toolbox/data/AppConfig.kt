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
}
