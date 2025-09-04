package software.sava.idl.generator;

import software.sava.core.programs.Discriminator;

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public interface NamedType {

  Pattern NEW_LINE_PATTERN = Pattern.compile("\n");

  static String formatComments(final Collection<String> docs) {
    return docs.stream()
        .map(doc -> String.format("// %s\n", NEW_LINE_PATTERN.matcher(doc).replaceAll("\n//")))
        .collect(Collectors.joining());
  }

  Discriminator discriminator();

  String name();

  List<String> docs();

  boolean index();

  private static String cleanName(final String name) {
    final int length = name.length();
    char c;
    for (int i = 0; i < length; ++i) {
      c = name.charAt(i);
      if (!Character.isAlphabetic(c)
          && !Character.isDigit(c)
          && c != '_') {
        int index = i;
        final char[] chars = name.toCharArray();
        chars[index] = '_';
        while (++index < length) {
          c = chars[index];
          if (!Character.isAlphabetic(c)
              && !Character.isDigit(c)
              && c != '_') {
            chars[index] = '_';
          }
        }
        return new String(chars);
      }
    }
    return name;
  }

  static NamedType createType(final Discriminator discriminator,
                              final String name,
                              final AnchorSerialization serialization,
                              final AnchorRepresentation representation,
                              final AnchorTypeContext type,
                              final List<String> docs,
                              final boolean index) {
    if (name == null) {
      return new AnchorNamedType(
          discriminator, '_' + type.type().name(),
          serialization == null ? AnchorSerialization.borsh : serialization,
          representation,
          type,
          docs == null ? IDL.NO_DOCS : docs,
          index
      );
    } else {
      final String cleanedName;
      if (AnchorNamedType.RESERVED_NAMES.contains(name)) {
        cleanedName = '_' + name;
      } else {
        cleanedName = cleanName(name);
      }
      return new AnchorNamedType(
          discriminator,
          cleanedName,
          serialization == null ? AnchorSerialization.borsh : serialization,
          representation,
          type,
          docs == null ? IDL.NO_DOCS : docs,
          index
      );
    }
  }

  static NamedType createType(final Discriminator discriminator, final String name, final AnchorTypeContext type) {
    return createType(discriminator, name, null, null, type, IDL.NO_DOCS, false);
  }

  NamedType rename(final String newName);

  AnchorTypeContext type();

  String docComments();

  int generateSerialization(final GenSrcContext genSrcContext,
                            final StringBuilder paramsBuilder,
                            final StringBuilder dataBuilder,
                            final StringBuilder stringsBuilder,
                            final StringBuilder dataLengthBuilder,
                            final boolean hasNext);

  String generateRecordField(final GenSrcContext genSrcContext);

  String generateStaticFactoryField(final GenSrcContext genSrcContext);

  String generateNewInstanceField(final GenSrcContext genSrcContext);

  String generateWrite(final GenSrcContext genSrcContext, final boolean hasNext);

  String generateRead(final GenSrcContext genSrcContext,
                      final boolean hasNext,
                      final boolean singleField,
                      final String offsetVarName);

  String generateLength(final GenSrcContext genSrcContext);

  void generateMemCompFilter(final GenSrcContext genSrcContext,
                             final StringBuilder builder,
                             final String offsetVarName);

  String arrayLengthConstant();
}
