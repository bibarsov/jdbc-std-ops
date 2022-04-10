package ru.bibarsov.jdbcstdops.core;

import static ru.bibarsov.jdbcstdops.util.Preconditions.checkArgument;
import static ru.bibarsov.jdbcstdops.util.Preconditions.checkNotNull;
import static ru.bibarsov.jdbcstdops.util.Preconditions.checkState;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.bibarsov.jdbcstdops.annotation.Column;
import ru.bibarsov.jdbcstdops.annotation.DbSideId;
import ru.bibarsov.jdbcstdops.annotation.Enumerated;
import ru.bibarsov.jdbcstdops.annotation.Id;
import ru.bibarsov.jdbcstdops.annotation.Table;
import ru.bibarsov.jdbcstdops.util.Pair;
import ru.bibarsov.jdbcstdops.util.ReflectionTools;
import ru.bibarsov.jdbcstdops.value.DeferredId;
import ru.bibarsov.jdbcstdops.value.OperationType;
import ru.bibarsov.jdbcstdops.value.QueryType;

@ParametersAreNonnullByDefault
public class StandardOperations<E, ID> {
  private static final Logger LOGGER = LoggerFactory.getLogger(StandardOperations.class);

  private final Map<OperationType, QueryFunction> queriesLambdas;
  private final NamedParameterJdbcTemplate jdbcTemplate;
  private final List<ColumnDefinition> columnDefinitions;
  private final ColumnDefinition idColumn;

  private final ColumnValueConverter columnValueConverter = new ColumnValueConverter();

  public StandardOperations(Class<E> entityClazz, NamedParameterJdbcTemplate jdbcTemplate) {
    checkArgument(entityClazz.getConstructors().length == 1);
    checkArgument(entityClazz.isAnnotationPresent(Table.class));
    String tableName = entityClazz.getAnnotation(Table.class).name();

    this.jdbcTemplate = jdbcTemplate;
    this.columnDefinitions = getColumnsDefinitions(entityClazz);
    this.idColumn = getIdColumnName(columnDefinitions);
    this.queriesLambdas = prepareQueriesLambdas(entityClazz, tableName);
  }

  public void create(E entity) {
    QueryFunction queryFunction = queriesLambdas.get(OperationType.CREATE);
    LinkedHashMap<String, Pair<QueryColDef, Object>> columnsToInsert
        = generateColumnsToInsert(entity);

    if (idColumn.idMetadata != null
        && idColumn.idMetadata.isDbSideGenerated
        && hasNoDeferredIdValue(entity)
    ) {
      Object idValue = checkNotNull(queryFunction.query(jdbcTemplate, (queryBuilder -> {
        queryBuilder.setGenerateAndReturnId(true);
        queryBuilder.setColumnsToInsert(columnsToInsert);
      })));
      setDeferredIdValue(entity, idValue);
    } else {
      //original entity keeps the same
      queryFunction.query(
          jdbcTemplate,
          queryBuilder -> {
            queryBuilder.setColumnsToInsert(columnsToInsert);
          }
      );
    }
  }

  public void createOrUpdate(E entity) {
    QueryFunction queryFunction = queriesLambdas.get(OperationType.CREATE_OR_UPDATE);
    LinkedHashMap<String, Pair<QueryColDef, Object>> columnsToInsert
        = generateColumnsToInsert(entity);

    if (idColumn.idMetadata != null
        && idColumn.idMetadata.isDbSideGenerated
        && hasNoDeferredIdValue(entity)
    ) {
      Object idValue = checkNotNull(queryFunction.query(jdbcTemplate, (queryBuilder -> {
        queryBuilder.setGenerateAndReturnId(true);
        queryBuilder.setColumnsToInsert(columnsToInsert);
      })));
      setDeferredIdValue(entity, idValue);
    } else {
      //original entity keeps the same
      queryFunction.query(
          jdbcTemplate,
          queryBuilder -> {
            queryBuilder.setColumnsToInsert(columnsToInsert);
          }
      );
    }
  }

  @Nullable
  public E findOne(ID id) {
    QueryFunction queryFunction = queriesLambdas.get(OperationType.FIND_ONE);
    //noinspection unchecked
    return (E) queryFunction.query(
        jdbcTemplate,
        queryBuilder -> queryBuilder.setCondition(toQueryColDef(idColumn), id)
    );
  }

  public Collection<E> getAll() {
    QueryFunction queryFunction = queriesLambdas.get(OperationType.GET_ALL);
    //noinspection unchecked
    return (List<E>) queryFunction.query(jdbcTemplate, (ignored -> {
    }));
  }

  public void deleteOne(ID id) {
    QueryFunction queryFunction = queriesLambdas.get(OperationType.DELETE);
    queryFunction.query(
        jdbcTemplate,
        queryBuilder -> queryBuilder.setCondition(toQueryColDef(idColumn), id)
    );
  }

  /**
   * Optimization for queries by providing ready to run lambdas
   *
   * @param entityClazz entity class
   * @param tableName   table for entity
   * @return pre-compiled queries
   */
  private Map<OperationType, QueryFunction> prepareQueriesLambdas(
      Class<E> entityClazz,
      String tableName
  ) {
    Map<OperationType, QueryFunction> result = new HashMap<>();
    for (OperationType operationType : OperationType.values()) {
      switch (operationType) {
        case CREATE:
          result.put(operationType, (jdbcTemplate, queryBuilderMutator) -> {
            var queryBuilder = new QueryBuilder(columnValueConverter)
                .setType(QueryType.INSERT)
                .setTableName(tableName)
                .setIdColumn(toQueryColDef(idColumn));
            queryBuilderMutator.accept(queryBuilder);
            Query query = queryBuilder.build();
            LOGGER.info("Running query: {} with params: {}", query.sqlQuery,
                query.parameterSource);
            if (!query.hasReturningStatement) {
              jdbcTemplate.update(
                  query.sqlQuery,
                  query.parameterSource
              );
              return null; //passed entity is the same as in database, so return nothing
            } else {
              return checkNotNull(jdbcTemplate.query(
                  query.sqlQuery,
                  query.parameterSource,
                  rs -> rs.next() ? rs.getObject(1) : null //getting generated id value
              ));
            }
          });
          break;
        case CREATE_OR_UPDATE:
          result.put(operationType, (jdbcTemplate, queryBuilderMutator) -> {
            var queryBuilder = new QueryBuilder(columnValueConverter)
                .setType(QueryType.UPSERT)
                .setTableName(tableName)
                .setIdColumn(toQueryColDef(idColumn));
            queryBuilderMutator.accept(queryBuilder);
            Query query = queryBuilder.build();
            LOGGER.info("Running query: {} with params: {}", query.sqlQuery, query.parameterSource);
            if (!query.hasReturningStatement) {
              jdbcTemplate.update(
                  query.sqlQuery,
                  query.parameterSource
              );
              return null; //passed entity is the same as in database, so return nothing
            } else {
              return checkNotNull(jdbcTemplate.query(
                  query.sqlQuery,
                  query.parameterSource,
                  rs -> rs.next() ? rs.getObject(1) : null //getting generated id value
              ));
            }
          });
          break;
        case FIND_ONE:
          List<String> columnsToInsert = columnDefinitions.stream()
              .map(c -> c.columnName)
              .collect(Collectors.toList());
          result.put(operationType, (jdbcTemplate, queryBuilderMutator) -> {
            var queryBuilder = new QueryBuilder(columnValueConverter)
                .setType(QueryType.SELECT)
                .setTableName(tableName)
                .setIdColumn(toQueryColDef(idColumn))
                .setColumnsToSelect(columnsToInsert);
            queryBuilderMutator.accept(queryBuilder);
            Query query = queryBuilder.build();
            LOGGER.info("Running query: {} with params: {}", query.sqlQuery, query.parameterSource);
            return jdbcTemplate.query(
                query.sqlQuery,
                query.parameterSource,
                rs -> {
                  if (rs.next()) {
                    E entity = mapResultSetToEntity(columnDefinitions, entityClazz, rs);
                    checkState(
                        !rs.next(),
                        "Query returned more than one result"
                    );
                    return entity;
                  }
                  return null;
                }
            );
          });
          break;
        case GET_ALL:
          List<String> columnsToSelect =
              columnDefinitions.stream().map(c -> c.columnName).collect(Collectors.toList());
          result.put(operationType, (jdbcTemplate, queryBuilderMutator) -> {
            var queryBuilder = new QueryBuilder(columnValueConverter)
                .setType(QueryType.SELECT)
                .setTableName(tableName)
                .setIdColumn(toQueryColDef(idColumn))
                .setColumnsToSelect(columnsToSelect);
            queryBuilderMutator.accept(queryBuilder);
            Query query = queryBuilder.build();
            LOGGER.info("Running query: {} with params: {}", query.sqlQuery, query.parameterSource);
            return checkNotNull(jdbcTemplate.query(
                query.sqlQuery,
                query.parameterSource,
                rs -> {
                  List<E> entities = new ArrayList<>();
                  while (rs.next()) {
                    entities.add(mapResultSetToEntity(columnDefinitions, entityClazz, rs));
                  }
                  return entities;
                }
            ));
          });
          break;
        case DELETE:
          result.put(operationType, (jdbcTemplate, queryBuilderMutator) -> {
            var queryBuilder = new QueryBuilder(columnValueConverter)
                .setType(QueryType.DELETE)
                .setTableName(tableName)
                .setIdColumn(toQueryColDef(idColumn));
            queryBuilderMutator.accept(queryBuilder);
            Query query = queryBuilder.build();
            LOGGER.info("Running query: {} with params: {}", query.sqlQuery, query.parameterSource);
            jdbcTemplate.update(
                query.sqlQuery,
                query.parameterSource
            );
            return null; //nothing
          });
          break;
        default:
          throw new IllegalStateException("Unexpected OperationType: " + operationType);
      }
    }
    return result;
  }

  private E mapResultSetToEntity(
      List<ColumnDefinition> columnDefinitions,
      Class<E> typeClass,
      ResultSet rs
  ) throws SQLException {
    Object[] constructorArgs = new Object[columnDefinitions.size()];
    for (int i = 0; i < columnDefinitions.size(); i++) {
      ColumnDefinition columnDefinition = columnDefinitions.get(i);
      IdMetadata idMetadata = columnDefinition.idMetadata;
      EnumMetadata enumMetadata = columnDefinition.enumMetadata;

      constructorArgs[i] = columnValueConverter.toJavaTypeValue(
          rs,
          columnDefinition.columnName,
          idMetadata != null && idMetadata.isDbSideGenerated ?
              DeferredId.class :
              ReflectionTools.primitiveToWrapper(columnDefinition.valueClass),
          enumMetadata
      );
      if (!columnDefinition.nullable && constructorArgs[i] == null) {
        throw new IllegalStateException(
            "Query returned null value for non-null column " + columnDefinition.columnName
        );
      }
    }
    try {
      Constructor<?> constructor = typeClass.getConstructors()[0];
      checkState(constructor.getParameterCount() == columnDefinitions.size());
      //noinspection unchecked
      return (E) constructor.newInstance(constructorArgs);
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  private LinkedHashMap<String, Pair<QueryColDef, Object>> generateColumnsToInsert(E entity) {
    //Map<ColumnName, JavaReflectionFieldValue>
    LinkedHashMap<String, Pair<QueryColDef, Object>> result = new LinkedHashMap<>();
    for (ColumnDefinition columnDefinition : columnDefinitions) {
      try {
        result.put(
            columnDefinition.columnName,
            Pair.of(
                toQueryColDef(columnDefinition),
                columnDefinition.javaReflectionField.get(entity)
            )
        );
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }
    return result;
  }

  private void setDeferredIdValue(E entity, Object rawIdValue) {
    try {
      Object deferredIdObj = idColumn.javaReflectionField.get(entity);
      idColumn.javaReflectionField.getType()
          .getDeclaredField("value")
          .set(deferredIdObj, rawIdValue); //settings mutable "value" to id
    } catch (IllegalAccessException | NoSuchFieldException e) {
      throw new RuntimeException(e);
    }
  }

  private boolean hasNoDeferredIdValue(E entity) {
    try {
      Object deferredIdObj = idColumn.javaReflectionField.get(entity);
      return idColumn.javaReflectionField.getType()
          .getDeclaredField("value")
          .get(deferredIdObj) == null;

    } catch (IllegalAccessException | NoSuchFieldException e) {
      throw new RuntimeException(e);
    }
  }

  private static ColumnDefinition getIdColumnName(List<ColumnDefinition> columnDefinitions) {
    for (ColumnDefinition columnDefinition : columnDefinitions) {
      if (columnDefinition.idMetadata != null) {
        return columnDefinition;
      }
    }
    throw new RuntimeException("Couldn't find ColumnDefinition for id ");
  }

  private static List<ColumnDefinition> getColumnsDefinitions(Class<?> entityClazz) {
    Field[] entityFields = entityClazz.getDeclaredFields();
    boolean hasIdField = false;
    List<ColumnDefinition> result = new ArrayList<>(entityFields.length);
    for (Field entityField : entityFields) {
      checkState(
          entityField.isAnnotationPresent(Column.class),
          "Field " + entityField.getName() + " should be annotated with @Column"
      );
      Column columnAnnotation = entityField.getAnnotation(Column.class);
      boolean hasIdAnnotation = entityField.isAnnotationPresent(Id.class);
      boolean hasEnumAnnotation = entityField.isAnnotationPresent(Enumerated.class);

      IdMetadata idMetadata = null;
      EnumMetadata enumMetadata = null;
      DbSideId dbSideId = null;
      if (hasIdAnnotation) {
        hasIdField = true;
        if (entityField.isAnnotationPresent(DbSideId.class)) {
          dbSideId = entityField.getAnnotation(DbSideId.class);
        }
        idMetadata = new IdMetadata(
            dbSideId != null, //isDbSideGenerated
            dbSideId != null ? dbSideId.sequenceName() : null //sequenceName
        );
      }
      if (hasEnumAnnotation) {
        Enumerated enumerated = entityField.getAnnotation(Enumerated.class);
        enumMetadata = new EnumMetadata(
            enumerated.accessorField().equals("") ? null : enumerated.accessorField(),
            enumerated.accessorMethod().equals("") ? null : enumerated.accessorMethod(),
            enumerated.builderMethod().equals("") ? null : enumerated.builderMethod()
        );
      }
      result.add(new ColumnDefinition(
          entityField,
          columnAnnotation.name(), //columnName
          entityField.getType(),
          idMetadata,
          enumMetadata,
          columnAnnotation.nullable() //nullable
      ));
    }
    if (!hasIdField) {
      throw new RuntimeException("Entity " + entityClazz.getName() + " should have Id field");
    }
    return Collections.unmodifiableList(result);
  }
  private static QueryColDef toQueryColDef(ColumnDefinition columnDefinition){
    return new QueryColDef(
        columnDefinition.columnName,
        columnDefinition.idMetadata,
        columnDefinition.enumMetadata
    );
  }
}
