package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

final class AccountValueNode extends NamedNode implements
    ContextualValueNode,
    ContextualValueNodeCondition,
    PdaSeedValueNodeValue {

  public AccountValueNode(final String name) {
    super(name);
  }

  public static AccountValueNode parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createAccountValueNode();
  }

  static final class Parser extends BaseParser {

    AccountValueNode createAccountValueNode() {
      return new AccountValueNode(name);
    }
  }
}
