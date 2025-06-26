package software.sava.anchor;

import software.sava.core.accounts.PublicKey;
import software.sava.core.programs.Discriminator;
import systems.comodal.jsoniter.JsonIterator;

import java.util.List;

import static java.util.Arrays.copyOfRange;
import static software.sava.core.crypto.Hash.sha256;

public final class AnchorUtil {

  public static final int DISCRIMINATOR_LENGTH = 8;
  private static final String GLOBAL_NAMESPACE = "global:";
  private static final String IDL_SEED = "anchor:idl";

  public static PublicKey createIdlAddress(final PublicKey program) {
    final var basePDA = PublicKey.findProgramAddress(List.of(), program);
    return PublicKey.createWithSeed(basePDA.publicKey(), IDL_SEED, program);
  }

  public static int writeDiscriminator(final Discriminator discriminator, final byte[] data, final int offset) {
    return writeDiscriminator(discriminator.data(), data, offset);
  }

  public static int writeDiscriminator(final byte[] discriminator, final byte[] data, final int offset) {
    System.arraycopy(discriminator, 0, data, offset, discriminator.length);
    return discriminator.length;
  }

  public static int writeDiscriminator(final Discriminator discriminator, final byte[] data) {
    return writeDiscriminator(discriminator.data(), data);
  }

  public static int writeDiscriminator(final byte[] discriminator, final byte[] data) {
    return writeDiscriminator(discriminator, data, 0);
  }

  public static Discriminator parseDiscriminator(final byte[] data, final int offset) {
    final byte[] discriminator = new byte[DISCRIMINATOR_LENGTH];
    System.arraycopy(data, offset, discriminator, 0, DISCRIMINATOR_LENGTH);
    return Discriminator.createDiscriminator(discriminator);
  }

  public static Discriminator parseDiscriminator(final byte[] data) {
    return parseDiscriminator(data, 0);
  }

  public static Discriminator parseDiscriminator(final JsonIterator ji) {
    final byte[] discriminator = new byte[AnchorUtil.DISCRIMINATOR_LENGTH];
    for (int i = 0; ji.readArray(); ++i) {
      discriminator[i] = (byte) ji.readInt();
    }
    return Discriminator.createDiscriminator(discriminator);
  }

  public static Discriminator toDiscriminator(final String namespace, final String name) {
    return Discriminator.createDiscriminator(copyOfRange(
        sha256(snakeCase(namespace + ':' + name).getBytes()),
        0, DISCRIMINATOR_LENGTH
    ));
  }

  public static Discriminator toDiscriminator(final String name) {
    return Discriminator.createDiscriminator(copyOfRange(
        sha256((GLOBAL_NAMESPACE + snakeCase(name)).getBytes()),
        0, DISCRIMINATOR_LENGTH
    ));
  }

  public static String snakeCase(final String notSnakeCased) {
    return snakeCase(notSnakeCased, false);
  }

  public static String snakeCase(final String notSnakeCased, final boolean upperCase) {
    if (notSnakeCased == null || notSnakeCased.isBlank()) {
      return notSnakeCased;
    }
    final int len = notSnakeCased.length();
    final char[] buf = new char[len << 1];
    char c = notSnakeCased.charAt(0);
    boolean changed = false;
    boolean separate;
    if (Character.isUpperCase(c)) {
      separate = false;
      if (upperCase) {
        buf[0] = c;
      } else {
        buf[0] = Character.toLowerCase(c);
        changed = c != buf[0];
      }
    } else if (c == '_') {
      separate = false;
      buf[0] = c;
    } else {
      separate = true;
      if (upperCase) {
        buf[0] = Character.toUpperCase(c);
        changed = c != buf[0];
      } else {
        buf[0] = c;
      }
    }
    int s = 1;
    for (int i = 1; i < len; ++i) {
      c = notSnakeCased.charAt(i);
      if (Character.isWhitespace(c)) {
        changed = true;
        continue;
      }
      if (Character.isUpperCase(c)) {
        if (separate) {
          buf[s++] = '_';
          changed = true;
        }
        if (upperCase) {
          buf[s] = c;
        } else {
          buf[s] = Character.toLowerCase(c);
          changed = c != buf[s];
        }
      } else if (c == '_') {
        separate = false;
        buf[s] = c;
      } else {
        separate = true;
        if (upperCase) {
          buf[s] = Character.toUpperCase(c);
          changed = c != buf[s];
        } else {
          buf[s] = c;
        }
      }
      ++s;
    }
    return changed ? new String(buf, 0, s) : notSnakeCased;
  }

  public static String camelCase(final String maybeSnakeCase) {
    return camelCase(maybeSnakeCase, false);
  }

  public static String camelCase(final String maybeSnakeCase, final boolean firstUpper) {
    if (maybeSnakeCase == null || maybeSnakeCase.isBlank()) {
      return maybeSnakeCase;
    }

    final boolean changedFirst;
    final int len = maybeSnakeCase.length();
    final char[] buf;
    int i = 0;
    char chr;
    for (; ; ) {
      chr = maybeSnakeCase.charAt(i);
      if (Character.isAlphabetic(chr)) {
        buf = new char[len - i];
        if (firstUpper ^ Character.isLowerCase(chr)) {
          changedFirst = i != 0;
          buf[0] = chr;
        } else {
          changedFirst = true;
          buf[0] = firstUpper ? Character.toUpperCase(chr) : Character.toLowerCase(chr);
        }
        ++i;
        break;
      } else if (++i == len) {
        return null;
      }
    }

    int c = 1;
    for (; i < len; ++i) {
      chr = maybeSnakeCase.charAt(i);
      if (Character.isWhitespace(chr)) {
        continue;
      }
      if (chr == '_') {
        if (++i == len) {
          break;
        }
        buf[c] = Character.toUpperCase(maybeSnakeCase.charAt(i));
      } else {
        buf[c] = chr;
      }
      ++c;
    }

    return !changedFirst && c == len ? maybeSnakeCase : new String(buf, 0, c);
  }

  private AnchorUtil() {
  }
}
