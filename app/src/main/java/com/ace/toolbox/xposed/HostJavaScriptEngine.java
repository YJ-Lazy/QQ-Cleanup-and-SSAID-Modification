package com.ace.toolbox.xposed;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.util.Log;
import android.widget.Toast;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import bsh.BshMethod;
import bsh.Interpreter;

/**
 * Persistent BeanShell runtime used by manual execution, optional auto-run and QQ event callbacks.
 *
 * Event callbacks never execute on QQ's hook thread. A bounded single-thread queue serializes the
 * interpreter and drops the oldest pending event if scripts cannot keep up.
 */
final class HostJavaScriptEngine {
    private static final String TAG = "ACE-JavaScript";
    private static final int MAX_SCRIPT_LENGTH = 32768;
    private static final AtomicBoolean AUTO_RUN_STARTED = new AtomicBoolean(false);
    private static final Object RUNTIME_LOCK = new Object();

    private static final ThreadPoolExecutor EVENT_EXECUTOR = new ThreadPoolExecutor(
            1, 1, 30L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(128),
            r -> {
                Thread t = new Thread(r, "ACE-script-events");
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.DiscardOldestPolicy()
    );

    private static volatile Activity currentActivity;
    private static Interpreter interpreter;
    private static String loadedSource = "";

    static final class RunResult {
        final boolean success;
        final String message;

        RunResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    static void attachActivity(Activity activity) {
        if (activity != null && HostPackages.QQ.equals(activity.getPackageName())) {
            currentActivity = activity;
        }
    }

    static void maybeAutoRun(Activity activity) {
        attachActivity(activity);
        if (activity == null) return;
        if (!HostConfig.javaScriptEnabled(activity)) return;
        if (!HostConfig.javaScriptAutoRun(activity)) return;
        if (!AUTO_RUN_STARTED.compareAndSet(false, true)) return;

        Runnable task = () -> {
            RunResult result = initialize(activity, false);
            if (result.success) {
                Log.i(TAG, "Auto-run initialized runtime: " + result.message);
            } else {
                Log.e(TAG, "Auto-run failed: " + result.message);
            }
        };
        try {
            android.view.View decor = activity.getWindow().getDecorView();
            if (decor != null) decor.postDelayed(task, 1200L);
            else activity.runOnUiThread(task);
        } catch (Throwable t) {
            Log.e(TAG, "Unable to schedule auto-run", t);
        }
    }

    /** Manual execution reloads main.java so saved edits take effect immediately. */
    static RunResult run(Activity activity) {
        attachActivity(activity);
        return initialize(activity, true);
    }

    static void dispatchReceive(Object msgRecord) {
        Activity a = currentActivity;
        if (!callbacksReady(a) || !HostConfig.javaScriptReceiveCallback(a)) return;
        AceScriptMessageEvent event = AceScriptMessageEvent.from("receive", msgRecord);
        EVENT_EXECUTOR.execute(() -> invokeEventMethod("onMsg", event));
    }

    static void dispatchSend(Object msgRecord) {
        Activity a = currentActivity;
        if (!callbacksReady(a) || !HostConfig.javaScriptSendCallback(a)) return;
        AceScriptMessageEvent event = AceScriptMessageEvent.from("send", msgRecord);
        EVENT_EXECUTOR.execute(() -> {
            // ACE-specific callback first. `getMsg(String)` from QFun is intentionally not used
            // to mutate QQ's outgoing object in this compatibility-first release.
            if (!invokeEventMethod("onSendMsg", event)) {
                invokeEventMethod("onSend", event);
            }
        });
    }

    static void dispatchGroup(AceScriptGroupEvent event) {
        Activity a = currentActivity;
        if (!callbacksReady(a) || !HostConfig.javaScriptGroupCallback(a) || event == null) return;
        EVENT_EXECUTOR.execute(() -> {
            invokeEventMethod("onGroupEvent", event);
            // QFun-style aliases are invoked only when the important IDs were extracted.
            // onGroupEvent(event) always receives the rawArgs for newer/obfuscated QQ builds.
            if ("join".equals(event.type)
                    && !event.troopUin.isEmpty() && !event.memberUin.isEmpty()) {
                invokeTyped("joinGroup",
                        new Class[]{String.class, String.class},
                        new Object[]{event.troopUin, event.memberUin});
            } else if ("quit".equals(event.type)
                    && !event.troopUin.isEmpty() && !event.memberUin.isEmpty()) {
                invokeTyped("quitGroup",
                        new Class[]{String.class, String.class},
                        new Object[]{event.troopUin, event.memberUin});
            } else if ("shutup".equals(event.type)
                    && !event.troopUin.isEmpty()) {
                invokeTyped("shutUpGroup",
                        new Class[]{String.class, String.class, Long.class, String.class},
                        new Object[]{
                                event.troopUin, event.memberUin,
                                event.durationSeconds, event.operatorUin
                        });
            }
        });
    }

    private static boolean callbacksReady(Activity activity) {
        if (activity == null || activity.isDestroyed()) return false;
        if (!HostConfig.javaScriptEnabled(activity)) return false;
        RunResult init = initialize(activity, false);
        if (!init.success) {
            Log.e(TAG, "Callback runtime unavailable: " + init.message);
            return false;
        }
        return true;
    }

    private static RunResult initialize(Activity activity, boolean forceReload) {
        if (activity == null) return new RunResult(false, "QQ Activity 尚未就绪");
        if (!HostConfig.javaScriptEnabled(activity)) {
            return new RunResult(false, "Java 脚本功能未启用");
        }

        String source = HostConfig.javaScriptSource(activity);
        if (source == null || source.trim().isEmpty()) {
            return new RunResult(false, "脚本内容为空，请先在 ACE App 中编辑并保存");
        }
        if (source.length() > MAX_SCRIPT_LENGTH) {
            return new RunResult(false, "脚本超过 32 KiB 限制");
        }

        synchronized (RUNTIME_LOCK) {
            if (!forceReload && interpreter != null && source.equals(loadedSource)) {
                return new RunResult(true, "脚本运行时已就绪");
            }
            try {
                Interpreter next = new Interpreter();
                AceScriptApi api = new AceScriptApi(activity);
                next.set("ace", api);
                next.set("context", activity);
                next.set("classLoader", activity.getClassLoader());
                next.set("packageName", activity.getPackageName());
                next.set("hostVersion", api.getHostVersion());
                next.set("ssaid", HostConfig.ssaidValue(activity));
                next.setClassLoader(activity.getClassLoader());

                Object result = next.eval(source);
                interpreter = next;
                loadedSource = source;
                return new RunResult(
                        true,
                        result == null ? "执行完成，回调运行时已加载" : String.valueOf(result)
                );
            } catch (Throwable t) {
                Log.e(TAG, "Script initialization failed", t);
                return new RunResult(false, errorMessage(t));
            }
        }
    }

    private static boolean invokeEventMethod(String name, Object event) {
        return invokeTyped(name, new Class[]{Object.class}, new Object[]{event});
    }

    private static boolean invokeTyped(String name, Class<?>[] types, Object[] args) {
        synchronized (RUNTIME_LOCK) {
            Interpreter bsh = interpreter;
            if (bsh == null) return false;
            try {
                BshMethod method = bsh.getNameSpace().getMethod(name, types);
                if (method == null) return false;
                method.invoke(args, bsh);
                return true;
            } catch (Throwable t) {
                Log.e(TAG, "Callback " + name + " failed", t);
                return false;
            }
        }
    }

    private static String errorMessage(Throwable t) {
        String m = t.getMessage();
        return m == null || m.trim().isEmpty() ? t.getClass().getSimpleName() : m;
    }

    public static final class AceScriptApi {
        private final Activity activity;

        AceScriptApi(Activity activity) {
            this.activity = activity;
        }

        public void log(String message) {
            Log.i(TAG, String.valueOf(message));
        }

        public void toast(String message) {
            final String text = String.valueOf(message);
            activity.runOnUiThread(() ->
                    Toast.makeText(activity, text, Toast.LENGTH_SHORT).show());
        }

        public String getPackageName() {
            return activity.getPackageName();
        }

        public String getHostVersion() {
            try {
                PackageInfo p = activity.getPackageManager()
                        .getPackageInfo(activity.getPackageName(), 0);
                return p.versionName == null ? "?" : p.versionName;
            } catch (Throwable ignored) {
                return "?";
            }
        }

        public String getSsaid() {
            return HostConfig.ssaidValue(activity);
        }

        public boolean isSsaidEnabled() {
            return HostConfig.ssaidEnabled(activity);
        }
    }

    private HostJavaScriptEngine() {}
}
