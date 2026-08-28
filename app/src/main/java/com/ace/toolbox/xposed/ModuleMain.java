package com.ace.toolbox.xposed;

import androidx.annotation.NonNull;

import io.github.libxposed.api.XposedModule;

public final class ModuleMain extends XposedModule {
    public ModuleMain() {
        super();
    }

    @Override
    public void onPackageReady(@NonNull PackageReadyParam param) {
        String pkg = param.getPackageName();
        if (!HostPackages.QQ.equals(pkg) && !HostPackages.WECHAT.equals(pkg)) return;
        HostHookInstaller.install(this, param);
    }
}
