package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

final class AccountBumpValueNode extends NamedNode implements ContextualValueNode {

  AccountBumpValueNode(final String name) {
    super(name);
  }

  static AccountBumpValueNode parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createAccountBumpValueNode();
  }

  private static final class Parser extends BaseParser {

    AccountBumpValueNode createAccountBumpValueNode() {
      return new AccountBumpValueNode(name);
    }
  }
}
