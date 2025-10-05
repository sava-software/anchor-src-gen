package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;

import java.util.function.Function;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

abstract class BaseNestedTypeNode {

  protected final TypeNode typeNode;

  BaseNestedTypeNode(final TypeNode typeNode) {
    this.typeNode = typeNode;
  }

  final TypeNode typeNode() {
    return typeNode;
  }

  static abstract class Parser implements FieldBufferPredicate {

    private final Function<JsonIterator, TypeNode> typeParser;
    protected TypeNode type;

    protected Parser(final Function<JsonIterator, TypeNode> typeParser) {
      this.typeParser = typeParser;
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("type", buf, offset, len)) {
        type = typeParser == null ? TypeNode.parse(ji) : typeParser.apply(ji);
      } else if (fieldEquals("kind", buf, offset, len)) {
        ji.skip();
      } else {
        throw new IllegalStateException(String.format(
            "Unhandled %s field %s.",
            getClass().getSimpleName(), new String(buf, offset, len)
        ));
      }
      return true;
    }
  }
}
