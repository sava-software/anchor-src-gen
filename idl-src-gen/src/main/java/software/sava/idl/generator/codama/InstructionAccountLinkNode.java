package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

final class InstructionAccountLinkNode extends BaseIxLinkNode implements LinkNode {

  InstructionAccountLinkNode(final String name, final InstructionLinkNode instruction) {
    super(name, instruction);
  }

  public static InstructionAccountLinkNode parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createInstructionAccountLinkNode();
  }

  static final class Parser extends BaseIxLinkNode.Parser {

    private Parser() {
    }

    InstructionAccountLinkNode createInstructionAccountLinkNode() {
      return new InstructionAccountLinkNode(name, instruction);
    }
  }
}
