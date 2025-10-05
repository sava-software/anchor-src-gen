package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;

import java.util.ArrayList;
import java.util.List;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

record EnumTypeNode(List<EnumVariantTypeNode> variants, TypeNode size) implements TypeNode {

  static EnumTypeNode parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createTypeNode();
  }

  private static final class Parser implements FieldBufferPredicate {

    private List<EnumVariantTypeNode> variants;
    private TypeNode size;

    EnumTypeNode createTypeNode() {
      return new EnumTypeNode(variants, size);
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("variants", buf, offset, len)) {
        variants = new ArrayList<>();
        while (ji.readArray()) {
          variants.add(EnumVariantTypeNode.parse(ji));
        }
      } else if (fieldEquals("size", buf, offset, len)) {
        size = TypeNode.parse(ji);
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
