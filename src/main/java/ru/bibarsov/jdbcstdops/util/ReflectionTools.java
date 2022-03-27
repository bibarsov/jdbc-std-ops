package ru.bibarsov.jdbcstdops.util;

import java.util.Map;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class ReflectionTools {

  private static final Map<Class<?>, Class<?>> primitiveToWrapperMap = Map.of(
      Boolean.TYPE, Boolean.class,
      Byte.TYPE, Byte.class,
      Character.TYPE, Character.class,
      Short.TYPE, Short.class,
      Integer.TYPE, Integer.class,
      Long.TYPE, Long.class,
      Double.TYPE, Double.class,
      Float.TYPE, Float.class,
      Void.TYPE, Void.TYPE
  );

  public static Class<?> primitiveToWrapper(@Nullable Class<?> cls) {
    Class<?> convertedClass = cls;
    if (cls != null && cls.isPrimitive()) {
      convertedClass = primitiveToWrapperMap.get(cls);
    }
    return convertedClass;
  }
}
