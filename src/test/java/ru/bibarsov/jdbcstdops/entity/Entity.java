package ru.bibarsov.jdbcstdops.entity;

import java.time.Instant;
import javax.annotation.Nullable;
import ru.bibarsov.jdbcstdops.annotation.Column;
import ru.bibarsov.jdbcstdops.annotation.Enumerated;
import ru.bibarsov.jdbcstdops.annotation.Id;
import ru.bibarsov.jdbcstdops.annotation.Table;
import javax.annotation.ParametersAreNonnullByDefault;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import ru.bibarsov.jdbcstdops.value.EntType1;
import ru.bibarsov.jdbcstdops.value.EntType2;
import ru.bibarsov.jdbcstdops.value.EntType3;

@Table(name = "entity")
@ParametersAreNonnullByDefault
public class Entity {

  @Id
  @Column(name = "id")
  public final long id;

  @Column(name = "name")
  public final String name;

  @Column(name = "nullname", nullable = true)
  @Nullable
  public final String nullName;

  @Enumerated
  @Column(name = "type_1")
  public final EntType1 entityType1;

  @Enumerated(accessorMethod = "toValue", builderMethod = "ofValue")
  @Column(name = "type_2")
  public final EntType2 entityType2;

  @Enumerated(accessorField = "value",builderMethod = "ofValue")
  @Column(name = "type_3")
  public final EntType3 entityType3;

  @Column(name = "created_at")
  public final Instant createdAt;

  public Entity(
      long id,
      String name,
      @Nullable String nullName,
      EntType1 entityType1,
      EntType2 entityType2,
      EntType3 entityType3,
      Instant createdAt
  ) {
    this.id = id;
    this.name = name;
    this.nullName = nullName;
    this.entityType1 = entityType1;
    this.entityType2 = entityType2;
    this.entityType3 = entityType3;
    this.createdAt = createdAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Entity entity = (Entity) o;

    if (id != entity.id) {
      return false;
    }
    return name.equals(entity.name);
  }

  @Override
  public int hashCode() {
    return (int) (id ^ (id >>> 32));
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }
}
