package software.sava.idl.generator.anchor;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public abstract class BaseNamedType implements NamedType {

  static final Set<String> RESERVED_NAMES = Set.of(
      "new",
      "offset"
  );

  protected final String name;
  protected final TypeContext type;
  protected final List<String> docs;
  protected final String docComments;
  protected final boolean index;

  protected BaseNamedType(final String name,
                          final TypeContext type,
                          final List<String> docs,
                          final String docComments,
                          final boolean index) {
    this.name = name;
    this.type = type;
    this.docs = docs;
    this.docComments = docComments;
    this.index = index;
  }

  @Override
  public String docComments() {
    return docComments;
  }

  @Override
  public int generateSerialization(final SrcGenContext srcGenContext,
                                   final StringBuilder paramsBuilder,
                                   final StringBuilder dataBuilder,
                                   final StringBuilder stringsBuilder,
                                   final StringBuilder dataLengthBuilder,
                                   final boolean hasNext) {
    return type.generateIxSerialization(srcGenContext, this, paramsBuilder, dataBuilder, stringsBuilder, dataLengthBuilder, hasNext);
  }

  @Override
  public String generateRecordField(final SrcGenContext srcGenContext) {
    return type.generateRecordField(srcGenContext, this, false);
  }

  @Override
  public String generateStaticFactoryField(final SrcGenContext srcGenContext) {
    return type.generateStaticFactoryField(srcGenContext, name, false);
  }

  @Override
  public String generateNewInstanceField(final SrcGenContext srcGenContext) {
    return type.generateNewInstanceField(srcGenContext, name);
  }

  @Override
  public String generateWrite(final SrcGenContext srcGenContext, final boolean hasNext) {
    return type.generateWrite(srcGenContext, name, hasNext);
  }

  @Override
  public String generateRead(final SrcGenContext srcGenContext,
                             final boolean hasNext,
                             final boolean singleField,
                             final String offsetVarName) {
    return type.generateRead(srcGenContext, name, hasNext, singleField, offsetVarName);
  }

  @Override
  public String generateLength(final SrcGenContext srcGenContext) {
    return type.generateLength(name, srcGenContext);
  }

  @Override
  public void generateMemCompFilter(final SrcGenContext srcGenContext,
                                    final StringBuilder builder,
                                    final String offsetVarName) {
    type.generateMemCompFilter(srcGenContext, builder, name, offsetVarName);
  }

  @Override
  public String arrayLengthConstant() {
    return type.arrayLengthConstant(name);
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public TypeContext type() {
    return type;
  }

  @Override
  public List<String> docs() {
    return docs;
  }

  @Override
  public boolean index() {
    return index;
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof final BaseNamedType that)) return false;
    return index == that.index
        && name.equals(that.name)
        && Objects.equals(type, that.type)
        && docs.equals(that.docs)
        && docComments.equals(that.docComments);
  }

  @Override
  public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + Objects.hashCode(type);
    result = 31 * result + docs.hashCode();
    result = 31 * result + docComments.hashCode();
    result = 31 * result + Boolean.hashCode(index);
    return result;
  }
}
