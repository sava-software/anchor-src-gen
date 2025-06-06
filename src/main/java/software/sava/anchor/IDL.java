package software.sava.anchor;

import software.sava.core.accounts.PublicKey;
import systems.comodal.jsoniter.JsonIterator;

import java.io.IOException;
import java.util.List;

import static software.sava.anchor.AnchorSourceGenerator.removeBlankLines;

public interface IDL {

  List<String> NO_DOCS = List.of();

  static AnchorIDL parseIDL(byte[] json) {
    try (final var ji = JsonIterator.parse(json)) {
      final int mark = ji.mark();
      final IDLType idlType;
      if (ji.skipUntil("metadata") != null
          && ji.skipUntil("origin") != null
          && "shank".equals(ji.readString())) {
        idlType = IDLType.SHANK;
      } else {
        idlType = IDLType.ANCHOR;
      }
      final var parser = new AnchorIDL.Parser(idlType);
      ji.reset(mark).testObject(parser);
      return parser.createIDL(json);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  String generateConstantsSource(final GenSrcContext genSrcContext);

  String generatePDASource(final GenSrcContext genSrcContext);

  String generateErrorSource(final GenSrcContext genSrcContext);

  String generateSource(final GenSrcContext genSrcContext);

  default String closeClass(final GenSrcContext genSrcContext,
                            final String className,
                            final StringBuilder builder) {
    builder.append(String.format("""
        private %s() {
        }""", className
    ).indent(genSrcContext.tabLength()));
    return removeBlankLines(builder.append('}').toString());
  }

  PublicKey address();

  String version();

  String name();

  String origin();

  List<String> docs();

  byte[] json();
}
