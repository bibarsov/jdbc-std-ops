package ru.bibarsov.jdbcstdops.entity;

import ru.bibarsov.jdbcstdops.annotation.Column;
import ru.bibarsov.jdbcstdops.annotation.DbSideId;
import ru.bibarsov.jdbcstdops.annotation.Id;
import ru.bibarsov.jdbcstdops.annotation.Table;
import javax.annotation.ParametersAreNonnullByDefault;
import ru.bibarsov.jdbcstdops.value.DeferredId;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@Table(name = "entity_deferred")
@ParametersAreNonnullByDefault
public class EntityWithDeferredId {

  @Id
  @DbSideId(sequenceName = "entity_deferred_id_seq", sequenceValueType = Long.class)
  @Column(name = "id")
  public final DeferredId<Long> id;

  @Column(name = "name")
  public final String name;

  public EntityWithDeferredId(DeferredId<Long> id, String name) {
    this.id = id;
    this.name = name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    EntityWithDeferredId that = (EntityWithDeferredId) o;

    if (id != null ? !id.equals(that.id) : that.id != null) {
      return false;
    }
    return name.equals(that.name);
  }

  @Override
  public int hashCode() {
    return id != null ? id.hashCode() : 0;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }
}
