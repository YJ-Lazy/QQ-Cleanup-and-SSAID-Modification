package com.ace.toolbox.xposed;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Stable wrapper passed to BeanShell callbacks. Public fields make BeanShell access convenient.
 */
public final class AceScriptMessageEvent {
    public final String direction;
    public final int chatType;
    public final String peerUid;
    public final String peerUin;
    public final String senderUid;
    public final String senderUin;
    public final long msgId;
    public final long time;
    public final String text;
    public final Object raw;

    private AceScriptMessageEvent(
            String direction, int chatType, String peerUid, String peerUin,
            String senderUid, String senderUin, long msgId, long time,
            String text, Object raw
    ) {
        this.direction = direction;
        this.chatType = chatType;
        this.peerUid = peerUid;
        this.peerUin = peerUin;
        this.senderUid = senderUid;
        this.senderUin = senderUin;
        this.msgId = msgId;
        this.time = time;
        this.text = text;
        this.raw = raw;
    }

    static AceScriptMessageEvent from(String direction, Object record) {
        if (record == null) {
            return new AceScriptMessageEvent(direction, 0, "", "", "", "", 0L, 0L, "", null);
        }
        return new AceScriptMessageEvent(
                direction,
                intValue(read(record, "chatType", "getChatType")),
                stringValue(read(record, "peerUid", "getPeerUid")),
                stringValue(read(record, "peerUin", "getPeerUin")),
                stringValue(read(record, "senderUid", "getSenderUid")),
                stringValue(read(record, "senderUin", "getSenderUin")),
                longValue(read(record, "msgId", "getMsgId")),
                longValue(read(record, "msgTime", "getMsgTime", "time", "getTime")),
                extractText(record),
                record
        );
    }

    private static String extractText(Object record) {
        Object direct = read(record, "msg", "getMsg", "msgText", "getMsgText", "content", "getContent");
        if (direct instanceof CharSequence) return direct.toString();

        Object elements = read(record, "elements", "getElements");
        if (!(elements instanceof Collection)) return "";

        StringBuilder out = new StringBuilder();
        for (Object element : (Collection<?>) elements) {
            if (element == null) continue;
            Object textElement = read(element, "textElement", "getTextElement");
            Object content = textElement == null
                    ? read(element, "content", "getContent")
                    : read(textElement, "content", "getContent");
            if (content instanceof CharSequence) {
                if (out.length() > 0) out.append(' ');
                out.append(content);
            }
        }
        return out.toString();
    }

    private static Object read(Object obj, String... names) {
        Class<?> c = obj.getClass();
        for (String name : names) {
            try {
                Method m = c.getMethod(name);
                if (m.getParameterTypes().length == 0) {
                    m.setAccessible(true);
                    return m.invoke(obj);
                }
            } catch (Throwable ignored) {}
            try {
                Field f = c.getField(name);
                f.setAccessible(true);
                return f.get(obj);
            } catch (Throwable ignored) {}
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(obj);
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private static String stringValue(Object v) {
        return v == null ? "" : String.valueOf(v);
    }

    private static int intValue(Object v) {
        return v instanceof Number ? ((Number) v).intValue() : 0;
    }

    private static long longValue(Object v) {
        return v instanceof Number ? ((Number) v).longValue() : 0L;
    }

    @Override
    public String toString() {
        return "AceScriptMessageEvent{" + direction + ", chatType=" + chatType
                + ", peer=" + (peerUin.isEmpty() ? peerUid : peerUin)
                + ", text=" + text + "}";
    }
}
