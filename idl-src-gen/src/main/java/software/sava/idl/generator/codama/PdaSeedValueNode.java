package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

final class PdaSeedValueNode extends NamedNode {

  private final PdaSeedValueNodeValue value;

  public PdaSeedValueNode(final String name, final PdaSeedValueNodeValue value) {
    super(name);
    this.value = value;
  }

  PdaSeedValueNodeValue value() {
    return value;
  }

  public static PdaSeedValueNode parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createPdaSeedValueNode();
  }

  static final class Parser extends BaseParser {

    private PdaSeedValueNodeValue value;

    PdaSeedValueNode createPdaSeedValueNode() {
      return new PdaSeedValueNode(name, value);
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("value", buf, offset, len)) {
        value = PdaSeedValueNodeValue.parse(ji);
        return true;
      } else {
        return super.test(buf, offset, len, ji);
      }
    }
  }
}
