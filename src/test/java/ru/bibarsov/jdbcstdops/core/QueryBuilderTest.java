package ru.bibarsov.jdbcstdops.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import javax.annotation.ParametersAreNonnullByDefault;
import org.junit.Assert;
import org.junit.Test;
import ru.bibarsov.jdbcstdops.util.Pair;
import ru.bibarsov.jdbcstdops.value.QueryType;

@ParametersAreNonnullByDefault
public class QueryBuilderTest {

  @Test
  public void testDummySelectQueryBuild() {
    QueryBuilder queryBuilder = new QueryBuilder(new ColumnValueConverter());
    queryBuilder.setType(QueryType.SELECT);
    queryBuilder.setTableName("foobar");
    queryBuilder.setColumnsToSelect(List.of("col1", "col2"));
    Query build = queryBuilder.build();
    Assert.assertEquals("SELECT col1,col2 FROM foobar", build.sqlQuery);
    Assert.assertEquals(
        0, //expected
        Objects.requireNonNull(build.parameterSource.getParameterNames()).length
    );
    Assert.assertEquals(Boolean.FALSE, build.hasReturningStatement);
  }

  @Test
  public void testDummyInsertQueryBuild() {
    QueryBuilder queryBuilder = new QueryBuilder(new ColumnValueConverter());
    queryBuilder.setType(QueryType.INSERT);
    queryBuilder.setTableName("foobar");
    QueryColDef col1 = new QueryColDef("col1", null, null);
    QueryColDef col2 = new QueryColDef("col2", null, null);
    queryBuilder.setColumnsToInsert(
      new LinkedHashMap<>(){{
        put("col1", Pair.of(col1, 1));
        put("col2", Pair.of(col2, 2));
      }}
    );
    Query build = queryBuilder.build();
    Assert.assertEquals("INSERT INTO foobar (col1,col2) VALUES (:col1,:col2)", build.sqlQuery);
    Assert.assertEquals(
        1, //expected
        Objects.requireNonNull(build.parameterSource.getValue("col1"))
    );
    Assert.assertEquals(
        2, //expected
        Objects.requireNonNull(build.parameterSource.getValue("col2"))
    );
    Assert.assertEquals(Boolean.FALSE, build.hasReturningStatement);
  }

  @Test
  public void testDummyUpsertQueryBuild() {
    QueryBuilder queryBuilder = new QueryBuilder(new ColumnValueConverter());
    queryBuilder.setType(QueryType.UPSERT);
    queryBuilder.setTableName("foobar");
    QueryColDef col1 = new QueryColDef(
        "col1", //columnName
        new IdMetadata(false, false, null),
        null //enumMetadata
    );
    QueryColDef col2 = new QueryColDef(
        "col2", //columnName
        null, //idMetadata
        null //enumMetadata
    );
    queryBuilder.setIdColumn(col1);
    queryBuilder.setColumnsToInsert(
        new LinkedHashMap<>(){{
          put("col1", Pair.of(col1, 1));
          put("col2", Pair.of(col2, 2));
        }}
    );
    Query build = queryBuilder.build();
    Assert.assertEquals(
        "INSERT INTO foobar (col1,col2) VALUES (:col1,:col2)"
            + " ON CONFLICT (col1) DO UPDATE SET col1 = :col1,col2 = :col2",
        build.sqlQuery
    );
    Assert.assertEquals(
        1, //expected
        Objects.requireNonNull(build.parameterSource.getValue("col1"))
    );
    Assert.assertEquals(
        2, //expected
        Objects.requireNonNull(build.parameterSource.getValue("col2"))
    );
    Assert.assertEquals(Boolean.FALSE, build.hasReturningStatement);
  }

  @Test
  public void testDummyDeleteQueryBuild() {
    QueryBuilder queryBuilder = new QueryBuilder(new ColumnValueConverter());
    queryBuilder.setType(QueryType.DELETE);
    queryBuilder.setTableName("foobar");
    Query build = queryBuilder.build();
    Assert.assertEquals("DELETE FROM foobar", build.sqlQuery);
    Assert.assertEquals(
        0, //expected
        Objects.requireNonNull(build.parameterSource.getParameterNames()).length
    );
    Assert.assertEquals(Boolean.FALSE, build.hasReturningStatement);
  }

  @Test
  public void testDeleteByIdQueryBuild() {
    QueryBuilder queryBuilder = new QueryBuilder(new ColumnValueConverter());
    queryBuilder.setType(QueryType.DELETE);
    queryBuilder.setTableName("foobar");
    queryBuilder.setCondition(
        new QueryColDef(
            "id", //columnName
            null, //idMetadata
            null  //enumMetadata
        ),
        1
    );
    Query build = queryBuilder.build();
    Assert.assertEquals("DELETE FROM foobar WHERE id = :id", build.sqlQuery);
    Assert.assertTrue(Objects.requireNonNull(build.parameterSource).hasValue("id"));
    Assert.assertEquals(
        1, //expected
        Objects.requireNonNull(build.parameterSource).getValue("id")
    );
    Assert.assertEquals(Boolean.FALSE, build.hasReturningStatement);
  }
}
