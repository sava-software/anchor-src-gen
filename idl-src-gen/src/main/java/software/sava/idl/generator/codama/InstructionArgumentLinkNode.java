package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

final class InstructionArgumentLinkNode extends BaseIxLinkNode implements LinkNode {

  InstructionArgumentLinkNode(final String name, final InstructionLinkNode instruction) {
    super(name, instruction);
  }

  public static InstructionArgumentLinkNode parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createInstructionArgumentLinkNode();
  }

  static final class Parser extends BaseIxLinkNode.Parser {

    private Parser() {
    }

    InstructionArgumentLinkNode createInstructionArgumentLinkNode() {
      return new InstructionArgumentLinkNode(name, instruction);
    }
  }
}
