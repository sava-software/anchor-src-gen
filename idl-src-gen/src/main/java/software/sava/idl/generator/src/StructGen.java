package software.sava.idl.generator.src;

import static software.sava.idl.generator.ParseUtil.removeBlankLines;

public class StructGen {

  public static int sigLine(final StringBuilder src, final String name, final boolean publicAccess) {
    int recordSigLineLength = 8 + name.length();
    if (publicAccess) {
      recordSigLineLength += 7;
      src.append("public ");
    }
    src.append("record ").append(name).append('(');
    return recordSigLineLength;
  }

  public static String emptyStruct(final String tab,
                                   final StringBuilder src,
                                   final String interfaceName,
                                   final String name) {
    src.append(String.format("""
        ) implements %s {
        
        """, interfaceName
    ));
    src.append(String.format("""
        private static final %s INSTANCE = new %s();
        
        public static %s read(final byte[] _data, final int offset) {
        %sreturn INSTANCE;
        }
        
        @Override
        public int write(final byte[] _data, final int offset) {
        %sreturn 0;
        }
        
        @Override
        public int l() {
        %sreturn 0;
        }
        """, name, name, name, tab, tab, tab
    ).indent(tab.length()));
    src.append('}');
    return removeBlankLines(src.toString());
  }
}
