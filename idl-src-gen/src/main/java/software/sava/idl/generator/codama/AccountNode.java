package software.sava.idl.generator.codama;

import software.sava.idl.generator.src.SrcUtil;
import systems.comodal.jsoniter.JsonIterator;

import java.util.ArrayList;
import java.util.List;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

/// This node defines an on-chain account. It is characterized by its name, data structure,
/// and optional attributes such as PDA definition and account discriminators.
///
/// The account node describes the structure and properties of blockchain accounts, including:
/// - Data structure definition using nested type nodes
/// - Optional PDA (Program Derived Address) linking
/// - Account discriminators for distinguishing between different account types
/// - Fixed size specification when applicable
///
/// @see <a href="https://github.com/codama-idl/codama/blob/main/packages/nodes/docs/AccountNode.md">AccountNode Documentation</a>
final class AccountNode extends DefinedTypeNode implements StructNode {

  private final PdaLinkNode pda;
  private final int size;
  private final List<DiscriminatorNode> discriminators;

  AccountNode(final String name,
              final TypeNode data,
              final List<String> docs,
              final String docComments,
              final PdaLinkNode pda,
              final int size,
              final List<DiscriminatorNode> discriminators) {
    super(name, data, docs, docComments);

    this.pda = pda;
    this.size = size;
    this.discriminators = discriminators;
  }

  /// The type node that describes the account's data. Note that it must be a struct
  /// so we can access its fields via other nodes.
  ///
  /// @see <a href="https://github.com/codama-idl/codama/blob/main/packages/nodes/docs/typeNodes/NestedTypeNode.md">NestedTypeNode</a>
  /// @see <a href="https://github.com/codama-idl/codama/blob/main/packages/nodes/docs/typeNodes/StructTypeNode.md">StructTypeNode</a>
  TypeNode data() {
    return type;
  }

  /// The link node that describes the account's PDA, if its address is derived from one.
  ///
  /// @see <a href="https://github.com/codama-idl/codama/blob/main/packages/nodes/docs/linkNodes/PdaLinkNode.md">PdaLinkNode</a>
  PdaLinkNode pda() {
    return pda;
  }

  /// The size of the account in bytes, if the account's data length is fixed.
  int size() {
    return size;
  }

  /// The nodes that distinguish this account from others in the program. If multiple
  /// discriminators are provided, they are combined using a logical AND operation.
  ///
  /// @see <a href="https://github.com/codama-idl/codama/blob/main/packages/nodes/docs/discriminatorNodes/README.md">DiscriminatorNode</a>
  List<DiscriminatorNode> discriminators() {
    return discriminators;
  }

  static AccountNode parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createAccountNode();
  }

  private static final class Parser extends BaseDocsParser {

    private TypeNode data;
    private PdaLinkNode pda;
    private int size;
    private List<DiscriminatorNode> discriminators;

    private Parser() {
    }

    AccountNode createAccountNode() {
      return new AccountNode(
          name,
          data,
          docs == null ? List.of() : docs,
          docs == null ? "" : SrcUtil.formatComments(docs),
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
        pda = PdaLinkNode.parse(ji);
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

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof final AccountNode that)) return false;
    if (!super.equals(o)) return false;
    return size == that.size && pda.equals(that.pda) && discriminators.equals(that.discriminators);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + pda.hashCode();
    result = 31 * result + size;
    result = 31 * result + discriminators.hashCode();
    return result;
  }
}
