package software.sava.idl.generator;

import systems.comodal.jsoniter.JsonIterator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class ParseUtil {

  public static List<String> parseDocs(final JsonIterator ji) {
    final var docs = new ArrayList<String>();
    while (ji.readArray()) {
      docs.add(ji.readString()); // .replace('`', '"')
    }
    return docs;
  }

  private ParseUtil() {
  }

  public static String removeBlankLines(final String str) {
    return Arrays.stream(str.split("\n"))
        .map(line -> !line.isEmpty() && line.isBlank() ? "" : line)
        .collect(Collectors.joining("\n", "", "\n"));
  }
}
