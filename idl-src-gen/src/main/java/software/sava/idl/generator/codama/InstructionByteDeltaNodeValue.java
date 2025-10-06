package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

sealed interface InstructionByteDeltaNodeValue permits
    AccountLinkNode,
    ArgumentValueNode,
    ValueNode.Number {

  static InstructionByteDeltaNodeValue parse(final JsonIterator ji) {
    final var kind = ji.skipObjField().readString();
    return switch (kind) {
      case "accountLinkNode" -> AccountLinkNode.parse(ji);
      case "numberValueNode" -> ValueNode.Number.parse(ji);
      default -> ArgumentValueNode.parse(ji);
    };
  }
}
