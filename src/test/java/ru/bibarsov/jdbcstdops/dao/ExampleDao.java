package ru.bibarsov.jdbcstdops.dao;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import ru.bibarsov.jdbcstdops.core.StandardOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.bibarsov.jdbcstdops.entity.Entity;

@ParametersAreNonnullByDefault
public class ExampleDao {

  private final NamedParameterJdbcTemplate jdbcTemplate;
  private final StandardOperations<Entity, Long> stdOps;

  public ExampleDao(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
    this.stdOps = new StandardOperations<>(Entity.class, jdbcTemplate);
  }

  @Nullable
  public Entity findOne(long id) {
    return stdOps.findOne(id);
  }
}
