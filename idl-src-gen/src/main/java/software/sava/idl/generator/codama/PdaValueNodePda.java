package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

public sealed interface PdaValueNodePda permits PdaLinkNode, PdaNode {

  static PdaValueNodePda parse(final JsonIterator ji) {
    final var kind = ji.skipObjField().readString();
    return switch (kind) {
      case "pdaLinkNode" -> PdaLinkNode.parse(ji);
      case "pdaNode" -> PdaNode.parse(ji);
      default -> throw new UnsupportedOperationException(kind);
    };
  }
}
