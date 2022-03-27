package ru.bibarsov.jdbcstdops.helper;

import javax.annotation.ParametersAreNonnullByDefault;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@ParametersAreNonnullByDefault
public class DatabaseManager {

  public static void initDatabase(NamedParameterJdbcTemplate jdbcTemplate) {
    jdbcTemplate.getJdbcTemplate().execute(
        ""
            + "      CREATE TABLE IF NOT EXISTS public.entity"
            + "          ("
            + "              id   bigint            NOT NULL PRIMARY KEY,"
            + "              name character varying NOT NULL"
            + "          );"
            + "      CREATE TABLE IF NOT EXISTS public.entity_deferred"
            + "          ("
            + "              id   bigint            NOT NULL PRIMARY KEY,"
            + "              name character varying NOT NULL"
            + "          );"
            + ""
            + "      CREATE SEQUENCE IF NOT EXISTS public.entity_deferred_id_seq;"
    );
  }

  public static void truncateDatabase(NamedParameterJdbcTemplate jdbcTemplate) {
    jdbcTemplate.getJdbcTemplate().execute(
        ""
            + "      DROP TABLE IF EXISTS public.entity;"
            + "      DROP TABLE IF EXISTS public.entity_deferred;"
            + "      DROP SEQUENCE IF EXISTS public.entity_deferred_id_seq;"
    );
  }
}
