package software.sava.idl.generator.anchor;

import systems.comodal.jsoniter.JsonIterator;
import systems.comodal.jsoniter.factory.ElementFactory;

import java.util.function.Supplier;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

final class AnchorErrorParser implements ElementFactory<AnchorErrorRecord> {

  static final Supplier<AnchorErrorParser> FACTORY = AnchorErrorParser::new;

  private int code;
  private String name;
  private String msg;

  AnchorErrorParser() {
  }

  @Override
  public AnchorErrorRecord create() {
    return AnchorErrorRecord.createError(code, name, msg);
  }

  @Override
  public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
    if (fieldEquals("code", buf, offset, len)) {
      this.code = ji.readInt();
    } else if (fieldEquals("name", buf, offset, len)) {
      this.name = ji.readString();
    } else if (fieldEquals("msg", buf, offset, len)) {
      this.msg = ji.readString();
    } else {
      ji.skip();
    }
    return true;
  }
}
