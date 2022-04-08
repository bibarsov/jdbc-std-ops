package ru.bibarsov.jdbcstdops.core;

import static ru.bibarsov.jdbcstdops.util.Preconditions.checkNotNull;
import static ru.bibarsov.jdbcstdops.util.Preconditions.checkState;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import ru.bibarsov.jdbcstdops.util.Pair;
import ru.bibarsov.jdbcstdops.value.DeferredId;
import ru.bibarsov.jdbcstdops.value.QueryType;

//TODO Use own ColumnDef type instead of
// ColumnDefinition (valueClass, javaReflectionField, nullable aren't needed)
@ParametersAreNonnullByDefault
public class QueryBuilder {

  private final ColumnValueConverter columnValueConverter;

  private QueryType queryType;
  private String tableName;

  private boolean built = false;
  //Map<ColumnName, Pair<ColumnDefinition, JavaReflectionFieldValue>>
  private Map<String, Pair<ColumnDefinition, Object>> conditions;
  private List<String> columnsToSelect;
  //Map<ColumnName, Pair<ColumnDefinition, JavaReflectionFieldValue>>
  private Map<String, Pair<ColumnDefinition, Object>> columnsToInsert;
  private ColumnDefinition idColumn;
  private boolean generateAndReturnId = false; //false by default

  public QueryBuilder(ColumnValueConverter columnValueConverter) {
    this.columnValueConverter = columnValueConverter;
  }

  public QueryBuilder setType(QueryType queryType) {
    this.queryType = queryType;
    return this;
  }

  public QueryBuilder setTableName(String tableName) {
    this.tableName = tableName;
    return this;
  }

  public QueryBuilder setIdColumn(ColumnDefinition idColumn) {
    this.idColumn = idColumn;
    return this;
  }

  public QueryBuilder setColumnsToSelect(List<String> columnNames) {
    checkState(!columnNames.isEmpty());
    this.columnsToSelect = Collections.unmodifiableList(columnNames);
    return this;
  }

  //todo generalize
  public QueryBuilder setColumnsToInsert(
      LinkedHashMap<String, Pair<ColumnDefinition, Object>> columnsToInsert
  ) {
    checkState(!columnsToInsert.isEmpty());
    this.columnsToInsert = Collections.unmodifiableMap(columnsToInsert);
    return this;
  }

  public QueryBuilder setGenerateAndReturnId(boolean generateAndReturnId) {
    this.generateAndReturnId = generateAndReturnId;
    return this;
  }

  public QueryBuilder setCondition(ColumnDefinition column, @Nullable Object value) {
    if (conditions == null) {
      this.conditions = new HashMap<>();
    }
    this.conditions.put(column.columnName, Pair.of(column, value));
    return this;
  }

  public Query build() {
    checkState(!built);
    checkState(tableName != null);
    checkState(queryType != null);
    built = true;
    switch (queryType) {
      case INSERT:
        return buildInsertQuery();
      case UPSERT:
        return buildUpsertQuery();
      case SELECT:
        return buildSelectQuery();
      case DELETE:
        return buildDeleteQuery();
      default:
        throw new IllegalStateException("Unexpected QueryType: " + queryType);
    }
  }

  private Query buildInsertQuery() {
    checkState(
        columnsToInsert != null && !columnsToInsert.isEmpty(),
        "Columns to insert must be provided"
    );
    StringBuilder queryString = new StringBuilder();
    queryString.append(String.format(
        "INSERT INTO %s (%s) VALUES (%s)",
        tableName,
        String.join(",", columnsToInsert.keySet()),
        columnsToInsert.keySet().stream()
            .map(c -> {
              if (generateAndReturnId && c.equals(idColumn.columnName)) {
                return String.format(
                    "nextval('%s')", //db dialect dependant!
                    checkNotNull(idColumn.idMetadata).sequenceName
                );
              }
              return ":" + c;
            })
            .collect(Collectors.joining(","))
    ));
    if (generateAndReturnId) {
      queryString.append(" RETURNING ").append(idColumn.columnName);
    }
    String query = queryString.toString();
    MapSqlParameterSource parameterSource = new MapSqlParameterSource();
    for (var columnToVal : columnsToInsert.entrySet()) {
      addValue(
          parameterSource,
          columnToVal.getKey(),
          checkNotNull(columnToVal.getValue().left),
          columnToVal.getValue().right
      );
    }
    return new Query(query, parameterSource, generateAndReturnId);
  }

  private Query buildUpsertQuery() {
    checkState(columnsToInsert != null && !columnsToInsert.isEmpty());
    StringBuilder queryString = new StringBuilder();
    queryString.append(String.format(
        "INSERT INTO %s (%s) VALUES (%s) ON CONFLICT (%s) DO UPDATE SET %s",
        tableName,
        String.join(",", columnsToInsert.keySet()),
        columnsToInsert.keySet().stream()
            .map(c -> {
              if (generateAndReturnId && c.equals(idColumn.columnName)) {
                return String.format(
                    "nextval('%s')", //db dialect dependant!
                    checkNotNull(idColumn.idMetadata).sequenceName
                );
              }
              return ":" + c;
            })
            .collect(Collectors.joining(","))
        ,
        idColumn.columnName, //todo support composite pk
        columnsToInsert.keySet().stream()
            .filter(c -> !(generateAndReturnId && c.equals(idColumn.columnName)))
            .map(c -> c + " = " + ":" + c)
            .collect(Collectors.joining(","))
    ));
    if (generateAndReturnId) {
      queryString.append(" RETURNING ").append(idColumn.columnName);
    }
    String query = queryString.toString();
    MapSqlParameterSource parameterSource = new MapSqlParameterSource();
    for (var columnToVal : columnsToInsert.entrySet()) {
      addValue(
          parameterSource,
          columnToVal.getKey(),
          checkNotNull(columnToVal.getValue().left),
          columnToVal.getValue().right
      );
    }
    return new Query(query, parameterSource, generateAndReturnId);

  }

  private Query buildSelectQuery() {
    checkState(
        columnsToSelect != null && !columnsToSelect.isEmpty(),
        "Columns to select must be provided"
    );
    StringBuilder queryString = new StringBuilder();
    queryString.append(String.format(
        "SELECT %s FROM %s",
        String.join(",", columnsToSelect),
        tableName
    ));
    MapSqlParameterSource parameterSource = new MapSqlParameterSource();
    if (conditions != null && !conditions.isEmpty()) {
      queryString.append(" WHERE ")
          .append(conditions.entrySet().stream()
              .map(
                  e -> {
                    if (e.getValue() == null) {
                      return e.getKey() + " IS NULL ";
                    }
                    return e.getKey() + " = :" + e.getKey();
                  }
              )
              .collect(Collectors.joining(" AND ")));
      for (var columnToVal : conditions.entrySet()) {
        addValue(
            parameterSource,
            columnToVal.getKey(),
            checkNotNull(columnToVal.getValue().left),
            columnToVal.getValue().right
        );
      }
    }
    return new Query(queryString.toString(), parameterSource, generateAndReturnId);
  }

  private Query buildDeleteQuery() {
    StringBuilder queryBuilder = new StringBuilder();
    queryBuilder.append(String.format(
        "DELETE FROM %s", //todo support composite id
        tableName
    ));
    MapSqlParameterSource parameterSource = new MapSqlParameterSource();
    if (conditions != null && !conditions.isEmpty()) {
      queryBuilder.append(" WHERE ")
          .append(conditions.entrySet().stream()
              .map(
                  e -> {
                    if (e.getValue() == null) {
                      return e.getKey() + " IS NULL ";
                    }
                    return e.getKey() + " = :" + e.getKey();
                  }
              )
              .collect(Collectors.joining(" AND ")));
      for (var columnToVal : conditions.entrySet()) {
        addValue(
            parameterSource,
            columnToVal.getKey(),
            checkNotNull(columnToVal.getValue().left),
            columnToVal.getValue().right
        );
      }
    }
    return new Query(queryBuilder.toString(), parameterSource, generateAndReturnId);
  }

  private void addValue(
      MapSqlParameterSource mapSqlParameterSource,
      String paramName,
      ColumnDefinition columnDefinition,
      @Nullable Object value
  ) {
    @Nullable Object dbTypeValue = columnValueConverter.toDbTypeValue(
        value,
        columnDefinition.enumMetadata
    );
    mapSqlParameterSource.addValue(paramName, dbTypeValue);
  }
}
