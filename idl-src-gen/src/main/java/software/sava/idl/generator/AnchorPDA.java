package software.sava.idl.generator;

import software.sava.anchor.AnchorUtil;
import software.sava.core.accounts.PublicKey;
import software.sava.core.encoding.ByteUtil;
import systems.comodal.jsoniter.CharBufferFunction;
import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;
import systems.comodal.jsoniter.ValueType;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static software.sava.core.accounts.PublicKey.PUBLIC_KEY_LENGTH;
import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

public record AnchorPDA(List<Seed> seeds, ProgramSeed program) {

  static AnchorPDA parsePDA(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createPDA();
  }

  public record AccountSeed(AnchorType type,
                            int index,
                            String path,
                            String camelPath) implements Seed, ProgramSeed {

    @Override
    public String varName(final GenSrcContext genSrcContext) {
      return camelPath + "Account.toByteArray()";
    }

    @Override
    public String fieldName(final GenSrcContext genSrcContext, final Set<String> deDuplicateKnown) {
      return String.format(
          "final %s %sAccount",
          type.javaType().getSimpleName(),
          camelPath
      );
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) return true;
      if (o instanceof AccountSeed seed) {
        return path.equals(seed.path);
      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return path.hashCode();
    }

    @Override
    public String varName() {
      return camelPath;
    }
  }

  public record ArgSeed(AnchorType type, int index, String path, String camelPath) implements Seed {

    @Override
    public String varName(final GenSrcContext genSrcContext) {
      return type == null || type.javaType().equals(byte[].class) ? camelPath : camelPath + "Bytes";
    }

    @Override
    public String fieldName(final GenSrcContext genSrcContext, final Set<String> deDuplicateKnown) {
      // TODO: generate more convenient methods based on type.
      return String.format(
          "final %s %s",
          type == null ? "byte[]" : type.javaType().getSimpleName(),
          camelPath
      );
    }

    public void serialize(final GenSrcContext genSrcContext, final StringBuilder src) {
      if (type != null) {
        final var serializationSrc = switch (type) {
          case bool -> String.format("""
                  final byte[] %sBytes = new byte[] {%s ? 1 : 0};""",
              camelPath, camelPath
          );
          case _enum -> String.format("""
                  final byte[] %sBytes = new byte[%s.l()];
                  %s.write(%sBytes);""",
              camelPath, camelPath, camelPath, camelPath
          );
          case publicKey -> String.format("""
                  final byte[] %sBytes = %s.toByteArray();""",
              camelPath, camelPath
          );
          case f32 -> {
            genSrcContext.addImport(ByteUtil.class);
            yield String.format("""
                    final byte[] %sBytes = new byte[Float.BYTES];
                    ByteUtil.putFloat32LE(%sBytes, 0, %s);""",
                camelPath, camelPath, camelPath
            );
          }
          case f64 -> {
            genSrcContext.addImport(ByteUtil.class);
            yield String.format("""
                    final byte[] %sBytes = new byte[Double.BYTES];
                    ByteUtil.putFloat64LE(%sBytes, 0, %s);""",
                camelPath, camelPath, camelPath
            );
          }
          case i8, u8 -> {
            genSrcContext.addImport(ByteUtil.class);
            yield String.format("""
                    final byte[] %sBytes = new byte[] {%s};""",
                camelPath, camelPath
            );
          }
          case i16, u16 -> {
            genSrcContext.addImport(ByteUtil.class);
            yield String.format("""
                    final byte[] %sBytes = new byte[Short.BYTES];
                    ByteUtil.putInt16LE(%sBytes, 0, %s);""",
                camelPath, camelPath, camelPath
            );
          }
          case i32, u32 -> {
            genSrcContext.addImport(ByteUtil.class);
            yield String.format("""
                    final byte[] %sBytes = new byte[Integer.BYTES];
                    ByteUtil.putInt32LE(%sBytes, 0, %s);""",
                camelPath, camelPath, camelPath
            );
          }
          case i64, u64 -> {
            genSrcContext.addImport(ByteUtil.class);
            yield String.format("""
                    final byte[] %sBytes = new byte[Long.BYTES];
                    ByteUtil.putInt64LE(%sBytes, 0, %s);""",
                camelPath, camelPath, camelPath
            );
          }
          case i128, u128 -> {
            genSrcContext.addImport(BigDecimal.class);
            genSrcContext.addImport(ByteUtil.class);
            yield String.format("""
                    final byte[] %sBytes = new byte[128];
                    ByteUtil.getUInt128LE(%sBytes, 0, %s);""",
                camelPath, camelPath, camelPath
            );
          }
          case i256, u256 -> {
            genSrcContext.addImport(BigDecimal.class);
            genSrcContext.addImport(ByteUtil.class);
            yield String.format("""
                    final byte[] %sBytes = new byte[256];
                    ByteUtil.getUInt256LE(%sBytes, 0, %s);""",
                camelPath, camelPath, camelPath
            );
          }
          case bytes -> null;
          default ->
              throw new UnsupportedOperationException("TODO: Support PDA serialization for " + type + " type args");
        };
        if (serializationSrc != null) {
          src.append(serializationSrc.indent(genSrcContext.tabLength() << 1));
        }
      }
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) return true;
      if (o instanceof ArgSeed seed) {
        return path.equals(seed.path);
      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return path.hashCode();
    }
  }

  public record ConstSeed(AnchorType type,
                          int index,
                          byte[] seed, String str,
                          boolean isReadable,
                          PublicKey maybeKnownPublicKey) implements Seed, ProgramSeed {

    static boolean isReadable(final String decoded) {
      return decoded.chars().allMatch(c -> c < 0x7F);
    }

    @Override
    public String varName() {
      return "program";
    }

    @Override
    public String varName(final GenSrcContext genSrcContext) {
      if (isReadable) {
        genSrcContext.addUS_ASCII_Import();
        return String.format("""
            "%s".getBytes(US_ASCII)""", str
        );
      } else if (maybeKnownPublicKey != null) {
        // TODO: fix naming if starts with digit.
        final var knownAccountRef = genSrcContext.accountMethods().get(maybeKnownPublicKey);
        if (knownAccountRef != null) {
          return knownAccountRef.callReference() + ".toByteArray()";
        } else {
          return pubKeyVarName() + ".toByteArray()";
        }
      } else {
        return "seed" + index;
      }
    }

    private String pubKeyVarName() {
      final var base58 = maybeKnownPublicKey.toBase58();
      return Character.isDigit(base58.charAt(0)) ? '_' + base58 : base58;
    }

    @Override
    public String fieldName(final GenSrcContext genSrcContext, final Set<String> deDuplicateKnown) {
      if (isReadable) {
        return null;
      } else if (maybeKnownPublicKey != null) {
        final var knownAccountRef = genSrcContext.accountMethods().get(maybeKnownPublicKey);
        if (knownAccountRef != null) {
          final var accountsClas = knownAccountRef.clas();
          final var field = String.format("final %s %s", accountsClas.getSimpleName(), AnchorUtil.camelCase(accountsClas.getSimpleName(), false));
          if (deDuplicateKnown.add(field)) {
            genSrcContext.addImport(accountsClas);
            return field;
          } else {
            return null;
          }
        } else {
          return "final PublicKey " + pubKeyVarName();
        }
      } else {
        return "final byte[] unknownSeedConstant" + index;
      }
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) return true;
      if (o instanceof ConstSeed other) {
        return Arrays.equals(this.seed, other.seed);
      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(seed);
    }
  }

  private enum Kind {
    account,
    arg,
    _const
  }

  private static final CharBufferFunction<Kind> KIND_PARSER = (buf, offset, len) -> {
    if (fieldEquals("account", buf, offset, len)) {
      return Kind.account;
    } else if (fieldEquals("arg", buf, offset, len)) {
      return Kind.arg;
    } else if (fieldEquals("const", buf, offset, len)) {
      return Kind._const;
    } else {
      throw new IllegalStateException("Unhandled AnchorPDA.Kind field " + new String(buf, offset, len));
    }
  };

  private static final class SeedParser implements FieldBufferPredicate {

    private Kind kind;
    private AnchorType type;
    private String account;
    private String path;
    private byte[] value;

    private SeedParser() {
    }

    private AccountSeed createAccountSeed(final int index) {
      return new AccountSeed(
          Objects.requireNonNullElse(type, AnchorType.publicKey),
          index,
          path,
          AnchorUtil.camelCase(path, false)
      );
    }

    private ArgSeed createArgSeed(final int index) {
      return new ArgSeed(
          type,
          index,
          path,
          AnchorUtil.camelCase(path.replace('.', '_'), false)
      );
    }

    private ConstSeed createConstSeed(final int index) {
      final var str = new String(value);
      PublicKey maybePublicKey;
      if (value.length == PUBLIC_KEY_LENGTH) {
        try {
          maybePublicKey = PublicKey.createPubKey(value);
        } catch (final RuntimeException e) {
          maybePublicKey = null;
        }
      } else {
        maybePublicKey = null;
      }
      return new ConstSeed(
          type,
          index,
          value, str,
          ConstSeed.isReadable(str),
          maybePublicKey
      );
    }

    Seed createSeed(final int index) {
      return switch (kind) {
        case account -> createAccountSeed(index);
        case arg -> createArgSeed(index);
        case _const -> createConstSeed(index);
      };
    }

    ProgramSeed createProgramSeed() {
      return switch (kind) {
        case account -> createAccountSeed(0);
        case _const -> createConstSeed(0);
        default -> throw new IllegalStateException("Unexpected PDA program kind: " + kind);
      };
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("kind", buf, offset, len)) {
        this.kind = ji.applyChars(KIND_PARSER);
      } else if (fieldEquals("value", buf, offset, len)) {
        final var next = ji.whatIsNext();
        if (next == ValueType.STRING) {
          this.value = ji.readString().getBytes();
        } else if (next == ValueType.ARRAY) {
          final byte[] seed = new byte[PUBLIC_KEY_LENGTH];
          int i = 0;
          for (; ji.readArray(); ++i) {
            seed[i] = (byte) ji.readInt();
          }
          this.value = i < PUBLIC_KEY_LENGTH
              ? Arrays.copyOfRange(seed, 0, i)
              : seed;
        } else {
          throw new IllegalStateException("Unhandled AnchorPDA.Seed.value type " + next);
        }
      } else if (fieldEquals("path", buf, offset, len)) {
        final var path = ji.readString().replace('.', '_');
        this.path = Character.isDigit(buf[offset]) ? '_' + path : path;
      } else if (fieldEquals("type", buf, offset, len)) {
        this.type = AnchorType.valueOf(ji.readString());
      } else if (fieldEquals("account", buf, offset, len)) {
        this.account = ji.readString();
      } else {
        throw new IllegalStateException("Unhandled AnchorPDA.Seed field " + new String(buf, offset, len));
      }
      return true;
    }
  }

  private static final class Parser implements FieldBufferPredicate {

    private List<Seed> seeds;
    private ProgramSeed program;

    private Parser() {
    }

    private AnchorPDA createPDA() {
      return new AnchorPDA(seeds, program);
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("seeds", buf, offset, len)) {
        final var seeds = new ArrayList<Seed>();
        for (int i = 0; ji.readArray(); ++i) {
          final var parser = new SeedParser();
          ji.testObject(parser);
          seeds.add(parser.createSeed(i));
        }
        this.seeds = List.copyOf(seeds);
      } else if (fieldEquals("program", buf, offset, len)) {
        final var parser = new SeedParser();
        ji.testObject(parser);
        this.program = parser.createProgramSeed();
      } else {
        throw new IllegalStateException("Unhandled AnchorPDA field " + new String(buf, offset, len));
      }
      return true;
    }
  }

  public void genSrc(final GenSrcContext genSrcContext,
                     final String name,
                     final StringBuilder out) {
    final var tab = genSrcContext.tab();
    final var signatureLine = tab + String.format("public static ProgramDerivedAddress %sPDA(", name);
    out.append(signatureLine);
    out.append("final PublicKey ");
    final var programFieldName = program == null ? "program" : program.varName();
    out.append(programFieldName);

    final var deduplicateKnown = HashSet.<String>newHashSet(seeds.size());
    final var fieldsList = seeds.stream()
        .map(seed -> seed.fieldName(genSrcContext, deduplicateKnown))
        .filter(Objects::nonNull)
        .toList();
    if (fieldsList.isEmpty()) {
      out.append(") {\n");
    } else {
      final var argTab = " ".repeat(signatureLine.length());
      final var fields = fieldsList.stream()
          .collect(Collectors.joining(",\n" + argTab, ",\n" + argTab, ") {\n"));
      out.append(fields);
      for (final var seed : seeds) {
        if (seed instanceof ArgSeed argSeed) {
          argSeed.serialize(genSrcContext, out);
        }
      }
    }

    final var paramRefs = seeds.stream()
        .map(seed -> seed.varName(genSrcContext))
        .collect(Collectors.joining(",\n"));

    out.append(tab).append(tab).append("""
        return PublicKey.findProgramAddress(List.of(
        """);
    out.append(paramRefs.indent(genSrcContext.tabLength() + (genSrcContext.tabLength() << 1)));
    out.append(tab).append(tab).append("), ").append(programFieldName).append(");\n");
    out.append(tab).append("}\n");
  }
}
