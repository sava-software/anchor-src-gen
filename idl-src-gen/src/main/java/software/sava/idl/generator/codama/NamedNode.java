package software.sava.idl.generator.codama;

import software.sava.idl.generator.anchor.IDL;

import java.util.List;

abstract sealed class NamedNode implements TypeNode permits AccountBumpValueNode,
    AccountValueNode,
    ArgumentValueNode,
    FieldDiscriminatorNode,
    NamedDocsNode,
    OrdinalNode,
    PdaSeedValueNode,
    ValueNode.Struct.Field {

  protected final String name;

  protected NamedNode(final String name) {
    this.name = name;
  }

  final String name() {
    return name;
  }

  List<String> docs() {
    return IDL.NO_DOCS;
  }

  String docComments() {
    return "";
  }

  void appendDocs(final StringBuilder src) {

  }
}
