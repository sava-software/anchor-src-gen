package software.sava.idl.generator.codama;

abstract class EnumNode extends NamedNode {

  protected final int ordinal;

  protected EnumNode(final String name, final int ordinal) {
    super(name);
    this.ordinal = ordinal;
  }

  public final int ordinal() {
    return ordinal;
  }
}
