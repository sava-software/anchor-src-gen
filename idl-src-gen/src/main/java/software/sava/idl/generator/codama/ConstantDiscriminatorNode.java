package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

record ConstantDiscriminatorNode(ValueNode.Constant constant, int offset) implements DiscriminatorNode {

  static ConstantDiscriminatorNode parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createConstantDiscriminatorNode();
  }

  private static final class Parser implements FieldBufferPredicate {

    private ValueNode.Constant constant;
    private int offset;

    ConstantDiscriminatorNode createConstantDiscriminatorNode() {
      return new ConstantDiscriminatorNode(constant, offset);
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("constant", buf, offset, len)) {
        constant = ValueNode.Constant.parse(ji);
      } else if (fieldEquals("offset", buf, offset, len)) {
        this.offset = ji.readInt();
      } else {
        throw new IllegalStateException("Unhandled field " + String.valueOf(buf, offset, len));
      }
      return true;
    }
  }
}
