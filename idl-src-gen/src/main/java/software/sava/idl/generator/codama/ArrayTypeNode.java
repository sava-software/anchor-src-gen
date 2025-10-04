package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

record ArrayTypeNode(TypeNode item, CountNode count) implements TypeNode {

  public static ArrayTypeNode parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createTypeNode();
  }

  static final class Parser implements FieldBufferPredicate {

    private TypeNode item;
    private CountNode count;

    ArrayTypeNode createTypeNode() {
      return new ArrayTypeNode(item, count);
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("item", buf, offset, len)) {
        item = TypeNode.parse(ji);
      } else if (fieldEquals("count", buf, offset, len)) {
        count = CountNode.parse(ji);
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
