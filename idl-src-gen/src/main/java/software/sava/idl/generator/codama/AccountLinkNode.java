package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

final class AccountLinkNode extends BaseProgramLinkNode implements LinkNode {

  AccountLinkNode(final String name, final ProgramLinkNode program) {
    super(name, program);
  }

  public static AccountLinkNode parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createAccountLinkNode();
  }

  static final class Parser extends BaseProgramLinkNode.Parser {

    private Parser() {
    }

    AccountLinkNode createAccountLinkNode() {
      return new AccountLinkNode(name, program);
    }
  }
}
