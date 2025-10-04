package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

import java.util.List;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

public final class InstructionAccountNode extends NamedDocsNode {

  private final boolean isWritable;
  private final boolean isSigner;
  private final boolean isOptional;
  private final InstructionInputValueNode defaultValue;

  public InstructionAccountNode(final String name,
                                final List<String> docs,
                                final boolean isWritable,
                                final boolean isSigner,
                                final boolean isOptional,
                                final InstructionInputValueNode defaultValue) {
    super(name, docs);
    this.isWritable = isWritable;
    this.isSigner = isSigner;
    this.isOptional = isOptional;
    this.defaultValue = defaultValue;
  }

  boolean isWritable() {
    return isWritable;
  }

  boolean isSigner() {
    return isSigner;
  }

  boolean isOptional() {
    return isOptional;
  }

  InstructionInputValueNode defaultValue() {
    return defaultValue;
  }

  public static InstructionAccountNode parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createInstructionAccountNode();
  }

  static final class Parser extends BaseDocsParser {

    private boolean isWritable;
    private boolean isSigner;
    private boolean isOptional;
    private InstructionInputValueNode defaultValue;

    private Parser() {
    }

    InstructionAccountNode createInstructionAccountNode() {
      return new InstructionAccountNode(
          name,
          docs == null ? List.of() : docs,
          isWritable,
          isSigner,
          isOptional,
          defaultValue
      );
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("isWritable", buf, offset, len)) {
        isWritable = ji.readBoolean();
        return true;
      } else if (fieldEquals("isSigner", buf, offset, len)) {
        if (ji.whatIsNext() == systems.comodal.jsoniter.ValueType.STRING) {
          final var stringValue = ji.readString();
          isSigner = "either".equals(stringValue) || Boolean.parseBoolean(stringValue);
        } else {
          isSigner = ji.readBoolean();
        }
        return true;
      } else if (fieldEquals("isOptional", buf, offset, len)) {
        isOptional = ji.readBoolean();
        return true;
      } else if (fieldEquals("defaultValue", buf, offset, len)) {
        defaultValue = InstructionInputValueNode.parse(ji);
        return true;
      } else {
        return super.test(buf, offset, len, ji);
      }
    }
  }
}
