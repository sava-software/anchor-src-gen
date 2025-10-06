package software.sava.idl.generator.anchor;

import software.sava.core.accounts.PublicKey;
import software.sava.idl.generator.src.SrcUtil;

import java.util.List;

public record AnchorAccountMeta(List<AnchorAccountMeta> nestedAccounts,
                                PublicKey address,
                                String actualName,
                                String name,
                                boolean writable,
                                boolean signer,
                                boolean isOptionalSigner,
                                String description,
                                List<String> docs,
                                boolean optional,
                                AnchorPDA pda,
                                List<String> relations) {

  public String docComments() {
    return SrcUtil.formatComments(this.docs);
  }
}
