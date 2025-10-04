package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

public sealed interface PdaSeedValueNodeValue permits
    AccountValueNode,
    ArgumentValueNode,
    ValueNode {

  static PdaSeedValueNodeValue parse(final JsonIterator ji) {
    final var kind = ji.skipObjField().readString();
    return switch (kind) {
      case "accountValueNode" -> AccountValueNode.parse(ji);
      case "argumentValueNode" -> ArgumentValueNode.parse(ji);
      default -> ValueNode.parse(ji);
    };
  }
}
