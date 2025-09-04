package software.sava.idl.generator;

import systems.comodal.jsoniter.JsonIterator;

import java.util.ArrayList;
import java.util.List;

public final class ParseUtil {

  static List<String> parseDocs(final JsonIterator ji) {
    final var docs = new ArrayList<String>();
    while (ji.readArray()) {
      docs.add(ji.readString()); // .replace('`', '"')
    }
    return docs;
  }

  private ParseUtil() {
  }
}
