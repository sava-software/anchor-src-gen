package software.sava.idl.generator.anchor;

import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;

import java.util.ArrayList;
import java.util.List;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

public record AnchorIdlDependency(String name, String version) {

  static List<AnchorIdlDependency> parseDependencies(final JsonIterator ji) {
    final var deployments = new ArrayList<AnchorIdlDependency>();
    while (ji.readArray()) {
      final var parser = new Parser();
      ji.testObject(parser);
      deployments.add(parser.createDependency());
    }
    return deployments;
  }

  private static final class Parser implements FieldBufferPredicate {

    private String name;
    private String version;

    private Parser() {
    }

    private AnchorIdlDependency createDependency() {
      return new AnchorIdlDependency(name, version);
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("name", buf, offset, len)) {
        this.name = ji.readString();
      } else if (fieldEquals("version", buf, offset, len)) {
        this.version = ji.readString();
      } else {
        throw new IllegalStateException("Unhandled AnchorIdlDependency field " + new String(buf, offset, len));
      }
      return true;
    }
  }
}
