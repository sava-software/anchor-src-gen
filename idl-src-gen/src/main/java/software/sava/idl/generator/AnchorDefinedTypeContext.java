package software.sava.idl.generator;

public sealed interface AnchorDefinedTypeContext
    extends AnchorTypeContext
    permits AnchorEnum, AnchorStruct, AnchorTypeContextList {
}
