package software.sava.anchor;

import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;
import systems.comodal.jsoniter.ValueType;
import systems.comodal.jsoniter.factory.ElementFactory;

import java.util.function.Supplier;
import java.util.regex.Pattern;

import static software.sava.anchor.AnchorType.ANCHOR_TYPE_PARSER;
import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

final class AnchorConstantParser implements ElementFactory<AnchorConstant> {

  static final Supplier<AnchorConstantParser> FACTORY = AnchorConstantParser::new;

  private static final Pattern SPACE_PATTERN = Pattern.compile("\\s+");
  private static final Pattern UNDERSCORE_PATTERN = Pattern.compile("_");

  private String name;
  private AnchorType type;
  private AnchorConstant value;

  AnchorConstantParser() {
  }

  @Override
  public AnchorConstant create() {
    return value;
  }

  private static String removeAll(final Pattern pattern, final String constant) {
    return pattern.matcher(constant).replaceAll("");
  }

  @Override
  public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
    if (fieldEquals("name", buf, offset, len)) {
      this.name = ji.readString();
    } else if (fieldEquals("type", buf, offset, len)) {
      if (ji.whatIsNext() == ValueType.OBJECT) {
        final var typeParser = new DefinedTypeParser();
        ji.testObject(typeParser);
        this.type = typeParser.type;
      } else {
        this.type = ji.applyChars(ANCHOR_TYPE_PARSER);
      }
    } else if (fieldEquals("value", buf, offset, len)) {
      final var value = ji.readString();
      this.value = switch (this.type) {
        case bytes -> {
          final var bracketsRemoved = value.substring(1, value.length() - 1);
          final var parts = bracketsRemoved.split(",\\s+");
          final byte[] bytes = new byte[parts.length];
          for (int i = 0; i < bytes.length; ++i) {
            bytes[i] = (byte) Integer.parseInt(parts[i]);
          }
          yield new AnchorBytesConstant(this.name, bytes);
        }
        case string -> new AnchorStringConstant(this.name,
            value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"'
                ? value.substring(1, value.length() - 1)
                : value
        );
        case i8, i16, i32 -> new AnchorIntConstant(this.name, Integer.parseInt(removeAll(SPACE_PATTERN, value)));
        case i64 -> new AnchorLongConstant(this.name, Long.parseLong(removeAll(SPACE_PATTERN, value)));
        case u8, u16, u32 -> new AnchorIntConstant(this.name, Integer.parseInt(removeAll(UNDERSCORE_PATTERN, value)));
        case u64, usize -> new AnchorLongConstant(this.name, Long.parseLong(removeAll(UNDERSCORE_PATTERN, value)));
        case i128, i256, u128, u256 -> new AnchorBigIntegerConstant(this.name, removeAll(UNDERSCORE_PATTERN, value));
        default -> throw new UnsupportedOperationException("TODO: Support constants for type: " + this.type);
      };
    } else {
      ji.skip();
    }
    return true;
  }

  private static final class DefinedTypeParser implements FieldBufferPredicate {

    private AnchorType type;

    private DefinedTypeParser() {
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("defined", buf, offset, len)) {
        this.type = ji.applyChars(ANCHOR_TYPE_PARSER);
      } else {
        throw new IllegalStateException("Unhandled AnchorConstantType field " + new String(buf, offset, len));
      }
      return true;
    }
  }
}
