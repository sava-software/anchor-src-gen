package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

record InstructionByteDeltaNode(InstructionByteDeltaNodeValue value, boolean withHeader, boolean subtract) {

  public static InstructionByteDeltaNode parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createInstructionByteDeltaNode();
  }

  static final class Parser implements FieldBufferPredicate {

    private InstructionByteDeltaNodeValue value;
    private boolean withHeader;
    private boolean subtract;

    private Parser() {
    }

    InstructionByteDeltaNode createInstructionByteDeltaNode() {
      return new InstructionByteDeltaNode(
          value,
          withHeader,
          subtract
      );
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("value", buf, offset, len)) {
        value = InstructionByteDeltaNodeValue.parse(ji);
        return true;
      } else if (fieldEquals("withHeader", buf, offset, len)) {
        withHeader = ji.readBoolean();
        return true;
      } else if (fieldEquals("subtract", buf, offset, len)) {
        subtract = ji.readBoolean();
        return true;
      } else {
        ji.skip();
        return true;
      }
    }
  }
}
