package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

import java.util.List;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

public final class InstructionArgumentNode extends NamedDocsNode {

  public enum Strategy {
    optional,
    omitted
  }

  private final TypeNode type;
  private final InstructionInputValueNode defaultValue;
  private final Strategy defaultValueStrategy;

  public InstructionArgumentNode(final String name,
                                 final List<String> docs,
                                 final TypeNode type,
                                 final InstructionInputValueNode defaultValue,
                                 final Strategy defaultValueStrategy) {
    super(name, docs);
    this.type = type;
    this.defaultValue = defaultValue;
    this.defaultValueStrategy = defaultValueStrategy;
  }

  TypeNode type() {
    return type;
  }

  InstructionInputValueNode defaultValue() {
    return defaultValue;
  }

  Strategy defaultValueStrategy() {
    return defaultValueStrategy;
  }

  public static InstructionArgumentNode parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createInstructionArgumentNode();
  }

  static final class Parser extends BaseDocsParser {

    private TypeNode type;
    private InstructionInputValueNode defaultValue;
    private Strategy defaultValueStrategy = Strategy.optional; // Default to "optional"

    private Parser() {
    }

    InstructionArgumentNode createInstructionArgumentNode() {
      return new InstructionArgumentNode(
          name,
          docs == null ? List.of() : docs,
          type,
          defaultValue,
          defaultValueStrategy
      );
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("type", buf, offset, len)) {
        type = TypeNode.parse(ji);
        return true;
      } else if (fieldEquals("defaultValue", buf, offset, len)) {
        defaultValue = InstructionInputValueNode.parse(ji);
        return true;
      } else if (fieldEquals("defaultValueStrategy", buf, offset, len)) {
        defaultValueStrategy = Strategy.valueOf(ji.readString());
        return true;
      } else {
        return super.test(buf, offset, len, ji);
      }
    }
  }
}
