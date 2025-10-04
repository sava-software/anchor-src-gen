package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

import java.util.List;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

public final class AccountNode extends NamedDocsNode {

  private final NestedTypeNode data;
  private final PdaLinkNode pda;

  public AccountNode(final String name,
                     final List<String> docs,
                     final NestedTypeNode data,
                     final PdaLinkNode pda) {
    super(name, docs);
    this.data = data;
    this.pda = pda;
  }

  public NestedTypeNode data() {
    return data;
  }

  public PdaLinkNode pda() {
    return pda;
  }

  public static AccountNode parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createAccountNode();
  }

  static final class Parser extends BaseDocsParser {

    private NestedTypeNode data;
    private PdaLinkNode pda;

    private Parser() {
    }

    AccountNode createAccountNode() {
      return new AccountNode(
          name,
          docs == null ? List.of() : docs,
          data,
          pda
      );
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("data", buf, offset, len)) {
        data = TypeNode.parseNestedTypeNode(ji, StructTypeNode::parse);
      } else if (fieldEquals("pda", buf, offset, len)) {
        pda = PdaLinkNode.parse(ji);
      } else {
        return super.test(buf, offset, len, ji);
      }
      return true;
    }
  }
}
