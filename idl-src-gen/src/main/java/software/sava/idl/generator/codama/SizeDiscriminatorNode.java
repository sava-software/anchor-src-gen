package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

record SizeDiscriminatorNode(int size) implements DiscriminatorNode {

  public static SizeDiscriminatorNode parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createSizeDiscriminatorNode();
  }

  static final class Parser implements FieldBufferPredicate {

    private int size;

    SizeDiscriminatorNode createSizeDiscriminatorNode() {
      return new SizeDiscriminatorNode(size);
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("size", buf, offset, len)) {
        size = ji.readInt();
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
