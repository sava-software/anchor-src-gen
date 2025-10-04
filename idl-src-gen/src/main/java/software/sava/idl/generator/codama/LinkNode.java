package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

sealed interface LinkNode permits
    AccountLinkNode,
    DefinedTypeLinkNode,
    InstructionAccountLinkNode,
    InstructionArgumentLinkNode,
    InstructionLinkNode,
    PdaLinkNode,
    ProgramLinkNode {

  static LinkNode parse(final JsonIterator ji) {
    final var kind = ji.skipObjField().readString();
    return switch (kind) {
      case "accountLinkNode" -> AccountLinkNode.parse(ji);
      case "definedTypeLinkNode" -> DefinedTypeLinkNode.parse(ji);
      case "instructionAccountLinkNode" -> InstructionAccountLinkNode.parse(ji);
      case "instructionArgumentLinkNode" -> InstructionArgumentLinkNode.parse(ji);
      case "instructionLinkNode" -> InstructionLinkNode.parse(ji);
      case "pdaLinkNode" -> PdaLinkNode.parse(ji);
      case "programLinkNode" -> ProgramLinkNode.parse(ji);
      default -> throw new UnsupportedOperationException(kind);
    };
  }

  public String name();
}
