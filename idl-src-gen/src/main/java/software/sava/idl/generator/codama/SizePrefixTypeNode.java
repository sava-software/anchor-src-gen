package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

import java.util.function.Function;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

final class SizePrefixTypeNode extends BaseNestedTypeNode implements NestedTypeNode {

  private final NestedTypeNode prefix;

  public SizePrefixTypeNode(final TypeNode typeNode, final NestedTypeNode prefix) {
    super(typeNode);
    this.prefix = prefix;
  }

  public NestedTypeNode prefix() {
    return prefix;
  }

  public static SizePrefixTypeNode parse(final JsonIterator ji,
                                         final Function<JsonIterator, TypeNode> typeParser) {
    final var parser = new Parser(typeParser);
    ji.testObject(parser);
    return parser.createTypeNode();
  }

  private static final class Parser extends BaseNestedTypeNode.Parser {

    private NestedTypeNode prefix;

    Parser(final Function<JsonIterator, TypeNode> typeParser) {
      super(typeParser);
    }

    SizePrefixTypeNode createTypeNode() {
      return new SizePrefixTypeNode(type, prefix);
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("prefix", buf, offset, len)) {
        prefix = TypeNode.parseNestedTypeNode(ji, NumberTypeNode::parse);
      } else {
        return super.test(buf, offset, len, ji);
      }
      return true;
    }
  }
}
