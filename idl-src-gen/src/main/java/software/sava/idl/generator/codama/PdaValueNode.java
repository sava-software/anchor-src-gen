package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;

import java.util.ArrayList;
import java.util.List;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

record PdaValueNode(PdaValueNodePda pda, List<PdaSeedValueNode> seeds) implements ContextualValueNode {

  static PdaValueNode parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createPdaValueNode();
  }

  private static final class Parser implements FieldBufferPredicate {

    private PdaValueNodePda pda;
    private List<PdaSeedValueNode> seeds;

    PdaValueNode createPdaValueNode() {
      return new PdaValueNode(pda, seeds == null ? List.of() : seeds);
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("pda", buf, offset, len)) {
        pda = PdaValueNodePda.parse(ji);
      } else if (fieldEquals("seeds", buf, offset, len)) {
        seeds = new ArrayList<>();
        while (ji.readArray()) {
          seeds.add(PdaSeedValueNode.parse(ji));
        }
      } else if (fieldEquals("kind", buf, offset, len)) {
        ji.skip();
      } else {
        throw new IllegalStateException(String.format(
            "Unhandled %s field %s.",
            getClass().getSimpleName(), new String(buf, offset, len)
        ));
      }
      return true;
    }
  }
}
