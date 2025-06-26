package software.sava.anchor;

import software.sava.core.programs.Discriminator;

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public interface NamedType {

  Pattern NEW_LINE_PATTERN = Pattern.compile("\n");

  static String formatComments(Collection<String> docs) {
    return docs.stream()
        .map(doc -> String.format("// %s\n", NEW_LINE_PATTERN.matcher(doc).replaceAll("\n//")))
        .collect(Collectors.joining());
  }

  Discriminator discriminator();

  String name();

  List<String> docs();

  boolean index();

  static NamedType createType(Discriminator discriminator,
                                    String name,
                                    AnchorSerialization serialization,
                                    AnchorRepresentation representation,
                                    AnchorTypeContext type,
                                    List<String> docs,
                                    boolean index) {
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
      return new AnchorNamedType(
          discriminator,
          AnchorNamedType.RESERVED_NAMES.contains(name) ? '_' + name : name,
          serialization == null ? AnchorSerialization.borsh : serialization,
          representation,
          type,
          docs == null ? IDL.NO_DOCS : docs,
          index
      );
    }
  }

  static NamedType createType(Discriminator discriminator,
                                    String name,
                                    AnchorTypeContext type) {
    return createType(discriminator, name, null, null, type, IDL.NO_DOCS, false);
  }

  NamedType rename(final String newName);

  AnchorTypeContext type();

  String docComments();

  int generateSerialization(GenSrcContext genSrcContext,
                            StringBuilder paramsBuilder,
                            StringBuilder dataBuilder,
                            StringBuilder stringsBuilder,
                            StringBuilder dataLengthBuilder,
                            boolean hasNext);

  String generateRecordField(GenSrcContext genSrcContext);

  String generateStaticFactoryField(GenSrcContext genSrcContext);

  String generateNewInstanceField(GenSrcContext genSrcContext);

  String generateWrite(GenSrcContext genSrcContext, boolean hasNext);

  String generateRead(GenSrcContext genSrcContext,
                      boolean hasNext,
                      boolean singleField,
                      String offsetVarName);

  String generateLength(GenSrcContext genSrcContext);

  void generateMemCompFilter(GenSrcContext genSrcContext,
                             StringBuilder builder,
                             String offsetVarName);
}
