package ru.bibarsov.jdbcstdops.core;

import java.lang.reflect.Field;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class ColumnDefinition {

  public final Field javaReflectionField;
  public final String columnName;
  public final Class<?> valueClass;
  @Nullable
  public final IdMetadata idMetadata;
  public final boolean nullable;

  public ColumnDefinition(
      Field javaReflectionField,
      String columnName,
      Class<?> valueClass,
      @Nullable IdMetadata idMetadata,
      boolean nullable
  ) {
    this.javaReflectionField = javaReflectionField;
    this.columnName = columnName;
    this.valueClass = valueClass;
    this.idMetadata = idMetadata;
    this.nullable = nullable;
  }

  @ParametersAreNonnullByDefault
  public static class IdMetadata {

    public final boolean isDbSideGenerated;
    @Nullable
    public final String sequenceName;

    public IdMetadata(boolean isDbSideGenerated, @Nullable String sequenceName) {
      this.isDbSideGenerated = isDbSideGenerated;
      this.sequenceName = sequenceName;
    }
  }
}
