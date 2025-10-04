package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

record ZeroableOptionTypeNode(TypeNode item, ValueNode.Constant zeroValue) implements TypeNode {

  public static ZeroableOptionTypeNode parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createTypeNode();
  }

  static final class Parser implements FieldBufferPredicate {

    private TypeNode item;
    private ValueNode.Constant zeroValue;

    ZeroableOptionTypeNode createTypeNode() {
      return new ZeroableOptionTypeNode(item, zeroValue);
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("item", buf, offset, len)) {
        item = TypeNode.parse(ji);
      } else if (fieldEquals("zeroValue", buf, offset, len)) {
        zeroValue = ValueNode.Constant.parse(ji);
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
