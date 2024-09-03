package software.sava.anchor;

import software.sava.core.borsh.Borsh;
import systems.comodal.jsoniter.JsonIterator;
import systems.comodal.jsoniter.ValueType;

import java.util.List;
import java.util.Map;

import static software.sava.anchor.AnchorArray.arrayDepthCode;
import static software.sava.anchor.AnchorStruct.generateRecord;
import static software.sava.anchor.AnchorType.*;

public record AnchorVector(AnchorTypeContext genericType, int depth) implements AnchorReferenceTypeContext {

  static AnchorVector parseVector(final JsonIterator ji) {
    for (int depth = 1; ; ) {
      final var jsonType = ji.whatIsNext();
      if (jsonType == ValueType.STRING) {
        final var genericType = ji.applyChars(ANCHOR_TYPE_PARSER).primitiveType();
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
          case array -> AnchorArray.parseArray(ji);
          case defined -> AnchorDefined.parseDefined(ji);
          case _enum -> AnchorEnum.parseEnum(ji);
          case option -> AnchorOption.parseOption(ji);
          case struct -> AnchorStruct.parseStruct(ji);
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
  public String typeName() {
    return genericType.typeName() + arrayDepthCode(depth);
  }

  @Override
  public String generateRecordField(final GenSrcContext genSrcContext, final AnchorNamedType context, final boolean optional) {
    return AnchorArray.generateRecordField(genSrcContext, genericType, depth, context);
  }

  @Override
  public String generateStaticFactoryField(final GenSrcContext genSrcContext, final String varName, final boolean optional) {
    return AnchorArray.generateStaticFactoryField(genSrcContext, genericType, depth, varName);
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
    if (depth > 2) {
      throw new UnsupportedOperationException("TODO: support vectors with more than 2 dimensions.");
    }
    final String readLine;
    if (genericType instanceof AnchorDefined) {
      final var borshMethodName = depth == 1 ? "readVector" : "readMultiDimensionVector";
      readLine = String.format("final var %s = Borsh.%s(%s.class, %s::read, _data, %s);",
          varName, borshMethodName, genericType.typeName(), genericType.typeName(), offsetVarName);
    } else {
      final var javaType = genericType.type().javaType().getSimpleName();
      final var borshMethodName = depth == 1
          ? String.format("read%sVector", javaType)
          : String.format("readMultiDimension%sVector", javaType);
      readLine = String.format("final var %s = Borsh.%s(_data, %s);",
          varName, borshMethodName, offsetVarName);
    }
    return hasNext
        ? readLine + String.format("\ni += Borsh.len(%s);", varName)
        : readLine;
  }

  @Override
  public String generateWrite(final GenSrcContext genSrcContext, final String varName, final boolean hasNext) {
    genSrcContext.addImport(Borsh.class);
    return hasNext
        ? String.format("i += Borsh.write(%s, _data, i);", varName)
        : String.format("Borsh.write(%s, _data, i);", varName);
  }

  @Override
  public String generateEnumRecord(final GenSrcContext genSrcContext,
                                   final String enumTypeName,
                                   final AnchorNamedType enumName,
                                   final int ordinal) {
    return generateRecord(
        genSrcContext,
        enumName,
        List.of(AnchorNamedType.createType(null, "val", this)),
        "",
        enumTypeName,
        ordinal
    );
  }

  @Override
  public String generateLength(final String varName, final GenSrcContext genSrcContext) {
    genSrcContext.addImport(Borsh.class);
    return String.format("Borsh.len(%s)", varName);
  }

  @Override
  public int generateIxSerialization(final GenSrcContext genSrcContext,
                                     final AnchorNamedType context,
                                     final StringBuilder paramsBuilder,
                                     final StringBuilder dataBuilder,
                                     final StringBuilder stringsBuilder,
                                     final StringBuilder dataLengthBuilder,
                                     final boolean hasNext) {
    paramsBuilder.append(context.docComments());
    final var varName = context.name();
    final var param = String.format("final %s%s %s,\n", genericType.typeName(), arrayDepthCode(depth), varName);
    paramsBuilder.append(param);
    genSrcContext.addImport(Borsh.class);
    dataLengthBuilder.append(String.format(" + Borsh.len(%s)", varName));
    dataBuilder.append(generateWrite(genSrcContext, varName, hasNext));
    if (genericType instanceof AnchorDefined) {
      genSrcContext.addDefinedImport(genericType.typeName());
    }
    return 0;
  }

  @Override
  public int fixedSerializedLength(final Map<String, AnchorNamedType> definedTypes) {
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
