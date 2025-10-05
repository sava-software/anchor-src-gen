package software.sava.idl.generator.anchor;

import systems.comodal.jsoniter.CharBufferFunction;
import systems.comodal.jsoniter.factory.ElementFactory;

import static software.sava.anchor.AnchorUtil.camelCase;

public interface NamedTypeParser extends ElementFactory<NamedType>, CharBufferFunction<NamedType> {

  static String cleanName(final String name, final boolean firstUpper) {
    int nameSpace = name.indexOf(':');
    if (nameSpace < 0) {
      return camelCase(name, firstUpper);
    } else {
      // Convert to snake case, then camel case.
      final char[] chars = new char[name.length()];
      for (int srcBegin = 0, destBegin = 0; ; ) {
        name.getChars(srcBegin, nameSpace, chars, destBegin);

        destBegin += nameSpace - srcBegin;
        chars[destBegin] = '_';
        ++destBegin;

        srcBegin = nameSpace + 2;
        nameSpace = name.indexOf(':', srcBegin);
        if (nameSpace < 0) {
          nameSpace = chars.length;
          name.getChars(srcBegin, nameSpace, chars, destBegin);
          destBegin += nameSpace - srcBegin;
          return camelCase(new String(chars, 0, destBegin), firstUpper);
        }
      }
    }
  }
}
