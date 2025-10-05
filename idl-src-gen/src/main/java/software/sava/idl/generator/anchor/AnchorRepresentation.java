package software.sava.idl.generator.anchor;

public sealed interface AnchorRepresentation permits AnchorCRepresentation, AnchorRustRepresentation, AnchorTransparentRepresentation {

  default boolean packed() {
    return false;
  }

  default int align() {
    throw new UnsupportedOperationException();
  }
}
