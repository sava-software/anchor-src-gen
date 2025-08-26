package software.sava.anchor;

import java.util.List;
import java.util.Map;

public sealed interface AnchorTypeContext permits AnchorDefinedTypeContext, AnchorReferenceTypeContext {

  AnchorType type();

  default boolean isFixedLength(final Map<String, NamedType> definedTypes) {
    return false;
  }

  default int serializedLength(final GenSrcContext genSrcContext, final boolean hasDiscriminator) {
    if (hasDiscriminator) {
      throw throwInvalidDataType();
    } else {
      return serializedLength(genSrcContext);
    }
  }

  default int serializedLength(final GenSrcContext genSrcContext) {
    throw throwInvalidDataType();
  }

  default int fixedSerializedLength(final GenSrcContext genSrcContext, final boolean hasDiscriminator) {
    return serializedLength(genSrcContext, hasDiscriminator);
  }

  default int fixedSerializedLength(final GenSrcContext genSrcContext) {
    return serializedLength(genSrcContext);
  }

  default void generateMemCompFilter(final GenSrcContext genSrcContext,
                                     final StringBuilder builder,
                                     final String varName,
                                     final String offsetVarName,
                                     final boolean optional) {
    throw throwInvalidDataType();
  }

  default void generateMemCompFilter(final GenSrcContext genSrcContext,
                                     final StringBuilder builder,
                                     final String varName,
                                     final String offsetVarName) {
    generateMemCompFilter(genSrcContext, builder, varName, offsetVarName, false);
  }

  default RuntimeException throwInvalidDataType() {
    throw AnchorPrimitive.throwInvalidDataType(this.getClass());
  }

  default int numElements() {
    throw throwInvalidDataType();
  }

  default AnchorTypeContext genericType() {
    throw throwInvalidDataType();
  }

  default String typeName() {
    throw throwInvalidDataType();
  }

  default String realTypeName() {
    return typeName();
  }

  default String optionalTypeName() {
    return typeName();
  }

  default int depth() {
    throw throwInvalidDataType();
  }

  default List<NamedType> values() {
    throw throwInvalidDataType();
  }

  default String generateRecordField(final GenSrcContext genSrcContext,
                                     final NamedType varName,
                                     final boolean optional) {
    throw throwInvalidDataType();
  }

  default String generateStaticFactoryField(final GenSrcContext genSrcContext,
                                            final String varName,
                                            final boolean optional) {
    throw throwInvalidDataType();
  }

  default String generateNewInstanceField(final GenSrcContext genSrcContext,
                                          final String varName) {
    throw throwInvalidDataType();
  }

  default String generateRead(final GenSrcContext genSrcContext, final String offsetVarName, final String varName) {
    throw throwInvalidDataType();
  }

  default String generateRead(final GenSrcContext genSrcContext,
                              final String varName,
                              final boolean hasNext,
                              final boolean singleField,
                              final String offsetVarName) {
    throw throwInvalidDataType();
  }

  default String generateWrite(final GenSrcContext genSrcContext,
                               final String varName,
                               final boolean hasNext) {
    throw throwInvalidDataType();
  }

  default String generateEnumRecord(final GenSrcContext genSrcContext,
                                    final String enumTypeName,
                                    final NamedType enumName,
                                    final int ordinal) {
    throw throwInvalidDataType();
  }

  default String generateLength(final String varName, final GenSrcContext genSrcContext) {
    throw throwInvalidDataType();
  }

  default int generateIxSerialization(final GenSrcContext genSrcContext,
                                      final NamedType context,
                                      final StringBuilder paramsBuilder,
                                      final StringBuilder dataBuilder,
                                      final StringBuilder stringsBuilder,
                                      final StringBuilder dataLengthBuilder,
                                      final boolean hasNext) {
    throw throwInvalidDataType();
  }

  default String arrayLengthConstant(final String varName) {
    return null;
  }
}
