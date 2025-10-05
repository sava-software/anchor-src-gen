package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

final class AccountValueNode extends NamedNode implements
    ContextualValueNode,
    ContextualValueNodeCondition,
    PdaSeedValueNodeValue,
    ResolverDefaultValueNodes {

  AccountValueNode(final String name) {
    super(name);
  }

  static AccountValueNode parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createAccountValueNode();
  }

  private static final class Parser extends BaseParser {

    AccountValueNode createAccountValueNode() {
      return new AccountValueNode(name);
    }
  }
}
