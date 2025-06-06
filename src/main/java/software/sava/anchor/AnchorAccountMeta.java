package software.sava.anchor;

import software.sava.core.accounts.PublicKey;

import java.util.List;

public record AnchorAccountMeta(List<AnchorAccountMeta> nestedAccounts,
                                PublicKey address,
                                String name,
                                boolean writable,
                                boolean signer,
                                String description,
                                List<String> docs,
                                boolean optional,
                                AnchorPDA pda,
                                List<String> relations) {

  public String docComments() {
    return NamedType.formatComments(this.docs);
  }
}
