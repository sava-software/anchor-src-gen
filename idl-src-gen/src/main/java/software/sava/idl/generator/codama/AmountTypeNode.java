package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

record AmountTypeNode(int decimals, String unit, NestedTypeNode number) implements TypeNode {

  public static AmountTypeNode parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createTypeNode();
  }

  static final class Parser implements FieldBufferPredicate {

    private int decimals;
    private String unit;
    private NestedTypeNode number;

    AmountTypeNode createTypeNode() {
      return new AmountTypeNode(decimals, unit, number);
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("decimals", buf, offset, len)) {
        decimals = ji.readInt();
      } else if (fieldEquals("unit", buf, offset, len)) {
        unit = ji.readString();
      } else if (fieldEquals("number", buf, offset, len)) {
        number = TypeNode.parseNestedTypeNode(ji, NumberTypeNode::parse);
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
