package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

sealed interface PdaSeedNode permits
    ConstantPdaSeedNode,
    VariablePdaSeedNode {

  static PdaSeedNode parse(final JsonIterator ji) {
    final var kind = ji.skipObjField().readString();
    return switch (kind) {
      case "constantPdaSeedNode" -> ConstantPdaSeedNode.parse(ji);
      case "variablePdaSeedNode" -> VariablePdaSeedNode.parse(ji);
      default -> throw new UnsupportedOperationException(kind);
    };
  }
}
