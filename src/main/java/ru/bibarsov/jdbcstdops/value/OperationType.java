package ru.bibarsov.jdbcstdops.value;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public enum OperationType {
  CREATE,
  CREATE_OR_UPDATE,
  GET_ALL,
  FIND_ONE,
  DELETE,
}
