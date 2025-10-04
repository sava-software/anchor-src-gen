package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.CharBufferFunction;
import systems.comodal.jsoniter.JsonIterator;

import java.util.function.Function;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

// https://github.com/codama-idl/codama/blob/main/packages/nodes/docs/typeNodes/PostOffsetTypeNode.md
final class PostOffsetTypeNode extends BaseNestedTypeNode implements NestedTypeNode {

  enum Strategy {
    absolute,
    padded,
    preOffset,
    relative
  }

  private final int offset;
  private final Strategy strategy;

  public PostOffsetTypeNode(final TypeNode typeNode, final int offset, final Strategy strategy) {
    super(typeNode);
    this.offset = offset;
    this.strategy = strategy;
  }

  public int offset() {
    return offset;
  }

  public Strategy strategy() {
    return strategy;
  }

  public static  PostOffsetTypeNode parse(final JsonIterator ji,
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

    PostOffsetTypeNode createTypeNode() {
      return new PostOffsetTypeNode(type, offset, strategy);
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
