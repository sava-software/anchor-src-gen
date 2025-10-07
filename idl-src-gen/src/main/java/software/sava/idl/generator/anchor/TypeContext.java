package software.sava.idl.generator.anchor;

import java.util.List;

public interface TypeContext {

  default boolean isString() {
    return false;
  }

  default boolean isFixedLength(final SrcGenContext srcGenContext) {
    return false;
  }

  default int serializedLength(final SrcGenContext srcGenContext, final boolean isAccount) {
    if (isAccount) {
      throw throwInvalidDataType();
    } else {
      return serializedLength(srcGenContext);
    }
  }

  default int serializedLength(final SrcGenContext srcGenContext) {
    throw throwInvalidDataType();
  }

  default int optimisticSerializedLength(final SrcGenContext srcGenContext, final boolean isAccount) {
    return serializedLength(srcGenContext, isAccount);
  }

  default int optimisticSerializedLength(final SrcGenContext srcGenContext) {
    return serializedLength(srcGenContext);
  }

  default void generateMemCompFilter(final SrcGenContext srcGenContext,
                                     final StringBuilder builder,
                                     final String varName,
                                     final String offsetVarName,
                                     final boolean optional) {
    throw throwInvalidDataType();
  }

  default void generateMemCompFilter(final SrcGenContext srcGenContext,
                                     final StringBuilder builder,
                                     final String varName,
                                     final String offsetVarName) {
    generateMemCompFilter(srcGenContext, builder, varName, offsetVarName, false);
  }

  default RuntimeException throwInvalidDataType() {
    return AnchorPrimitive.throwInvalidDataType(this.getClass());
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

  default String generateRecordField(final SrcGenContext srcGenContext,
                                     final NamedType varName,
                                     final boolean optional) {
    throw throwInvalidDataType();
  }

  default String generateStaticFactoryField(final SrcGenContext srcGenContext,
                                            final String varName,
                                            final boolean optional) {
    throw throwInvalidDataType();
  }

  default String generateNewInstanceField(final SrcGenContext srcGenContext,
                                          final String varName) {
    throw throwInvalidDataType();
  }

  default String generateRead(final SrcGenContext srcGenContext, final String offsetVarName, final String varName) {
    throw throwInvalidDataType();
  }

  default String generateRead(final SrcGenContext srcGenContext,
                              final String varName,
                              final boolean hasNext,
                              final boolean singleField,
                              final String offsetVarName) {
    throw throwInvalidDataType();
  }

  default String generateWrite(final SrcGenContext srcGenContext,
                               final String varName,
                               final boolean hasNext) {
    throw throwInvalidDataType();
  }

  default String generateEnumRecord(final SrcGenContext srcGenContext,
                                    final String enumTypeName,
                                    final NamedType enumName,
                                    final int ordinal) {
    throw throwInvalidDataType();
  }

  default String generateLength(final String varName, final SrcGenContext srcGenContext) {
    throw throwInvalidDataType();
  }

  default int generateIxSerialization(final SrcGenContext srcGenContext,
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
