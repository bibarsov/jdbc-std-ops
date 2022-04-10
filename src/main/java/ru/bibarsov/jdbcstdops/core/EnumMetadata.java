package ru.bibarsov.jdbcstdops.core;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class EnumMetadata {

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

