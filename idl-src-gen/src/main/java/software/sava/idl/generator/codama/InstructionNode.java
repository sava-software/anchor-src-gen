package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

import java.util.ArrayList;
import java.util.List;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

final class InstructionNode extends NamedDocsNode {

  enum AccountStrategy {
    omitted,
    programId
  }

  private final AccountStrategy optionalAccountStrategy;
  private final List<InstructionAccountNode> accounts;
  private final List<InstructionArgumentNode> arguments;
  private final List<InstructionArgumentNode> extraArguments;
  private final List<InstructionRemainingAccountsNode> remainingAccounts;
  private final List<InstructionByteDeltaNode> byteDeltas;
  private final List<DiscriminatorNode> discriminators;
  private final List<InstructionNode> subInstructions;

  InstructionNode(final String name,
                  final List<String> docs,
                  final AccountStrategy optionalAccountStrategy,
                  final List<InstructionAccountNode> accounts,
                  final List<InstructionArgumentNode> arguments,
                  final List<InstructionArgumentNode> extraArguments,
                  final List<InstructionRemainingAccountsNode> remainingAccounts,
                  final List<InstructionByteDeltaNode> byteDeltas,
                  final List<DiscriminatorNode> discriminators,
                  final List<InstructionNode> subInstructions) {
    super(name, docs);
    this.optionalAccountStrategy = optionalAccountStrategy;
    this.accounts = accounts;
    this.arguments = arguments;
    this.extraArguments = extraArguments;
    this.remainingAccounts = remainingAccounts;
    this.byteDeltas = byteDeltas;
    this.discriminators = discriminators;
    this.subInstructions = subInstructions;
  }

  AccountStrategy optionalAccountStrategy() {
    return optionalAccountStrategy;
  }

  List<InstructionAccountNode> accounts() {
    return accounts;
  }

  List<InstructionArgumentNode> arguments() {
    return arguments;
  }

  List<InstructionArgumentNode> extraArguments() {
    return extraArguments;
  }

  List<InstructionRemainingAccountsNode> remainingAccounts() {
    return remainingAccounts;
  }

  List<InstructionByteDeltaNode> byteDeltas() {
    return byteDeltas;
  }

  List<DiscriminatorNode> discriminators() {
    return discriminators;
  }

  List<InstructionNode> subInstructions() {
    return subInstructions;
  }

  static InstructionNode parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createInstructionNode();
  }

  private static final class Parser extends BaseDocsParser {

    private AccountStrategy optionalAccountStrategy;
    private List<InstructionAccountNode> accounts;
    private List<InstructionArgumentNode> arguments;
    private List<InstructionArgumentNode> extraArguments;
    private List<InstructionRemainingAccountsNode> remainingAccounts;
    private List<InstructionByteDeltaNode> byteDeltas;
    private List<DiscriminatorNode> discriminators;
    private List<InstructionNode> subInstructions;

    private Parser() {
    }

    InstructionNode createInstructionNode() {
      return new InstructionNode(
          name,
          docs == null ? List.of() : docs,
          optionalAccountStrategy,
          accounts == null ? List.of() : accounts,
          arguments == null ? List.of() : arguments,
          extraArguments == null ? List.of() : extraArguments,
          remainingAccounts == null ? List.of() : remainingAccounts,
          byteDeltas == null ? List.of() : byteDeltas,
          discriminators == null ? List.of() : discriminators,
          subInstructions == null ? List.of() : subInstructions
      );
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("optionalAccountStrategy", buf, offset, len)) {
        optionalAccountStrategy = AccountStrategy.valueOf(ji.readString());
        return true;
      } else if (fieldEquals("accounts", buf, offset, len)) {
        accounts = new ArrayList<>();
        while (ji.readArray()) {
          accounts.add(InstructionAccountNode.parse(ji));
        }
        return true;
      } else if (fieldEquals("arguments", buf, offset, len)) {
        arguments = new ArrayList<>();
        while (ji.readArray()) {
          arguments.add(InstructionArgumentNode.parse(ji));
        }
        return true;
      } else if (fieldEquals("extraArguments", buf, offset, len)) {
        extraArguments = new ArrayList<>();
        while (ji.readArray()) {
          extraArguments.add(InstructionArgumentNode.parse(ji));
        }
        return true;
      } else if (fieldEquals("remainingAccounts", buf, offset, len)) {
        remainingAccounts = new ArrayList<>();
        while (ji.readArray()) {
          remainingAccounts.add(InstructionRemainingAccountsNode.parse(ji));
        }
        return true;
      } else if (fieldEquals("byteDeltas", buf, offset, len)) {
        byteDeltas = new ArrayList<>();
        while (ji.readArray()) {
          byteDeltas.add(InstructionByteDeltaNode.parse(ji));
        }
        return true;
      } else if (fieldEquals("discriminators", buf, offset, len)) {
        discriminators = new ArrayList<>();
        while (ji.readArray()) {
          discriminators.add(DiscriminatorNode.parse(ji));
        }
        return true;
      } else if (fieldEquals("subInstructions", buf, offset, len)) {
        subInstructions = new ArrayList<>();
        while (ji.readArray()) {
          subInstructions.add(InstructionNode.parse(ji));
        }
        return true;
      } else {
        return super.test(buf, offset, len, ji);
      }
    }
  }
}
