package com.ace.toolbox.xposed;

import java.util.ArrayList;
import java.util.List;

/** Generic group-change event passed to BeanShell. */
public final class AceScriptGroupEvent {
    public final String type;
    public final String troopUin;
    public final String memberUin;
    public final String operatorUin;
    public final long durationSeconds;
    public final Object[] rawArgs;

    AceScriptGroupEvent(
            String type, String troopUin, String memberUin,
            String operatorUin, long durationSeconds, Object[] rawArgs
    ) {
        this.type = type;
        this.troopUin = troopUin == null ? "" : troopUin;
        this.memberUin = memberUin == null ? "" : memberUin;
        this.operatorUin = operatorUin == null ? "" : operatorUin;
        this.durationSeconds = durationSeconds;
        this.rawArgs = rawArgs == null ? new Object[0] : rawArgs;
    }

    static AceScriptGroupEvent bestEffort(String type, Object[] args) {
        List<String> strings = new ArrayList<>();
        long duration = 0L;
        if (args != null) {
            for (Object arg : args) {
                if (arg instanceof CharSequence) strings.add(arg.toString());
                if (arg instanceof Number) duration = ((Number) arg).longValue();
            }
        }
        String troop = strings.size() > 0 ? strings.get(0) : "";
        String member = strings.size() > 1 ? strings.get(1) : "";
        String operator = strings.size() > 2 ? strings.get(2) : "";
        return new AceScriptGroupEvent(type, troop, member, operator, duration, args);
    }

    @Override
    public String toString() {
        return "AceScriptGroupEvent{" + type + ", troop=" + troopUin
                + ", member=" + memberUin + ", operator=" + operatorUin + "}";
    }
}
