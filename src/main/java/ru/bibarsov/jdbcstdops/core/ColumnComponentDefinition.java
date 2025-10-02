package ru.bibarsov.jdbcstdops.core;

import java.lang.reflect.Field;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.Nullable;

@ParametersAreNonnullByDefault
public class ColumnComponentDefinition {

  public final Field javaReflectionField;
  public final String columnName;
  public final Class<?> valueClass;
  @Nullable
  public final EnumMetadata enumMetadata;
  public final boolean nullable;

  public ColumnComponentDefinition(
      Field javaReflectionField,
      String columnName,
      Class<?> valueClass,
      @Nullable EnumMetadata enumMetadata,
      boolean nullable
  ) {
    this.javaReflectionField = javaReflectionField;
    this.javaReflectionField.setAccessible(true);
    this.columnName = columnName;
    this.valueClass = valueClass;
    this.enumMetadata = enumMetadata;
    this.nullable = nullable;
  }

  public Object readValue(@Nullable Object owner) {
    if (owner == null) {
      return null;
    }
    try {
      return javaReflectionField.get(owner);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }
}
