package software.sava.idl.generator;

import systems.comodal.jsoniter.JsonIterator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class ParseUtil {

  private static final Set<String> RESERVED_NAMES = Set.of(
      "new",
      "offset"
  );

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

  public static String checkTypeName(final String name) {
    return RESERVED_NAMES.contains(name)
        ? '_' + name
        : cleanName(name);
  }

  private static String cleanName(final String name) {
    final int length = name.length();
    char c;
    for (int i = 0; i < length; ++i) {
      c = name.charAt(i);
      if (!Character.isAlphabetic(c) && !Character.isDigit(c) && c != '_') {
        int index = i;
        final char[] chars = name.toCharArray();
        chars[index] = '_';
        while (++index < length) {
          c = chars[index];
          if (!Character.isAlphabetic(c) && !Character.isDigit(c) && c != '_') {
            chars[index] = '_';
          }
        }
        return new String(chars);
      }
    }
    return name;
  }
}
