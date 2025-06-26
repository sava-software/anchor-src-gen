package software.sava.anchor;

import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;

import java.util.ArrayList;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

public record AnchorIdlDeployments(String mainnet,
                                   String testnet,
                                   String devnet,
                                   String localnet) {

  static ArrayList<AnchorIdlDeployments> parseDeployments(final JsonIterator ji) {
    final var deployments = new ArrayList<AnchorIdlDeployments>();
    while (ji.readArray()) {
      final var parser = new Parser();
      ji.testObject(parser);
      deployments.add(parser.createDeployments());
    }
    return deployments;
  }

  private static final class Parser implements FieldBufferPredicate {

    private String mainnet;
    private String testnet;
    private String devnet;
    private String localnet;

    private Parser() {
    }

    private AnchorIdlDeployments createDeployments() {
      return new AnchorIdlDeployments(
          mainnet,
          testnet,
          devnet,
          localnet
      );
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("mainnet", buf, offset, len)) {
        this.mainnet = ji.readString();
      } else if (fieldEquals("testnet", buf, offset, len)) {
        this.testnet = ji.readString();
      } else if (fieldEquals("devnet", buf, offset, len)) {
        this.devnet = ji.readString();
      } else if (fieldEquals("localnet", buf, offset, len)) {
        this.localnet = ji.readString();
      } else {
        throw new IllegalStateException("Unhandled AnchorIdlDeployments field " + new String(buf, offset, len));
      }
      return true;
    }
  }
}
