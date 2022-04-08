package ru.bibarsov.jdbcstdops.util;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

@Immutable
@ParametersAreNonnullByDefault
public class Pair<L, R> {

  @Nullable
  public final L left;
  @Nullable
  public final R right;

  public static <L, R> Pair<L, R> of(@Nullable L left, @Nullable R right) {
    return new Pair<>(left, right);
  }

  private Pair(@Nullable L left, @Nullable R right) {
    this.left = left;
    this.right = right;
  }
}
