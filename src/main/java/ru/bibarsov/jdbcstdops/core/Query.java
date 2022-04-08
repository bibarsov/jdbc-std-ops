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

  @Override
  public String toString() {
    return "Query{" +
        "sqlQuery='" + sqlQuery + '\'' +
        ", parameterSource=" + parameterSource +
        ", hasReturningStatement=" + hasReturningStatement +
        '}';
  }
}
