package software.sava.idl.generator.anchor;

public sealed interface AnchorDefinedTypeContext
    extends AnchorTypeContext
    permits AnchorEnum, AnchorStruct, AnchorTypeContextList {
}
