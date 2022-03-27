package ru.bibarsov.jdbcstdops.value;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public enum QueryType {
  INSERT,
  UPSERT,
  SELECT,
  DELETE
}
