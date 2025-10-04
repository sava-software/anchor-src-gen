package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

import java.util.ArrayList;
import java.util.List;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

public final class AccountNode extends NamedDocsNode {

  private final TypeNode data;
  private final LinkNode pda;
  private final int size;
  private final List<DiscriminatorNode> discriminators;

  public AccountNode(final String name,
                     final List<String> docs,
                     final TypeNode data,
                     final LinkNode pda,
                     final int size,
                     final List<DiscriminatorNode> discriminators) {
    super(name, docs);
    this.data = data;
    this.pda = pda;
    this.size = size;
    this.discriminators = discriminators;
  }

  public TypeNode data() {
    return data;
  }

  public LinkNode pda() {
    return pda;
  }

  public int size() {
    return size;
  }

  public List<DiscriminatorNode> discriminators() {
    return discriminators;
  }

  public static AccountNode parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createAccountNode();
  }

  static final class Parser extends BaseDocsParser {

    private TypeNode data;
    private LinkNode pda;
    private int size;
    private List<DiscriminatorNode> discriminators;

    private Parser() {
    }

    AccountNode createAccountNode() {
      return new AccountNode(
          name,
          docs == null ? List.of() : docs,
          data,
          pda,
          size,
          discriminators == null ? List.of() : discriminators
      );
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("data", buf, offset, len)) {
        data = TypeNode.parse(ji);
        return true;
      } else if (fieldEquals("pda", buf, offset, len)) {
        pda = LinkNode.parse(ji);
        return true;
      } else if (fieldEquals("size", buf, offset, len)) {
        size = ji.readInt();
        return true;
      } else if (fieldEquals("discriminators", buf, offset, len)) {
        discriminators = new ArrayList<>();
        while (ji.readArray()) {
          discriminators.add(DiscriminatorNode.parse(ji));
        }
        return true;
      } else {
        return super.test(buf, offset, len, ji);
      }
    }
  }
}
