package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

sealed interface ContextualValueNode extends InstructionInputValueNode permits
    AccountBumpValueNode,
    AccountValueNode,
    ArgumentValueNode,
    ConditionalValueNode,
    IdentityValueNode,
    PayerValueNode,
    PdaValueNode,
    ProgramIdValueNode,
    ResolverValueNode {

  static ContextualValueNode parse(final JsonIterator ji) {
    final var kind = ji.skipObjField().readString();
    return switch (kind) {
      case "accountBumpValueNode" -> AccountBumpValueNode.parse(ji);
      case "accountValueNode" -> AccountValueNode.parse(ji);
      case "argumentValueNode" -> ArgumentValueNode.parse(ji);
      case "conditionalValueNode" -> ConditionalValueNode.parse(ji);
      case "identityValueNode" -> IdentityValueNode.parse(ji);
      case "payerValueNode" -> PayerValueNode.parse(ji);
      case "pdaValueNode" -> PdaValueNode.parse(ji);
      case "programIdValueNode" -> ProgramIdValueNode.parse(ji);
      case "resolverValueNode" -> ResolverValueNode.parse(ji);
      default -> throw new UnsupportedOperationException(kind);
    };
  }
}
