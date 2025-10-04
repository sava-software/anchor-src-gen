package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

import java.util.function.Function;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

final class SentinelTypeNode extends BaseNestedTypeNode implements NestedTypeNode {

  private final ValueNode.Constant sentinel;

  public SentinelTypeNode(final TypeNode typeNode, final ValueNode.Constant sentinel) {
    super(typeNode);
    this.sentinel = sentinel;
  }

  ValueNode.Constant sentinel() {
    return sentinel;
  }

  public static SentinelTypeNode parse(final JsonIterator ji,
                                       final Function<JsonIterator, TypeNode> typeParser) {
    final var parser = new Parser(typeParser);
    ji.testObject(parser);
    return parser.createTypeNode();
  }

  private static final class Parser extends BaseNestedTypeNode.Parser {

    private ValueNode.Constant sentinel;

    Parser(final Function<JsonIterator, TypeNode> typeParser) {
      super(typeParser);
    }

    SentinelTypeNode createTypeNode() {
      return new SentinelTypeNode(type, sentinel);
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("sentinel", buf, offset, len)) {
        this.sentinel = ValueNode.Constant.parse(ji);
      } else {
        return super.test(buf, offset, len, ji);
      }
      return true;
    }
  }
}
