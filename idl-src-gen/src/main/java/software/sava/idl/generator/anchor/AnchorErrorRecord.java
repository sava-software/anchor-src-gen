package software.sava.idl.generator.anchor;

import software.sava.anchor.AnchorUtil;

public record AnchorErrorRecord(int code,
                                String name,
                                String msg,
                                String className) implements AnchorError {

  static AnchorErrorRecord createError(final int code,
                                       final String name,
                                       final String msg) {
    final var className = AnchorUtil.camelCase(name, true);
    return new AnchorErrorRecord(code, name, msg, className);
  }

  void generateSource(final SrcGenContext srcGenContext, final StringBuilder out) {
    final var tab = srcGenContext.tab();
    out.append(String.format("""
            
            record %s(int code, String msg) implements %sError {
            
            """,
        className, srcGenContext.programName()
    ));
    out.append(tab).append(String.format("""
        public static final %s INSTANCE = new %s(
        """, className, className));
    out.append(tab).append(tab).append(tab).append(code).append(String.format("""
        , "%s"
        """, msg));
    out.append(tab).append(");\n}\n");
  }
}
