package ru.bibarsov.jdbcstdops.core;

import static ru.bibarsov.jdbcstdops.util.Preconditions.checkNotNull;

import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import ru.bibarsov.jdbcstdops.value.DeferredId;

@ParametersAreNonnullByDefault
public class ColumnValueConverter {

  @Nullable
  public Object toDbTypeValue(
      @Nullable Object javaValue,
      QueryColDef queryColDef
  ) {
    if (javaValue == null) {
      return null;
    }
    if (javaValue instanceof Enum) {
      EnumMetadata metadata = checkNotNull(
          queryColDef.enumMetadata,
          "No enumMetadata is provided for class " + javaValue.getClass()
      );
      if (metadata.accessorField != null) {
        return toRawValueViaField((Enum<?>) javaValue, metadata.accessorField);
      } else if (metadata.accessorMethod != null) {
        return toRawValueViaMethod((Enum<?>) javaValue, metadata.accessorMethod);
      } else {
        return ((Enum<?>) javaValue).name();
      }
    }
    if (javaValue instanceof DeferredId<?>) {
      return ((DeferredId<?>) javaValue).value;
    }
    if (javaValue instanceof Instant) {
      return new Timestamp(((Instant) javaValue).toEpochMilli());
    }
    return javaValue;
  }

  @Nullable
  public Object toJavaTypeValue(
      ResultSet rs,
      String columnName,
      Class<?> clazz,
      @Nullable EnumMetadata enumMetadata
  ) throws SQLException {
    if (clazz.equals(Instant.class)) {
      Timestamp timestamp = rs.getObject(
          columnName,
          Timestamp.class
      );
      return Instant.ofEpochMilli(timestamp.getTime());
    }
    if (clazz.equals(DeferredId.class)) {
      DeferredId<Object> deferredId = new DeferredId<>();
      deferredId.value = rs.getObject(columnName);
      return deferredId;
    }
    if (clazz.equals(Integer.class)) {
      int value = rs.getInt(columnName);
      return rs.wasNull() ? null : value;
    }
    if (clazz.equals(Long.class)) {
      long value = rs.getLong(columnName);
      return rs.wasNull() ? null : value;
    }
    if (clazz.equals(Short.class)) {
      short value = rs.getShort(columnName);
      return rs.wasNull() ? null : value;
    }
    if (clazz.equals(Byte.class)) {
      byte value = rs.getByte(columnName);
      return rs.wasNull() ? null : value;
    }
    if (clazz.equals(Double.class)) {
      double value = rs.getDouble(columnName);
      return rs.wasNull() ? null : value;
    }
    if (clazz.equals(Float.class)) {
      float value = rs.getFloat(columnName);
      return rs.wasNull() ? null : value;
    }
    if (clazz.isEnum()) {
      EnumMetadata safeEnumMetadata = checkNotNull(
          enumMetadata,
          "No enumMetadata is provided for class " + clazz
      );
      Object object = rs.getObject(columnName);
      if (object == null) {
        return null;
      }
      if (safeEnumMetadata.builderMethod != null) {
        return createInstanceViaBuilder(clazz, safeEnumMetadata.builderMethod, object);
      }
      @SuppressWarnings({"unchecked", "rawtypes"})
      Enum enumValue = Enum.valueOf((Class) clazz, (String) object);
      return enumValue;
    }

    return rs.getObject(
        columnName,
        clazz
    );
  }

  private static Object createInstanceViaBuilder(Class<?> clazz, String methodName, Object object) {
    List<Method> methods = Arrays.stream(clazz.getDeclaredMethods())
        .filter(m -> m.getName().equals(methodName))
        .collect(Collectors.toList());
    if (methods.size() > 1) {
      throw new IllegalStateException(
          "There should be only one builder method in Enum class: " + clazz);
    }
    if (methods.size() == 0) {
      throw new IllegalStateException("Couldn't find builder method in Enum class: " + clazz);
    }
    Object result = null;
    try {
      result = methods.get(0).invoke(clazz, object);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
    return checkNotNull(
        result,
        "builder method should return only non-null value, class: " + clazz
    );
  }

  @SuppressWarnings("rawtypes")
  private static Object toRawValueViaMethod(Enum<?> javaValue, String methodName) {
    Class<? extends Enum> aClass = javaValue.getClass();
    Object result = null;
    try {
      result = aClass.getDeclaredMethod(methodName).invoke(javaValue);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
    return checkNotNull(
        result,
        "accessorMethod should return non-null value for class " + aClass
    );
  }

  @SuppressWarnings("rawtypes")
  private static Object toRawValueViaField(Enum<?> javaValue, String fieldName) {
    Class<? extends Enum> aClass = javaValue.getClass();
    Object result = null;
    try {
      result = aClass.getDeclaredField(fieldName).get(javaValue);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
    return checkNotNull(
        result,
        "accessorMethod should return non-null value for class " + aClass
    );
  }
}
