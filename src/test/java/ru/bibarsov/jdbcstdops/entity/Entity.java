package ru.bibarsov.jdbcstdops.entity;

import ru.bibarsov.jdbcstdops.annotation.Column;
import ru.bibarsov.jdbcstdops.annotation.Id;
import ru.bibarsov.jdbcstdops.annotation.Table;
import javax.annotation.ParametersAreNonnullByDefault;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@Table(name = "entity")
@ParametersAreNonnullByDefault
public class Entity {

  @Id
  @Column(name = "id")
  public final long id;

  @Column(name = "name")
  public final String name;

  public Entity(long id, String name) {
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
