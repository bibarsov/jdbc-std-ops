package ru.bibarsov.jdbcstdops.core;

import java.util.Objects;
import javax.annotation.ParametersAreNonnullByDefault;
import org.junit.Assert;
import org.junit.Test;
import ru.bibarsov.jdbcstdops.value.QueryType;

@ParametersAreNonnullByDefault
public class QueryBuilderTest {

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
        new ColumnDefinition(
            null, //javaReflectionField
            "id", //columnName
            null, //valueClass
            null, //idMetadata
            null, //enumMetadata
            false //nullable
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
