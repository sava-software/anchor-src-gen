package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.CharBufferFunction;
import systems.comodal.jsoniter.JsonIterator;

import java.util.function.Function;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

final class PreOffsetTypeNode extends BaseNestedTypeNode implements NestedTypeNode {

  enum Strategy {
    absolute,
    padded,
    relative
  }

  private final int offset;
  private final Strategy strategy;

  PreOffsetTypeNode(final TypeNode typeNode, final int offset, final Strategy strategy) {
    super(typeNode);
    this.offset = offset;
    this.strategy = strategy;
  }

  int offset() {
    return offset;
  }

  Strategy strategy() {
    return strategy;
  }

  static  PreOffsetTypeNode parse(final JsonIterator ji,
                                                                final Function<JsonIterator, TypeNode> typeParser) {
    final var parser = new Parser(typeParser);
    ji.testObject(parser);
    return parser.createTypeNode();
  }

  private static final class Parser extends BaseNestedTypeNode.Parser {

    private int offset;
    private Strategy strategy;

    Parser(final Function<JsonIterator, TypeNode> typeParser) {
      super(typeParser);
    }

    PreOffsetTypeNode createTypeNode() {
      return new PreOffsetTypeNode(type, offset, strategy);
    }

    private static final CharBufferFunction<Strategy> STRATEGY_PARSER = (buf, offset, len) -> {
      final String strategyStr = new String(buf, offset, len);
      return Strategy.valueOf(strategyStr);
    };

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("offset", buf, offset, len)) {
        this.offset = ji.readInt();
      } else if (fieldEquals("strategy", buf, offset, len)) {
        strategy = ji.applyChars(STRATEGY_PARSER);
      } else {
        return super.test(buf, offset, len, ji);
      }
      return true;
    }
  }
}
