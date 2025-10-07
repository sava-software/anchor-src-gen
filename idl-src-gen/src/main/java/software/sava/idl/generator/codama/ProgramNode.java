package software.sava.idl.generator.codama;

import software.sava.core.accounts.PublicKey;
import software.sava.rpc.json.PublicKeyEncoding;
import systems.comodal.jsoniter.JsonIterator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
  private final Map<String, AccountNode> accountMap;
  private final List<InstructionNode> instructions;
  private final List<DefinedTypeNode> definedTypes;
  private final Map<String, DefinedTypeNode> definedTypeMap;
  private final List<PdaNode> pdas;
  private final List<ErrorNode> errors;

  ProgramNode(final String name,
              final List<String> docs,
              final PublicKey publicKey,
              final String version,
              final Origin origin,
              final List<AccountNode> accounts,
              final Map<String, AccountNode> accountMap,
              final List<InstructionNode> instructions,
              final List<DefinedTypeNode> definedTypes,
              final Map<String, DefinedTypeNode> definedTypeMap,
              final List<PdaNode> pdas,
              final List<ErrorNode> errors) {
    super(name, docs);
    this.publicKey = publicKey;
    this.version = version;
    this.origin = origin;
    this.accounts = accounts;
    this.accountMap = accountMap;
    this.instructions = instructions;
    this.definedTypes = definedTypes;
    this.definedTypeMap = definedTypeMap;
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

  AccountNode account(final String name) {
    return accountMap.get(name);
  }

  List<InstructionNode> instructions() {
    return instructions;
  }

  List<DefinedTypeNode> definedTypes() {
    return definedTypes;
  }

  DefinedTypeNode definedType(final String name) {
    return definedTypeMap.get(name);
  }

  boolean isDefinedType(final String typeName) {
    return definedTypeMap.containsKey(typeName);
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
    private Map<String, AccountNode> accountMap;
    private List<InstructionNode> instructions;
    private List<DefinedTypeNode> definedTypes;
    private Map<String, DefinedTypeNode> definedTypeMap;
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
          accountMap == null ? Map.of() : accountMap,
          instructions == null ? List.of() : instructions,
          definedTypes == null ? List.of() : definedTypes,
          definedTypeMap == null ? Map.of() : definedTypeMap,
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
        final var accounts = new ArrayList<AccountNode>();
        while (ji.readArray()) {
          accounts.add(AccountNode.parse(ji));
        }
        this.accounts = List.copyOf(accounts);
        this.accountMap = HashMap.newHashMap(accounts.size());
        for (final var account : accounts) {
          this.accountMap.put(account.name(), account);
        }
        return true;
      } else if (fieldEquals("instructions", buf, offset, len)) {
        final var instructions = new ArrayList<InstructionNode>();
        while (ji.readArray()) {
          instructions.add(InstructionNode.parse(ji));
        }
        this.instructions = List.copyOf(instructions);
        return true;
      } else if (fieldEquals("definedTypes", buf, offset, len)) {
        final var definedTypes = new ArrayList<DefinedTypeNode>();
        while (ji.readArray()) {
          definedTypes.add(DefinedTypeNode.parse(ji));
        }
        this.definedTypes = List.copyOf(definedTypes);
        this.definedTypeMap = HashMap.newHashMap(definedTypes.size());
        for (final var definedType : definedTypes) {
          this.definedTypeMap.put(definedType.name(), definedType);
        }
        return true;
      } else if (fieldEquals("pdas", buf, offset, len)) {
        final var pdas = new ArrayList<PdaNode>();
        while (ji.readArray()) {
          pdas.add(PdaNode.parse(ji));
        }
        this.pdas = List.copyOf(pdas);
        return true;
      } else if (fieldEquals("errors", buf, offset, len)) {
        final var errors = new ArrayList<ErrorNode>();
        while (ji.readArray()) {
          errors.add(ErrorNode.parse(ji));
        }
        this.errors = List.copyOf(errors);
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
