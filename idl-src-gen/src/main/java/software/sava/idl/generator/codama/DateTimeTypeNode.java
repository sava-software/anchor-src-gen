package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

record DateTimeTypeNode(TypeNode number) implements TypeNode {

  public static DateTimeTypeNode parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createTypeNode();
  }

  static final class Parser implements FieldBufferPredicate {

    private TypeNode number;

    DateTimeTypeNode createTypeNode() {
      return new DateTimeTypeNode(number);
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("number", buf, offset, len)) {
        number = TypeNode.parse(ji, NumberTypeNode::parse);
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
