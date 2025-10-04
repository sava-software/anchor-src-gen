package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

final class FieldDiscriminatorNode extends NamedNode implements DiscriminatorNode {

  private final int offset;

  public FieldDiscriminatorNode(final String name, final int offset) {
    super(name);
    this.offset = offset;
  }

  int offset() {
    return offset;
  }

  public static FieldDiscriminatorNode parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createFieldDiscriminatorNode();
  }

  static final class Parser extends BaseParser {

    private int offset;

    FieldDiscriminatorNode createFieldDiscriminatorNode() {
      return new FieldDiscriminatorNode(name, offset);
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("offset", buf, offset, len)) {
        this.offset = ji.readInt();
        return true;
      } else {
        return super.test(buf, offset, len, ji);
      }
    }
  }
}
