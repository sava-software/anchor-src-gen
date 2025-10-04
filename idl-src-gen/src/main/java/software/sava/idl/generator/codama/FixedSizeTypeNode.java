package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

import java.util.function.Function;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

final class FixedSizeTypeNode extends BaseNestedTypeNode implements NestedTypeNode {

  private final int size;

  public FixedSizeTypeNode(final TypeNode typeNode, final int size) {
    super(typeNode);
    this.size = size;
  }

  int size() {
    return size;
  }

  public static FixedSizeTypeNode parse(final JsonIterator ji,
                                        final Function<JsonIterator, TypeNode> typeParser) {
    final var parser = new Parser(typeParser);
    ji.testObject(parser);
    return parser.createTypeNode();
  }

  private static final class Parser extends BaseNestedTypeNode.Parser {

    private int size;

    Parser(final Function<JsonIterator, TypeNode> typeParser) {
      super(typeParser);
    }

    FixedSizeTypeNode createTypeNode() {
      return new FixedSizeTypeNode(type, size);
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("size", buf, offset, len)) {
        size = ji.readInt();
      } else {
        return super.test(buf, offset, len, ji);
      }
      return true;
    }
  }
}
