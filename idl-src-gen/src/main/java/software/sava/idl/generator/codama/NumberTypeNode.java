package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.CharBufferFunction;
import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;

import java.nio.ByteOrder;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

record NumberTypeNode(NumberFormat format, ByteOrder endian) implements TypeNode {

  static NumberTypeNode parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createTypeNode();
  }

  private static final class Parser implements FieldBufferPredicate {

    private NumberFormat format;
    private ByteOrder endian;

    NumberTypeNode createTypeNode() {
      return new NumberTypeNode(format, endian == null ? ByteOrder.LITTLE_ENDIAN : endian);
    }

    private static final CharBufferFunction<ByteOrder> BYTE_ORDER_PARSER = (buf, offset, len) -> {
      if (fieldEquals("le", buf, offset, len)) {
        return ByteOrder.LITTLE_ENDIAN;
      } else if (fieldEquals("be", buf, offset, len)) {
        return ByteOrder.BIG_ENDIAN;
      } else {
        throw new IllegalStateException("Unhandled endian type " + new String(buf, offset, len));
      }
    };

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("format", buf, offset, len)) {
        format = ji.applyChars(NumberFormat.NUMBER_FORMAT_PARSER);
      } else if (fieldEquals("endian", buf, offset, len)) {
        endian = ji.applyChars(BYTE_ORDER_PARSER);
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
