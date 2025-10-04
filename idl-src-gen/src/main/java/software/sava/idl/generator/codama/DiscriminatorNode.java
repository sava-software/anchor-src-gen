package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

public sealed interface DiscriminatorNode permits
    ConstantDiscriminatorNode,
    FieldDiscriminatorNode,
    SizeDiscriminatorNode {

  static DiscriminatorNode parse(final JsonIterator ji) {
    final var kind = ji.skipObjField().readString();
    return switch (kind) {
      case "constantDiscriminatorNode" -> ConstantDiscriminatorNode.parse(ji);
      case "fieldDiscriminatorNode" -> FieldDiscriminatorNode.parse(ji);
      case "sizeDiscriminatorNode" -> SizeDiscriminatorNode.parse(ji);
      default -> throw new UnsupportedOperationException(kind);
    };
  }
}
