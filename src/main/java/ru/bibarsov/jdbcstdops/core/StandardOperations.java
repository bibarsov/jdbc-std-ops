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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
  private final List<QueryColDef> idColumns;
  private final List<String> selectColumns;

  private final ColumnValueConverter columnValueConverter = new ColumnValueConverter();

  public StandardOperations(Class<E> entityClazz, NamedParameterJdbcTemplate jdbcTemplate) {
    checkArgument(entityClazz.getConstructors().length == 1);
    checkArgument(entityClazz.isAnnotationPresent(Table.class));
    String tableName = entityClazz.getAnnotation(Table.class).name();

    this.jdbcTemplate = jdbcTemplate;
    this.columnDefinitions = getColumnsDefinitions(entityClazz);
    this.idColumn = getIdColumnName(columnDefinitions);
    this.idColumns = Collections.unmodifiableList(buildIdColumns(idColumn));
    this.selectColumns = Collections.unmodifiableList(flattenColumnNames(columnDefinitions));
    this.queriesLambdas = prepareQueriesLambdas(entityClazz, tableName);
  }

  public void create(E entity) {
    QueryFunction queryFunction = queriesLambdas.get(OperationType.CREATE);
    LinkedHashMap<String, Pair<QueryColDef, Object>> columnsToInsert
        = generateColumnsToInsert(entity);

  IdMetadata idMetadata = idColumn.idMetadata;
  if (idMetadata != null
    && idMetadata.isDbSideGenerated
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

  IdMetadata idMetadata = idColumn.idMetadata;
  if (idMetadata != null
    && idMetadata.isDbSideGenerated
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
    @SuppressWarnings("unchecked")
    E result = (E) queryFunction.query(
        jdbcTemplate,
        queryBuilder -> applyIdConditions(queryBuilder, id)
    );
    return result;
  }

  public List<E> getAll() {
    QueryFunction queryFunction = queriesLambdas.get(OperationType.GET_ALL);
    @SuppressWarnings("unchecked")
    List<E> result = (List<E>) queryFunction.query(jdbcTemplate, (ignored -> {
    }));
    return result;
  }

  public List<E> getAll(int offset, int limit) {
    QueryFunction queryFunction = queriesLambdas.get(OperationType.GET_ALL);
    @SuppressWarnings("unchecked")
    List<E> result = (List<E>) queryFunction.query(jdbcTemplate, (queryBuilder -> {
      queryBuilder.setOffset(offset).setLimit(limit);
    }));
    return result;
  }

  public void deleteOne(ID id) {
    QueryFunction queryFunction = queriesLambdas.get(OperationType.DELETE);
    queryFunction.query(
        jdbcTemplate,
        queryBuilder -> applyIdConditions(queryBuilder, id)
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
                .setIdColumns(idColumns);
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
                .setIdColumns(idColumns);
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
          result.put(operationType, (jdbcTemplate, queryBuilderMutator) -> {
            var queryBuilder = new QueryBuilder(columnValueConverter)
                .setType(QueryType.SELECT)
                .setTableName(tableName)
        .setIdColumns(idColumns)
        .setColumnsToSelect(selectColumns);
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
          result.put(operationType, (jdbcTemplate, queryBuilderMutator) -> {
            var queryBuilder = new QueryBuilder(columnValueConverter)
                .setType(QueryType.SELECT)
                .setTableName(tableName)
                .setIdColumns(idColumns)
                .setColumnsToSelect(selectColumns);
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
                .setIdColumns(idColumns);
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
      Object value = columnDefinition.isComposite()
          ? mapCompositeValue(columnDefinition, rs)
          : mapSimpleValue(columnDefinition, rs);
      if (!columnDefinition.nullable && value == null) {
        throw new IllegalStateException(
            "Query returned null value for non-null column " + columnDefinition.columnName
        );
      }
      constructorArgs[i] = value;
    }
    try {
      Constructor<?> constructor = typeClass.getConstructors()[0];
      checkState(constructor.getParameterCount() == columnDefinitions.size());
      @SuppressWarnings("unchecked")
      E instance = (E) constructor.newInstance(constructorArgs);
      return instance;
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  private Object mapSimpleValue(ColumnDefinition columnDefinition, ResultSet rs)
      throws SQLException {
    IdMetadata idMetadata = columnDefinition.idMetadata;
    EnumMetadata enumMetadata = columnDefinition.enumMetadata;
    String columnName = checkNotNull(
        columnDefinition.columnName,
        "Column name must be set for simple field " + columnDefinition.javaReflectionField.getName()
    );
    return columnValueConverter.toJavaTypeValue(
        rs,
        columnName,
        idMetadata != null && idMetadata.isDbSideGenerated
            ? DeferredId.class
            : ReflectionTools.primitiveToWrapper(columnDefinition.valueClass),
        enumMetadata
    );
  }

  private Object mapCompositeValue(ColumnDefinition columnDefinition, ResultSet rs)
      throws SQLException {
    checkState(columnDefinition.isComposite(), "ColumnDefinition is not composite");
    Constructor<?> constructor = checkNotNull(
        columnDefinition.compositeConstructor,
        "Composite constructor is not defined for field "
            + columnDefinition.javaReflectionField.getName()
    );
    Object[] args = new Object[columnDefinition.compositeComponents.size()];
    for (int i = 0; i < columnDefinition.compositeComponents.size(); i++) {
      ColumnComponentDefinition component = columnDefinition.compositeComponents.get(i);
      Object value = columnValueConverter.toJavaTypeValue(
          rs,
          component.columnName,
          ReflectionTools.primitiveToWrapper(component.valueClass),
          component.enumMetadata
      );
      if (!component.nullable && value == null) {
        throw new IllegalStateException(
            "Query returned null value for non-null column " + component.columnName
        );
      }
      args[i] = value;
    }
    try {
      return constructor.newInstance(args);
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  private LinkedHashMap<String, Pair<QueryColDef, Object>> generateColumnsToInsert(E entity) {
    //Map<ColumnName, JavaReflectionFieldValue>
    LinkedHashMap<String, Pair<QueryColDef, Object>> result = new LinkedHashMap<>();
    for (ColumnDefinition columnDefinition : columnDefinitions) {
      try {
        if (columnDefinition.isComposite()) {
          Object compositeValue = columnDefinition.javaReflectionField.get(entity);
          compositeValue = checkNotNull(
              compositeValue,
              "Composite id field " + columnDefinition.javaReflectionField.getName()
                  + " must not be null"
          );
          for (ColumnComponentDefinition component : columnDefinition.compositeComponents) {
            result.put(
                component.columnName,
                Pair.of(
                    toQueryColDef(component, columnDefinition),
                    component.readValue(compositeValue)
                )
            );
          }
        } else {
          result.put(
              checkNotNull(columnDefinition.columnName,
                  "Column name must not be null for non composite field"),
              Pair.of(
                  toQueryColDef(columnDefinition),
                  columnDefinition.javaReflectionField.get(entity)
              )
          );
        }
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

  private void applyIdConditions(QueryBuilder queryBuilder, Object idValue) {
    if (idColumn.isComposite()) {
      Object compositeId = checkNotNull(idValue, "Composite id value must not be null");
      for (ColumnComponentDefinition component : idColumn.compositeComponents) {
        queryBuilder.setCondition(
            toQueryColDef(component, idColumn),
            component.readValue(compositeId)
        );
      }
    } else {
      queryBuilder.setCondition(toQueryColDef(idColumn), idValue);
    }
  }

  private static List<QueryColDef> buildIdColumns(ColumnDefinition idColumnDefinition) {
    if (idColumnDefinition.isComposite()) {
      return idColumnDefinition.compositeComponents.stream()
          .map(component -> toQueryColDef(component, idColumnDefinition))
          .collect(Collectors.toList());
    }
    return List.of(toQueryColDef(idColumnDefinition));
  }

  private static List<String> flattenColumnNames(List<ColumnDefinition> definitions) {
    List<String> columns = new ArrayList<>();
    for (ColumnDefinition definition : definitions) {
      if (definition.isComposite()) {
        for (ColumnComponentDefinition component : definition.compositeComponents) {
          columns.add(component.columnName);
        }
      } else {
        columns.add(checkNotNull(
            definition.columnName,
            "Column name must be provided for field "
                + definition.javaReflectionField.getName()
        ));
      }
    }
    return columns;
  }

  private static List<ColumnDefinition> getColumnsDefinitions(Class<?> entityClazz) {
    Field[] entityFields = entityClazz.getDeclaredFields();
    boolean hasIdField = false;
    List<ColumnDefinition> result = new ArrayList<>(entityFields.length);
    for (Field entityField : entityFields) {
      entityField.setAccessible(true);
      checkState(
          entityField.isAnnotationPresent(Column.class) || checkIsCompositeKey(entityField),
          "Field " + entityField.getName() + " should be annotated with @Column "
          + "or with @Id and compositeKey=true"
      );
      Column columnAnnotation = entityField.getAnnotation(Column.class);
      boolean hasIdAnnotation = entityField.isAnnotationPresent(Id.class);
      boolean hasEnumAnnotation = entityField.isAnnotationPresent(Enumerated.class);

      IdMetadata idMetadata = null;
      EnumMetadata enumMetadata = null;
      DbSideId dbSideId = null;
      Pair<Constructor<?>, List<ColumnComponentDefinition>> compositeMetadata = null;
      if (hasIdAnnotation) {
        Id idAnnotation = entityField.getAnnotation(Id.class);
        hasIdField = true;
        if (entityField.isAnnotationPresent(DbSideId.class)) {
          if (idAnnotation.compositeKey()) {
            throw new IllegalStateException(
                "Entity field " + entityField.getName() +
                " can't be composite and db-side generated key at the same time");
          }
          dbSideId = entityField.getAnnotation(DbSideId.class);
        }
        idMetadata = new IdMetadata(
            dbSideId != null,
            idAnnotation.compositeKey(),
            dbSideId != null ? dbSideId.sequenceName() : null
        );
        if (idAnnotation.compositeKey()) {
          compositeMetadata = buildCompositeMetadata(entityField);
        }
      }
      if (hasEnumAnnotation) {
        Enumerated enumerated = entityField.getAnnotation(Enumerated.class);
        enumMetadata = new EnumMetadata(
            enumerated.accessorField().equals("") ? null : enumerated.accessorField(),
            enumerated.accessorMethod().equals("") ? null : enumerated.accessorMethod(),
            enumerated.builderMethod().equals("") ? null : enumerated.builderMethod()
        );
      }
    List<ColumnComponentDefinition> componentDefinitions =
      compositeMetadata != null
        ? Objects.requireNonNull(compositeMetadata.right)
        : Collections.<ColumnComponentDefinition>emptyList();
    result.add(new ColumnDefinition(
      entityField,
      columnAnnotation != null ? columnAnnotation.name() : null,
      entityField.getType(),
      idMetadata,
      enumMetadata,
      columnAnnotation != null && columnAnnotation.nullable(),
      componentDefinitions,
      compositeMetadata != null ? compositeMetadata.left : null
    ));
    }
    if (!hasIdField) {
      throw new RuntimeException("Entity " + entityClazz.getName() + " should have Id field");
    }
    return Collections.unmodifiableList(result);
  }

  private static Pair<Constructor<?>, List<ColumnComponentDefinition>> buildCompositeMetadata(
      Field idField
  ) {
    Class<?> compositeClass = idField.getType();
    Field[] declaredFields = compositeClass.getDeclaredFields();
    List<ColumnComponentDefinition> components = new ArrayList<>();
    for (Field declaredField : declaredFields) {
      declaredField.setAccessible(true);
      Column column = declaredField.getAnnotation(Column.class);
      if (column == null) {
        continue;
      }
      EnumMetadata componentEnumMetadata = null;
      if (declaredField.isAnnotationPresent(Enumerated.class)) {
        Enumerated enumerated = declaredField.getAnnotation(Enumerated.class);
        componentEnumMetadata = new EnumMetadata(
            enumerated.accessorField().equals("") ? null : enumerated.accessorField(),
            enumerated.accessorMethod().equals("") ? null : enumerated.accessorMethod(),
            enumerated.builderMethod().equals("") ? null : enumerated.builderMethod()
        );
      }
      components.add(new ColumnComponentDefinition(
          declaredField,
          column.name(),
          declaredField.getType(),
          componentEnumMetadata,
          column.nullable()
      ));
    }
    checkState(!components.isEmpty(),
        "Composite id field " + idField.getName() + " must declare at least one @Column");
    Constructor<?> constructor = findMatchingConstructor(compositeClass, components);
    constructor.setAccessible(true);
    return Pair.of(constructor, Collections.unmodifiableList(components));
  }

  private static Constructor<?> findMatchingConstructor(
      Class<?> targetClass,
      List<ColumnComponentDefinition> components
  ) {
    Constructor<?>[] constructors = targetClass.getDeclaredConstructors();
    Constructor<?> match = null;
    for (Constructor<?> constructor : constructors) {
      Class<?>[] parameterTypes = constructor.getParameterTypes();
      if (parameterTypes.length != components.size()) {
        continue;
      }
      boolean matches = true;
      for (int i = 0; i < parameterTypes.length; i++) {
        Class<?> parameterType = ReflectionTools.primitiveToWrapper(parameterTypes[i]);
        Class<?> componentType = ReflectionTools.primitiveToWrapper(components.get(i).valueClass);
        if (!parameterType.isAssignableFrom(componentType)) {
          matches = false;
          break;
        }
      }
      if (matches) {
        checkState(match == null,
            "Multiple constructors match composite id mapping for " + targetClass.getName());
        match = constructor;
      }
    }
    checkState(match != null,
        "Couldn't find constructor matching composite id mapping for " + targetClass.getName());
    return match;
  }

  private static boolean checkIsCompositeKey(Field entityField) {
    Id annotation = entityField.getAnnotation(Id.class);
    if (annotation!=null) {
      return annotation.compositeKey();
    }
    return false;
  }

  private static QueryColDef toQueryColDef(ColumnDefinition columnDefinition){
  checkState(!columnDefinition.isComposite(),
    "ColumnDefinition " + columnDefinition.javaReflectionField.getName() + " is composite");
  return new QueryColDef(
    checkNotNull(columnDefinition.columnName,
      "Column name must be provided for field "
        + columnDefinition.javaReflectionField.getName()),
    columnDefinition.idMetadata,
    columnDefinition.enumMetadata
  );
  }

  private static QueryColDef toQueryColDef(
    ColumnComponentDefinition componentDefinition,
    ColumnDefinition parentDefinition
  ) {
  return new QueryColDef(
    componentDefinition.columnName,
    parentDefinition.idMetadata,
    componentDefinition.enumMetadata
  );
  }
}
