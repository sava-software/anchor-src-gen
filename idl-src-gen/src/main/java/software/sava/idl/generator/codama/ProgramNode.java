package software.sava.idl.generator.codama;

import software.sava.core.accounts.PublicKey;
import software.sava.rpc.json.PublicKeyEncoding;
import systems.comodal.jsoniter.JsonIterator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

final class ProgramNode extends NamedDocsNode {

  enum Origin {
    anchor,
    shank
  }

  private final PublicKey publicKey;
  private final String version;
  private final Origin origin;
  private final List<AccountNode> accounts;
  private final List<InstructionNode> instructions;
  private final List<DefinedTypeNode> definedTypes;
  private final List<PdaNode> pdas;
  private final List<ErrorNode> errors;

  ProgramNode(final String name,
              final List<String> docs,
              final PublicKey publicKey,
              final String version,
              final Origin origin,
              final List<AccountNode> accounts,
              final List<InstructionNode> instructions,
              final List<DefinedTypeNode> definedTypes,
              final List<PdaNode> pdas,
              final List<ErrorNode> errors) {
    super(name, docs);
    this.publicKey = publicKey;
    this.version = version;
    this.origin = origin;
    this.accounts = accounts;
    this.instructions = instructions;
    this.definedTypes = definedTypes;
    this.pdas = pdas;
    this.errors = errors;
  }

  PublicKey publicKey() {
    return publicKey;
  }

  String version() {
    return version;
  }

  Origin origin() {
    return origin;
  }

  List<AccountNode> accounts() {
    return accounts;
  }

  List<InstructionNode> instructions() {
    return instructions;
  }

  List<DefinedTypeNode> definedTypes() {
    return definedTypes;
  }

  List<PdaNode> pdas() {
    return pdas;
  }

  List<ErrorNode> errors() {
    return errors;
  }

  static ProgramNode parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createProgramNode();
  }

  private static final class Parser extends BaseDocsParser {

    private PublicKey publicKey;
    private String version;
    private Origin origin;
    private List<AccountNode> accounts;
    private List<InstructionNode> instructions;
    private List<DefinedTypeNode> definedTypes;
    private List<PdaNode> pdas;
    private List<ErrorNode> errors;

    private Parser() {
    }

    ProgramNode createProgramNode() {
      return new ProgramNode(
          name,
          docs == null ? List.of() : docs,
          publicKey,
          version,
          origin,
          accounts == null ? List.of() : accounts,
          instructions == null ? List.of() : instructions,
          definedTypes == null ? List.of() : definedTypes,
          pdas == null ? List.of() : pdas,
          errors == null ? List.of() : errors
      );
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("publicKey", buf, offset, len)) {
        publicKey = PublicKeyEncoding.parseBase58Encoded(ji);
        return true;
      } else if (fieldEquals("version", buf, offset, len)) {
        version = ji.readString();
        return true;
      } else if (fieldEquals("origin", buf, offset, len)) {
        origin = Origin.valueOf(ji.readString());
        return true;
      } else if (fieldEquals("accounts", buf, offset, len)) {
        accounts = new ArrayList<>();
        while (ji.readArray()) {
          accounts.add(AccountNode.parse(ji));
        }
        return true;
      } else if (fieldEquals("instructions", buf, offset, len)) {
        instructions = new ArrayList<>();
        while (ji.readArray()) {
          instructions.add(InstructionNode.parse(ji));
        }
        return true;
      } else if (fieldEquals("definedTypes", buf, offset, len)) {
        definedTypes = new ArrayList<>();
        while (ji.readArray()) {
          definedTypes.add(DefinedTypeNode.parse(ji));
        }
        return true;
      } else if (fieldEquals("pdas", buf, offset, len)) {
        pdas = new ArrayList<>();
        while (ji.readArray()) {
          pdas.add(PdaNode.parse(ji));
        }
        return true;
      } else if (fieldEquals("errors", buf, offset, len)) {
        errors = new ArrayList<>();
        while (ji.readArray()) {
          errors.add(ErrorNode.parse(ji));
        }
        return true;
      } else {
        return super.test(buf, offset, len, ji);
      }
    }
  }

  static void main() throws IOException {
    final var rootNode = RootNode.parse(JsonIterator.parse(Files.readAllBytes(Path.of("idls/program_metadata.json"))));
    System.out.println(rootNode);
  }
}
