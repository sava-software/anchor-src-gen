package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

import java.util.List;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

final class VariablePdaSeedNode extends NamedDocsNode implements PdaSeedNode {

  private final TypeNode type;

  public VariablePdaSeedNode(final String name, final List<String> docs, final TypeNode type) {
    super(name, docs);
    this.type = type;
  }

  public TypeNode type() {
    return type;
  }

  public static VariablePdaSeedNode parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createVariablePdaSeedNode();
  }

  static final class Parser extends BaseDocsParser {

    private TypeNode type;

    VariablePdaSeedNode createVariablePdaSeedNode() {
      return new VariablePdaSeedNode(name, docs == null ? List.of() : docs, type);
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("type", buf, offset, len)) {
        type = TypeNode.parse(ji);
        return true;
      } else {
        return super.test(buf, offset, len, ji);
      }
    }
  }
}
