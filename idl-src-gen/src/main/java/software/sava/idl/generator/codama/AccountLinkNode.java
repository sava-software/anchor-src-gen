package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

final class AccountLinkNode extends BaseProgramLinkNode implements LinkNode, InstructionByteDeltaNodeValue {

  AccountLinkNode(final String name, final ProgramLinkNode program) {
    super(name, program);
  }

  static AccountLinkNode parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createAccountLinkNode();
  }

  private static final class Parser extends BaseProgramLinkNode.Parser {

    private Parser() {
    }

    AccountLinkNode createAccountLinkNode() {
      return new AccountLinkNode(name, program);
    }
  }
}
