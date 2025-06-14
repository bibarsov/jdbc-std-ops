package ru.bibarsov.jdbcstdops.entity;

import java.time.Instant;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import ru.bibarsov.jdbcstdops.annotation.Column;
import ru.bibarsov.jdbcstdops.annotation.Enumerated;
import ru.bibarsov.jdbcstdops.annotation.Id;
import ru.bibarsov.jdbcstdops.annotation.Table;
import ru.bibarsov.jdbcstdops.value.EntType1;
import ru.bibarsov.jdbcstdops.value.EntType2;
import ru.bibarsov.jdbcstdops.value.EntType3;

@Table(name = "entity_composite")
@ParametersAreNonnullByDefault
public final class EntityWithCompositeId {

  @Id(compositeKey = true)
  public final EntityId id;
  @Column(name = "name")
  public final String name;

  public EntityWithCompositeId(

      EntityId id,

      String name

  ) {
    this.id = id;
    this.name = name;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

  public EntityId id() {
    return id;
  }

  public String name() {
    return name;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    var that = (EntityWithCompositeId) obj;
    return Objects.equals(this.id, that.id) &&
           Objects.equals(this.name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name);
  }

  public static final class EntityId {

    @Column(name = "first_part")
    public final int firstPart;
    @Column(name = "second_part")
    public final int secondPart;

    public EntityId(
        int firstPart,
        int secondPart

    ) {
      this.firstPart = firstPart;
      this.secondPart = secondPart;
    }

    public int firstPart() {
      return firstPart;
    }

    public int secondPart() {
      return secondPart;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj == null || obj.getClass() != this.getClass()) {
        return false;
      }
      var that = (EntityId) obj;
      return this.firstPart == that.firstPart &&
             this.secondPart == that.secondPart;
    }

    @Override
    public int hashCode() {
      return Objects.hash(firstPart, secondPart);
    }

    @Override
    public String toString() {
      return "EntityId[" +
             "firstPart=" + firstPart + ", " +
             "secondPart=" + secondPart + ']';
    }


  }
}
