package software.sava.idl.generator.anchor;

import software.sava.anchor.AnchorUtil;
import software.sava.core.accounts.PublicKey;
import software.sava.core.accounts.meta.AccountMeta;
import software.sava.core.programs.Discriminator;
import software.sava.core.tx.Instruction;
import software.sava.idl.generator.src.SrcUtil;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static software.sava.idl.generator.ParseUtil.removeBlankLines;

public record AnchorInstruction(Discriminator discriminator,
                                String name,
                                List<String> docs,
                                List<AnchorAccountMeta> accounts,
                                List<NamedType> args) {

  private static String formatKeyName(final AnchorAccountMeta accountMeta) {
    return SrcUtil.formatKeyName(accountMeta.name());
  }

  public String generateFactorySource(final SrcGenContext srcGenContext, final String parentTab) {
    final var tab = srcGenContext.tab();
    final var builder = new StringBuilder(2_048);

    srcGenContext.addImport(Discriminator.class);
    srcGenContext.addStaticImport(Discriminator.class, "toDiscriminator");
    builder.append(SrcUtil.formatDiscriminator(name, discriminator == null ? AnchorUtil.toDiscriminator(name) : discriminator));
    builder.append("\n\n");

    final var keyParamsBuilder = new StringBuilder(1_024);
    final var programMetaReference = String.format("invoked%sProgramMeta", srcGenContext.programName());
    keyParamsBuilder.append("final AccountMeta ").append(programMetaReference).append(",\n");
    srcGenContext.addImport(AccountMeta.class);

    final var stringsBuilder = new StringBuilder(1_024);
    final var createKeysBuilder = new StringBuilder(1_024);

    final var dataTab = parentTab + " ".repeat(srcGenContext.tabLength());
    final var keyTab = dataTab + tab;
    createKeysBuilder.append(dataTab).append("final var keys = ");
    if (accounts.isEmpty()) {
      createKeysBuilder.append("AccountMeta.NO_KEYS;\n\n");
    } else {
      final var knownAccounts = srcGenContext.accountMethods();
      final var knownAccountClasses = accounts.stream()
          .<Class<?>>mapMulti((accountMeta, downstream) -> {
            final var knownAccount = knownAccounts.get(accountMeta.address());
            if (knownAccount != null) {
              downstream.accept(knownAccount.clas());
            }
          }).collect(Collectors.toSet());
      if (!knownAccountClasses.isEmpty()) {
        for (final var accountsClas : knownAccountClasses) {
          keyParamsBuilder.append(String.format("""
              final %s %s,
              """, accountsClas.getSimpleName(), AnchorUtil.camelCase(accountsClas.getSimpleName(), false)
          ));
          srcGenContext.addImport(accountsClas);
        }
      }

      for (final var accountMeta : accounts) {
        if (!knownAccounts.containsKey(accountMeta.address())) {
          keyParamsBuilder.append(accountMeta.docComments());
          final var formattedName = AnchorInstruction.formatKeyName(accountMeta);
          keyParamsBuilder.append("final PublicKey ").append(formattedName).append(",\n");
        }
      }

      srcGenContext.addImport(PublicKey.class);
      srcGenContext.addImport(List.class);
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
            srcGenContext.addStaticImport(Objects.class, "requireNonNullElse");
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
            srcGenContext.addStaticImport(AccountMeta.class, "createWritableSigner");
          } else {
            append = String.format("createReadOnlySigner(%s)", varName);
            srcGenContext.addStaticImport(AccountMeta.class, "createReadOnlySigner");
          }
        } else if (accountMeta.writable()) {
          append = String.format("createWrite(%s)", varName);
          srcGenContext.addStaticImport(AccountMeta.class, "createWrite");
        } else {
          append = String.format("createRead(%s)", varName);
          srcGenContext.addStaticImport(AccountMeta.class, "createRead");
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
        dataLength += arg.generateSerialization(srcGenContext, paramsBuilder, dataBuilder, stringsBuilder, dataLengthBuilder, hasNext);
        dataBuilder.append('\n');
        if (!hasNext) {
          break;
        }
      }
      dataLengthAdds = dataLengthBuilder.toString();
      dataSerialization = dataBuilder.toString();
    }

    if (docs != null && !docs.isEmpty()) {
      for (final var doc : docs) {
        builder.append(tab).append("// ").append(doc).append('\n');
      }
    }
    final var methodSignature = String.format("%spublic static Instruction %s(", tab, name);
    builder.append(methodSignature);

    // Parameters
    paramsBuilder.setLength(paramsBuilder.length() - 2);
    paramsBuilder.append(") {\n");
    final var paramTab = " ".repeat(methodSignature.length());
    final var params = paramsBuilder.toString().indent(paramTab.length()).stripLeading();
    builder.append(SrcUtil.replaceNewLinesIfLessThan(params, numArgs + accounts.size(), 3));

    // Keys
    builder.append(createKeysBuilder);

    // String -> byte[]
    builder.append(stringsBuilder.toString().indent(dataTab.length()));

    final var discriminatorReference = SrcUtil.formatDiscriminatorReference(name);
    // Data, create and Instruction.
    if (numArgs > 0) {
      dataLength += AnchorUtil.DISCRIMINATOR_LENGTH;
      builder.append(String.format("""
              final byte[] _data = new byte[%s];
              int i = %s.write(_data, 0);
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
          programMetaReference
      ).indent(parentTab.length()));
    } else {
      builder.append(String.format("""          
                return Instruction.createInstruction(%s, keys, %s);
              }
              """,
          programMetaReference, discriminatorReference
      ).indent(parentTab.length()));
    }
    srcGenContext.addImport(Instruction.class);

    if (!args.isEmpty()) {
      final var ixCamelName = AnchorUtil.camelCase(name, true);
      var typeName = ixCamelName + "IxData";
      if (srcGenContext.isDefinedType(typeName)) {
        typeName = ixCamelName + "IxRecord";
        for (int i = 2; srcGenContext.isDefinedType(typeName); ++i) {
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
          "",
          false
      );
      final var sourceCode = struct.generateSource(srcGenContext, namedType);
      final var injectKey = "implements Borsh {";
      int offset = sourceCode.indexOf(injectKey) + injectKey.length();
      final var header = sourceCode.substring(0, offset);
      final var readHelper = String.format("""
              
              
              public static %s read(final Instruction instruction) {
              %sreturn read(instruction.data(), instruction.offset());
              }""",
          typeName, tab
      ).indent(srcGenContext.tabLength());
      final var withHelper = header + readHelper + sourceCode.substring(offset + 1);
      builder.append(withHelper.indent(srcGenContext.tabLength()));
    }

    return removeBlankLines(builder.toString());
  }
}
