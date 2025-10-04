package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

public sealed interface InstructionInputValueNode permits
    ContextualValueNode,
    ProgramLinkNode,
    ValueNode {

  static InstructionInputValueNode parse(final JsonIterator ji) {
    final var kind = ji.skipObjField().readString();
    return switch (kind) {
      case "programIdValueNode" -> ProgramIdValueNode.parse(ji);
      // ContextualValueNode
      case "accountBumpValueNode" -> AccountBumpValueNode.parse(ji);
      case "accountValueNode" -> AccountValueNode.parse(ji);
      case "argumentValueNode" -> ArgumentValueNode.parse(ji);
      case "identityValueNode" -> IdentityValueNode.parse(ji);
      case "payerValueNode" -> PayerValueNode.parse(ji);
      case "programLinkNode" -> ProgramLinkNode.parse(ji);
      default -> ValueNode.parse(ji);
    };
  }
}
