package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

record OptionTypeNode(TypeNode item, NestedTypeNode prefix, boolean fixed) implements TypeNode {

  public static OptionTypeNode parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createTypeNode();
  }

  static final class Parser implements FieldBufferPredicate {

    private TypeNode item;
    private NestedTypeNode prefix;
    private boolean fixed;

    OptionTypeNode createTypeNode() {
      return new OptionTypeNode(item, prefix, fixed);
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("item", buf, offset, len)) {
        item = TypeNode.parse(ji);
      } else if (fieldEquals("prefix", buf, offset, len)) {
        prefix = TypeNode.parseNestedTypeNode(ji, NumberTypeNode::parse);
      } else if (fieldEquals("fixed", buf, offset, len)) {
        fixed = ji.readBoolean();
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
