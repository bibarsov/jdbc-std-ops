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
import ru.bibarsov.jdbcstdops.util.Pair;
import ru.bibarsov.jdbcstdops.value.QueryType;

@ParametersAreNonnullByDefault
public class QueryBuilder {

  private final ColumnValueConverter columnValueConverter;

  private QueryType queryType;
  private String tableName;

  private boolean built = false;
  //Map<ColumnName, Pair<ColumnDefinition, JavaReflectionFieldValue>>
  private Map<String, Pair<QueryColDef, Object>> conditions;
  private List<String> columnsToSelect;
  //Map<ColumnName, Pair<ColumnDefinition, JavaReflectionFieldValue>>
  private Map<String, Pair<QueryColDef, Object>> columnsToInsert;
  private List<QueryColDef> idColumns = Collections.emptyList();
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

  public QueryBuilder setIdColumns(List<QueryColDef> idColumns) {
    checkState(!idColumns.isEmpty(), "Id columns must not be empty");
    this.idColumns = Collections.unmodifiableList(idColumns);
    return this;
  }

  public QueryBuilder setIdColumn(QueryColDef idColumn) {
    return setIdColumns(List.of(idColumn));
  }

  public QueryBuilder setColumnsToSelect(List<String> columnNames) {
    checkState(!columnNames.isEmpty());
    this.columnsToSelect = Collections.unmodifiableList(columnNames);
    return this;
  }

  //todo generalize
  public QueryBuilder setColumnsToInsert(
      LinkedHashMap<String, Pair<QueryColDef, Object>> columnsToInsert
  ) {
    checkState(!columnsToInsert.isEmpty());
    this.columnsToInsert = Collections.unmodifiableMap(columnsToInsert);
    return this;
  }

  public QueryBuilder setGenerateAndReturnId(boolean generateAndReturnId) {
    this.generateAndReturnId = generateAndReturnId;
    return this;
  }

  public QueryBuilder setCondition(QueryColDef column, @Nullable Object value) {
    checkNotNull(column.columnName, "Column name must not be null");
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
    QueryColDef generatedIdColumn = findGeneratedIdColumn();
    if (generateAndReturnId) {
      checkState(
          generatedIdColumn != null,
          "generateAndReturnId requires a generated id column"
      );
    }
    queryString.append(String.format(
        "INSERT INTO %s (%s) VALUES (%s)",
        tableName,
        String.join(",", columnsToInsert.keySet()),
        columnsToInsert.keySet().stream()
            .map(c -> {
              if (generateAndReturnId && generatedIdColumn != null
                  && c.equals(generatedIdColumn.columnName)) {
                return String.format(
                    "nextval('%s')", //db dialect dependant!
                    checkNotNull(checkNotNull(generatedIdColumn.idMetadata).sequenceName)
                );
              }
              return ":" + c;
            })
            .collect(Collectors.joining(","))
    ));
    if (generateAndReturnId && generatedIdColumn != null) {
      queryString.append(" RETURNING ").append(generatedIdColumn.columnName);
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
    checkState(!idColumns.isEmpty(), "UPSERT requires id columns");
    QueryColDef generatedIdColumn = findGeneratedIdColumn();
    if (generateAndReturnId) {
      checkState(
          generatedIdColumn != null,
          "generateAndReturnId requires a generated id column"
      );
    }
    List<String> idColumnNames = idColumns.stream()
        .map(c -> checkNotNull(c.columnName))
        .collect(Collectors.toList());
    StringBuilder queryString = new StringBuilder();
    queryString.append(String.format(
        "INSERT INTO %s (%s) VALUES (%s) ON CONFLICT (%s) DO UPDATE SET %s",
        tableName,
        String.join(",", columnsToInsert.keySet()),
        columnsToInsert.keySet().stream()
            .map(c -> {
              if (generateAndReturnId && generatedIdColumn != null
                  && c.equals(generatedIdColumn.columnName)) {
                return String.format(
                    "nextval('%s')", //db dialect dependant!
                    checkNotNull(checkNotNull(generatedIdColumn.idMetadata).sequenceName)
                );
              }
              return ":" + c;
            })
            .collect(Collectors.joining(","))
        ,
        String.join(",", idColumnNames),
        columnsToInsert.keySet().stream()
            .filter(c -> !idColumnNames.contains(c))
            .map(c -> c + " = " + ":" + c)
            .collect(Collectors.joining(","))
    ));
    if (generateAndReturnId && generatedIdColumn != null) {
      queryString.append(" RETURNING ").append(generatedIdColumn.columnName);
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
        "DELETE FROM %s",
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
      QueryColDef queryColDef,
      @Nullable Object value
  ) {
    @Nullable Object dbTypeValue = columnValueConverter.toDbTypeValue(
        value,
        queryColDef
    );
    mapSqlParameterSource.addValue(paramName, dbTypeValue);
  }

  @Nullable
  private QueryColDef findGeneratedIdColumn() {
    if (idColumns == null) {
      return null;
    }
    return idColumns.stream()
        .filter(col -> {
          IdMetadata metadata = col.idMetadata;
          return metadata != null && metadata.isDbSideGenerated;
        })
        .findFirst()
        .orElse(null);
  }
}
