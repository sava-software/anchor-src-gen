package software.sava.anchor;

import software.sava.core.accounts.ProgramDerivedAddress;
import software.sava.core.accounts.PublicKey;
import software.sava.rpc.json.PublicKeyEncoding;
import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static software.sava.anchor.AnchorNamedType.NO_DOCS;
import static software.sava.anchor.AnchorSourceGenerator.removeBlankLines;
import static systems.comodal.jsoniter.JsonIterator.fieldEquals;
import static systems.comodal.jsoniter.factory.ElementFactory.parseList;

// https://github.com/acheroncrypto/anchor/blob/fix-idl/lang/syn/src/idl/types.rs
// https://github.com/coral-xyz/anchor/blob/master/ts/packages/anchor/src/idl.ts
public record AnchorIDL(PublicKey address,
                        String version,
                        String name,
                        List<AnchorConstant> constants,
                        List<AnchorInstruction> instructions,
                        Map<String, AnchorNamedType> accounts,
                        Map<String, AnchorNamedType> types,
                        List<AnchorNamedType> events,
                        List<AnchorErrorRecord> errors,
                        AnchorIdlMetadata metaData,
                        List<String> docs,
                        byte[] json) {

  public static AnchorIDL parseIDL(final byte[] json) {
    final var parser = new Parser();
    try (final var ji = JsonIterator.parse(json)) {
      ji.testObject(parser);
      return parser.createIDL(json);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  public String generateConstantsSource(final GenSrcContext genSrcContext) {
    if (constants == null || constants.isEmpty()) {
      return null;
    }

    final var constantsBuilder = new StringBuilder(1_024);
    for (final var constant : constants) {
      constant.toSrc(genSrcContext, constantsBuilder);
    }

    final var out = new StringBuilder(constantsBuilder.length() << 1);
    genSrcContext.appendPackage(out);
    if (genSrcContext.appendImports(out)) {
      out.append('\n');
    }

    final var className = genSrcContext.programName() + "Constants";
    out.append(String.format("""
        public final class %s {
        
        """, className));
    out.append(constantsBuilder);

    return closeClass(genSrcContext, className, out);
  }

  public String generatePDASource(final GenSrcContext genSrcContext) {
    final var pdaAccounts = new TreeMap<String, AnchorPDA>();
    final var distinct = new HashSet<AnchorPDA>();
    for (final var ix : instructions) {
      for (final var account : ix.accounts()) {
        final var pda = account.pda();
        if (pda != null) {
          var previous = pdaAccounts.putIfAbsent(account.name(), pda);
          if (previous != null && !distinct.contains(pda)) {
            for (int i = 1; previous != null; ++i) {
              previous = pdaAccounts.putIfAbsent(account.name() + i, pda);
            }
          }
          distinct.add(pda);
        }
      }
    }

    if (pdaAccounts.isEmpty()) {
      return null;
    }

    final var pdaBuilder = new StringBuilder(4_096);
    pdaAccounts.forEach((name, pda) -> {
      pda.genSrc(genSrcContext, name, pdaBuilder);
      pdaBuilder.append('\n');
    });

    final var out = new StringBuilder(pdaBuilder.length() << 1);
    genSrcContext.appendPackage(out);

    genSrcContext.addImport(ProgramDerivedAddress.class);
    genSrcContext.addImport(PublicKey.class);
    genSrcContext.addImport(List.class);
    genSrcContext.appendImports(out);

    final var className = genSrcContext.programName() + "PDAs";
    out.append(String.format("""
        
        public final class %s {
        
        """, className));
    out.append(pdaBuilder);
    return closeClass(genSrcContext, className, out);
  }

  public String generateErrorSource(final GenSrcContext genSrcContext) {
    if (errors.isEmpty()) {
      return null;
    }
    final var errorClassBuilder = new StringBuilder(4_096);
    for (final var error : errors) {
      error.generateSource(genSrcContext, errorClassBuilder);
    }

    final var out = new StringBuilder(4_096);
    genSrcContext.appendPackage(out);

    genSrcContext.addImport(ProgramError.class.getName());
    genSrcContext.appendImports(out);

    final var className = genSrcContext.programName() + "Error";
    out.append(String.format("""
        
        public sealed interface %s extends ProgramError permits
        """, className));

    final var tab = genSrcContext.tab();
    final var iterator = errors.iterator();
    for (AnchorErrorRecord error; ; ) {
      error = iterator.next();
      out.append(tab).append(tab).append(className).append('.').append(error.className());
      if (iterator.hasNext()) {
        out.append(",\n");
      } else {
        out.append(" {\n\n");
        break;
      }
    }

    out.append(tab).append(String.format("static %s getInstance(final int errorCode) {\n", className));
    out.append(tab).append(tab).append("return switch (errorCode) {\n");

    for (final var error : errors) {
      out.append(tab).append(tab).append(tab);
      out.append(String.format("case %d -> %s.INSTANCE;\n", error.code(), error.className()));
    }
    out.append(tab).append(tab).append(tab);
    out.append(String.format("""
        default -> throw new IllegalStateException("Unexpected %s error code: " + errorCode);
        """, genSrcContext.programName()));
    out.append(tab).append(tab).append("};\n");
    out.append(tab).append("}\n");
    out.append(errorClassBuilder.toString().indent(genSrcContext.tabLength()));
    out.append("}");
    return removeBlankLines(out.toString());
  }

  public String generateSource(final GenSrcContext genSrcContext) {
    final var pdaAccounts = HashMap.newHashMap(instructions.size() << 1);
    final var ixBuilder = new StringBuilder();
    for (final var ix : instructions) {
      ixBuilder.append('\n').append(ix.generateFactorySource(genSrcContext, "  "));
      for (final var account : ix.accounts()) {
        final var pda = account.pda();
        if (pda != null) {
          pdaAccounts.put(account.name(), pda);
        }
      }
    }

    final var builder = new StringBuilder(4_096);
    genSrcContext.appendPackage(builder);

    genSrcContext.appendImports(builder);

    final var className = genSrcContext.programName() + "Program";
    builder.append(String.format("""
        
        public final class %s {
        """, className));
    builder.append(ixBuilder).append('\n');
    return closeClass(genSrcContext, className, builder);
  }

  private String closeClass(final GenSrcContext genSrcContext,
                            final String className,
                            final StringBuilder builder) {
    builder.append(String.format("""
        private %s() {
        }""", className).indent(genSrcContext.tabLength()));
    return removeBlankLines(builder.append('}').toString());
  }

  private static final class Parser implements FieldBufferPredicate {

    private PublicKey address;
    private String version;
    private String name;
    private List<AnchorConstant> constants;
    private List<AnchorInstruction> instructions;
    private Map<String, AnchorNamedType> accounts;
    private Map<String, AnchorNamedType> types;
    private List<AnchorNamedType> events;
    private List<AnchorErrorRecord> errors;
    private AnchorIdlMetadata metaData;
    private List<String> docs;

    private Parser() {
    }

    private AnchorIDL createIDL(final byte[] json) {
      return new AnchorIDL(
          address,
          version == null ? metaData.version() : version,
          name == null ? metaData.name() : name,
          constants,
          instructions,
          accounts == null ? Map.of() : accounts,
          types == null ? Map.of() : types,
          events == null ? List.of() : events,
          errors == null ? List.of() : errors,
          metaData,
          docs == null ? NO_DOCS : docs,
          json
      );
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("address", buf, offset, len)) {
        this.address = PublicKeyEncoding.parseBase58Encoded(ji);
      } else if (fieldEquals("version", buf, offset, len)) {
        this.version = ji.readString();
      } else if (fieldEquals("name", buf, offset, len)) {
        this.name = ji.readString();
      } else if (fieldEquals("constants", buf, offset, len)) {
        this.constants = parseList(ji, AnchorConstantParser.FACTORY);
      } else if (fieldEquals("instructions", buf, offset, len)) {
        this.instructions = parseList(ji, AnchorInstructionParser.FACTORY);
      } else if (fieldEquals("accounts", buf, offset, len)) {
        this.accounts = parseList(ji, AnchorNamedTypeParser.UPPER_FACTORY).stream()
            .collect(Collectors.toUnmodifiableMap(AnchorNamedType::name, Function.identity()));
      } else if (fieldEquals("types", buf, offset, len)) {
        this.types = parseList(ji, AnchorNamedTypeParser.UPPER_FACTORY).stream()
            .collect(Collectors.toUnmodifiableMap(AnchorNamedType::name, Function.identity()));
      } else if (fieldEquals("events", buf, offset, len)) {
        this.events = parseList(ji, AnchorNamedTypeParser.UPPER_FACTORY).stream().map(nt -> {
          if (nt.type() instanceof AnchorTypeContextList(final List<AnchorNamedType> fields)) {
            return new AnchorNamedType(
                null,
                nt.name(),
                AnchorSerialization.borsh,
                null,
                new AnchorStruct(fields),
                nt.docs(),
                nt.index()
            );
          } else {
            return nt;
          }
        }).toList();
      } else if (fieldEquals("errors", buf, offset, len)) {
        this.errors = parseList(ji, AnchorErrorParser.FACTORY);
      } else if (fieldEquals("metadata", buf, offset, len)) {
        this.metaData = AnchorIdlMetadata.parseMetadata(ji);
      } else if (fieldEquals("docs", buf, offset, len)) {
        final var docs = new ArrayList<String>();
        while (ji.readArray()) {
          docs.add(ji.readString());
        }
        this.docs = docs;
      } else {
        throw new IllegalStateException("Unhandled AnchorIDL field " + new String(buf, offset, len));
      }
      return true;
    }
  }
}
