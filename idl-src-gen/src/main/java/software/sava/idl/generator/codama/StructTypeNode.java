package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;

import java.util.ArrayList;
import java.util.List;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

record StructTypeNode(List<StructFieldTypeNode> fields) implements TypeNode {

  static StructTypeNode parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createTypeNode();
  }

  private static final class Parser implements FieldBufferPredicate {

    private List<StructFieldTypeNode> fields;

    StructTypeNode createTypeNode() {
      return new StructTypeNode(fields);
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("fields", buf, offset, len)) {
        fields = new ArrayList<>();
        while (ji.readArray()) {
          fields.add(StructFieldTypeNode.parse(ji));
        }
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
