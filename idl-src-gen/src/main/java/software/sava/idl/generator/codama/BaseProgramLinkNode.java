package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

abstract class BaseProgramLinkNode extends BaseLinkNode {

  protected final ProgramLinkNode program;

  protected BaseProgramLinkNode(final String name, final ProgramLinkNode program) {
    super(name);
    this.program = program;
  }

  final ProgramLinkNode program() {
    return program;
  }

  static abstract class Parser extends BaseParser {

    protected ProgramLinkNode program;

    protected Parser() {
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("program", buf, offset, len)) {
        program = ProgramLinkNode.parse(ji);
        return true;
      } else {
        return super.test(buf, offset, len, ji);
      }
    }
  }

  @Override
  public final boolean equals(final Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    final var that = (BaseProgramLinkNode) o;
    return program.equals(that.program);
  }

  @Override
  public final int hashCode() {
    return program.hashCode();
  }
}
