package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

import java.util.ArrayList;
import java.util.List;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

public final class PdaNode extends NamedDocsNode implements PdaValueNodePda {

  private final List<PdaSeedNode> seeds;

  public PdaNode(final String name,
                 final List<String> docs,
                 final List<PdaSeedNode> seeds) {
    super(name, docs);
    this.seeds = seeds;
  }

  public List<PdaSeedNode> seeds() {
    return seeds;
  }

  public static PdaNode parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createPdaNode();
  }

  static final class Parser extends BaseDocsParser {

    private List<PdaSeedNode> seeds;

    private Parser() {
    }

    PdaNode createPdaNode() {
      return new PdaNode(
          name,
          docs == null ? List.of() : docs,
          seeds == null ? List.of() : seeds
      );
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("seeds", buf, offset, len)) {
        seeds = new ArrayList<>();
        while (ji.readArray()) {
          seeds.add(PdaSeedNode.parse(ji));
        }
        return true;
      } else {
        return super.test(buf, offset, len, ji);
      }
    }
  }
}
