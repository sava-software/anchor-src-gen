package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

final class InstructionLinkNode extends BaseProgramLinkNode implements LinkNode {

  InstructionLinkNode(final String name, final ProgramLinkNode program) {
    super(name, program);
  }

  static InstructionLinkNode parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createInstructionLinkNode();
  }

  private static final class Parser extends BaseProgramLinkNode.Parser {

    private Parser() {
    }

    InstructionLinkNode createInstructionLinkNode() {
      return new InstructionLinkNode(name, program);
    }
  }
}
