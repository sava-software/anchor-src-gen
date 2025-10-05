package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

record PrefixedCountNode(TypeNode prefix) implements CountNode {

  public static PrefixedCountNode parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createPrefixedCountNode();
  }

  static final class Parser implements FieldBufferPredicate {

    private TypeNode prefix;

    PrefixedCountNode createPrefixedCountNode() {
      return new PrefixedCountNode(prefix);
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("prefix", buf, offset, len)) {
        prefix = TypeNode.parse(ji, NumberTypeNode::parse);
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
