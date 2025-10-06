package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

sealed interface InstructionInputValueNode permits
    ContextualValueNode,
    ProgramLinkNode,
    ValueNode {

  static InstructionInputValueNode parse(final JsonIterator ji) {
    final var kind = ji.skipObjField().readString();
    return switch (kind) {
      case "programLinkNode" -> ProgramLinkNode.parse(ji);
      // ContextualValueNode
      case "accountBumpValueNode" -> AccountBumpValueNode.parse(ji);
      case "accountValueNode" -> AccountValueNode.parse(ji);
      case "argumentValueNode" -> ArgumentValueNode.parse(ji);
      case "conditionalValueNode" -> ConditionalValueNode.parse(ji);
      case "identityValueNode" -> IdentityValueNode.parse(ji);
      case "payerValueNode" -> PayerValueNode.parse(ji);
      case "pdaValueNode" -> PdaValueNode.parse(ji);
      case "programIdValueNode" -> ProgramIdValueNode.parse(ji);
      case "resolverValueNode" -> ResolverValueNode.parse(ji);
      default -> ValueNode.parse(ji, kind);
    };
  }
}
