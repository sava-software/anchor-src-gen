package software.sava.idl.generator.codama;

sealed interface NestedTypeNode extends TypeNode permits
    FixedSizeTypeNode,
    HiddenPrefixTypeNode,
    HiddenSuffixTypeNode,
    PostOffsetTypeNode,
    PreOffsetTypeNode,
    SentinelTypeNode,
    SizePrefixTypeNode {

  TypeNode typeNode();

//  default TypeNode leafTypeNode() {
//    var typeNode = typeNode();
//    for (; ; ) {
//      if (typeNode instanceof NestedTypeNode nestedTypeNode) {
//        typeNode = nestedTypeNode.typeNode();
//      } else {
//        return typeNode;
//      }
//    }
//  }
}
