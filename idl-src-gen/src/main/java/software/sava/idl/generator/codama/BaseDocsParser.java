package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

import java.util.ArrayList;
import java.util.List;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

abstract class BaseDocsParser extends BaseParser {

  protected List<String> docs;

  @Override
  public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
    if (fieldEquals("docs", buf, offset, len)) {
      final var docs = new ArrayList<String>();
      while (ji.readArray()) {
        docs.add(ji.readString());
      }
      this.docs = List.copyOf(docs);
    } else {
      return super.test(buf, offset, len, ji);
    }
    return true;
  }
}
