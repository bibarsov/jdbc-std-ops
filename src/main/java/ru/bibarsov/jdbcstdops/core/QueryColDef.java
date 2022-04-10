package ru.bibarsov.jdbcstdops.core;

import java.lang.reflect.Field;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class QueryColDef {

  public final String columnName;
  @Nullable
  public final IdMetadata idMetadata;
  @Nullable
  public final EnumMetadata enumMetadata;

  public QueryColDef(
      String columnName,
      @Nullable IdMetadata idMetadata,
      @Nullable EnumMetadata enumMetadata
  ) {
    this.columnName = columnName;
    this.idMetadata = idMetadata;
    this.enumMetadata = enumMetadata;
  }
}
