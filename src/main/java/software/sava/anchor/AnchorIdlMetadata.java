package software.sava.anchor;

import software.sava.core.accounts.PublicKey;
import software.sava.rpc.json.PublicKeyEncoding;
import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;

import java.util.List;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

public record AnchorIdlMetadata(String name,
                                String version,
                                String spec,
                                String description,
                                String repository,
                                List<AnchorIdlDependency> dependencies,
                                String contact,
                                List<AnchorIdlDeployments> deployments,
                                PublicKey address,
                                String origin) {

  static AnchorIdlMetadata parseMetadata(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createMetadata();
  }

  private static final class Parser implements FieldBufferPredicate {

    private String name;
    private String version;
    private String spec;
    private String description;
    private String repository;
    private List<AnchorIdlDependency> dependencies;
    private String contact;
    private List<AnchorIdlDeployments> deployments;
    private PublicKey address;
    private String origin;

    private Parser() {
    }

    private AnchorIdlMetadata createMetadata() {
      return new AnchorIdlMetadata(
          name,
          version,
          spec,
          description,
          repository,
          dependencies,
          contact,
          deployments,
          address,
          origin
      );
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("name", buf, offset, len)) {
        this.name = ji.readString();
      } else if (fieldEquals("version", buf, offset, len)) {
        this.version = ji.readString();
      } else if (fieldEquals("spec", buf, offset, len)) {
        this.spec = ji.readString();
      } else if (fieldEquals("description", buf, offset, len)) {
        this.description = ji.readString();
      } else if (fieldEquals("repository", buf, offset, len)) {
        this.repository = ji.readString();
      } else if (fieldEquals("dependencies", buf, offset, len)) {
        this.dependencies = AnchorIdlDependency.parseDependencies(ji);
      } else if (fieldEquals("contact", buf, offset, len)) {
        this.contact = ji.readString();
      } else if (fieldEquals("deployments", buf, offset, len)) {
        this.deployments = AnchorIdlDeployments.parseDeployments(ji);
      } else if (fieldEquals("address", buf, offset, len)) {
        this.address = PublicKeyEncoding.parseBase58Encoded(ji);
      } else if (fieldEquals("origin", buf, offset, len)) {
        this.origin = ji.readString();
      } else {
        throw new IllegalStateException("Unhandled AnchorIdlMetadata field [" + new String(buf, offset, len) + ']');
      }
      return true;
    }
  }
}
