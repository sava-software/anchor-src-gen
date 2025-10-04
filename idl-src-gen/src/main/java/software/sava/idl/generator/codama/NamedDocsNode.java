package software.sava.idl.generator.codama;

import java.util.List;

abstract class NamedDocsNode extends NamedNode {

  protected final List<String> docs;

  protected NamedDocsNode(final String name, final List<String> docs) {
    super(name);
    this.docs = docs;
  }

  final List<String> docs() {
    return docs;
  }
}
