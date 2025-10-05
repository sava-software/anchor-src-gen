package software.sava.idl.generator.anchor;

import software.sava.core.accounts.PublicKey;
import systems.comodal.jsoniter.CharBufferFunction;
import systems.comodal.jsoniter.JsonIterator;
import systems.comodal.jsoniter.ValueType;

import java.math.BigInteger;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

public enum AnchorType {

  alias,
  array,
  bool(boolean.class, Boolean.class, 1),
  bytes(byte[].class, -1),
  defined,
  _enum(Enum.class, 1),
  f32(float.class, OptionalDouble.class, Float.BYTES),
  f64(double.class, OptionalDouble.class, Double.BYTES),
  i8(byte.class, int.class, OptionalInt.class, 1),
  i16(short.class, int.class, OptionalInt.class, Short.BYTES),
  i32(int.class, OptionalInt.class, Integer.BYTES),
  i64(long.class, OptionalLong.class, Long.BYTES),
  i128(BigInteger.class, Long.BYTES << 1),
  i256(BigInteger.class, Long.BYTES << 2),
  // i256(BigInteger.class, Long.BYTES << 2),
  option,
  publicKey(PublicKey.class, PublicKey.PUBLIC_KEY_LENGTH),
  string(String.class, -1),
  struct,
  u8(byte.class, int.class, OptionalInt.class, 1),
  u16(short.class, int.class, OptionalInt.class, Short.BYTES),
  u32(int.class, OptionalInt.class, Integer.BYTES),
  u64(long.class, OptionalLong.class, Long.BYTES),
  usize(long.class, OptionalLong.class, Long.BYTES),
  u128(BigInteger.class, Long.BYTES << 1),
  u256(BigInteger.class, Long.BYTES << 2),
  vec;

  static final CharBufferFunction<AnchorType> ANCHOR_TYPE_PARSER = (buf, offset, len) -> {
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
          case '2' -> i256;
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
          case '2' -> u256;
          default -> throw throwUnsupportedType(buf, offset, len);
        };
        case 5 -> usize;
        default -> throw throwUnsupportedType(buf, offset, len);
      };
    }

    if (fieldEquals("array", buf, offset, len)) {
      return array;
    } else if (fieldEquals("bool", buf, offset, len)) {
      return bool;
    } else if (fieldEquals("bytes", buf, offset, len)) {
      return bytes;
    } else if (fieldEquals("defined", buf, offset, len)) {
      return defined;
    } else if (fieldEquals("enum", buf, offset, len)) {
      return _enum;
    } else if (fieldEquals("f32", buf, offset, len)) {
      return f32;
    } else if (fieldEquals("f64", buf, offset, len)) {
      return f64;
    } else if (fieldEquals("option", buf, offset, len)) {
      return option;
    } else if (fieldEquals("pubkey", buf, offset, len) || fieldEquals("publicKey", buf, offset, len)) {
      return publicKey;
    } else if (fieldEquals("string", buf, offset, len)) {
      return string;
    } else if (fieldEquals("struct", buf, offset, len)) {
      return struct;
    } else if (fieldEquals("vec", buf, offset, len)) {
      return vec;
    } else if (fieldEquals("alias", buf, offset, len)) {
      return alias;
    } else {
      throw throwUnsupportedType(buf, offset, len);
    }
  };

  private static AnchorTypeContext handleBytes(final AnchorType primitive) {
    return primitive == bytes
        ? new AnchorVector(AnchorType.u8.primitiveType, 1)
        : primitive.primitiveType();
  }

  static AnchorTypeContext parsePrimitive(final JsonIterator ji) {
    return handleBytes(ji.applyChars(ANCHOR_TYPE_PARSER));
  }

  static AnchorTypeContext parsePrimitive(final char[] chars, final int offset, final int len) {
    return handleBytes(ANCHOR_TYPE_PARSER.apply(chars, offset, len));
  }

  static final CharBufferFunction<AnchorType> ANCHOR_OBJECT_TYPE_PARSER = (buf, offset, len) -> {
    if (fieldEquals("kind", buf, offset, len)) {
      return null;
    } else if (fieldEquals("vec", buf, offset, len)) {
      return vec;
    } else if (fieldEquals("array", buf, offset, len)) {
      return array;
    } else if (fieldEquals("defined", buf, offset, len)) {
      return defined;
    } else if (fieldEquals("option", buf, offset, len)) {
      return option;
    } else {
      throw throwUnsupportedType(buf, offset, len);
    }
  };

  private final Class<?> realJavaType;
  private final Class<?> javaType;
  private final Class<?> optionalJavaType;
  private final int dataLength;
  private final AnchorPrimitive primitiveType;

  AnchorType(final Class<?> realJavaType,
             final Class<?> javaType,
             final Class<?> optionalJavaType,
             final int dataLength) {
    this.realJavaType = realJavaType;
    this.javaType = javaType;
    this.optionalJavaType = optionalJavaType;
    this.dataLength = dataLength;
    this.primitiveType = new AnchorPrimitive(this);
  }

  AnchorType(final Class<?> javaType, final Class<?> optionalJavaType, final int dataLength) {
    this(javaType, javaType, optionalJavaType, dataLength);
  }

  AnchorType(final Class<?> javaType, final int dataLength) {
    this(javaType, javaType, dataLength);
  }

  AnchorType() {
    this(null, -1);
  }

  static AnchorTypeContext parseContextType(final IDLType idlType, final JsonIterator ji) {
    final var jsonType = ji.whatIsNext();
    if (jsonType == ValueType.STRING) {
      return AnchorType.parsePrimitive(ji);
    } else if (jsonType == ValueType.OBJECT) {
      var anchorType = ji.applyObjField(ANCHOR_OBJECT_TYPE_PARSER);
      if (anchorType == null) {
        anchorType = ji.applyChars(ANCHOR_TYPE_PARSER);
      }
      return switch (anchorType) {
        case alias -> {
          final var typeContext = parseContextType(idlType, ji.skipUntil("value"));
          ji.closeObj();
          yield typeContext;
        }
        case array -> AnchorArray.parseArray(idlType, ji);
        case defined -> AnchorDefined.parseDefined(idlType, ji);
        case _enum -> AnchorEnum.parseEnum(idlType, ji);
        case option -> AnchorOption.parseOption(idlType, ji);
        case struct -> AnchorStruct.parseStruct(idlType, ji);
        case vec -> AnchorVector.parseVector(idlType, ji);
        default -> throw new IllegalStateException("Unexpected value: " + anchorType);
      };
    } else {
      throw new IllegalStateException(String.format("TODO: Support %s Anchor types", jsonType));
    }
  }

  private static RuntimeException throwUnsupportedType(final char[] buf, final int offset, final int len) {
    throw new UnsupportedOperationException("TODO: support type " + new String(buf, offset, len));
  }

  public AnchorPrimitive primitiveType() {
    return primitiveType;
  }

  public int dataLength() {
    return dataLength;
  }

  public Class<?> javaType() {
    return javaType;
  }

  public Class<?> realJavaType() {
    return realJavaType;
  }

  public Class<?> optionalJavaType() {
    return optionalJavaType;
  }
}
