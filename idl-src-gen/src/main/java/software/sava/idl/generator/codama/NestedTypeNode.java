package software.sava.idl.generator.codama;

public sealed interface NestedTypeNode extends TypeNode permits
    FixedSizeTypeNode,
    HiddenPrefixTypeNode,
    HiddenSuffixTypeNode,
    PostOffsetTypeNode,
    PreOffsetTypeNode,
    SentinelTypeNode,
    SizePrefixTypeNode {
}
