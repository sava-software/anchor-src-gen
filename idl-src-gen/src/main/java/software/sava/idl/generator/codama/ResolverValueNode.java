package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

import java.util.ArrayList;
import java.util.List;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

final class ResolverValueNode extends NamedDocsNode implements ContextualValueNode {

  private final List<ResolverDefaultValueNodes> dependsOn;

  ResolverValueNode(final String name,
                    final List<String> docs,
                    final List<ResolverDefaultValueNodes> dependsOn) {
    super(name, docs);
    this.dependsOn = dependsOn;
  }

  List<ResolverDefaultValueNodes> dependsOn() {
    return dependsOn;
  }

  static ResolverValueNode parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createResolverValueNode();
  }

  private static final class Parser extends BaseDocsParser {

    private List<ResolverDefaultValueNodes> dependsOn;

    private Parser() {
    }

    ResolverValueNode createResolverValueNode() {
      return new ResolverValueNode(
          name,
          docs == null ? List.of() : docs,
          dependsOn == null ? List.of() : dependsOn
      );
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("dependsOn", buf, offset, len)) {
        dependsOn = new ArrayList<>();
        while (ji.readArray()) {
          dependsOn.add(ResolverDefaultValueNodes.parse(ji));
        }
        return true;
      } else {
        return super.test(buf, offset, len, ji);
      }
    }
  }
}
