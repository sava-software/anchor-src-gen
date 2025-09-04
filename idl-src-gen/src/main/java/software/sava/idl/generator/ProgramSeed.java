package software.sava.idl.generator;

public sealed interface ProgramSeed permits AnchorPDA.AccountSeed, AnchorPDA.ConstSeed {

  String varName();
}
