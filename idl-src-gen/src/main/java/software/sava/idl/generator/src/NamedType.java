package software.sava.idl.generator.src;

import software.sava.core.programs.Discriminator;
import software.sava.idl.generator.anchor.SrcGenContext;

import java.util.List;

public interface NamedType {

  Discriminator discriminator();

  String name();

  List<String> docs();

  void appendDocs(final StringBuilder src);

  NamedType rename(final String newName);

  TypeContext type();

  String docComments();

  int generateSerialization(final SrcGenContext srcGenContext,
                            final StringBuilder paramsBuilder,
                            final StringBuilder dataBuilder,
                            final StringBuilder stringsBuilder,
                            final StringBuilder dataLengthBuilder,
                            final boolean hasNext);

  String generateRecordField(final SrcGenContext srcGenContext);

  String generateStaticFactoryField(final SrcGenContext srcGenContext);

  String generateNewInstanceField(final SrcGenContext srcGenContext);

  String generateWrite(final SrcGenContext srcGenContext, final boolean hasNext);

  String generateRead(final SrcGenContext srcGenContext,
                      final boolean hasNext,
                      final boolean singleField,
                      final String offsetVarName);

  String generateLength(final SrcGenContext srcGenContext);

  void generateMemCompFilter(final SrcGenContext srcGenContext,
                             final StringBuilder builder,
                             final String offsetVarName);

  String arrayLengthConstant();
}
