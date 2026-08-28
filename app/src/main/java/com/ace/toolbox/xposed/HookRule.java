package com.ace.toolbox.xposed;

import java.util.List;

final class HookRule {
    final int schema;
    final String packageName;
    final String version;
    final String methodName;
    final List<String> settingClasses;

    HookRule(int schema, String packageName, String version, String methodName, List<String> settingClasses) {
        this.schema = schema;
        this.packageName = packageName;
        this.version = version;
        this.methodName = methodName;
        this.settingClasses = settingClasses;
    }
}
