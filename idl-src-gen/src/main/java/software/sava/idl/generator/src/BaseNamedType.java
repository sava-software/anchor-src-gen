package software.sava.idl.generator.src;

import software.sava.idl.generator.anchor.SrcGenContext;

import java.util.List;
import java.util.Objects;

public abstract class BaseNamedType<C extends TypeContext> implements NamedType {

  protected final String name;
  protected final C type;
  protected final List<String> docs;
  protected final String docComments;

  protected BaseNamedType(final String name,
                          final C type,
                          final List<String> docs,
                          final String docComments) {
    this.name = name;
    this.type = type;
    this.docs = docs;
    this.docComments = docComments;
  }

  @Override
  public final String name() {
    return name;
  }

  @Override
  public final C type() {
    return type;
  }

  @Override
  public final List<String> docs() {
    return docs;
  }

  @Override
  public final String docComments() {
    return docComments;
  }

  @Override
  public final void appendDocs(final StringBuilder src) {
    src.append(docComments);
  }

  @Override
  public final int generateSerialization(final SrcGenContext srcGenContext,
                                         final StringBuilder paramsBuilder,
                                         final StringBuilder dataBuilder,
                                         final StringBuilder stringsBuilder,
                                         final StringBuilder dataLengthBuilder,
                                         final boolean hasNext) {
    return type.generateIxSerialization(srcGenContext, this, paramsBuilder, dataBuilder, stringsBuilder, dataLengthBuilder, hasNext);
  }

  @Override
  public final String generateRecordField(final SrcGenContext srcGenContext) {
    return type.generateRecordField(srcGenContext, this, false);
  }

  @Override
  public final String generateStaticFactoryField(final SrcGenContext srcGenContext) {
    return type.generateStaticFactoryField(srcGenContext, name, false);
  }

  @Override
  public final String generateNewInstanceField(final SrcGenContext srcGenContext) {
    return type.generateNewInstanceField(srcGenContext, name);
  }

  @Override
  public final String generateWrite(final SrcGenContext srcGenContext, final boolean hasNext) {
    return type.generateWrite(srcGenContext, name, hasNext);
  }

  @Override
  public final String generateRead(final SrcGenContext srcGenContext,
                                   final boolean hasNext,
                                   final boolean singleField,
                                   final String offsetVarName) {
    return type.generateRead(srcGenContext, name, hasNext, singleField, offsetVarName);
  }

  @Override
  public final String generateLength(final SrcGenContext srcGenContext) {
    return type.generateLength(name, srcGenContext);
  }

  @Override
  public final void generateMemCompFilter(final SrcGenContext srcGenContext,
                                          final StringBuilder builder,
                                          final String offsetVarName) {
    type.generateMemCompFilter(srcGenContext, builder, name, offsetVarName);
  }

  @Override
  public String arrayLengthConstant() {
    return type.arrayLengthConstant(name);
  }

  @Override
  public boolean equals(final Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    final var that = (BaseNamedType<?>) o;
    return name.equals(that.name)
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
    return result;
  }

  @Override
  public String toString() {
    return "BaseNamedType{" +
        "name='" + name + '\'' +
        ", type=" + type +
        ", docs=" + docs +
        ", docComments='" + docComments + '\'' +
        '}';
  }
}
