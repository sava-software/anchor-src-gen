package software.sava.idl.generator;

import systems.comodal.jsoniter.JsonIterator;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class ParseUtil {

  public static List<String> parseDocs(final JsonIterator ji) {
    final var docs = new ArrayList<String>();
    for (String line; ji.readArray(); ) {
      line = ji.readString();
      if (line.isBlank()) {
        docs.add("");
      } else {
        docs.add(line);
      }
    }
    return docs;
  }

  private static final Pattern NEW_LINE_PATTERN = Pattern.compile("\n");

  public static String removeBlankLines(final String str) {
    return NEW_LINE_PATTERN.splitAsStream(str)
        .map(line -> line.isBlank() ? "" : line)
        .collect(Collectors.joining("\n", "", "\n"));
  }

  private ParseUtil() {
  }
}
