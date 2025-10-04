package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

final class PdaLinkNode extends BaseProgramLinkNode implements LinkNode {

  PdaLinkNode(final String name, final ProgramLinkNode program) {
    super(name, program);
  }

  public static PdaLinkNode parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createPdaLinkNode();
  }

  static final class Parser extends BaseProgramLinkNode.Parser {

    private Parser() {
    }

    PdaLinkNode createPdaLinkNode() {
      return new PdaLinkNode(name, program);
    }
  }
}
