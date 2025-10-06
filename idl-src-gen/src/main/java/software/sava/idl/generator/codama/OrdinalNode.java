package software.sava.idl.generator.codama;

abstract sealed class OrdinalNode extends NamedNode permits EnumEmptyVariantTypeNode,
    EnumStructVariantTypeNode,
    EnumTupleVariantTypeNode {

  protected final int ordinal;

  protected OrdinalNode(final String name, final int ordinal) {
    super(name);
    this.ordinal = ordinal;
  }

  final int ordinal() {
    return ordinal;
  }
}
