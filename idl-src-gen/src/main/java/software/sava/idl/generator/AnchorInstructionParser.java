package software.sava.idl.generator;

import software.sava.anchor.AnchorUtil;
import software.sava.core.programs.Discriminator;
import systems.comodal.jsoniter.JsonIterator;
import systems.comodal.jsoniter.factory.ElementFactory;

import java.util.ArrayList;
import java.util.List;

import static software.sava.idl.generator.ParseUtil.parseDocs;
import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

final class AnchorInstructionParser implements ElementFactory<AnchorInstruction> {

  private final IDLType idlType;
  private Discriminator discriminator;
  private String name;
  private List<String> docs;
  private List<AnchorAccountMeta> accounts;
  private List<NamedType> args;

  AnchorInstructionParser(final IDLType idlType) {
    this.idlType = idlType;
  }

  private static void accumulateNestedAccounts(final List<AnchorAccountMeta> parentAccounts,
                                               final List<AnchorAccountMeta> accounts) {
    for (final var account : parentAccounts) {
      final var nestedAccounts = account.nestedAccounts();
      if (nestedAccounts.isEmpty()) {
        accounts.add(account);
      } else {
        accumulateNestedAccounts(nestedAccounts, accounts);
      }
    }
  }

  private ArrayList<AnchorAccountMeta> accumulateNestedAccounts() {
    final var accounts = new ArrayList<AnchorAccountMeta>();
    for (final var account : this.accounts) {
      final var nestedAccounts = account.nestedAccounts();
      if (nestedAccounts.isEmpty()) {
        accounts.add(account);
      } else {
        accumulateNestedAccounts(nestedAccounts, accounts);
      }
    }
    return accounts;
  }

  @Override
  public AnchorInstruction create() {
    for (final var account : accounts) {
      final var nestedAccounts = account.nestedAccounts();
      if (!nestedAccounts.isEmpty()) {
        return new AnchorInstruction(
            discriminator,
            name,
            docs,
            accumulateNestedAccounts(),
            args
        );
      }
    }
    return new AnchorInstruction(
        discriminator,
        name,
        docs,
        accounts,
        args
    );
  }

  @Override
  public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
    if (fieldEquals("accounts", buf, offset, len)) {
      this.accounts = ElementFactory.parseList(ji, AnchorAccountMetaParser.FACTORY);
    } else if (fieldEquals("args", buf, offset, len)) {
      this.args = ElementFactory.parseList(ji, idlType.lowerTypeParserFactory());
    } else if (fieldEquals("discriminator", buf, offset, len)) {
      this.discriminator = AnchorUtil.parseDiscriminator(ji);
    } else if (fieldEquals("name", buf, offset, len)) {
      this.name = AnchorUtil.camelCase(ji.readString(), false);
    } else if (fieldEquals("docs", buf, offset, len)) {
      this.docs = parseDocs(ji);
    } else {
      ji.skip();
    }
    return true;
  }
}
