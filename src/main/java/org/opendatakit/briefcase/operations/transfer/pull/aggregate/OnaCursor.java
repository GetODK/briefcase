package org.opendatakit.briefcase.operations.transfer.pull.aggregate;

import static org.opendatakit.briefcase.operations.transfer.pull.aggregate.Cursor.Type.ONA;

import java.util.Objects;
import java.util.Optional;

/**
 * This class represents Ona's implementation of a Cursor.
 * <p>
 * It's much simpler than the {@link AggregateCursor} because
 * it only stores a numeric ID.
 * <p>
 * This makes it possible to support the "start from last" feature,
 * but not the "start from date", since Ona cursors don't include
 * any date.
 */
public class OnaCursor implements Cursor {
  private final Optional<Long> value;

  private OnaCursor(Optional<Long> value) {
    this.value = value;
  }

  public static Cursor from(String rawValue) {
    return new OnaCursor(Optional.ofNullable(rawValue).map(Long::parseLong));
  }

  @Override
  public int compareTo(Cursor o) {
    return Long.compare(value.orElse(-1L), ((OnaCursor) o).value.orElse(-1L));
  }

  @Override
  public Type getType() {
    return ONA;
  }

  @Override
  public String getValue() {
    return value.map(Object::toString).orElse("");
  }

  @Override
  public boolean isEmpty() {
    return value.isEmpty();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    OnaCursor onaCursor = (OnaCursor) o;
    return Objects.equals(value, onaCursor.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    return "OnaCursor{" +
        "value=" + value +
        '}';
  }
}
