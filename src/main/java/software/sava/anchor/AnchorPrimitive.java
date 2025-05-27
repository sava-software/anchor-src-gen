package software.sava.anchor;

import software.sava.core.accounts.PublicKey;
import software.sava.core.borsh.Borsh;
import software.sava.core.borsh.RustEnum;
import software.sava.core.encoding.ByteUtil;
import software.sava.core.rpc.Filter;

import java.math.BigInteger;
import java.util.Map;

import static software.sava.anchor.AnchorType.*;

public record AnchorPrimitive(AnchorType type) implements AnchorReferenceTypeContext {

  static RuntimeException throwInvalidDataType(final Class<? extends AnchorTypeContext> type) {
    throw new UnsupportedOperationException(type.getSimpleName());
  }

  static void addParam(final StringBuilder paramsBuilder,
                       final String type,
                       final String varName) {
    final var param = String.format("final %s %s,\n", type, varName);
    paramsBuilder.append(param);
  }

  @Override
  public boolean isFixedLength(final Map<String, NamedType> definedTypes) {
    return type.dataLength() > 0;
  }

  @Override
  public int serializedLength(final GenSrcContext genSrcContext) {
    return type.dataLength();
  }

  @Override
  public void generateMemCompFilter(final GenSrcContext genSrcContext,
                                    final StringBuilder builder,
                                    final String varName,
                                    final String offsetVarName,
                                    final boolean optional) {
    final String serializeCode;
    if (optional) {
      serializeCode = switch (type) {
        case bool ->
            String.format("return Filter.createMemCompFilter(%s, new byte[]{(byte) 1, (byte) (%s ? 1 : 0)});", offsetVarName, varName);
        case bytes -> String.format("""
                final byte[] _data = new byte[5 + %s.length];
                _data[0] = 1;
                Borsh.writeVector(%s, _data, 1);
                return Filter.createMemCompFilter(%s, _data);""",
            varName, varName, offsetVarName
        );
        case i8, u8 ->
            String.format("return Filter.createMemCompFilter(%s, new byte[]{(byte) 1, (byte) %s});", offsetVarName, varName);
        case f32, f64, i16, u16, i32, u32, i64, u64, usize, i128, u128, i256, u256 -> String.format("""
                final byte[] _data = new byte[%d];
                _data[0] = 1;
                %s
                return Filter.createMemCompFilter(%s, _data);""",
            1 + type.dataLength(), generateWrite(varName).replaceFirst("i", "1"), offsetVarName
        );
        case publicKey -> String.format("""
                final byte[] _data = new byte[33];
                _data[0] = 1;
                %s.write(_data, 1);
                return Filter.createMemCompFilter(%s, _data);""",
            varName, offsetVarName
        );
        case string -> String.format("""
                final byte[] bytes = %s.getBytes(UTF_8);
                final byte[] _data = new byte[5 + bytes.length];
                _data[0] = 1;
                Borsh.writeVector(bytes, _data, 1);
                return Filter.createMemCompFilter(%s, _data);""",
            varName, offsetVarName
        );
        default -> throw new IllegalStateException("Unexpected type: " + type);
      };
    } else {
      serializeCode = switch (type) {
        case bool ->
            String.format("return Filter.createMemCompFilter(%s, new byte[]{(byte) (%s ? 1 : 0)});", offsetVarName, varName);
        case bytes -> String.format("""
                final byte[] _data = new byte[4 + %s.length];
                Borsh.writeVector(%s, _data, i);
                return Filter.createMemCompFilter(%s, _data);""",
            varName, varName, offsetVarName
        );
        case i8, u8 ->
            String.format("return Filter.createMemCompFilter(%s, new byte[]{(byte) %s});", offsetVarName, varName);
        case f32, f64, i16, u16, i32, u32, i64, u64, usize, i128, u128, i256, u256 -> String.format("""
                final byte[] _data = new byte[%d];
                %s
                return Filter.createMemCompFilter(%s, _data);""",
            type.dataLength(), generateWrite(varName).replaceFirst("i", "0"), offsetVarName
        );
        case publicKey -> String.format("return Filter.createMemCompFilter(%s, %s);", offsetVarName, varName);
        case string -> String.format("""
                final byte[] bytes = %s.getBytes(UTF_8);
                final byte[] _data = new byte[4 + bytes.length];
                Borsh.writeVector(bytes, _data, 0);
                return Filter.createMemCompFilter(%s, _data);""",
            varName, offsetVarName
        );
        default -> throw new IllegalStateException("Unexpected type: " + type);
      };
    }

    builder.append(String.format("""
            
            public static Filter create%sFilter(final %s %s) {
            %s}
            """,
        AnchorUtil.camelCase(varName, true), typeName(), varName, serializeCode.indent(genSrcContext.tabLength())
    ));
    genSrcContext.addImport(Filter.class);
  }

  @Override
  public String typeName() {
    return type.javaType().getSimpleName();
  }

  @Override
  public String realTypeName() {
    return type.realJavaType().getSimpleName();
  }

  @Override
  public String optionalTypeName() {
    return type.optionalJavaType().getSimpleName();
  }

  @Override
  public String generateRecordField(final GenSrcContext genSrcContext,
                                    final NamedType context,
                                    final boolean optional) {
    final var varName = context.name();
    final var typeName = optional ? optionalTypeName() : typeName();
    if (type == string) {
      genSrcContext.addImport(String.class);
      return String.format("%s%s %s, byte[] _%s", context.docComments(), typeName, varName, varName);
    } else {
      switch (type) {
        case publicKey -> genSrcContext.addImport(PublicKey.class);
        case i128, u128, i256, u256 -> genSrcContext.addImport(BigInteger.class);
        default -> {
          if (optional) {
            genSrcContext.addImport(type.optionalJavaType());
          }
        }
      }
      return String.format("%s%s %s", context.docComments(), typeName, varName);
    }
  }

  @Override
  public String generateStaticFactoryField(final GenSrcContext genSrcContext,
                                           final String varName,
                                           final boolean optional) {
    return String.format("%s %s", optional ? optionalTypeName() : typeName(), varName);
  }

  @Override
  public String generateNewInstanceField(final GenSrcContext genSrcContext, final String varName) {
    if (type == string) {
      genSrcContext.addUTF_8Import();
      return String.format("%s, %s.getBytes(UTF_8)", varName, varName);
    } else {
      return varName;
    }
  }

  @Override
  public String generateRead(final GenSrcContext genSrcContext, final String offsetVarName) {
    switch (type) {
      case f32 -> genSrcContext.addStaticImport(ByteUtil.class, "getFloat32LE");
      case f64 -> genSrcContext.addStaticImport(ByteUtil.class, "getFloat64LE");
      case i16, u16 -> genSrcContext.addStaticImport(ByteUtil.class, "getInt16LE");
      case i32, u32 -> genSrcContext.addStaticImport(ByteUtil.class, "getInt32LE");
      case i64, u64, usize -> genSrcContext.addStaticImport(ByteUtil.class, "getInt64LE");
      case i128, u128 -> genSrcContext.addStaticImport(ByteUtil.class, "getInt128LE");
      case i256, u256 -> genSrcContext.addStaticImport(ByteUtil.class, "getInt256LE");
      case publicKey -> genSrcContext.addStaticImport(PublicKey.class, "readPubKey");
    }
    return switch (type) {
      case bool -> String.format("_data[%s] == 1", offsetVarName);
      case f32 -> String.format("getFloat32LE(_data, %s)", offsetVarName);
      case f64 -> String.format("getFloat64LE(_data, %s)", offsetVarName);
      case i8 -> String.format("_data[%s]", offsetVarName);
      case u8 -> String.format("_data[%s] & 0xFF", offsetVarName);
      case i16, u16 -> String.format("getInt16LE(_data, %s)", offsetVarName);
      case i32, u32 -> String.format("getInt32LE(_data, %s)", offsetVarName);
      case i64, u64, usize -> String.format("getInt64LE(_data, %s)", offsetVarName);
      case i128, u128 -> String.format("getInt128LE(_data, %s)", offsetVarName);
      case i256, u256 -> String.format("getInt256LE(_data, %s)", offsetVarName);
      case publicKey -> String.format("readPubKey(_data, %s)", offsetVarName);
      default -> throw new IllegalStateException("Unexpected type: " + type);
    };
  }

  @Override
  public String generateRead(final GenSrcContext genSrcContext,
                             final String varName,
                             final boolean hasNext,
                             final boolean singleField,
                             final String offsetVarName) {
    if (type == string) {
      genSrcContext.addImport(Borsh.class);
      if (hasNext) {
        genSrcContext.addStaticImport(ByteUtil.class, "getInt32LE");
      }
      final var readLine = String.format("final var %s = Borsh.string(_data, %s);", varName, offsetVarName);
      return hasNext
          ? readLine + "\ni += (Integer.BYTES + getInt32LE(_data, i));"
          : readLine;
    } else if (type == bytes) {
      genSrcContext.addImport(Borsh.class);
      final var readLine = String.format("final byte[] %s = Borsh.readbyteVector(_data, %s);", varName, offsetVarName);
      return hasNext
          ? readLine + String.format("""
          
          i += Borsh.lenVector(%s);""", varName
      )
          : readLine;
    } else {
      final var read = generateRead(genSrcContext, offsetVarName);
      final int dataLength = type.dataLength();
      final var readLine = String.format("final var %s = %s;", varName, read);
      if (dataLength == 1) {
        return hasNext
            ? readLine + "\n++i;"
            : readLine;
      } else {
        return hasNext
            ? readLine + String.format("%ni += %d;", dataLength)
            : readLine;
      }
    }
  }

  private String generateWrite(final String varName) {
    return String.format(switch (type) {
          case bool -> "_data[i] = (byte) (%s ? 1 : 0);";
          case f32 -> "putFloat32LE(_data, i, %s);";
          case f64 -> "putFloat64LE(_data, i, %s);";
          case i8, u8 -> "_data[i] = (byte) %s;";
          case i16, u16 -> "putInt16LE(_data, i, %s);";
          case i32, u32 -> "putInt32LE(_data, i, %s);";
          case i64, u64, usize -> "putInt64LE(_data, i, %s);";
          case i128, u128 -> "putInt128LE(_data, i, %s);";
          case i256, u256 -> "putInt256LE(_data, i, %s);";
          case publicKey -> "%s.write(_data, i);";
          default -> throw new IllegalStateException("Unexpected type: " + type);
        }, varName
    );
  }

  @Override
  public String generateWrite(final GenSrcContext genSrcContext, final String varName, final boolean hasNext) {
    if (type == string) {
      genSrcContext.addUTF_8Import();
      genSrcContext.addImport(Borsh.class);
      return String.format("%sBorsh.writeVector(_%s, _data, i);", hasNext ? "i += " : "", varName);
    } else if (type == bytes) {
      genSrcContext.addImport(Borsh.class);
      return String.format("%sBorsh.writeVector(%s, _data, i);", hasNext ? "i += " : "", varName);
    } else {
      switch (type) {
        case f32 -> genSrcContext.addStaticImport(ByteUtil.class, "putFloat32LE");
        case f64 -> genSrcContext.addStaticImport(ByteUtil.class, "putFloat64LE");
        case i16, u16 -> genSrcContext.addStaticImport(ByteUtil.class, "putInt16LE");
        case i32, u32 -> genSrcContext.addStaticImport(ByteUtil.class, "putInt32LE");
        case i64, u64, usize -> genSrcContext.addStaticImport(ByteUtil.class, "putInt64LE");
        case i128, u128 -> genSrcContext.addStaticImport(ByteUtil.class, "putInt128LE");
        case i256, u256 -> genSrcContext.addStaticImport(ByteUtil.class, "putInt256LE");
      }
      final var write = generateWrite(varName);
      if (hasNext) {
        final int dataLength = type.dataLength();
        if (dataLength == 1) {
          return write + "\n++i;";
        } else {
          return write + String.format("\ni += %d;", dataLength);
        }
      } else {
        return write;
      }
    }
  }

  @Override
  public String generateEnumRecord(final GenSrcContext genSrcContext,
                                   final String enumTypeName,
                                   final NamedType enumName,
                                   final int ordinal) {
    final var name = enumName.name();
    if (type == string) {
      genSrcContext.addUTF_8Import();
      genSrcContext.addImport(Borsh.class);
      return String.format("""
              record %s(byte[] val, java.lang.String _val) implements EnumString, %s {
              
                public static %s createRecord(final java.lang.String val) {
                  return new %s(val.getBytes(UTF_8), val);
                }
              
                public static %s read(final byte[] data, final int offset) {
                  return createRecord(Borsh.string(data, offset));
                }
              
                @Override
                public int ordinal() {
                  return %d;
                }
              }""",
          name, enumTypeName, name, name, name, ordinal
      );
    }
    final var enumType = switch (type) {
      case bool -> RustEnum.EnumBool.class;
      case bytes -> RustEnum.EnumBytes.class;
      case f32 -> RustEnum.EnumFloat32.class;
      case f64 -> RustEnum.EnumFloat64.class;
      case i8, u8 -> RustEnum.EnumInt8.class;
      case i16, u16 -> RustEnum.EnumInt16.class;
      case i32, u32 -> RustEnum.EnumInt32.class;
      case i64, u64, usize -> RustEnum.EnumInt64.class;
      case i128, u128 -> {
        genSrcContext.addImport(BigInteger.class);
        yield RustEnum.EnumInt128.class;
      }
      case i256, u256 -> {
        genSrcContext.addImport(BigInteger.class);
        yield RustEnum.EnumInt256.class;
      }
      case publicKey -> {
        genSrcContext.addImport(PublicKey.class);
        yield RustEnum.EnumPublicKey.class;
      }
      default -> throw new IllegalStateException("Unexpected type: " + type);
    };

    final var recordSignature = switch (type) {
      case bool -> "(boolean val)";
      case f32 -> "(float val)";
      case f64 -> "(double val)";
      case i8, u8, i16, u16, i32, u32 -> "(int val)";
      case i64, u64, usize -> "(long val)";
      case i128, u128, i256, u256 -> "(BigInteger val)";
      case publicKey -> "(PublicKey val)";
      case bytes -> "(byte[] val)";
      default -> throw new IllegalStateException("Unexpected type: " + type);
    };

    if (type == bool) {
      return String.format("""
              record %s%s implements %s, %s {
              
                public static final %s TRUE = new %s(true);
                public static final %s FALSE = new %s(false);
              
                public static %s read(final byte[] _data, int i) {
                  return %s ? %s.TRUE : %s.FALSE;
                }
              
                @Override
                public int ordinal() {
                  return %d;
                }
              }""",
          name, recordSignature, enumType.getSimpleName(), enumTypeName,
          name, name,
          name, name,
          name,
          generateRead(genSrcContext, "i"), name, name,
          ordinal
      );
    } else {
      return String.format("""
              record %s%s implements %s, %s {
              
                public static %s read(final byte[] _data, int i) {
                  return new %s(%s);
                }
              
                @Override
                public int ordinal() {
                  return %d;
                }
              }""",
          name, recordSignature, enumType.getSimpleName(), enumTypeName,
          name,
          name, generateRead(genSrcContext, "i"),
          ordinal
      );
    }
  }

  @Override
  public String generateLength(final String varName, final GenSrcContext genSrcContext) {
    if (type == string) {
      genSrcContext.addImport(Borsh.class);
      return String.format("Borsh.lenVector(_%s)", varName);
    } else if (type == bytes) {
      genSrcContext.addImport(Borsh.class);
      return String.format("Borsh.lenVector(%s)", varName);
    } else {
      return Integer.toString(type.dataLength());
    }
  }

  @Override
  public int generateIxSerialization(final GenSrcContext genSrcContext,
                                     final NamedType context,
                                     final StringBuilder paramsBuilder,
                                     final StringBuilder dataBuilder,
                                     final StringBuilder stringsBuilder,
                                     final StringBuilder dataLengthBuilder,
                                     final boolean hasNext) {
    paramsBuilder.append(context.docComments());
    final var varName = context.name();
    final var javaType = type.javaType();
    addParam(paramsBuilder, javaType.getSimpleName(), varName);
    if (type == string) {
      genSrcContext.addUTF_8Import();
      stringsBuilder.append(String.format("final byte[] _%s = %s.getBytes(UTF_8);\n", varName, varName));
      genSrcContext.addImport(Borsh.class);
      dataLengthBuilder.append(String.format(" + Borsh.lenVector(_%s)", varName));
      dataBuilder.append(generateWrite(genSrcContext, varName, hasNext));
      return 4;
    } else if (type == bytes) {
      genSrcContext.addImport(Borsh.class);
      dataLengthBuilder.append(String.format(" + Borsh.lenVector(%s)", varName));
      dataBuilder.append(generateWrite(genSrcContext, varName, hasNext));
      return 4;
    } else {
      switch (type) {
        case publicKey -> genSrcContext.addImport(PublicKey.class);
        case i128, u128, i256, u256 -> genSrcContext.addImport(BigInteger.class);
      }
      dataBuilder.append(generateWrite(genSrcContext, varName, hasNext));
      return type.dataLength();
    }
  }
}
