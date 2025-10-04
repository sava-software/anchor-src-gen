package software.sava.idl.generator.codama;

abstract class BaseLinkNode {

  protected final String name;

  protected BaseLinkNode(final String name) {
    this.name = name;
  }

  public final String name() {
    return name;
  }
}
