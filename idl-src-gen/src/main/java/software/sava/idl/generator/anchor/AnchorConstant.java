package software.sava.idl.generator.anchor;

public sealed interface AnchorConstant permits BaseAnchorConstant {

  String name();

  byte[] bytes();

  void toSrc(final SrcGenContext srcGenContext, final StringBuilder src);
}
