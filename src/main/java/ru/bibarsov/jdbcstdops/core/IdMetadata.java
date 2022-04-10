package ru.bibarsov.jdbcstdops.core;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class IdMetadata {

  public final boolean isDbSideGenerated;
  @Nullable
  public final String sequenceName;

  public IdMetadata(boolean isDbSideGenerated, @Nullable String sequenceName) {
    this.isDbSideGenerated = isDbSideGenerated;
    this.sequenceName = sequenceName;
  }
}
