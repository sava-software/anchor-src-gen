package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

public sealed interface CountNode extends TypeNode permits
    FixedCountNode,
    PrefixedCountNode,
    RemainderCountNode {

  static CountNode parse(final JsonIterator ji) {
    final var kind = ji.skipObjField().readString();
    return switch (kind) {
      case "fixedCountNode" -> FixedCountNode.parse(ji);
      case "prefixedCountNode" -> PrefixedCountNode.parse(ji);
      case "remainderCountNode" -> RemainderCountNode.parse(ji);
      default -> throw new UnsupportedOperationException(kind);
    };
  }
}
