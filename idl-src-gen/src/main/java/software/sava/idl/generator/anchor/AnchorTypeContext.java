package software.sava.idl.generator.anchor;

public sealed interface AnchorTypeContext extends TypeContext permits AnchorDefinedTypeContext,
    AnchorReferenceTypeContext {

  AnchorType type();
}
