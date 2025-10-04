package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

abstract class BaseParser implements FieldBufferPredicate {

  protected String name;

  @Override
  public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
    if (fieldEquals("name", buf, offset, len)) {
      name = ji.readString();
    } else if (fieldEquals("kind", buf, offset, len)) {
      ji.skip();
    } else {
      throw new IllegalStateException(String.format(
          "Unhandled %s field %s.",
          getClass().getSimpleName(), new String(buf, offset, len)
      ));
    }
    return true;
  }
}
