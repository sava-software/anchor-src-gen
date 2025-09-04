package software.sava.idl.generator;

abstract sealed class BaseAnchorConstant implements AnchorConstant permits AnchorBigIntegerConstant, AnchorBytesConstant, AnchorIntConstant, AnchorLongConstant, AnchorStringConstant {

  protected final String name;

  BaseAnchorConstant(final String name) {
    this.name = name;
  }

  @Override
  public final String name() {
    return name;
  }

  @Override
  public byte[] bytes() {
    throw new IllegalStateException();
  }
}
