package software.sava.idl.generator.codama;

import software.sava.idl.generator.src.SrcUtil;

import java.util.List;

abstract sealed class NamedDocsNode extends NamedNode permits DefinedTypeNode,
    ErrorNode,
    InstructionAccountNode,
    InstructionArgumentNode,
    InstructionNode,
    PdaNode,
    ProgramNode,
    ResolverValueNode,
    VariablePdaSeedNode {

  protected final List<String> docs;
  protected final String docComments;

  protected NamedDocsNode(final String name, final List<String> docs) {
    super(name);
    this.docs = docs;
    this.docComments = SrcUtil.formatComments(docs);
  }

  final List<String> docs() {
    return docs;
  }

  final String docComments() {
    return docComments;
  }

  @Override
  final void appendDocs(final StringBuilder src) {
    src.append(docComments);
  }
}
