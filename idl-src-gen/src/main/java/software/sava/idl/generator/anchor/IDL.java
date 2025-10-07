package software.sava.idl.generator.anchor;

import software.sava.core.accounts.PublicKey;
import systems.comodal.jsoniter.JsonIterator;

import java.io.IOException;
import java.util.List;

import static software.sava.idl.generator.ParseUtil.removeBlankLines;

public interface IDL {

  System.Logger logger = System.getLogger(IDL.class.getName());

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
    } catch (final RuntimeException ex) {
      logger.log(System.Logger.Level.ERROR, "Failed to parse IDL: \n" + new String(json));
      throw ex;
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  String generateConstantsSource(final SrcGenContext srcGenContext);

  String generatePDASource(final SrcGenContext srcGenContext);

  String generateErrorSource(final SrcGenContext srcGenContext);

  String generateSource(final SrcGenContext srcGenContext);

  default String closeClass(final SrcGenContext srcGenContext,
                            final String className,
                            final StringBuilder builder) {
    builder.append(String.format("""
        private %s() {
        }""", className
    ).indent(srcGenContext.tabLength()));
    return removeBlankLines(builder.append('}').toString());
  }

  IDLType type();

  PublicKey address();

  String version();

  String name();

  String origin();

  List<String> docs();

  byte[] json();
}
