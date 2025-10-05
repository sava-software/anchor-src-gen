package software.sava.idl.generator.anchor;

public sealed interface ProgramSeed permits AnchorPDA.AccountSeed, AnchorPDA.ConstSeed {

  String varName();
}
