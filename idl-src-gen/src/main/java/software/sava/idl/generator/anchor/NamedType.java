package software.sava.idl.generator.anchor;

import software.sava.core.programs.Discriminator;

import java.util.List;

public interface NamedType {

  Discriminator discriminator();

  String name();

  List<String> docs();

  boolean index();

  static String cleanName(final String name) {
    final int length = name.length();
    char c;
    for (int i = 0; i < length; ++i) {
      c = name.charAt(i);
      if (!Character.isAlphabetic(c) && !Character.isDigit(c) && c != '_') {
        int index = i;
        final char[] chars = name.toCharArray();
        chars[index] = '_';
        while (++index < length) {
          c = chars[index];
          if (!Character.isAlphabetic(c) && !Character.isDigit(c) && c != '_') {
            chars[index] = '_';
          }
        }
        return new String(chars);
      }
    }
    return name;
  }

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
