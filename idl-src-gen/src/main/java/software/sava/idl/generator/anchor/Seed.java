package software.sava.idl.generator.anchor;

import java.util.Set;

public sealed interface Seed permits AnchorPDA.AccountSeed, AnchorPDA.ArgSeed, AnchorPDA.ConstSeed {

  int index();

  AnchorType type();

  String varName(final SrcGenContext srcGenContext);

  String fieldName(final SrcGenContext srcGenContext, final Set<String> deDuplicateKnown);
}
