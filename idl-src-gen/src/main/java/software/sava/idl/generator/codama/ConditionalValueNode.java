package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

record ConditionalValueNode(ContextualValueNodeCondition condition,
                            ValueNode value,
                            InstructionInputValueNode ifTrue,
                            InstructionInputValueNode ifFalse) implements ContextualValueNode {

  public static ConditionalValueNode parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createConditionalValueNode();
  }

  static final class Parser implements FieldBufferPredicate {

    private ContextualValueNodeCondition condition;
    private ValueNode value;
    private InstructionInputValueNode ifTrue;
    private InstructionInputValueNode ifFalse;

    ConditionalValueNode createConditionalValueNode() {
      return new ConditionalValueNode(condition, value, ifTrue, ifFalse);
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("condition", buf, offset, len)) {
        condition = ContextualValueNodeCondition.parse(ji);
      } else if (fieldEquals("value", buf, offset, len)) {
        value = ValueNode.parse(ji);
      } else if (fieldEquals("ifTrue", buf, offset, len)) {
        ifTrue = InstructionInputValueNode.parse(ji);
      } else if (fieldEquals("ifFalse", buf, offset, len)) {
        ifFalse = InstructionInputValueNode.parse(ji);
      } else if (fieldEquals("kind", buf, offset, len)) {
        ji.skip();
      } else {
        throw new IllegalStateException(String.format(
            "Unhandled %s field %s.",
            getClass().getSimpleName(), new String(buf, offset, len)
        ));
      }
      return true;
    }
  }
}
