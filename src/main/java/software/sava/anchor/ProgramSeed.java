package software.sava.anchor;

public sealed interface ProgramSeed permits AnchorPDA.AccountSeed, AnchorPDA.ConstSeed {

  String varName();
}
