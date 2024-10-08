package software.sava.anchor;

import software.sava.core.programs.Discriminator;
import systems.comodal.jsoniter.JsonIterator;
import systems.comodal.jsoniter.factory.ElementFactory;

import java.util.List;
import java.util.function.Supplier;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

final class AnchorInstructionParser implements ElementFactory<AnchorInstruction> {

  static final Supplier<AnchorInstructionParser> FACTORY = AnchorInstructionParser::new;

  private Discriminator discriminator;
  private String name;
  private List<AnchorAccountMeta> accounts;
  private List<AnchorNamedType> args;

  AnchorInstructionParser() {
  }

  @Override
  public AnchorInstruction create() {
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
      this.args = ElementFactory.parseList(ji, AnchorNamedTypeParser.LOWER_FACTORY);
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
