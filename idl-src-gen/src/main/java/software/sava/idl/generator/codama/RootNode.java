package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

import java.util.ArrayList;
import java.util.List;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

record RootNode(String standard, String version, ProgramNode program, List<ProgramNode> additionalPrograms) {

  public static RootNode parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createRootNode();
  }

  static final class Parser extends BaseParser {

    private String standard;
    private String version;
    private ProgramNode program;
    private List<ProgramNode> additionalPrograms;

    private Parser() {
    }

    RootNode createRootNode() {
      return new RootNode(
          standard,
          version,
          program,
          additionalPrograms == null ? List.of() : additionalPrograms
      );
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("standard", buf, offset, len)) {
        standard = ji.readString();
        return true;
      } else if (fieldEquals("version", buf, offset, len)) {
        version = ji.readString();
        return true;
      } else if (fieldEquals("program", buf, offset, len)) {
        program = ProgramNode.parse(ji);
        return true;
      } else if (fieldEquals("additionalPrograms", buf, offset, len)) {
        additionalPrograms = new ArrayList<>();
        while (ji.readArray()) {
          additionalPrograms.add(ProgramNode.parse(ji));
        }
        return true;
      } else {
        return super.test(buf, offset, len, ji);
      }
    }
  }
}
