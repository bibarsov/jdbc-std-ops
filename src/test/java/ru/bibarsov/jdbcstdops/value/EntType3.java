package ru.bibarsov.jdbcstdops.value;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public enum EntType3 {

  G("g"),
  H("h"),
  I("i");

  public final String value;

  public static EntType3 ofValue(String raw) {
    for (EntType3 type3 : EntType3.values()) {
      if (type3.value.equals(raw)) {
        return type3;
      }
    }
    throw new IllegalArgumentException();
  }

  EntType3(String value) {
    this.value = value;
  }
}
