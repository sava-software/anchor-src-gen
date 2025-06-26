package software.sava.anchor;

import java.util.Set;

public sealed interface Seed permits AnchorPDA.AccountSeed, AnchorPDA.ArgSeed, AnchorPDA.ConstSeed {

  int index();

  AnchorType type();

  String varName(final GenSrcContext genSrcContext);

  String fieldName(final GenSrcContext genSrcContext, final Set<String> deDuplicateKnown);
}
