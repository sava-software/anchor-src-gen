package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.CharBufferFunction;

import java.math.BigInteger;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

public enum NumberFormat {

  f32(float.class, OptionalDouble.class, Float.BYTES),
  f64(double.class, OptionalDouble.class, Double.BYTES),
  i8(byte.class, int.class, OptionalInt.class, 1),
  i16(short.class, int.class, OptionalInt.class, Short.BYTES),
  i32(int.class, OptionalInt.class, Integer.BYTES),
  i64(long.class, OptionalLong.class, Long.BYTES),
  i128(BigInteger.class, Long.BYTES << 1),
  u8(byte.class, int.class, OptionalInt.class, 1),
  shortU16(int.class, OptionalInt.class, -1),
  u16(short.class, int.class, OptionalInt.class, Short.BYTES),
  u32(int.class, OptionalInt.class, Integer.BYTES),
  u64(long.class, OptionalLong.class, Long.BYTES),
  u128(BigInteger.class, Long.BYTES << 1);


  private final Class<?> realJavaType;
  private final Class<?> javaType;
  private final Class<?> optionalJavaType;
  private final int dataLength;

  NumberFormat(final Class<?> realJavaType,
               final Class<?> javaType,
               final Class<?> optionalJavaType,
               final int dataLength) {
    this.realJavaType = realJavaType;
    this.javaType = javaType;
    this.optionalJavaType = optionalJavaType;
    this.dataLength = dataLength;
  }

  NumberFormat(final Class<?> javaType, final Class<?> optionalJavaType, final int dataLength) {
    this(javaType, javaType, optionalJavaType, dataLength);
  }

  NumberFormat(final Class<?> javaType, final int dataLength) {
    this(javaType, javaType, dataLength);
  }


  private static RuntimeException throwUnsupportedType(final char[] buf, final int offset, final int len) {
    return new UnsupportedOperationException("TODO: support type " + new String(buf, offset, len));
  }

  static final CharBufferFunction<NumberFormat> NUMBER_FORMAT_PARSER = (buf, offset, len) -> {
    final char c = buf[offset];
    if (c == 'i') {
      return switch (len) {
        case 2 -> i8;
        case 3 -> switch (buf[offset + 1]) {
          case '1' -> i16;
          case '3' -> i32;
          case '6' -> i64;
          default -> throw throwUnsupportedType(buf, offset, len);
        };
        case 4 -> switch (buf[offset + 1]) {
          case '1' -> i128;
          default -> throw throwUnsupportedType(buf, offset, len);
        };
        default -> throw throwUnsupportedType(buf, offset, len);
      };
    } else if (c == 'u') {
      return switch (len) {
        case 2 -> u8;
        case 3 -> switch (buf[offset + 1]) {
          case '1' -> u16;
          case '3' -> u32;
          case '6' -> u64;
          default -> throw throwUnsupportedType(buf, offset, len);
        };
        case 4 -> switch (buf[offset + 1]) {
          case '1' -> u128;
          default -> throw throwUnsupportedType(buf, offset, len);
        };
        default -> throw throwUnsupportedType(buf, offset, len);
      };
    }

    if (fieldEquals("f32", buf, offset, len)) {
      return f32;
    } else if (fieldEquals("f64", buf, offset, len)) {
      return f64;
    } else if (fieldEquals("shortU16", buf, offset, len)) {
      return shortU16;
    } else {
      throw throwUnsupportedType(buf, offset, len);
    }
  };
}
