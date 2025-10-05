package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

record MapTypeNode(TypeNode key, TypeNode value, CountNode count) implements TypeNode {

  static MapTypeNode parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createTypeNode();
  }

  private static final class Parser implements FieldBufferPredicate {

    private TypeNode key;
    private TypeNode value;
    private CountNode count;

    MapTypeNode createTypeNode() {
      return new MapTypeNode(key, value, count);
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("key", buf, offset, len)) {
        key = TypeNode.parse(ji);
      } else if (fieldEquals("value", buf, offset, len)) {
        value = TypeNode.parse(ji);
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
