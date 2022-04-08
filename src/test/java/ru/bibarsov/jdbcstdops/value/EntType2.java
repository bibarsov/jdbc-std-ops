package ru.bibarsov.jdbcstdops.value;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public enum EntType2 {

  D("d"),
  E("e"),
  F("f");

  private final String value;

  public static EntType2 ofValue(String raw) {
    for (EntType2 type2 : EntType2.values()) {
      if (type2.value.equals(raw)) {
        return type2;
      }
    }
    throw new IllegalArgumentException();
  }
  public String toValue() {
    return value;
  }

  EntType2(String value) {
    this.value = value;
  }
}
