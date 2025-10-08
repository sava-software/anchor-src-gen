package software.sava.idl.generator.src;

import software.sava.anchor.AnchorUtil;
import software.sava.core.programs.Discriminator;

import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Locale.ENGLISH;

public final class SrcUtil {

  public static final Pattern NEW_LINE_PATTERN = Pattern.compile("\n");

  public static String formatComments(final Collection<String> docs) {
    return docs.stream()
        .map(doc -> String.format("// %s\n", NEW_LINE_PATTERN.matcher(doc).replaceAll("\n//")))
        .collect(Collectors.joining());
  }

  private SrcUtil() {
  }

  public static String replaceNewLinesIfLessThan(final String lines, final int numLines, final int limit) {
    return numLines < limit && !lines.contains("//") ? lines.replaceAll("\n +", " ") : lines;
  }

  public static String replaceNewLinesIfLessThan(final StringBuilder lines, final int numLines, final int limit) {
    return replaceNewLinesIfLessThan(lines.toString(), numLines, limit);
  }

  public static String replaceNewLinesIfLessThan(final String lines, final int limit) {
    int numNewLines = 0;
    for (int from = 0, to = lines.length(); from < to; ++from) {
      if (lines.indexOf('\n', from) > 0) {
        ++numNewLines;
      }
    }
    return replaceNewLinesIfLessThan(lines, numNewLines, limit);
  }

  public static String replaceNewLinesIfLessThan(final StringBuilder lines, final int limit) {
    return replaceNewLinesIfLessThan(lines.toString(), limit);
  }

  public static String formatKeyName(final String name) {
    return name.endsWith("Key") || name.endsWith("key") ? name : name + "Key";
  }

  public static String formatDiscriminatorReference(final String ixName) {
    return String.format("%s_DISCRIMINATOR", AnchorUtil.snakeCase(ixName).toUpperCase(ENGLISH));
  }

  public static String formatDiscriminator(final String ixName, final Discriminator discriminator) {
    return Arrays.stream(discriminator.toIntArray())
        .mapToObj(Integer::toString)
        .collect(Collectors.joining(", ",
            String.format("  public static final Discriminator %s = toDiscriminator(", formatDiscriminatorReference(ixName)), ");"
        ));
  }

}
