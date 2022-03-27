package ru.bibarsov.jdbcstdops.core;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

@Immutable
@ParametersAreNonnullByDefault
public class Query {

  public final String sqlQuery;
  public final SqlParameterSource parameterSource;
  public final boolean hasReturningStatement;

  public Query(
      String sqlQuery,
      SqlParameterSource parameterSource,
      boolean hasReturningStatement
  ) {
    this.sqlQuery = sqlQuery;
    this.parameterSource = parameterSource;
    this.hasReturningStatement = hasReturningStatement;
  }
}
