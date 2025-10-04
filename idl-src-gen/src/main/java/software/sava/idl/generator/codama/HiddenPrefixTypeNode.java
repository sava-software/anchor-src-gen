package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

final class HiddenPrefixTypeNode extends BaseNestedTypeNode implements NestedTypeNode {

  private final List<ValueNode.Constant> prefix;

  public HiddenPrefixTypeNode(final TypeNode typeNode, final List<ValueNode.Constant> prefix) {
    super(typeNode);
    this.prefix = prefix;
  }

  public List<ValueNode.Constant> prefix() {
    return prefix;
  }

  public static HiddenPrefixTypeNode parse(final JsonIterator ji) {
    return parse(ji, null);
  }

  public static HiddenPrefixTypeNode parse(final JsonIterator ji,
                                           final Function<JsonIterator, TypeNode> typeParser) {
    final var parser = new Parser(typeParser);
    ji.testObject(parser);
    return parser.createTypeNode();
  }

  private static final class Parser extends BaseNestedTypeNode.Parser {

    private List<ValueNode.Constant> prefix;

    Parser(final Function<JsonIterator, TypeNode> typeParser) {
      super(typeParser);
    }

    HiddenPrefixTypeNode createTypeNode() {
      return new HiddenPrefixTypeNode(type, prefix);
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("prefix", buf, offset, len)) {
        this.prefix = new ArrayList<>();
        while (ji.readArray()) {
          this.prefix.add(ValueNode.Constant.parse(ji));
        }
      } else {
        return super.test(buf, offset, len, ji);
      }
      return true;
    }
  }
}
