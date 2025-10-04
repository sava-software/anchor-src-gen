package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

final class DefinedTypeLinkNode extends BaseLinkNode implements LinkNode {

  private final String program;

  DefinedTypeLinkNode(final String name, final String program) {
    super(name);
    this.program = program;
  }

  String program() {
    return program;
  }

  public static DefinedTypeLinkNode parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createDefinedTypeLinkNode();
  }

  static final class Parser extends BaseParser {

    private String program;

    DefinedTypeLinkNode createDefinedTypeLinkNode() {
      return new DefinedTypeLinkNode(name, program);
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("program", buf, offset, len)) {
        program = ji.readString();
        return true;
      } else {
        return super.test(buf, offset, len, ji);
      }
    }
  }
}
