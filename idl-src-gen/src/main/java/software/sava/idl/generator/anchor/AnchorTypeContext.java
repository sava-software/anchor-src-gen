package software.sava.idl.generator.anchor;

import software.sava.idl.generator.src.TypeContext;

public sealed interface AnchorTypeContext extends TypeContext permits
    AnchorDefinedTypeContext, AnchorReferenceTypeContext {

  AnchorType type();
}
