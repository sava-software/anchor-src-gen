package software.sava.anchor;

public sealed interface AnchorDefinedTypeContext
    extends AnchorTypeContext
    permits AnchorEnum, AnchorStruct, AnchorTypeContextList {
}
