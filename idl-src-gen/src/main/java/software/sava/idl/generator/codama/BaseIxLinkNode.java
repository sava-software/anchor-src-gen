package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

abstract class BaseIxLinkNode extends BaseLinkNode {

  protected final InstructionLinkNode instruction;

  protected BaseIxLinkNode(final String name, final InstructionLinkNode instruction) {
    super(name);
    this.instruction = instruction;
  }

  final InstructionLinkNode instruction() {
    return instruction;
  }

  static abstract class Parser extends BaseParser {

    protected InstructionLinkNode instruction;

    protected Parser() {
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("instruction", buf, offset, len)) {
        instruction = InstructionLinkNode.parse(ji);
        return true;
      } else {
        return super.test(buf, offset, len, ji);
      }
    }
  }
}
