package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

record StringTypeNode(StringEncoding encoding) implements TypeNode {

  public static StringTypeNode parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createTypeNode();
  }

  static final class Parser implements FieldBufferPredicate {

    private StringEncoding encoding;

    StringTypeNode createTypeNode() {
      return new StringTypeNode(encoding);
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("encoding", buf, offset, len)) {
        encoding = StringEncoding.valueOf(ji.readString());
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
