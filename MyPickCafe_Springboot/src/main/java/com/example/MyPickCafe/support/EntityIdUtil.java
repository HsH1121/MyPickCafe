package com.example.MyPickCafe.support;

import jakarta.persistence.Id;
import java.lang.reflect.Field;

public final class EntityIdUtil {

    private EntityIdUtil() {}

    public static String findIdFieldName(Class<?> type) {
        Class<?> t = type;
        while (t != null && t != Object.class) {
            for (Field f : t.getDeclaredFields()) {
                if (f.isAnnotationPresent(Id.class)) {
                    return f.getName();
                }
            }
            t = t.getSuperclass();
        }
        throw new IllegalStateException("@Id 필드가 없습니다: " + type.getName());
    }

    public static Object getId(Object entity) {
        if (entity == null) return null;
        try {
            String idFieldName = findIdFieldName(entity.getClass());
            Field f = getField(entity.getClass(), idFieldName);
            f.setAccessible(true);
            return f.get(entity);
        } catch (Exception e) {
            throw new RuntimeException("리플렉션으로 id 를 읽지 못했습니다.", e);
        }
    }

    public static void setId(Object entity, Object id) {
        if (entity == null) return;
        try {
            String idFieldName = findIdFieldName(entity.getClass());
            Field f = getField(entity.getClass(), idFieldName);
            f.setAccessible(true);
            f.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException("리플렉션으로 id 를 설정하지 못했습니다.", e);
        }
    }

    private static Field getField(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> t = type;
        while (t != null && t != Object.class) {
            try {
                return t.getDeclaredField(name);
            } catch (NoSuchFieldException ignore) {
                t = t.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
