package software.sava.anchor;

import software.sava.core.accounts.PublicKey;
import software.sava.core.borsh.Borsh;
import systems.comodal.jsoniter.JsonIterator;
import systems.comodal.jsoniter.ValueType;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static software.sava.anchor.AnchorStruct.generateRecord;
import static software.sava.anchor.AnchorType.*;

public record AnchorArray(AnchorTypeContext genericType,
                          int depth,
                          int numElements) implements AnchorReferenceTypeContext {

  static String arrayDepthCode(final int depth) {
    return "[]".repeat(depth);
  }

  static void addImports(final GenSrcContext genSrcContext,
                         final AnchorTypeContext genericType) {
    switch (genericType.type()) {
      case publicKey -> genSrcContext.addImport(PublicKey.class);
      case string -> genSrcContext.addImport(String.class);
      case u128, u256, i128, i256 -> genSrcContext.addImport(BigInteger.class);
    }
  }

  static String generateRecordField(final GenSrcContext genSrcContext,
                                    final AnchorTypeContext genericType,
                                    final int depth,
                                    final NamedType context) {
    addImports(genSrcContext, genericType);
    final var docs = context.docComments();
    final var varName = context.name();
    final var typeName = genericType.realTypeName();
    final var depthCode = arrayDepthCode(depth);
    return genericType.type() == string
        ? String.format("%s%s%s %s, byte[]%s _%s", docs, typeName, depthCode, varName, depthCode, varName)
        : String.format("%s%s%s %s", docs, typeName, depthCode, varName);
  }

  static String generateStaticFactoryField(final GenSrcContext genSrcContext,
                                           final AnchorTypeContext genericType,
                                           final int depth,
                                           final String context) {
    addImports(genSrcContext, genericType);
    final var typeName = genericType.realTypeName();
    final var depthCode = arrayDepthCode(depth);
    return String.format("%s%s %s", typeName, depthCode, context);
  }

  static String generateNewInstanceField(final AnchorTypeContext genericType, final String varName) {
    return genericType.type() == string
        ? String.format("%s, Borsh.getBytes(%s)", varName, varName)
        : varName;
  }

  static AnchorArray parseArray(final IDLType idlType, final JsonIterator ji) {
    for (int depth = 1; ; ) {
      final var jsonType = ji.whatIsNext();
      if (jsonType == ValueType.ARRAY) {
        final var genericType = AnchorType.parseContextType(idlType, ji.openArray());
        if (genericType instanceof AnchorDefined || genericType instanceof AnchorArray) {
          ji.closeObj();
        }
        final int len = ji.continueArray().readInt();
        final var array = new AnchorArray(genericType, depth, len);
        do {
          ji.closeArray();
        } while (--depth > 0);
        return array;
      } else if (jsonType == ValueType.OBJECT) {
        var anchorType = ji.applyObjField(ANCHOR_OBJECT_TYPE_PARSER);
        if (anchorType == null) {
          anchorType = ji.applyChars(ANCHOR_TYPE_PARSER);
        }
        if (anchorType == array) {
          ++depth;
          continue;
        }
        throw new IllegalStateException("Unexpected value: " + anchorType);
      } else {
        throw new IllegalStateException(String.format("TODO: Support %s Anchor types", jsonType));
      }
    }
  }

  @Override
  public AnchorType type() {
    return array;
  }

  @Override
  public boolean isFixedLength(final Map<String, NamedType> definedTypes) {
    return genericType.isFixedLength(definedTypes);
  }

  @Override
  public int serializedLength(final GenSrcContext genSrcContext) {
    return genericType.isFixedLength(genSrcContext.definedTypes())
        ? (depth * numElements) * genericType.serializedLength(genSrcContext, genSrcContext.isAccount(genericType.typeName()))
        : genericType.serializedLength(genSrcContext);
  }

  @Override
  public void generateMemCompFilter(final GenSrcContext genSrcContext,
                                    final StringBuilder builder,
                                    final String varName,
                                    final String offsetVarName,
                                    final boolean optional) {
    // TODO
  }

  @Override
  public String typeName() {
    return genericType.realTypeName() + arrayDepthCode(depth);
  }

  @Override
  public String optionalTypeName() {
    return genericType.optionalTypeName() + arrayDepthCode(depth);
  }

  @Override
  public String generateRecordField(final GenSrcContext genSrcContext,
                                    final NamedType varName,
                                    final boolean optional) {
    return generateRecordField(genSrcContext, genericType, depth, varName);
  }

  @Override
  public String generateStaticFactoryField(final GenSrcContext genSrcContext,
                                           final String varName,
                                           final boolean optional) {
    return generateStaticFactoryField(genSrcContext, genericType, depth, varName);
  }

  @Override
  public String generateRead(final GenSrcContext genSrcContext,
                             final String varName,
                             final boolean hasNext,
                             final boolean singleField,
                             final String offsetVarName) {
    if (depth > 2) {
      throw new UnsupportedOperationException("TODO: supports multi dimensional arrays.");
    }
    final var incrementOffset = hasNext ? String.format("%s += ", offsetVarName) : "";
    if (genericType instanceof AnchorDefined) {
      return String.format("""
              final var %s = new %s[%d]%s;
              %sBorsh.readArray(%s, %s::read, _data, %s);""",
          varName,
          genericType.typeName(),
          numElements,
          arrayDepthCode(depth - 1),
          incrementOffset,
          varName,
          genericType.typeName(),
          offsetVarName
      );
    } else if (genericType instanceof AnchorArray array) {
      var fixedArray = String.format("[%d][%d]", numElements, array.numElements);
      var next = array.genericType();
      for (; ; ) {
        if (next instanceof AnchorArray anchorArray) {
          fixedArray = String.format("%s[%d]", fixedArray, anchorArray.numElements);
          next = anchorArray.genericType();
        } else {
          fixedArray = next.realTypeName() + fixedArray;
          break;
        }
      }
      if (next instanceof AnchorDefined) {
        return String.format("""
                final var %s = new %s;
                %sBorsh.readArray(%s, %s::read, _data, %s);""",
            varName,
            fixedArray,
            incrementOffset,
            varName,
            genericType.typeName(),
            offsetVarName
        );
      } else {
        return String.format("""
                final var %s = new %s;
                %sBorsh.readArray(%s, _data, %s);""",
            varName,
            fixedArray,
            incrementOffset,
            varName,
            offsetVarName
        );
      }
    } else {
      return String.format("""
              final var %s = new %s[%d];
              %sBorsh.readArray(%s, _data, %s);""",
          varName,
          genericType.realTypeName(),
          numElements,
          incrementOffset,
          varName,
          offsetVarName
      );
    }
  }

  @Override
  public String generateNewInstanceField(final GenSrcContext genSrcContext, final String varName) {
    return generateNewInstanceField(genericType, varName);
  }

  @Override
  public String generateWrite(final GenSrcContext genSrcContext,
                              final String varName,
                              final boolean hasNext) {
    genSrcContext.addImport(Borsh.class);
    return hasNext
        ? String.format("i += Borsh.writeArray(%s, _data, i);", varName)
        : String.format("Borsh.writeArray(%s, _data, i);", varName);
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
    return String.format("Borsh.lenArray(%s)", varName);
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
    dataLengthBuilder.append(String.format(" + Borsh.lenArray(%s)", varName));
    dataBuilder.append(generateWrite(genSrcContext, varName, hasNext));
    if (genericType instanceof AnchorDefined) {
      genSrcContext.addDefinedImport(genericType.typeName());
    }
    return 0;
  }
}
