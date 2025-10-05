package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

import java.util.function.Function;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

final class HiddenSuffixTypeNode extends BaseNestedTypeNode implements NestedTypeNode {

  private final ValueNode.Constant[] suffix;

  HiddenSuffixTypeNode(final TypeNode typeNode, final ValueNode.Constant[] suffix) {
    super(typeNode);
    this.suffix = suffix;
  }

  ValueNode.Constant[] suffix() {
    return suffix;
  }

  static HiddenSuffixTypeNode parse(final JsonIterator ji,
                                           final Function<JsonIterator, TypeNode> typeParser) {
    final var parser = new Parser(typeParser);
    ji.testObject(parser);
    return parser.createTypeNode();
  }

  private static final class Parser extends BaseNestedTypeNode.Parser {

    private ValueNode.Constant[] suffix;

    Parser(final Function<JsonIterator, TypeNode> typeParser) {
      super(typeParser);
    }

    HiddenSuffixTypeNode createTypeNode() {
      return new HiddenSuffixTypeNode(type, suffix);
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("suffix", buf, offset, len)) {
        // For now, skip the suffix parsing - would need ValueNode.Constant parser
        ji.skip();
      } else {
        return super.test(buf, offset, len, ji);
      }
      return true;
    }
  }
}
