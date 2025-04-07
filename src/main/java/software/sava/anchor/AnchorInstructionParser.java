package software.sava.anchor;

import software.sava.core.programs.Discriminator;
import systems.comodal.jsoniter.JsonIterator;
import systems.comodal.jsoniter.factory.ElementFactory;

import java.util.ArrayList;
import java.util.List;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

final class AnchorInstructionParser implements ElementFactory<AnchorInstruction> {

  private final IDLType idlType;
  private Discriminator discriminator;
  private String name;
  private List<AnchorAccountMeta> accounts;
  private List<AnchorNamedType> args;

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
            accumulateNestedAccounts(),
            args
        );
      }
    }
    return new AnchorInstruction(
        discriminator,
        name,
        accounts,
        args
    );
  }

  @Override
  public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
    if (fieldEquals("accounts", buf, offset, len)) {
      this.accounts = ElementFactory.parseList(ji, AnchorAccountMetaParser.FACTORY);
    } else if (fieldEquals("args", buf, offset, len)) {
      this.args = ElementFactory.parseList(ji, idlType.lowerFactory());
    } else if (fieldEquals("discriminator", buf, offset, len)) {
      this.discriminator = AnchorUtil.parseDiscriminator(ji);
    } else if (fieldEquals("name", buf, offset, len)) {
      this.name = AnchorUtil.camelCase(ji.readString(), false);
    } else {
      ji.skip();
    }
    return true;
  }
}
