package software.sava.idl.generator.codama;

import software.sava.rpc.json.PublicKeyEncoding;
import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;

import java.util.ArrayList;
import java.util.List;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

public sealed interface ValueNode extends InstructionInputValueNode, PdaSeedValueNodeValue permits
    ValueNode.Array,
    ValueNode.Boolean,
    ValueNode.Bytes,
    ValueNode.Constant,
    ValueNode.Enum,
    ValueNode.Map,
    ValueNode.None,
    ValueNode.Number,
    ValueNode.PublicKey,
    ValueNode.Set,
    ValueNode.Some,
    ValueNode.String,
    ValueNode.Struct,
    ValueNode.Tuple {

  static ValueNode parse(final JsonIterator ji, final java.lang.String kind) {
    return switch (kind) {
      case "arrayValueNode" -> Array.parse(ji);
      case "booleanValueNode" -> Boolean.parse(ji);
      case "bytesValueNode" -> Bytes.parse(ji);
      case "constantValueNode" -> Constant.parse(ji);
      case "enumValueNode" -> Enum.parse(ji);
      case "mapValueNode" -> Map.parse(ji);
      case "noneValueNode" -> None.parse(ji);
      case "numberValueNode" -> Number.parse(ji);
      case "publicKeyValueNode" -> PublicKey.parse(ji);
      case "setValueNode" -> Set.parse(ji);
      case "someValueNode" -> Some.parse(ji);
      case "stringValueNode" -> String.parse(ji);
      case "structValueNode" -> Struct.parse(ji);
      case "tupleValueNode" -> Tuple.parse(ji);
      default -> throw new UnsupportedOperationException(kind);
    };
  }

  static ValueNode parse(final JsonIterator ji) {
    final var kind = ji.skipUntil("kind").readString();
    return parse(ji, kind);
  }

  private static List<ValueNode> parseArray(final JsonIterator ji) {
    final var items = new ArrayList<ValueNode>();
    while (ji.readArray()) {
      items.add(ValueNode.parse(ji));
    }
    return items;
  }

  private static List<Map.Entry> parseEntryArray(final JsonIterator ji) {
    final var entries = new ArrayList<Map.Entry>();
    while (ji.readArray()) {
      entries.add(Map.Entry.parse(ji));
    }
    return entries;
  }

  record Array(List<ValueNode> items) implements ValueNode {

    public static Array parse(final JsonIterator ji) {
      final var parser = new Parser();
      ji.testObject(parser);
      return parser.createArray();
    }

    static final class Parser implements FieldBufferPredicate {

      private List<ValueNode> items;

      Array createArray() {
        return new Array(items);
      }

      @Override
      public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
        if (fieldEquals("items", buf, offset, len)) {
          this.items = parseArray(ji);
        } else {
          throw new IllegalStateException("Unhandled field " + java.lang.String.valueOf(buf, offset, len));
        }
        return true;
      }
    }
  }

  // https://github.com/codama-idl/codama/blob/main/packages/nodes/docs/valueNodes/BytesValueNode.md
  record Bytes(StringEncoding encoding, java.lang.String data) implements ValueNode {

    public static Bytes parse(final JsonIterator ji) {
      final var parser = new Parser();
      ji.testObject(parser);
      return parser.createBytes();
    }

    static final class Parser implements FieldBufferPredicate {

      private StringEncoding encoding;
      private java.lang.String data;

      Bytes createBytes() {
        return new Bytes(encoding, data);
      }

      @Override
      public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
        if (fieldEquals("encoding", buf, offset, len)) {
          encoding = StringEncoding.valueOf(ji.readString());
        } else if (fieldEquals("data", buf, offset, len)) {
          data = ji.readString();
        } else {
          throw new IllegalStateException("Unhandled field " + java.lang.String.valueOf(buf, offset, len));
        }
        return true;
      }
    }
  }

  record Boolean(boolean val) implements ValueNode {

    public static Boolean parse(final JsonIterator ji) {
      final var parser = new Parser();
      ji.testObject(parser);
      return parser.createBoolean();
    }

    static final class Parser implements FieldBufferPredicate {

      private boolean val;

      Boolean createBoolean() {
        return new Boolean(val);
      }

      @Override
      public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
        if (fieldEquals("value", buf, offset, len)) {
          val = ji.readBoolean();
        } else {
          throw new IllegalStateException("Unhandled field " + java.lang.String.valueOf(buf, offset, len));
        }
        return true;
      }
    }
  }

  record Constant(TypeNode type, ValueNode val) implements ValueNode {

    public static Constant parse(final JsonIterator ji) {
      final var parser = new Parser();
      ji.testObject(parser);
      return parser.createConstant();
    }

    static final class Parser implements FieldBufferPredicate {

      private TypeNode type;
      private ValueNode val;

      Constant createConstant() {
        return new Constant(type, val);
      }

      @Override
      public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
        if (fieldEquals("type", buf, offset, len)) {
          type = TypeNode.parse(ji);
        } else if (fieldEquals("value", buf, offset, len)) {
          val = ValueNode.parse(ji);
        } else {
          throw new IllegalStateException("Unhandled field " + java.lang.String.valueOf(buf, offset, len));
        }
        return true;
      }
    }
  }

  record Enum(java.lang.String variant, DefinedTypeLinkNode _enum, ValueNode val) implements ValueNode {

    public static Enum parse(final JsonIterator ji) {
      final var parser = new Parser();
      ji.testObject(parser);
      return parser.createEnum();
    }

    static final class Parser implements FieldBufferPredicate {

      private java.lang.String variant;
      private DefinedTypeLinkNode _enum;
      private ValueNode val;

      Enum createEnum() {
        return new Enum(variant, _enum, val);
      }

      @Override
      public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
        if (fieldEquals("variant", buf, offset, len)) {
          variant = ji.readString();
        } else if (fieldEquals("enum", buf, offset, len)) {
          _enum = DefinedTypeLinkNode.parse(ji);
        } else if (fieldEquals("value", buf, offset, len)) {
          val = ValueNode.parse(ji);
        } else {
          throw new IllegalStateException("Unhandled field " + java.lang.String.valueOf(buf, offset, len));
        }
        return true;
      }
    }
  }

  record Map(TypeNode type, List<Entry> entries) implements ValueNode {

    public static Map parse(final JsonIterator ji) {
      final var parser = new Parser();
      ji.testObject(parser);
      return parser.createMap();
    }

    static final class Parser implements FieldBufferPredicate {

      private TypeNode type;
      private List<Entry> entries;

      Map createMap() {
        return new Map(type, entries == null ? List.of() : entries);
      }

      @Override
      public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
        if (fieldEquals("type", buf, offset, len)) {
          type = TypeNode.parse(ji);
        } else if (fieldEquals("entries", buf, offset, len)) {
          entries = parseEntryArray(ji);
        } else {
          throw new IllegalStateException("Unhandled field " + java.lang.String.valueOf(buf, offset, len));
        }
        return true;
      }
    }

    record Entry(ValueNode key, ValueNode val) {

      public static Entry parse(final JsonIterator ji) {
        final var parser = new Parser();
        ji.testObject(parser);
        return parser.createEntry();
      }

      static final class Parser implements FieldBufferPredicate {

        private ValueNode key;
        private ValueNode val;

        Entry createEntry() {
          return new Entry(key, val);
        }

        @Override
        public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
          if (fieldEquals("key", buf, offset, len)) {
            key = ValueNode.parse(ji);
          } else if (fieldEquals("val", buf, offset, len)) {
            val = ValueNode.parse(ji);
          } else {
            throw new IllegalStateException("Unhandled field " + java.lang.String.valueOf(buf, offset, len));
          }
          return true;
        }
      }
    }
  }

  record None() implements ValueNode {

    private static final None INSTANCE = new None();

    public static None parse(final JsonIterator ji) {
      ji.skipRestOfObject();
      return INSTANCE;
    }
  }

  record Number(long number) implements ValueNode, InstructionByteDeltaNodeValue {

    public static Number parse(final JsonIterator ji) {
      final var parser = new Parser();
      ji.testObject(parser);
      return parser.createNumber();
    }

    static final class Parser implements FieldBufferPredicate {

      private long number;

      Number createNumber() {
        return new Number(number);
      }

      @Override
      public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
        if (fieldEquals("number", buf, offset, len)) {
          number = ji.readLong();
        } else {
          throw new IllegalStateException("Unhandled field " + java.lang.String.valueOf(buf, offset, len));
        }
        return true;
      }
    }
  }

  record PublicKey(software.sava.core.accounts.PublicKey publicKey, java.lang.String identifier) implements ValueNode {

    public static PublicKey parse(final JsonIterator ji) {
      final var parser = new Parser();
      ji.testObject(parser);
      return parser.createPublicKey();
    }

    static final class Parser implements FieldBufferPredicate {

      private software.sava.core.accounts.PublicKey publicKey;
      private java.lang.String identifier;

      PublicKey createPublicKey() {
        return new PublicKey(publicKey, identifier);
      }

      @Override
      public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
        if (fieldEquals("publicKey", buf, offset, len)) {
          this.publicKey = PublicKeyEncoding.parseBase58Encoded(ji);
        } else if (fieldEquals("identifier", buf, offset, len)) {
          identifier = ji.readString();
        } else {
          throw new IllegalStateException("Unhandled field " + java.lang.String.valueOf(buf, offset, len));
        }
        return true;
      }
    }
  }

  record Set(List<ValueNode> items) implements ValueNode {

    public static Set parse(final JsonIterator ji) {
      final var parser = new Parser();
      ji.testObject(parser);
      return parser.createSet();
    }

    static final class Parser implements FieldBufferPredicate {

      private List<ValueNode> items;

      Set createSet() {
        return new Set(items);
      }

      @Override
      public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
        if (fieldEquals("items", buf, offset, len)) {
          this.items = parseArray(ji);
        } else {
          throw new IllegalStateException("Unhandled field " + java.lang.String.valueOf(buf, offset, len));
        }
        return true;
      }
    }
  }

  record Some(ValueNode val) implements ValueNode {

    public static Some parse(final JsonIterator ji) {
      final var parser = new Parser();
      ji.testObject(parser);
      return parser.createSome();
    }

    static final class Parser implements FieldBufferPredicate {

      private ValueNode val;

      Some createSome() {
        return new Some(val);
      }

      @Override
      public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
        if (fieldEquals("value", buf, offset, len)) {
          val = ValueNode.parse(ji);
        } else {
          throw new IllegalStateException("Unhandled field " + java.lang.String.valueOf(buf, offset, len));
        }
        return true;
      }
    }
  }

  record String(java.lang.String string) implements ValueNode {

    public static String parse(final JsonIterator ji) {
      final var parser = new Parser();
      ji.testObject(parser);
      return parser.createString();
    }

    static final class Parser implements FieldBufferPredicate {

      private java.lang.String string;

      String createString() {
        return new String(string);
      }

      @Override
      public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
        if (fieldEquals("string", buf, offset, len)) {
          this.string = ji.readString();
        } else {
          throw new IllegalStateException("Unhandled field " + java.lang.String.valueOf(buf, offset, len));
        }
        return true;
      }
    }
  }

  record Struct(List<Field> fields) implements ValueNode {

    public static Struct parse(final JsonIterator ji) {
      final var parser = new Parser();
      ji.testObject(parser);
      return parser.createStruct();
    }

    static final class Parser implements FieldBufferPredicate {

      private List<Field> fields;

      Struct createStruct() {
        return new Struct(fields);
      }

      @Override
      public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
        if (fieldEquals("fields", buf, offset, len)) {
          fields = new ArrayList<>();
          while (ji.readArray()) {
            fields.add(Field.parse(ji));
          }
        } else {
          throw new IllegalStateException("Unhandled field " + java.lang.String.valueOf(buf, offset, len));
        }
        return true;
      }
    }

    static final class Field extends NamedNode {

      private final ValueNode val;

      public Field(final java.lang.String name, final ValueNode val) {
        super(name);
        this.val = val;
      }

      public ValueNode val() {
        return val;
      }

      public static Field parse(final JsonIterator ji) {
        final var parser = new Parser();
        ji.testObject(parser);
        return parser.createField();
      }

      static final class Parser extends BaseParser {

        private ValueNode val;

        Field createField() {
          return new Field(name, val);
        }

        @Override
        public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
          if (fieldEquals("value", buf, offset, len)) {
            val = ValueNode.parse(ji);
            return true;
          } else {
            return super.test(buf, offset, len, ji);
          }
        }
      }
    }
  }

  record Tuple(List<ValueNode> items) implements ValueNode {

    public static Tuple parse(final JsonIterator ji) {
      final var parser = new Parser();
      ji.testObject(parser);
      return parser.createTuple();
    }

    static final class Parser implements FieldBufferPredicate {

      private List<ValueNode> items;

      Tuple createTuple() {
        return new Tuple(items);
      }

      @Override
      public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
        if (fieldEquals("items", buf, offset, len)) {
          this.items = parseArray(ji);
        } else {
          throw new IllegalStateException("Unhandled field " + java.lang.String.valueOf(buf, offset, len));
        }
        return true;
      }
    }
  }
}
