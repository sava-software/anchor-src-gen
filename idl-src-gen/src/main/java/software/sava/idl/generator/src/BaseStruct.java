package software.sava.idl.generator.src;

import java.util.List;

public abstract class BaseStruct<F extends NamedType> implements IdlStruct {

  protected static final String LENGTH_ADD_ALIGN_TAB = " ".repeat("retur".length());

  protected final List<F> fields;

  protected BaseStruct(final List<F> fields) {
    this.fields = fields;
  }

  public final List<F> fields() {
    return fields;
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof final BaseStruct<?> that)) return false;
    return fields.equals(that.fields);
  }

  @Override
  public int hashCode() {
    return fields.hashCode();
  }
}
