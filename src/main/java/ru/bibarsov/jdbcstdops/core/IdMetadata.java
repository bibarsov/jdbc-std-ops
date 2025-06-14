package ru.bibarsov.jdbcstdops.core;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class IdMetadata {

  public final boolean isDbSideGenerated;
  public final boolean isCompositeKey;
  @Nullable
  public final String sequenceName;

  public IdMetadata(boolean isDbSideGenerated, boolean isCompositeKey, @Nullable String sequenceName) {
    this.isDbSideGenerated = isDbSideGenerated;
    this.isCompositeKey = isCompositeKey;
    this.sequenceName = sequenceName;
  }
}
