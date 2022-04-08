package ru.bibarsov.jdbcstdops.core;

import static ru.bibarsov.jdbcstdops.util.Preconditions.checkArgument;

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
  @Nullable
  public final EnumMetadata enumMetadata;
  public final boolean nullable;

  public ColumnDefinition(
      Field javaReflectionField,
      String columnName,
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
  @ParametersAreNonnullByDefault
  public static class EnumMetadata {

    @Nullable
    public final String accessorField;
    @Nullable
    public final String accessorMethod;
    @Nullable
    public final String builderMethod;

    public EnumMetadata(
        @Nullable String accessorField,
        @Nullable String accessorMethod,
        @Nullable String builderMethod
    ) {
      this.accessorField = accessorField;
      this.accessorMethod = accessorMethod;
      this.builderMethod = builderMethod;
    }
  }
}
