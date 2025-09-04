package software.sava.idl.generator;

import software.sava.anchor.ProgramError;
import software.sava.core.accounts.ProgramDerivedAddress;
import software.sava.core.accounts.PublicKey;
import software.sava.core.tx.Transaction;
import software.sava.rpc.json.PublicKeyEncoding;
import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static software.sava.idl.generator.AnchorSourceGenerator.removeBlankLines;
import static software.sava.idl.generator.ParseUtil.parseDocs;
import static systems.comodal.jsoniter.JsonIterator.fieldEquals;
import static systems.comodal.jsoniter.factory.ElementFactory.parseList;

// https://github.com/acheroncrypto/anchor/blob/fix-idl/lang/syn/src/idl/types.rs
// https://github.com/coral-xyz/anchor/blob/master/ts/packages/anchor/src/idl.ts
public final class AnchorIDL extends RootIDL implements IDL {

  private final List<AnchorConstant> constants;
  private final List<AnchorInstruction> instructions;
  private final Map<String, NamedType> accounts;
  private final Map<String, NamedType> types;
  private final List<NamedType> events;
  private final List<AnchorErrorRecord> errors;
  private final AnchorIdlMetadata metaData;

  AnchorIDL(final PublicKey address,
            final String version,
            final String name,
            final String origin,
            final List<AnchorConstant> constants,
            final List<AnchorInstruction> instructions,
            final Map<String, NamedType> accounts,
            final Map<String, NamedType> types,
            final List<NamedType> events,
            final List<AnchorErrorRecord> errors,
            final AnchorIdlMetadata metaData,
            final List<String> docs,
            final byte[] json) {
    super(address, version, name, origin, docs, json);
    this.constants = constants;
    this.instructions = instructions;
    this.accounts = accounts;
    this.types = types;
    this.events = events;
    this.errors = errors;
    this.metaData = metaData;
  }

  public List<AnchorConstant> constants() {
    return constants;
  }

  public List<AnchorInstruction> instructions() {
    return instructions;
  }

  public Map<String, NamedType> accounts() {
    return accounts;
  }

  public Map<String, NamedType> types() {
    return types;
  }

  public List<NamedType> events() {
    return events;
  }

  public List<AnchorErrorRecord> errors() {
    return errors;
  }

  public AnchorIdlMetadata metaData() {
    return metaData;
  }

  @Override
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
        
        """, className
    ));
    out.append(constantsBuilder);

    return closeClass(genSrcContext, className, out);
  }

  @Override
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
        
        """, className
    ));
    out.append(pdaBuilder);
    return closeClass(genSrcContext, className, out);
  }

  @Override
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
        """, className
    ));

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
        """, genSrcContext.programName()
    ));
    out.append(tab).append(tab).append("};\n");
    out.append(tab).append("}\n");
    out.append(errorClassBuilder.toString().indent(genSrcContext.tabLength()));
    out.append("}");
    return removeBlankLines(out.toString());
  }

  @Override
  public String generateSource(final GenSrcContext genSrcContext) {
    final var pdaAccounts = HashMap.newHashMap(instructions.size() << 1);
    final var ixBuilder = new StringBuilder();
    for (final var ix : instructions) {
      for (final var account : ix.accounts()) {
        final var pda = account.pda();
        if (pda != null) {
          pdaAccounts.put(account.name(), pda);
        }
      }
      if (ix.accounts().size() <= Transaction.MAX_ACCOUNTS) {
        ixBuilder.append('\n').append(ix.generateFactorySource(genSrcContext, "  "));
      }
    }

    final var builder = new StringBuilder(4_096);
    genSrcContext.appendPackage(builder);

    genSrcContext.appendImports(builder);

    final var className = genSrcContext.programName() + "Program";
    builder.append(String.format("""
        
        public final class %s {
        """, className
    ));
    builder.append(ixBuilder).append('\n');
    return closeClass(genSrcContext, className, builder);
  }


  static final class Parser implements FieldBufferPredicate {

    private final IDLType idlType;
    private PublicKey address;
    private String version;
    private String name;
    private String origin;
    private List<AnchorConstant> constants;
    private List<AnchorInstruction> instructions;
    private Map<String, NamedType> accounts;
    private Map<String, NamedType> types;
    private List<NamedType> events;
    private List<AnchorErrorRecord> errors;
    private AnchorIdlMetadata metaData;
    private List<String> docs;

    Parser(final IDLType idlType) {
      this.idlType = idlType;
    }

    AnchorIDL createIDL(final byte[] json) {
      return new AnchorIDL(
          address,
          version == null ? metaData.version() : version,
          name == null ? metaData.name() : name,
          origin == null ? metaData == null ? null : metaData.origin() : origin,
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
      } else if (fieldEquals("origin", buf, offset, len)) {
        this.origin = ji.readString();
      } else if (fieldEquals("constants", buf, offset, len)) {
        this.constants = parseList(ji, AnchorConstantParser.FACTORY);
      } else if (fieldEquals("instructions", buf, offset, len)) {
        this.instructions = parseList(ji, idlType.instructionParserFactory());
      } else if (fieldEquals("accounts", buf, offset, len)) {
        this.accounts = parseList(ji, idlType.upperTypeParserFactory()).stream()
            .collect(Collectors.toUnmodifiableMap(NamedType::name, Function.identity()));
      } else if (fieldEquals("types", buf, offset, len)) {
        this.types = parseList(ji, idlType.upperTypeParserFactory()).stream()
            .collect(Collectors.toUnmodifiableMap(NamedType::name, Function.identity()));
      } else if (fieldEquals("events", buf, offset, len)) {
        this.events = parseList(ji, idlType.upperTypeParserFactory()).stream().map(nt -> {
          if (nt.type() instanceof AnchorTypeContextList(final List<NamedType> fields)) {
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
        if (!errors.isEmpty()) {
          final var deduplicatedNames = new ArrayList<AnchorErrorRecord>(errors.size());
          final var duplicates = new TreeMap<String, Integer>(String.CASE_INSENSITIVE_ORDER);
          int numDuplicates = 0;
          for (final var error : errors) {
            final int numInstances = duplicates.compute(error.className(), (name, count) -> count == null ? 1 : count + 1);
            if (numInstances > 1) {
              deduplicatedNames.add(new AnchorErrorRecord(
                  error.code(),
                  error.name(),
                  error.msg(),
                  error.className() + numInstances
              ));
              ++numDuplicates;
            } else {
              deduplicatedNames.add(error);
            }
          }
          if (numDuplicates > 0) {
            this.errors = deduplicatedNames;
          }
        }
      } else if (fieldEquals("metadata", buf, offset, len)) {
        this.metaData = AnchorIdlMetadata.parseMetadata(ji);
      } else if (fieldEquals("docs", buf, offset, len)) {
        this.docs = parseDocs(ji);
      } else {
        throw new IllegalStateException("Unhandled AnchorIDL field " + new String(buf, offset, len));
      }
      return true;
    }
  }
}
