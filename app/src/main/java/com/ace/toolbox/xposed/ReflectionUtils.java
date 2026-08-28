package com.ace.toolbox.xposed;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

final class ReflectionUtils {
    static Method findMethod(Class<?> type, String name, Class<?>... params) {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try {
                Method m = c.getDeclaredMethod(name, params);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) {}
        }
        return null;
    }

    static Method findOneArgMethod(Class<?> type, String name) {
        for (Method m : type.getMethods()) {
            if (m.getName().equals(name) && m.getParameterTypes().length == 1) return m;
        }
        for (Method m : type.getDeclaredMethods()) {
            if (m.getName().equals(name) && m.getParameterTypes().length == 1) {
                m.setAccessible(true);
                return m;
            }
        }
        return null;
    }

    static Object constructPreference(Class<?> preferenceClass, android.content.Context context) throws Exception {
        for (Constructor<?> ctor : preferenceClass.getDeclaredConstructors()) {
            Class<?>[] p = ctor.getParameterTypes();
            if (p.length >= 1 && android.content.Context.class.isAssignableFrom(p[0])) {
                ctor.setAccessible(true);
                Object[] args = new Object[p.length];
                args[0] = context;
                for (int i = 1; i < p.length; i++) {
                    if (p[i] == int.class) args[i] = 0;
                    else if (p[i] == boolean.class) args[i] = false;
                    else args[i] = null;
                }
                return ctor.newInstance(args);
            }
        }
        throw new NoSuchMethodException("No Context constructor: " + preferenceClass);
    }

    private ReflectionUtils() {}
}
