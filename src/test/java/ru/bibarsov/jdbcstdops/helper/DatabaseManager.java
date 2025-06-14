package ru.bibarsov.jdbcstdops.helper;

import javax.annotation.ParametersAreNonnullByDefault;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.bibarsov.jdbcstdops.value.EntType1;

@ParametersAreNonnullByDefault
public class DatabaseManager {

  public static void initDatabase(NamedParameterJdbcTemplate jdbcTemplate) {
    jdbcTemplate.getJdbcTemplate().execute(
        ""
            + "      CREATE TABLE IF NOT EXISTS public.entity"
            + "          ("
            + "              id         bigint                      NOT NULL PRIMARY KEY,"
            + "              name       character varying           NOT NULL,"
            + "              nullname   character varying           NULL,"
            + "              type_1     character varying           NOT NULL,"
            + "              type_2     character varying           NOT NULL,"
            + "              type_3     character varying           NOT NULL,"
            + "              created_at timestamp without time zone NOT NULL"
            + "          );"
            + "      CREATE TABLE IF NOT EXISTS public.entity_deferred"
            + "          ("
            + "              id   bigint            NOT NULL PRIMARY KEY,"
            + "              name character varying NOT NULL"
            + "          );"
        + "      CREATE TABLE IF NOT EXISTS public.entity_composite"
        + "          ("
        + "              first_part   bigint            NOT NULL,"
        + "              second_part   bigint            NOT NULL,"
        + "              name character varying NOT NULL,"
        + "              CONSTRAINT entity_composite_pkey PRIMARY KEY (first_part, second_part)"
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
            + "      DROP TABLE IF EXISTS public.entity_composite;"
            + "      DROP SEQUENCE IF EXISTS public.entity_deferred_id_seq;"
    );
  }
}
