package ru.bibarsov.jdbcstdops.core;

import java.lang.reflect.Field;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class ColumnDefinition {

  public final Field javaReflectionField;
  @Nullable
  public final String columnName;
  public final Class<?> valueClass;
  @Nullable
  public final IdMetadata idMetadata;
  @Nullable
  public final EnumMetadata enumMetadata;
  public final boolean nullable;
  public final List<ColumnComponentDefinition> compositeComponents;
  @Nullable
  public final Constructor<?> compositeConstructor;

  public ColumnDefinition(
      Field javaReflectionField,
      @Nullable String columnName,
      Class<?> valueClass,
      @Nullable IdMetadata idMetadata,
      @Nullable EnumMetadata enumMetadata,
      boolean nullable,
      List<ColumnComponentDefinition> compositeComponents,
      @Nullable Constructor<?> compositeConstructor
  ) {
    this.javaReflectionField = javaReflectionField;
    this.columnName = columnName;
    this.valueClass = valueClass;
    this.idMetadata = idMetadata;
    this.enumMetadata = enumMetadata;
    this.nullable = nullable;
    this.compositeComponents = compositeComponents;
    this.compositeConstructor = compositeConstructor;
  }

  public ColumnDefinition(
      Field javaReflectionField,
      @Nullable String columnName,
      Class<?> valueClass,
      @Nullable IdMetadata idMetadata,
      @Nullable EnumMetadata enumMetadata,
      boolean nullable
  ) {
  this(
    javaReflectionField,
    columnName,
    valueClass,
    idMetadata,
    enumMetadata,
    nullable,
    Collections.emptyList(),
    null
  );
  }

  public boolean isComposite() {
    return !compositeComponents.isEmpty();
  }
}
