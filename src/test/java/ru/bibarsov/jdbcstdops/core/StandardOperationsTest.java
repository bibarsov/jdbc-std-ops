package ru.bibarsov.jdbcstdops.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.bibarsov.jdbcstdops.util.Preconditions.checkNotNull;

import java.time.Instant;
import ru.bibarsov.jdbcstdops.entity.Entity;
import ru.bibarsov.jdbcstdops.entity.EntityWithCompositeId;
import ru.bibarsov.jdbcstdops.entity.EntityWithCompositeId.EntityId;
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
import ru.bibarsov.jdbcstdops.value.EntType1;
import ru.bibarsov.jdbcstdops.value.EntType2;
import ru.bibarsov.jdbcstdops.value.EntType3;

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
    var entity = new Entity(
        1L,
        "example-1",
        null,
        EntType1.A,
        EntType2.E,
        EntType3.G,
        Instant.EPOCH
    );
    entityStdOps.create(entity);
    Assert.assertEquals(entity, entityStdOps.findOne(1L));

    //entity with deferredId
    var entityDefStdOps = new StandardOperations<>(EntityWithDeferredId.class, jdbcTemplate);
    var defEx = new EntityWithDeferredId(DeferredId.create(), "example-2");
    entityDefStdOps.create(defEx);
    Assert.assertEquals(defEx, entityDefStdOps.findOne(1L));


    //entity with composite id
    var entityCompositeStdOps = new StandardOperations<>(EntityWithCompositeId.class, jdbcTemplate);
    var entComp = new EntityWithCompositeId(new EntityId(1,2), "example-1");
    entityCompositeStdOps.create(entComp);
//    Assert.assertEquals(entComp, entityCompositeStdOps.findOne(new EntityId(1,2)));
  }

  @Test
  public void testGetAll() {
    var entityStdOps = new StandardOperations<>(Entity.class, jdbcTemplate);
    List<Entity> entities = List.of(
        new Entity(1L, "example-1", null, EntType1.A, EntType2.D, EntType3.G, Instant.EPOCH),
        new Entity(2L, "example-2", "notnull", EntType1.B, EntType2.E, EntType3.H, Instant.EPOCH),
        new Entity(3L, "example-3", null, EntType1.C, EntType2.F, EntType3.I, Instant.EPOCH)
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
    var entity = new Entity(1L, "simple", null, EntType1.A, EntType2.F, EntType3.G, Instant.EPOCH);
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
    var entity2 = new EntityWithDeferredId(DeferredId.ofImmediateId(2L), "new2");
    entityDefStdOps.create(entity);
    entityDefStdOps.create(entity2);
    entityDefStdOps.deleteOne(checkNotNull(entity.id.value));
    Assert.assertNull(entityDefStdOps.findOne(checkNotNull(entity.id.value)));
    Assert.assertEquals(entity2, entityDefStdOps.findOne(2L));
    entityDefStdOps.deleteOne(checkNotNull(entity.id.value));
  }
}
