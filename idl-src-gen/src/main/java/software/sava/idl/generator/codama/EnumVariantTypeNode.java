package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

sealed interface EnumVariantTypeNode permits
    EnumEmptyVariantTypeNode,
    EnumStructVariantTypeNode,
    EnumTupleVariantTypeNode {

  static EnumVariantTypeNode parse(final JsonIterator ji) {
    final var kind = ji.skipObjField().readString();
    return switch (kind) {
      case "enumEmptyVariantTypeNode" -> EnumEmptyVariantTypeNode.parse(ji);
      case "enumStructVariantTypeNode" -> EnumStructVariantTypeNode.parse(ji);
      case "enumTupleVariantTypeNode" -> EnumTupleVariantTypeNode.parse(ji);
      default -> throw new UnsupportedOperationException(kind);
    };
  }
}
