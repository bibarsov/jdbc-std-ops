package ru.bibarsov.jdbcstdops.value;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Holder for db-generated identifier
 *
 * @param <T> type of value
 */
@ParametersAreNonnullByDefault
public class DeferredId<T> {

  //mutable
  @Nullable
  public volatile T value;

  public static <T> DeferredId<T> create() {
    return new DeferredId<>();
  }

  public static <T> DeferredId<T> ofImmediateId(T value) {
    DeferredId<T> id = new DeferredId<>();
    id.value = value;
    return id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    DeferredId<?> that = (DeferredId<?>) o;

    return value != null ? value.equals(that.value) : that.value == null;
  }

  @Override
  public int hashCode() {
    return value != null ? value.hashCode() : 0;
  }

  @Override
  public String toString() {
    return "DeferredId{" +
        "value=" + value +
        '}';
  }
}
