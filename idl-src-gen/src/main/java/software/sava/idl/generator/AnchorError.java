package software.sava.idl.generator;

public interface AnchorError {

  static AnchorError createError(final int code, final String name, final String msg) {
    return AnchorErrorRecord.createError(code, name, msg);
  }

  int code();

  String name();

  String msg();
}
