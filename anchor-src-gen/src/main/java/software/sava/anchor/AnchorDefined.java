package software.sava.anchor;

import software.sava.core.borsh.Borsh;
import software.sava.core.rpc.Filter;
import systems.comodal.jsoniter.CharBufferFunction;
import systems.comodal.jsoniter.ContextFieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;
import systems.comodal.jsoniter.ValueType;

import java.util.Map;

import static software.sava.anchor.AnchorType.*;
import static software.sava.anchor.AnchorUtil.camelCase;
import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

public record AnchorDefined(String typeName) implements AnchorReferenceTypeContext {

  private static final CharBufferFunction<AnchorTypeContext> SHANK_DEFINED_TYPE_PARSER = (buf, offset, len) -> {
    if (fieldEquals("PodBool", buf, offset, len)) {
      return new AnchorPrimitive(bool);
    } else if (fieldEquals("PodU16", buf, offset, len)) {
      return new AnchorPrimitive(u16);
    } else if (fieldEquals("PodU32", buf, offset, len)) {
      return new AnchorPrimitive(u32);
    } else if (fieldEquals("PodU64", buf, offset, len)) {
      return new AnchorPrimitive(u64);
    } else if (fieldEquals("PodU128", buf, offset, len)) {
      return new AnchorPrimitive(u128);
    } else if (fieldEquals("PodU256", buf, offset, len)) {
      return new AnchorPrimitive(u256);
    } else {
      return new AnchorDefined(NamedTypeParser.cleanName(new String(buf, offset, len), true));
    }
  };

  static AnchorTypeContext parseDefined(final IDLType idlType, final JsonIterator ji) {
    if (idlType == IDLType.SHANK) {
      return ji.applyChars(SHANK_DEFINED_TYPE_PARSER);
    } else if (ji.whatIsNext() == ValueType.STRING) {
      return new AnchorDefined(NamedTypeParser.cleanName(ji.readString(), true));
    } else {
      return ji.testObject(new Builder(), PARSER).create();
    }
  }

  private static final ContextFieldBufferPredicate<Builder> PARSER = (builder, buf, offset, len, ji) -> {
    if (fieldEquals("name", buf, offset, len)) {
      builder.name = NamedTypeParser.cleanName(ji.readString(), true);
    } else {
      // https://github.com/coral-xyz/anchor/blob/020a3046582944030f17b6524006eeaf26951cb8/ts/packages/anchor/src/idl.ts#L255
      throw new IllegalStateException("Unhandled defined type field " + new String(buf, offset, len));
    }
    return true;
  };

  private static final class Builder {

    private String name;

    private Builder() {
    }

    private AnchorTypeContext create() {
      return new AnchorDefined(name);
    }
  }

  @Override
  public AnchorType type() {
    return AnchorType.defined;
  }

  @Override
  public String generateRecordField(final GenSrcContext genSrcContext,
                                    final NamedType context,
                                    final boolean optional) {
    return String.format("%s%s %s", context.docComments(), typeName, context.name());
  }

  @Override
  public String generateStaticFactoryField(final GenSrcContext genSrcContext,
                                           final String varName,
                                           final boolean optional) {
    return String.format("%s %s", typeName, varName);
  }

  @Override
  public String generateRead(final GenSrcContext genSrcContext, final String offsetVarName) {
    return String.format("%s.read(_data, i);", typeName);
  }

  @Override
  public String generateRead(final GenSrcContext genSrcContext,
                             final String varName,
                             final boolean hasNext,
                             final boolean singleField,
                             final String offsetVarName) {
    final var readLine = String.format("final var %s = %s.read(_data, %s);", varName, typeName, offsetVarName);
    return hasNext
        ? readLine + String.format("%ni += Borsh.len(%s);", varName)
        : readLine;
  }

  @Override
  public String generateNewInstanceField(final GenSrcContext genSrcContext, final String varName) {
    return varName;
  }

  @Override
  public String generateWrite(final GenSrcContext genSrcContext, final String varName, final boolean hasNext) {
    return String.format("%sBorsh.write(%s, _data, i);", hasNext ? "i += " : "", varName);
  }

  @Override
  public String generateEnumRecord(final GenSrcContext genSrcContext,
                                   final String enumTypeName,
                                   final NamedType enumName,
                                   final int ordinal) {
    final var name = enumName.name();
    final var valTypeName = enumName.name().equals(typeName)
        ? genSrcContext.typePackage() + '.' + typeName
        : typeName;
    return String.format("""
            record %s(%s val) implements BorshEnum, %s {
            
              public static %s read(final byte[] _data, final int offset) {
                return new %s(%s.read(_data, offset));
              }
            
              @Override
              public int ordinal() {
                return %d;
              }
            }""",
        name, valTypeName, enumTypeName,
        name,
        name, valTypeName,
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
                                     final NamedType context,
                                     final StringBuilder paramsBuilder,
                                     final StringBuilder dataBuilder,
                                     final StringBuilder stringsBuilder,
                                     final StringBuilder dataLengthBuilder,
                                     final boolean hasNext) {
    genSrcContext.addDefinedImport(typeName);
    paramsBuilder.append(context.docComments());
    final var varName = context.name();
    paramsBuilder.append(String.format("final %s %s,\n", typeName, varName));
    genSrcContext.addImport(Borsh.class);
    dataLengthBuilder.append(String.format(" + Borsh.len(%s)", varName));
    dataBuilder.append(generateWrite(genSrcContext, varName, hasNext));
    return 0;
  }

  @Override
  public boolean isFixedLength(final Map<String, NamedType> definedTypes) {
    final var definedType = definedTypes.get(typeName);
    if (definedType == null) {
      throw new IllegalStateException("Failed to find defined type " + typeName);
    }
    return definedType.type().isFixedLength(definedTypes);
  }

  @Override
  public int serializedLength(final GenSrcContext genSrcContext) {
    return genSrcContext.definedTypes().get(typeName).type().serializedLength(genSrcContext, genSrcContext.isAccount(typeName));
  }

  @Override
  public void generateMemCompFilter(final GenSrcContext genSrcContext,
                                    final StringBuilder builder,
                                    final String varName,
                                    final String offsetVarName,
                                    final boolean optional) {
    builder.append(String.format("""
            
            public static Filter create%sFilter(final %s %s) {
            %sreturn Filter.createMemCompFilter(%s, %s.%s());
            }
            """,
        camelCase(varName, true), typeName(), varName, genSrcContext.tab(), offsetVarName, varName, optional ? "writeOptional" : "write"
    ));
    genSrcContext.addImport(Filter.class);
  }
}
