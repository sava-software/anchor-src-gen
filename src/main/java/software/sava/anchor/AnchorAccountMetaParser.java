package software.sava.anchor;

import software.sava.core.accounts.PublicKey;
import software.sava.rpc.json.PublicKeyEncoding;
import systems.comodal.jsoniter.JsonIterator;
import systems.comodal.jsoniter.factory.ElementFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static software.sava.anchor.AnchorNamedType.NO_DOCS;
import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

final class AnchorAccountMetaParser implements ElementFactory<AnchorAccountMeta> {

  static final Supplier<AnchorAccountMetaParser> FACTORY = AnchorAccountMetaParser::new;

  private PublicKey address;
  private String name;
  private boolean writable;
  private boolean optional;
  private boolean signer;
  private String desc;
  private List<String> docs;
  private AnchorPDA pda;
  private List<String> relations;
  private List<AnchorAccountMeta> accounts;

  AnchorAccountMetaParser() {
  }

  @Override
  public AnchorAccountMeta create() {
    return new AnchorAccountMeta(
        accounts,
        address,
        name,
        writable,
        signer,
        desc,
        docs == null ? desc == null || desc.isBlank() ? NO_DOCS : List.of(desc) : docs,
        optional,
        pda,
        relations
    );
  }

  @Override
  public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
    if (fieldEquals("accounts", buf, offset, len)) {
      final var accounts = new ArrayList<AnchorAccountMeta>();
      for (AnchorAccountMetaParser parser; ji.readArray(); ) {
        parser = new AnchorAccountMetaParser();
        ji.testObject(parser);
        accounts.add(parser.create());
      }
      this.accounts = accounts;
    } else if (fieldEquals("address", buf, offset, len)) {
      this.address = PublicKeyEncoding.parseBase58Encoded(ji);
    } else if (fieldEquals("desc", buf, offset, len)) {
      this.desc = ji.readString();
    } else if (fieldEquals("docs", buf, offset, len)) {
      final var docs = new ArrayList<String>();
      while (ji.readArray()) {
        docs.add(ji.readString());
      }
      this.docs = docs;
    } else if (fieldEquals("isMut", buf, offset, len) || fieldEquals("writable", buf, offset, len)) {
      this.writable = ji.readBoolean();
    } else if (fieldEquals("optional", buf, offset, len) || fieldEquals("isOptional", buf, offset, len)) {
      this.optional = ji.readBoolean();
    } else if (fieldEquals("isSigner", buf, offset, len) || fieldEquals("signer", buf, offset, len)) {
      this.signer = ji.readBoolean();
    } else if (fieldEquals("name", buf, offset, len)) {
      this.name = AnchorUtil.camelCase(ji.readString(), false);
    } else if (fieldEquals("pda", buf, offset, len)) {
      this.pda = AnchorPDA.parsePDA(ji);
    } else if (fieldEquals("relations", buf, offset, len)) {
      final var relations = new ArrayList<String>();
      while (ji.readArray()) {
        relations.add(ji.readString());
      }
      this.relations = relations;
    } else {
      throw new IllegalStateException("Unhandled AnchorAccountMeta field " + new String(buf, offset, len));
    }
    return true;
  }
}
