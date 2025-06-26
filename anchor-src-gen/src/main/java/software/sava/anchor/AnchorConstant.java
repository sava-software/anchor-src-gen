package software.sava.anchor;

public sealed interface AnchorConstant permits BaseAnchorConstant {

  String name();

  byte[] bytes();

  void toSrc(final GenSrcContext genSrcContext, final StringBuilder src);
}
