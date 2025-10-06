package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

sealed interface ContextualValueNodeCondition permits
    AccountValueNode,
    ArgumentValueNode {

  static ContextualValueNodeCondition parse(final JsonIterator ji) {
    final var kind = ji.skipObjField().readString();
    return switch (kind) {
      case "accountValueNode" -> AccountValueNode.parse(ji);
      case "argumentValueNode" -> ArgumentValueNode.parse(ji);
      default -> throw new UnsupportedOperationException(kind);
    };
  }
}
