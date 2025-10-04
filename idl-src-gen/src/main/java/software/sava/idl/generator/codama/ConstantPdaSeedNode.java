package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

record ConstantPdaSeedNode(TypeNode type, ValueNode value) implements PdaSeedNode {

  public static ConstantPdaSeedNode parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createConstantPdaSeedNode();
  }

  static final class Parser implements FieldBufferPredicate {

    private TypeNode type;
    private ValueNode value;

    ConstantPdaSeedNode createConstantPdaSeedNode() {
      return new ConstantPdaSeedNode(type, value);
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("type", buf, offset, len)) {
        type = TypeNode.parse(ji);
      } else if (fieldEquals("value", buf, offset, len)) {
        value = ValueNode.parse(ji);
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
