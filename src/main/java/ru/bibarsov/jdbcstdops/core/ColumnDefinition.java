package ru.bibarsov.jdbcstdops.core;

import static ru.bibarsov.jdbcstdops.util.Preconditions.checkArgument;

import java.lang.reflect.Field;
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

  public ColumnDefinition(
      Field javaReflectionField,
      @Nullable String columnName,
      Class<?> valueClass,
      @Nullable IdMetadata idMetadata,
      @Nullable EnumMetadata enumMetadata,
      boolean nullable
  ) {
    this.javaReflectionField = javaReflectionField;
    this.columnName = columnName;
    this.valueClass = valueClass;
    this.idMetadata = idMetadata;
    this.enumMetadata = enumMetadata;
    this.nullable = nullable;
  }
}
