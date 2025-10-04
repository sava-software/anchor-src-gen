package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;
import systems.comodal.jsoniter.ValueType;

import java.util.List;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

record InstructionRemainingAccountsNode(List<String> docs,
                                      boolean isOptional,
                                      boolean isSigner,
                                      ArgumentValueNode value) {

  public static InstructionRemainingAccountsNode parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createInstructionRemainingAccountsNode();
  }

  static final class Parser extends BaseDocsParser {

    private boolean isOptional;
    private boolean isSigner;
    private ArgumentValueNode value;

    private Parser() {
    }

    InstructionRemainingAccountsNode createInstructionRemainingAccountsNode() {
      return new InstructionRemainingAccountsNode(
          docs == null ? List.of() : docs,
          isOptional,
          isSigner,
          value
      );
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("isOptional", buf, offset, len)) {
        isOptional = ji.readBoolean();
        return true;
      } else if (fieldEquals("isSigner", buf, offset, len)) {
        if (ji.whatIsNext() == ValueType.STRING) {
          final var stringValue = ji.readString();
          isSigner = "either".equals(stringValue) || Boolean.parseBoolean(stringValue);
        } else {
          isSigner = ji.readBoolean();
        }
        return true;
      } else if (fieldEquals("value", buf, offset, len)) {
        value = ArgumentValueNode.parse(ji);
        return true;
      } else {
        return super.test(buf, offset, len, ji);
      }
    }
  }
}
