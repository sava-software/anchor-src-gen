package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

final class ProgramLinkNode extends BaseLinkNode implements LinkNode, InstructionInputValueNode {

  ProgramLinkNode(final String name) {
    super(name);
  }

  static ProgramLinkNode parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createProgramLinkNode();
  }

  private static final class Parser extends BaseParser {

    private Parser() {
    }

    ProgramLinkNode createProgramLinkNode() {
      return new ProgramLinkNode(name);
    }
  }
}
