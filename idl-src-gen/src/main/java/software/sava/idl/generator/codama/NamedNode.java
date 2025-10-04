package software.sava.idl.generator.codama;

abstract class NamedNode {

  protected final String name;

  protected NamedNode(final String name) {
    this.name = name;
  }

  final String name() {
    return name;
  }
}
