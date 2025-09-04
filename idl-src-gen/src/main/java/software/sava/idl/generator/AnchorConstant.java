package software.sava.idl.generator;

public sealed interface AnchorConstant permits BaseAnchorConstant {

  String name();

  byte[] bytes();

  void toSrc(final GenSrcContext genSrcContext, final StringBuilder src);
}
