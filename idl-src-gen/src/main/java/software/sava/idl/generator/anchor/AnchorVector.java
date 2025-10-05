package software.sava.idl.generator.anchor;

import software.sava.core.borsh.Borsh;
import systems.comodal.jsoniter.JsonIterator;
import systems.comodal.jsoniter.ValueType;

import java.util.List;

import static software.sava.idl.generator.anchor.AnchorArray.arrayDepthCode;
import static software.sava.idl.generator.anchor.AnchorStruct.generateRecord;
import static software.sava.idl.generator.anchor.AnchorType.*;

public record AnchorVector(AnchorTypeContext genericType, int depth) implements AnchorReferenceTypeContext {

  static AnchorVector parseVector(final IDLType idlType, final JsonIterator ji) {
    for (int depth = 1; ; ) {
      final var jsonType = ji.whatIsNext();
      if (jsonType == ValueType.STRING) {
        final var genericType = AnchorType.parsePrimitive(ji);
        closeObjects(ji, depth - 1);
        return new AnchorVector(genericType, depth);
      } else if (jsonType == ValueType.OBJECT) {
        var anchorType = ji.applyObjField(ANCHOR_OBJECT_TYPE_PARSER);
        if (anchorType == null) {
          anchorType = ji.applyChars(ANCHOR_TYPE_PARSER);
        }
        if (anchorType == vec) {
          ++depth;
          continue;
        }
        final var genericType = switch (anchorType) {
          case array -> AnchorArray.parseArray(idlType, ji);
          case defined -> AnchorDefined.parseDefined(idlType, ji);
          case _enum -> AnchorEnum.parseEnum(idlType, ji);
          case option -> AnchorOption.parseOption(idlType, ji);
          case struct -> AnchorStruct.parseStruct(idlType, ji);
          default -> throw new IllegalStateException("Unexpected value: " + anchorType);
        };
        closeObjects(ji, depth);
        return new AnchorVector(genericType, depth);
      } else {
        throw new IllegalStateException(String.format("TODO: Support %s Anchor types", jsonType));
      }
    }
  }

  private static void closeObjects(final JsonIterator ji, int depth) {
    while (depth-- > 0) {
      ji.closeObj();
    }
  }

  @Override
  public AnchorType type() {
    return vec;
  }

  @Override
  public int serializedLength(final GenSrcContext genSrcContext) {
    return -1;
  }

  @Override
  public String typeName() {
    return genericType.realTypeName() + arrayDepthCode(depth);
  }

  @Override
  public String generateRecordField(final GenSrcContext genSrcContext,
                                    final NamedType context,
                                    final boolean optional) {
    return AnchorArray.generateRecordField(genSrcContext, genericType, depth, context);
  }

  @Override
  public String generateStaticFactoryField(final GenSrcContext genSrcContext,
                                           final String varName,
                                           final boolean optional) {
    return AnchorArray.generateStaticFactoryField(genSrcContext, genericType, depth, varName);
  }

  @Override
  public String generateNewInstanceField(final GenSrcContext genSrcContext, final String varName) {
    return AnchorArray.generateNewInstanceField(genericType, varName);
  }

  private static String typeQualifier(final AnchorTypeContext typeContext) {
    return switch (typeContext.type()) {
      case i128, u128 -> "128";
      case i256, u256 -> "256";
      default -> "";
    };
  }

  private String typeQualifier() {
    return typeQualifier(genericType);
  }

  private static String realTypeNameQualifier(final AnchorTypeContext typeContext) {
    return switch (typeContext.type()) {
      case i128, u128 -> "128";
      case i256, u256 -> "256";
      case bytes -> "byte";
      default -> typeContext.realTypeName();
    };
  }

  private String realTypeNameQualifier() {
    return realTypeNameQualifier(genericType);
  }

  @Override
  public String generateRead(final GenSrcContext genSrcContext,
                             final String varName,
                             final boolean hasNext,
                             final boolean singleField,
                             final String offsetVarName) {
    if (depth > 2) {
      throw new UnsupportedOperationException("TODO: support vectors with more than 2 dimensions.");
    }
    final String readLine;
    switch (genericType) {
      case AnchorDefined t -> {
        final var borshMethodName = depth == 1 ? "readVector" : "readMultiDimensionVector";
        readLine = String.format(
            "final var %s = Borsh.%s(%s.class, %s::read, _data, %s);",
            varName, borshMethodName, genericType.typeName(), genericType.typeName(), offsetVarName
        );
      }
      case AnchorArray array -> {
        final var next = array.genericType();
        if (next instanceof AnchorDefined) {
          readLine = String.format("final var %s = Borsh.readMultiDimensionVectorArray(%s.class, %s::read, %d, _data, %s);",
              varName,
              next.typeName(), next.typeName(),
              array.numElements(),
              offsetVarName
          );
          return hasNext
              ? readLine + String.format("%n%s += Borsh.lenVectorArray(%s);", offsetVarName, varName)
              : readLine;
        } else {
          readLine = String.format("final var %s = Borsh.readMultiDimension%sVectorArray(%d, _data, %s);",
              varName,
              realTypeNameQualifier(next),
              array.numElements(),
              offsetVarName
          );
          return hasNext
              ? readLine + String.format("%n%s += Borsh.len%sVectorArray(%s);", offsetVarName, typeQualifier(next), varName)
              : readLine;
        }
      }
      case AnchorVector vector -> {
        final var next = vector.genericType();
        if (next instanceof AnchorDefined) {
          readLine = String.format("final var %s = Borsh.readMultiDimensionVector(%s.class, %s::read, _data, %s);",
              varName,
              next.typeName(), next.typeName(),
              offsetVarName
          );
        } else {
          readLine = String.format("final var %s = Borsh.readMultiDimension%sVector(_data, %s);",
              varName,
              realTypeNameQualifier(next),
              offsetVarName
          );
          return hasNext
              ? readLine + String.format("%n%s += Borsh.len%sVector(%s);", offsetVarName, typeQualifier(next), varName)
              : readLine;
        }
      }
      default -> {
        final var borshMethodName = depth == 1
            ? String.format("read%sVector", realTypeNameQualifier())
            : String.format("readMultiDimension%sVector", realTypeNameQualifier());
        readLine = String.format("final var %s = Borsh.%s(_data, %s);",
            varName, borshMethodName, offsetVarName
        );
        return hasNext
            ? readLine + String.format("%n%s += Borsh.len%sVector(%s);", offsetVarName, typeQualifier(), varName)
            : readLine;
      }
    }
    return hasNext
        ? readLine + String.format("%n%s += Borsh.lenVector(%s);", offsetVarName, varName)
        : readLine;
  }

  @Override
  public String generateWrite(final GenSrcContext genSrcContext, final String varName, final boolean hasNext) {
    genSrcContext.addImport(Borsh.class);
    final var typeQualifier = typeQualifier();
    if (genericType instanceof AnchorArray array) {
      final int numElements = array.numElements();
      return hasNext
          ? String.format("i += Borsh.write%sVectorArrayChecked(%s, %s, _data, i);", typeQualifier, varName, numElements)
          : String.format("Borsh.writeVector%sArrayChecked(%s, %s, _data, i);", typeQualifier, varName, numElements);
    } else {
      return hasNext
          ? String.format("i += Borsh.write%sVector(%s, _data, i);", typeQualifier, varName)
          : String.format("Borsh.write%sVector(%s, _data, i);", typeQualifier, varName);
    }
  }

  @Override
  public String generateEnumRecord(final GenSrcContext genSrcContext,
                                   final String enumTypeName,
                                   final NamedType enumName,
                                   final int ordinal) {
    return generateRecord(
        genSrcContext,
        enumName,
        List.of(NamedType.createType(null, "val", this)),
        "",
        enumTypeName,
        ordinal
    );
  }

  @Override
  public String generateLength(final String varName, final GenSrcContext genSrcContext) {
    genSrcContext.addImport(Borsh.class);
    return genericType instanceof AnchorArray
        ? String.format("Borsh.len%sVectorArray(%s)", typeQualifier(), varName)
        : String.format("Borsh.len%sVector(%s)", typeQualifier(), varName);
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
    final var param = String.format("final %s%s %s,\n", genericType.realTypeName(), arrayDepthCode(depth), varName);
    paramsBuilder.append(param);
    genSrcContext.addImport(Borsh.class);
    if (genericType instanceof AnchorArray) {
      dataLengthBuilder.append(String.format(" + Borsh.len%sVectorArray(%s)", typeQualifier(), varName));
    } else {
      dataLengthBuilder.append(String.format(" + Borsh.len%sVector(%s)", typeQualifier(), varName));
    }
    dataBuilder.append(generateWrite(genSrcContext, varName, hasNext));
    if (genericType instanceof AnchorDefined) {
      genSrcContext.addDefinedImport(genericType.typeName());
    }
    return 0;
  }

  @Override
  public int fixedSerializedLength(final GenSrcContext genSrcContext) {
    return Integer.BYTES;
  }

  @Override
  public void generateMemCompFilter(final GenSrcContext genSrcContext,
                                    final StringBuilder builder,
                                    final String varName,
                                    final String offsetVarName,
                                    final boolean optional) {
    // TODO
  }
}
