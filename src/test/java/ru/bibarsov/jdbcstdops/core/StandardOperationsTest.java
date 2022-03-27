package ru.bibarsov.jdbcstdops.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.bibarsov.jdbcstdops.util.Preconditions.checkNotNull;

import ru.bibarsov.jdbcstdops.entity.Entity;
import ru.bibarsov.jdbcstdops.entity.EntityWithDeferredId;
import ru.bibarsov.jdbcstdops.helper.DatabaseManager;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.ParametersAreNonnullByDefault;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.bibarsov.jdbcstdops.value.DeferredId;

@ParametersAreNonnullByDefault
public class StandardOperationsTest {

  private static EmbeddedPostgres db;
  private static NamedParameterJdbcTemplate jdbcTemplate;

  @BeforeClass
  public static void beforeClass() throws IOException {
    db = EmbeddedPostgres.builder().start();
    jdbcTemplate = new NamedParameterJdbcTemplate(db.getTemplateDatabase());
  }

  @AfterClass
  public static void afterClass() throws IOException {
    db.close();
  }

  @Before
  public void beforeTest() {
    DatabaseManager.initDatabase(jdbcTemplate);
  }

  @After
  public void afterTest() {
    DatabaseManager.truncateDatabase(jdbcTemplate);
  }

  @Test
  public void testFindOne() {
    //simple entity
    var entityStdOps = new StandardOperations<>(Entity.class, jdbcTemplate);
    var entity = new Entity(1L, "example-1");
    entityStdOps.create(entity);
    Assert.assertEquals(entity, entityStdOps.findOne(1L));

    //entity with deferredId
    var entityDefStdOps = new StandardOperations<>(EntityWithDeferredId.class, jdbcTemplate);
    var defEx = new EntityWithDeferredId(DeferredId.create(), "example-2");
    entityDefStdOps.create(defEx);
    Assert.assertEquals(defEx, entityDefStdOps.findOne(1L));
  }

  @Test
  public void testGetAll() {
    var entityStdOps = new StandardOperations<>(Entity.class, jdbcTemplate);
    List<Entity> entities = List.of(
        new Entity(1L, "example-1"),
        new Entity(2L, "example-2"),
        new Entity(3L, "example-3")
    );
    for (Entity entity : entities) {
      entityStdOps.create(entity);
    }
    List<Entity> all = entityStdOps.getAll().stream()
        .sorted(Comparator.comparingLong(e -> e.id))
        .collect(Collectors.toList());
    assertThat(all, CoreMatchers.is(entities));
  }

  @Test
  public void testGetAllForDeferred() {
    var entityDefStdOps = new StandardOperations<>(EntityWithDeferredId.class, jdbcTemplate);
    List<EntityWithDeferredId> entities = List.of(
        new EntityWithDeferredId(DeferredId.create(), "example-1"),
        new EntityWithDeferredId(DeferredId.create(), "example-2"),
        new EntityWithDeferredId(DeferredId.create(), "example-3")
    );
    for (EntityWithDeferredId entity : entities) {
      entityDefStdOps.create(entity);
    }
    List<EntityWithDeferredId> all = entityDefStdOps.getAll().stream()
        .sorted(Comparator.comparingLong(e -> checkNotNull(e.id.value)))
        .collect(Collectors.toList());
    assertThat(all, CoreMatchers.is(entities));
  }

  @Test
  public void testCreate() {
    var entityStdOps = new StandardOperations<>(Entity.class, jdbcTemplate);
    var entity = new Entity(1L, "simple");
    entityStdOps.create(entity);
    Assert.assertEquals(entity, entityStdOps.findOne(1L));
  }

  @Test
  public void testCreateForDeferred() {
    var entityDefStdOps = new StandardOperations<>(EntityWithDeferredId.class, jdbcTemplate);

    var entityDefEmpty = new EntityWithDeferredId(DeferredId.create(), "def-empty");
    entityDefStdOps.create(entityDefEmpty);
    Assert.assertEquals(entityDefEmpty, entityDefStdOps.findOne(1L));

    var entityDefExpl = new EntityWithDeferredId(DeferredId.ofImmediateId(3L), "def-explicit");
    entityDefStdOps.create(entityDefExpl);
    Assert.assertEquals(entityDefExpl, entityDefStdOps.findOne(3L));
  }

  @Test
  public void testCreateOrUpdate() {
    var entityDefStdOps = new StandardOperations<>(EntityWithDeferredId.class, jdbcTemplate);
    var entity = new EntityWithDeferredId(DeferredId.create(), "old");
    var updatedEntity = new EntityWithDeferredId(DeferredId.ofImmediateId(1L), "new");
    entityDefStdOps.createOrUpdate(entity);
    entityDefStdOps.createOrUpdate(updatedEntity);
    Assert.assertEquals(updatedEntity, entityDefStdOps.findOne(1L));
  }

  @Test
  public void testDelete() {
    var entityDefStdOps = new StandardOperations<>(EntityWithDeferredId.class, jdbcTemplate);
    var entity = new EntityWithDeferredId(DeferredId.ofImmediateId(1L), "new");
    entityDefStdOps.create(entity);
    entityDefStdOps.deleteOne(checkNotNull(entity.id.value));
    Assert.assertNull(entityDefStdOps.findOne(checkNotNull(entity.id.value)));
    entityDefStdOps.deleteOne(checkNotNull(entity.id.value));
  }
}
