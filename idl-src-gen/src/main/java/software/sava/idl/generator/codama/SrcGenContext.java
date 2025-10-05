package software.sava.idl.generator.codama;

record SrcGenContext(String tab,
                     ProgramNode program) {

  int tabLength() {
    return tab.length();
  }
}
