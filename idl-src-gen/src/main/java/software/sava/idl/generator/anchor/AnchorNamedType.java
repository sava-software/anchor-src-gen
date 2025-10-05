package software.sava.idl.generator.anchor;

import software.sava.core.programs.Discriminator;

import java.util.List;
import java.util.Set;

public record AnchorNamedType(Discriminator discriminator,
                              String name,
                              AnchorSerialization serialization,
                              AnchorRepresentation representation,
                              AnchorTypeContext type,
                              List<String> docs,
                              boolean index) implements NamedType {

  static final Set<String> RESERVED_NAMES = Set.of(
      "new",
      "offset"
  );

  @Override
  public NamedType rename(final String newName) {
    return new AnchorNamedType(
        discriminator,
        newName,
        serialization,
        representation,
        type,
        docs,
        index
    );
  }

  @Override
  public String docComments() {
    return NamedType.formatComments(this.docs);
  }

  @Override
  public int generateSerialization(final GenSrcContext genSrcContext,
                                   final StringBuilder paramsBuilder,
                                   final StringBuilder dataBuilder,
                                   final StringBuilder stringsBuilder,
                                   final StringBuilder dataLengthBuilder,
                                   final boolean hasNext) {
    return type.generateIxSerialization(genSrcContext, this, paramsBuilder, dataBuilder, stringsBuilder, dataLengthBuilder, hasNext);
  }

  @Override
  public String generateRecordField(final GenSrcContext genSrcContext) {
    return type.generateRecordField(genSrcContext, this, false);
  }

  @Override
  public String generateStaticFactoryField(final GenSrcContext genSrcContext) {
    return type.generateStaticFactoryField(genSrcContext, name, false);
  }

  @Override
  public String generateNewInstanceField(final GenSrcContext genSrcContext) {
    return type.generateNewInstanceField(genSrcContext, name);
  }

  @Override
  public String generateWrite(final GenSrcContext genSrcContext, final boolean hasNext) {
    return type.generateWrite(genSrcContext, name, hasNext);
  }

  @Override
  public String generateRead(final GenSrcContext genSrcContext,
                             final boolean hasNext,
                             final boolean singleField,
                             final String offsetVarName) {
    return type.generateRead(genSrcContext, name, hasNext, singleField, offsetVarName);
  }

  @Override
  public String generateLength(final GenSrcContext genSrcContext) {
    return type.generateLength(name, genSrcContext);
  }

  @Override
  public void generateMemCompFilter(final GenSrcContext genSrcContext,
                                    final StringBuilder builder,
                                    final String offsetVarName) {
    type.generateMemCompFilter(genSrcContext, builder, name, offsetVarName);
  }

  @Override
  public String arrayLengthConstant() {
    return type.arrayLengthConstant(name);
  }
}
