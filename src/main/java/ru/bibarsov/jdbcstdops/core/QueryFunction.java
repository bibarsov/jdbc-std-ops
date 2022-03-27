package ru.bibarsov.jdbcstdops.core;

import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * Domain-specific analogue of BiFunction for queries lambdas preparation
 */
@FunctionalInterface
public interface QueryFunction {

  @Nullable
  Object query(
      NamedParameterJdbcTemplate jdbcTemplate,
      Consumer<QueryBuilder> queryBuilderMutator
  );
}
