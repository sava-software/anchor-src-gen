package software.sava.anchor;

import software.sava.core.accounts.PublicKey;
import software.sava.core.accounts.meta.AccountMeta;
import software.sava.core.programs.Discriminator;
import software.sava.core.tx.Instruction;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Locale.ENGLISH;
import static software.sava.anchor.AnchorSourceGenerator.removeBlankLines;

public record AnchorInstruction(Discriminator discriminator,
                                String name,
                                List<AnchorAccountMeta> accounts,
                                List<NamedType> args) {

  static String replaceNewLinesIfLessThan(final String lines, final int numLines, final int limit) {
    return numLines < limit && !lines.contains("//") ? lines.replaceAll("\n +", " ") : lines;
  }

  static String replaceNewLinesIfLessThan(final StringBuilder lines, final int numLines, final int limit) {
    return replaceNewLinesIfLessThan(lines.toString(), numLines, limit);
  }

  static String replaceNewLinesIfLessThan(final String lines, final int limit) {
    int numNewLines = 0;
    for (int from = 0, to = lines.length(); from < to; ++from) {
      if (lines.indexOf('\n', from) > 0) {
        ++numNewLines;
      }
    }
    return replaceNewLinesIfLessThan(lines, numNewLines, limit);
  }

  static String replaceNewLinesIfLessThan(final StringBuilder lines, final int limit) {
    return replaceNewLinesIfLessThan(lines.toString(), limit);
  }

  private static String formatKeyName(final AnchorAccountMeta accountMeta) {
    final var name = accountMeta.name();
    return name.endsWith("Key") || name.endsWith("key") ? name : name + "Key";
  }

  private static String formatDiscriminatorReference(final String ixName) {
    return String.format("%s_DISCRIMINATOR", AnchorUtil.snakeCase(ixName).toUpperCase(ENGLISH));
  }

  private static String formatDiscriminator(final String ixName, final Discriminator discriminator) {
    return Arrays.stream(discriminator.toIntArray())
        .mapToObj(Integer::toString)
        .collect(Collectors.joining(", ",
            String.format("  public static final Discriminator %s = toDiscriminator(", formatDiscriminatorReference(ixName)), ");"));
  }

  public String generateFactorySource(final GenSrcContext genSrcContext, final String parentTab) {
    final var tab = genSrcContext.tab();
    final var builder = new StringBuilder(2_048);

    genSrcContext.addImport(Discriminator.class);
    genSrcContext.addStaticImport(Discriminator.class, "toDiscriminator");
    builder.append(formatDiscriminator(name, discriminator == null ? AnchorUtil.toDiscriminator(name) : discriminator));
    builder.append("\n\n");

    final var keyParamsBuilder = new StringBuilder(1_024);
    final var programMetaReference = String.format("invoked%sProgramMeta", genSrcContext.programName());
    keyParamsBuilder.append("final AccountMeta ").append(programMetaReference).append(",\n");
    genSrcContext.addImport(AccountMeta.class);

    final var stringsBuilder = new StringBuilder(1_024);
    final var createKeysBuilder = new StringBuilder(1_024);

    final var dataTab = parentTab + " ".repeat(genSrcContext.tabLength());
    final var keyTab = dataTab + tab;
    createKeysBuilder.append(dataTab).append("final var keys = ");
    if (accounts.isEmpty()) {
      createKeysBuilder.append("AccountMeta.NO_KEYS;\n\n");
    } else {
      final var knownAccounts = genSrcContext.accountMethods();
      final var knownAccountClasses = accounts.stream()
          .map(AnchorAccountMeta::address)
          .map(knownAccounts::get)
          .filter(Objects::nonNull)
          .map(AccountReferenceCall::clas)
          .collect(Collectors.toSet());
      if (!knownAccountClasses.isEmpty()) {
        for (final var accountsClas : knownAccountClasses) {
          keyParamsBuilder.append(String.format("""
              final %s %s,
              """, accountsClas.getSimpleName(), AnchorUtil.camelCase(accountsClas.getSimpleName(), false)));
          genSrcContext.addImport(accountsClas);
        }
      }

      accounts.stream()
          .filter(account -> !knownAccounts.containsKey(account.address()))
          .peek(context -> keyParamsBuilder.append(context.docComments()))
          .map(AnchorInstruction::formatKeyName)
          .forEach(name -> keyParamsBuilder.append("final PublicKey ").append(name).append(",\n"));

      genSrcContext.addImport(PublicKey.class);
      genSrcContext.addImport(List.class);
      createKeysBuilder.append("List.of(");
      final var accountsIterator = accounts.iterator();
      for (AnchorAccountMeta accountMeta; ; ) {
        accountMeta = accountsIterator.next();
        final var knowAccount = knownAccounts.get(accountMeta.address());
        String varName;
        if (knowAccount != null) {
          varName = knowAccount.callReference();
        } else {
          final var accountMetaName = accountMeta.name();
          varName = accountMetaName.endsWith("Key") || accountMetaName.endsWith("key")
              ? accountMetaName
              : accountMetaName + "Key";
          if (accountMeta.optional()) {
            genSrcContext.addStaticImport(Objects.class, "requireNonNullElse");
            varName = String.format(
                "requireNonNullElse(%s, %s.publicKey())",
                varName, programMetaReference
            );
          }
        }
        final String append;
        if (accountMeta.signer()) {
          if (accountMeta.writable()) {
            append = String.format("createWritableSigner(%s)", varName);
            genSrcContext.addStaticImport(AccountMeta.class, "createWritableSigner");
          } else {
            append = String.format("createReadOnlySigner(%s)", varName);
            genSrcContext.addStaticImport(AccountMeta.class, "createReadOnlySigner");
          }
        } else if (accountMeta.writable()) {
          append = String.format("createWrite(%s)", varName);
          genSrcContext.addStaticImport(AccountMeta.class, "createWrite");
        } else {
          append = String.format("createRead(%s)", varName);
          genSrcContext.addStaticImport(AccountMeta.class, "createRead");
        }
        createKeysBuilder.append("\n").append(keyTab).append(append);

        if (accountsIterator.hasNext()) {
          createKeysBuilder.append(',');
        } else {
          createKeysBuilder.append('\n').append(dataTab).append(");\n\n");
          break;
        }
      }
    }
    final var paramsBuilder = new StringBuilder(keyParamsBuilder.length() << 1);
    paramsBuilder.append(keyParamsBuilder);
    final int numArgs = args.size();
    final String dataSerialization;
    final String dataLengthAdds;
    int dataLength = 0;
    if (numArgs == 0) {
      dataSerialization = null;
      dataLengthAdds = null;
    } else {
      final var dataLengthBuilder = new StringBuilder(512);
      final var dataBuilder = new StringBuilder(1_204);
      for (final var argsIterator = args.iterator(); ; ) {
        final var arg = argsIterator.next();
        final boolean hasNext = argsIterator.hasNext();
        dataLength += arg.generateSerialization(genSrcContext, paramsBuilder, dataBuilder, stringsBuilder, dataLengthBuilder, hasNext);
        dataBuilder.append('\n');
        if (!hasNext) {
          break;
        }
      }
      dataLengthAdds = dataLengthBuilder.toString();
      dataSerialization = dataBuilder.toString();
    }

    final var methodSignature = String.format("%spublic static Instruction %s(", tab, name);
    builder.append(methodSignature);

    // Parameters
    paramsBuilder.setLength(paramsBuilder.length() - 2);
    paramsBuilder.append(") {\n");
    final var paramTab = " ".repeat(methodSignature.length());
    final var params = paramsBuilder.toString().indent(paramTab.length()).stripLeading();
    builder.append(replaceNewLinesIfLessThan(params, numArgs + accounts.size(), 3));

    // Keys
    builder.append(createKeysBuilder);

    // String -> byte[]
    builder.append(stringsBuilder.toString().indent(dataTab.length()));

    final var discriminatorReference = formatDiscriminatorReference(name);
    // Data, create and Instruction.
    if (numArgs > 0) {
      genSrcContext.addStaticImport(AnchorUtil.class, "writeDiscriminator");
      dataLength += AnchorUtil.DISCRIMINATOR_LENGTH;
      builder.append(String.format("""
              final byte[] _data = new byte[%s];
              int i = writeDiscriminator(%s, _data, 0);
              """,
          dataLengthAdds.contains("\n")
              ? String.format("\n%s%s%d%s\n", tab, tab, dataLength, dataLengthAdds)
              : String.format("%d%s", dataLength, dataLengthAdds),
          discriminatorReference
      ).indent(dataTab.length()));
      builder.append(dataSerialization.indent(dataTab.length()));
      builder.append(String.format("""
              
                return Instruction.createInstruction(%s, keys, _data);
              }
              """,
          programMetaReference).indent(parentTab.length()));
    } else {
      builder.append(String.format("""          
                return Instruction.createInstruction(%s, keys, %s);
              }
              """,
          programMetaReference, discriminatorReference).indent(parentTab.length()));
    }
    genSrcContext.addImport(Instruction.class);

    if (!args.isEmpty()) {
      final var definedTypes = genSrcContext.definedTypes();
      final var ixCamelName = AnchorUtil.camelCase(name, true);
      var typeName = ixCamelName + "IxData";
      if (definedTypes.containsKey(typeName)) {
        typeName = ixCamelName + "IxRecord";
        for (int i = 2; definedTypes.containsKey(typeName); ++i) {
          typeName = ixCamelName + "IxData" + i;
        }
      }
      final var struct = new AnchorStruct(args);
      final var namedType = new AnchorNamedType(
          discriminator,
          typeName,
          null,
          null,
          struct,
          List.of(),
          false
      );
      final var sourceCode = struct.generateSource(genSrcContext, namedType);
      final var injectKey = "implements Borsh {";
      int offset = sourceCode.indexOf(injectKey) + injectKey.length();
      final var header = sourceCode.substring(0, offset);
      final var readHelper = String.format("""
              
              
              public static %s read(final Instruction instruction) {
              %sreturn read(instruction.data(), instruction.offset());
              }""",
          typeName, tab
      ).indent(genSrcContext.tabLength());
      final var withHelper = header + readHelper + sourceCode.substring(offset + 1);
      builder.append(withHelper.indent(genSrcContext.tabLength()));
    }

    return removeBlankLines(builder.toString());
  }
}
