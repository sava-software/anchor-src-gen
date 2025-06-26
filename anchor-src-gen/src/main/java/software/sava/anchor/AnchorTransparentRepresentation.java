package software.sava.anchor;

import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

public record AnchorTransparentRepresentation() implements AnchorRepresentation {

  static final AnchorTransparentRepresentation INSTANCE = new AnchorTransparentRepresentation();

  static AnchorRepresentation parseRepresentation(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createRepresentation();
  }

  private static final class Parser implements FieldBufferPredicate {

    private String kind;
    private boolean packed;
    private int align;

    private Parser() {
    }

    private AnchorRepresentation createRepresentation() {
      return switch (kind) {
        case "rust" -> new AnchorRustRepresentation(packed, align);
        case "c" -> new AnchorCRepresentation(packed, align);
        case "transparent" -> AnchorTransparentRepresentation.INSTANCE;
        default -> throw new IllegalStateException("Unexpected value: " + kind);
      };
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("kind", buf, offset, len)) {
        this.kind = ji.readString();
      } else if (fieldEquals("packed", buf, offset, len)) {
        this.packed = ji.readBoolean();
      } else if (fieldEquals("align", buf, offset, len)) {
        this.align = ji.readInt();
      } else {
        throw new IllegalStateException("Unhandled AnchorRepresentation field " + new String(buf, offset, len));
      }
      return true;
    }
  }
}
