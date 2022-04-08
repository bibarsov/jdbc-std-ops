package ru.bibarsov.jdbcstdops.util;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class Preconditions {

  public static void checkArgument(boolean expression) {
    if (!expression) {
      throw new IllegalArgumentException();
    }
  }

  public static <T> T checkNotNull(@Nullable T reference) {
    return checkNotNull(reference, null);
  }

  public static <T> T checkNotNull(@Nullable T reference, @Nullable String errorDescription) {
    if (reference == null) {
      throw new NullPointerException(errorDescription);
    }
    return reference;
  }

  public static void checkState(boolean expression) {
    checkState(expression, null);
  }

  public static void checkState(boolean expression, @Nullable String errorDescription) {
    if (!expression) {
      throw new IllegalStateException(errorDescription);
    }
  }
}
