package software.sava.anchor;

import software.sava.core.borsh.Borsh;
import software.sava.core.borsh.RustEnum;
import systems.comodal.jsoniter.JsonIterator;
import systems.comodal.jsoniter.ValueType;

import static software.sava.anchor.AnchorPrimitive.addParam;
import static software.sava.anchor.AnchorType.*;

public record AnchorOption(AnchorTypeContext genericType) implements AnchorReferenceTypeContext {

  private static String presentCode(final AnchorType anchorType, final String read) {
    return switch (anchorType) {
      case array, bytes, string, vec, defined, i128, u128, i256, u256, publicKey, bool -> read;
      case i8, u8, i16, u16, i32, u32 -> "OptionalInt.of(" + read + ')';
      case i64, u64, usize -> "OptionalLong.of(" + read + ')';
      case f32, f64 -> "OptionalDouble.of(" + read + ')';
      default -> null;
    };
  }

  private static String notPresentCode(final AnchorType anchorType) {
    return switch (anchorType) {
      case array, bytes, string, vec, defined, i128, u128, i256, u256, publicKey, bool -> "null";
      case i8, u8, i16, u16, i32, u32 -> "OptionalInt.empty()";
      case i64, u64, usize -> "OptionalLong.empty()";
      case f32, f64 -> "OptionalDouble.empty()";
      default -> null;
    };
  }

  private static String presentCode(final AnchorType anchorType) {
    return switch (anchorType) {
      case array, bytes, string, vec, defined, i128, u128, i256, u256, publicKey, bool -> " != null";
      case i8, u8, i16, u16, i32, u32, i64, u64, usize, f32, f64 -> ".isPresent()";
      default -> null;
    };
  }

  private static String notPresentCheckCode(final AnchorType anchorType, final String varName) {
    return switch (anchorType) {
      case array, vec, bytes -> String.format("%s == null || %s.length == 0", varName, varName);
      case string -> String.format("_%s == null || _%s.length == 0", varName, varName);
      case i8, u8, i16, u16, i32, u32, i64, u64, usize, f32, f64 ->
          String.format("%s == null || %s.isEmpty()", varName, varName);
      case defined, i128, u128, i256, u256, publicKey, bool -> String.format("%s == null", varName);
      default -> throw new UnsupportedOperationException("TODO: support optional " + anchorType);
    };
  }

  static AnchorOption parseOption(final IDLType idlType, final JsonIterator ji) {
    final AnchorTypeContext genericType;
    final var jsonType = ji.whatIsNext();
    if (jsonType == ValueType.STRING) {
      genericType = ji.applyChars(ANCHOR_TYPE_PARSER).primitiveType();
    } else if (jsonType == ValueType.OBJECT) {
      genericType = AnchorType.parseContextType(idlType, ji);
      ji.closeObj();
    } else {
      throw new IllegalStateException(String.format("TODO: Support %s Anchor types", jsonType));
    }
    return new AnchorOption(genericType);
  }

  @Override
  public AnchorType type() {
    return genericType.type();
  }

  @Override
  public String generateRecordField(final GenSrcContext genSrcContext,
                                    final NamedType varName,
                                    final boolean optional) {
    return genericType.generateRecordField(genSrcContext, varName, true);
  }

  @Override
  public String generateStaticFactoryField(final GenSrcContext genSrcContext,
                                           final String varName,
                                           final boolean optional) {
    return genericType.generateStaticFactoryField(genSrcContext, varName, true);
  }

  @Override
  public String generateNewInstanceField(final GenSrcContext genSrcContext, final String varName) {
    return AnchorArray.generateNewInstanceField(genericType, varName);
  }

  @Override
  public String generateRead(final GenSrcContext genSrcContext,
                             final String varName,
                             final boolean hasNext,
                             final boolean singleField,
                             final String offsetVarName) {
    final var read = genericType.generateRead(
        genSrcContext,
        varName,
        hasNext,
        singleField,
        singleField ? offsetVarName + " + 1" : offsetVarName
    );
    final int i = read.indexOf('=');
    final var readCall = read.substring(i + 2);

    final var cutVarName = read.substring(0, i - 1);
    if (hasNext) {
      final int sizeLine = readCall.lastIndexOf('\n');
      return String.format("""
              %s = _data[i++] == 0 ? %s : %s;
              if (%s%s) {
              %s%s
              }""",
          cutVarName,
          notPresentCode(type()),
          presentCode(type(), readCall.substring(0, sizeLine - 1)),
          varName, presentCode(type()),
          genSrcContext.tab(),
          readCall.substring(sizeLine + 1)
      );
    }
    final var type = genericType.type();
    if (type == array) {
      final var constructAndRead = readCall.substring(0, readCall.length() - 1);
      final int newLine = constructAndRead.indexOf('\n');
      if (newLine > 0) {
        return String.format("""
                %s = _data[%s] == 0 ? null : %s
                if (%s != null) {
                %s%s;
                }""",
            cutVarName,
            singleField ? offsetVarName : offsetVarName + "++",
            constructAndRead.substring(0, newLine),
            varName,
            genSrcContext.tab(),
            constructAndRead.substring(newLine + 1)
        );
      }
    }
    return String.format("%s = _data[%s] == 0 ? %s : %s;",
        cutVarName,
        singleField ? offsetVarName : offsetVarName + "++",
        notPresentCode(type()),
        presentCode(type(), readCall.substring(0, readCall.length() - 1))
    );
  }

  @Override
  public String generateWrite(final GenSrcContext genSrcContext,
                              final String varName,
                              final boolean hasNext) {
    genSrcContext.addImport(Borsh.class);
    return switch (genericType.type()) {
      case bytes ->
          String.format((hasNext ? "i += Borsh.writeOptionalVector(%s, _data, i);" : "Borsh.writeOptionalVector(%s, _data, i);"), varName);
      case string ->
          String.format((hasNext ? "i += Borsh.writeOptionalVector(_%s, _data, i);" : "Borsh.writeOptionalVector(_%s, _data, i);"), varName);
      case f32 ->
          String.format((hasNext ? "i += Borsh.writeOptionalfloat(%s, _data, i);" : "Borsh.writeOptionalfloat(%s, _data, i);"), varName);
      case i8, u8 ->
          String.format((hasNext ? "i += Borsh.writeOptionalbyte(%s, _data, i);" : "Borsh.writeOptionalbyte(%s, _data, i);"), varName);
      case i16, u16 ->
          String.format((hasNext ? "i += Borsh.writeOptionalshort(%s, _data, i);" : "Borsh.writeOptionalshort(%s, _data, i);"), varName);
      case i128, u128 ->
          String.format((hasNext ? "i += Borsh.write128Optional(%s, _data, i);" : "Borsh.write128Optional(%s, _data, i);"), varName);
      case i256, u256 ->
          String.format((hasNext ? "i += Borsh.write256Optional(%s, _data, i);" : "Borsh.write256Optional(%s, _data, i);"), varName);
      case array, vec -> String.format("""
              if (%s) {
              %s_data[i++] = 0;
              } else {
              %s_data[i++] = 1;
              %s}""",
          notPresentCheckCode(type(), varName),
          genSrcContext.tab(), genSrcContext.tab(),
          genericType.generateWrite(genSrcContext, varName, hasNext).indent(genSrcContext.tabLength())
      );
      default ->
          String.format((hasNext ? "i += Borsh.writeOptional(%s, _data, i);" : "Borsh.writeOptional(%s, _data, i);"), varName);
    };
  }

  @Override
  public int fixedSerializedLength(final GenSrcContext genSrcContext) {
    final boolean hasDiscriminator = genSrcContext.isAccount(genericType.typeName());
    return 1 + (genericType.isFixedLength(genSrcContext.definedTypes())
        ? genericType.serializedLength(genSrcContext, hasDiscriminator)
        : genericType.fixedSerializedLength(genSrcContext, hasDiscriminator));
  }

  @Override
  public String generateLength(final String varName, final GenSrcContext genSrcContext) {
    final var notPresentCheckCode = notPresentCheckCode(type(), varName);
    return String.format("(%s ? 1 : (1 + %s))", notPresentCheckCode, genericType.generateLength(varName, genSrcContext));
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
    final var type = genericType.type();
    final var notPresentCheckCode = notPresentCheckCode(type, varName);
    addParam(paramsBuilder, genericType.optionalTypeName(), varName);
    if (type == string) {
      genSrcContext.addUTF_8Import();
      stringsBuilder.append(String.format("""
              final byte[] _%s;
              final int _%sLen;
              if (%s == null || %s.isBlank()) {
                _%s = null;
                _%sLen = 1;
              } else {
                _%s = %s.getBytes(UTF_8);
                _%sLen = 5 + _%s.length;
              }
              """,
          varName, varName, varName, varName, varName, varName, varName, varName, varName, varName
      ));
      dataLengthBuilder.append(String.format(" + _%sLen", varName));
    } else {
      final var optionalType = type.optionalJavaType();
      if (optionalType != null) {
        genSrcContext.addImport(optionalType);
      } else if (type == defined) {
        genSrcContext.addDefinedImport(genericType.typeName());
      }

      final var tab = genSrcContext.tab();
      dataLengthBuilder.append('\n').append(tab).append(tab);

      final int dataLength = type.dataLength();
      if (dataLength < 0) {
        dataLengthBuilder.append(String.format("+ (%s ? 1 : (1 + %s))", notPresentCheckCode, genericType.generateLength(varName, genSrcContext)));
      } else {
        dataLengthBuilder.append(String.format("+ (%s ? 1 : %d)", notPresentCheckCode, 1 + dataLength));
      }
    }
    dataBuilder.append(generateWrite(genSrcContext, varName, hasNext));
    return 0;
  }

  @Override
  public String generateEnumRecord(final GenSrcContext genSrcContext,
                                   final String enumTypeName,
                                   final NamedType enumName,
                                   final int ordinal) {
    final var name = enumName.name();
    final var type = type();
    if (type == string) {
      genSrcContext.addUTF_8Import();
      return String.format("""
              record %s(byte[] val, java.lang.String _val) implements EnumString, %s {
              
                public static %s createRecord(final java.lang.String val) {
                  return val == null ? null : new %s(val.getBytes(UTF_8), val);
                }
              
                public static %s read(final byte[] data, final int offset) {
                  return data[i++] == 0 ? null : createRecord(Borsh.string(data + 1, offset));
                }
              
                @Override
                public int ordinal() {
                  return %d;
                }
              }""",
          name, enumTypeName, name, name, name, ordinal
      );
    } else if (type == defined || type == array || type == vec) {
      return genericType.generateEnumRecord(genSrcContext, enumTypeName, enumName, ordinal);
    } else {
      genSrcContext.addImport(type.optionalJavaType());

      final var enumType = switch (type) {
        case bool -> RustEnum.OptionalEnumBool.class;
        case bytes -> RustEnum.OptionalEnumBytes.class;
        case f32 -> RustEnum.OptionalEnumFloat32.class;
        case f64 -> RustEnum.OptionalEnumFloat64.class;
        case i8, u8 -> RustEnum.OptionalEnumInt8.class;
        case i16, u16 -> RustEnum.OptionalEnumInt16.class;
        case i32, u32 -> RustEnum.OptionalEnumInt32.class;
        case i64, u64, usize -> RustEnum.OptionalEnumInt64.class;
        case i128, u128 -> RustEnum.OptionalEnumInt128.class;
        case i256, u256 -> RustEnum.OptionalEnumInt256.class;
        case publicKey -> RustEnum.OptionalEnumPublicKey.class;
        default -> throw new IllegalStateException("Unexpected value: " + type);
      };

      final var recordSignature = String.format("(%s val)", type.optionalJavaType().getSimpleName());
      return String.format("""
              record %s%s implements %s, %s {
              
                public static %s read(final byte[] _data, int i) {
                  return new %s(_data[i++] == 0 ? %s : %s);
                }
              
                @Override
                public int ordinal() {
                  return %d;
                }
              }""",
          name, recordSignature, enumType.getSimpleName(), enumTypeName,
          name,
          name, notPresentCode(type), presentCode(type(), genericType.generateRead(genSrcContext, "i")),
          ordinal
      );
    }
  }

  @Override
  public void generateMemCompFilter(final GenSrcContext genSrcContext,
                                    final StringBuilder builder,
                                    final String varName,
                                    final String offsetVarName,
                                    final boolean optional) {
    genericType.generateMemCompFilter(genSrcContext, builder, varName, offsetVarName, true);
  }
}
