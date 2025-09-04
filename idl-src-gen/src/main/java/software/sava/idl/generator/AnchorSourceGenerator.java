package software.sava.idl.generator;

import software.sava.anchor.AnchorUtil;
import software.sava.core.accounts.PublicKey;
import software.sava.rpc.json.http.client.SolanaRpcClient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static java.lang.System.Logger.Level.ERROR;
import static java.nio.file.StandardOpenOption.*;

public record AnchorSourceGenerator(Path sourceDirectory,
                                    String packageName,
                                    boolean exportPackages,
                                    int tabLength,
                                    AnchorIDL idl) implements Runnable {

  private static final System.Logger logger = System.getLogger(AnchorSourceGenerator.class.getName());

  static String removeBlankLines(final String str) {
    return Arrays.stream(str.split("\n"))
        .map(line -> !line.isEmpty() && line.isBlank() ? "" : line)
        .collect(Collectors.joining("\n", "", "\n"));
  }

  public static CompletableFuture<AnchorIDL> fetchIDL(final PublicKey idlAddress, final SolanaRpcClient rpcClient) {
    return rpcClient.getAccountInfo(idlAddress, OnChainIDL.FACTORY).thenApply(idlAccountInfo -> {
      final var idl = idlAccountInfo.data();
      if (idl == null) {
        return null;
      } else {
        final byte[] json = idl.json();
//            try {
//              Files.write(Path.of(idlAddress + "_idl.json"), json, CREATE, WRITE, TRUNCATE_EXISTING);
//            } catch (final IOException e) {
//              throw new UncheckedIOException(e);
//            }
        return IDL.parseIDL(json);
      }
    });
  }

  public static CompletableFuture<AnchorIDL> fetchIDLForProgram(final PublicKey programAddress,
                                                                final SolanaRpcClient rpcClient) {
    final var idlAddress = AnchorUtil.createIdlAddress(programAddress);
    return fetchIDL(idlAddress, rpcClient);
  }

  public static CompletableFuture<AnchorIDL> fetchIDL(final HttpClient httpClient, final URI idlURL) {
    final var idlRequest = HttpRequest.newBuilder().uri(idlURL).GET().build();
    return httpClient.sendAsync(idlRequest, HttpResponse.BodyHandlers.ofByteArray())
        .thenApply(response -> IDL.parseIDL(response.body()));
  }

  private static void createDirectories(final Path path) {
    try {
      Files.createDirectories(path);
    } catch (final IOException e) {
      throw new UncheckedIOException("Failed to create source directory.", e);
    }
  }

  private static void clearAndReCreateDirectory(final Path path) {
    if (Files.exists(path)) {
      try (final var stream = Files.walk(path)) {
        stream.sorted(Comparator.reverseOrder()).forEach(p -> {
          try {
            Files.delete(p);
          } catch (final IOException e) {
            throw new UncheckedIOException("Failed to delete generated source file.", e);
          }
        });
      } catch (final IOException e) {
        throw new UncheckedIOException("Failed to delete and re-create source directories.", e);
      }
    }
    createDirectories(path);
  }

  public void run() {
    var fullSrcDir = sourceDirectory;
    for (final var pkgDirectory : packageName.split("\\.")) {
      fullSrcDir = fullSrcDir.resolve(pkgDirectory);
    }
    clearAndReCreateDirectory(fullSrcDir);

    try {
      Files.write(fullSrcDir.resolve("idl.json"), idl.json(), CREATE, WRITE, TRUNCATE_EXISTING);
    } catch (final IOException e) {
      throw new UncheckedIOException("Failed to write idl json file.", e);
    }

    final var typesDir = fullSrcDir.resolve("types");
    final var typesPackage = packageName + ".types";
    createDirectories(typesDir);

    final var programName = AnchorUtil.camelCase(idl.name(), true);
    final var tab = " ".repeat(tabLength);
    final var imports = new TreeSet<String>();
    final var staticImports = new TreeSet<String>();
    final var accountMethods = HashMap.<PublicKey, AccountReferenceCall>newHashMap(1_024);
    AccountReferenceCall.generateMainNetNativeAccounts(accountMethods);

    final var definedTypes = HashMap.<String, NamedType>newHashMap(idl.types().size() + idl.accounts().size());
    definedTypes.putAll(idl.types());
    for (final var entry : idl.accounts().entrySet()) {
      final var namedType = entry.getValue();
      if (namedType.type() instanceof AnchorStruct || namedType.type() instanceof AnchorEnum) {
        if (definedTypes.put(entry.getKey(), namedType) != null) {
          throw new IllegalStateException("Defined account type name collision with defined type " + entry.getKey());
        }
      }
    }

    final var genSrcContext = new GenSrcContext(
        idl.accounts().keySet(),
        definedTypes,
        imports,
        staticImports,
        tab,
        packageName,
        typesPackage,
        programName,
        accountMethods
    );

    try {
      final var programSource = idl.generateSource(genSrcContext);
      Files.writeString(fullSrcDir.resolve(programName + "Program.java"), programSource, CREATE, TRUNCATE_EXISTING, WRITE);
    } catch (final RuntimeException ex) {
      logger.log(ERROR, "Failed to generate source for " + idl.name());
      throw ex;
    } catch (final IOException e) {
      throw new UncheckedIOException("Failed to write Program source code file.", e);
    }

    genSrcContext.clearImports();
    try {
      final var pdaSource = idl.generatePDASource(genSrcContext);
      if (pdaSource != null && !pdaSource.isBlank()) {
        try {
          Files.writeString(fullSrcDir.resolve(programName + "PDAs.java"), pdaSource, CREATE, TRUNCATE_EXISTING, WRITE);
        } catch (final IOException e) {
          throw new UncheckedIOException("Failed to write PDA source code file.", e);
        }
      }
    } catch (final RuntimeException ex) {
      logger.log(ERROR, "Failed to generate PDA source for " + idl.name());
      throw ex;
    }

    genSrcContext.clearImports();
    try {
      final var constantsSource = idl.generateConstantsSource(genSrcContext);
      if (constantsSource != null && !constantsSource.isBlank()) {
        try {
          Files.writeString(fullSrcDir.resolve(programName + "Constants.java"), constantsSource, CREATE, TRUNCATE_EXISTING, WRITE);
        } catch (final IOException e) {
          throw new UncheckedIOException("Failed to write Constants source code file.", e);
        }
      }
    } catch (final RuntimeException ex) {
      logger.log(ERROR, "Failed to generate constants source for " + idl.name());
      throw ex;
    }

    genSrcContext.clearImports();
    try {
      final var errorSource = idl.generateErrorSource(genSrcContext);
      if (errorSource != null && !errorSource.isBlank()) {
        try {
          Files.writeString(fullSrcDir.resolve(programName + "Error.java"), errorSource, CREATE, TRUNCATE_EXISTING, WRITE);
        } catch (final IOException e) {
          throw new UncheckedIOException("Failed to write error source code file.", e);
        }
      }
    } catch (final RuntimeException ex) {
      logger.log(ERROR, "Failed to generate error source for " + idl.name());
      throw ex;
    }

    final var types = idl.types();
    final var accounts = new HashSet<String>();
    for (final var account : idl.accounts().values()) {
      genSrcContext.clearImports();
      final var namedType = account.type() == null ? types.get(account.name()) : account;
      accounts.add(namedType.name());
      try {
        final var sourceCode = switch (namedType.type()) {
          case AnchorStruct struct ->
              struct.generateSource(genSrcContext, genSrcContext.typePackage(), namedType, true, account);
          case AnchorEnum anchorEnum -> anchorEnum.generateSource(genSrcContext, namedType, true);
          case null, default -> throw new IllegalStateException("Unexpected anchor defined type " + namedType);
        };
        Files.writeString(typesDir.resolve(namedType.name() + ".java"), sourceCode, CREATE, TRUNCATE_EXISTING, WRITE);
      } catch (final RuntimeException ex) {
        logger.log(ERROR, String.format("Failed to generate account %s source for %s.", namedType.name(), idl.name()));
        throw ex;
      } catch (final IOException e) {
        throw new UncheckedIOException("Failed to write Account source code file.", e);
      }
    }

    for (final var namedType : idl.types().values()) {
      if (accounts.contains(namedType.name())) {
        continue;
      }
      genSrcContext.clearImports();
      try {
        final var sourceCode = switch (namedType.type()) {
          case AnchorStruct struct ->
              struct.generateSource(genSrcContext, genSrcContext.typePackage(), namedType, false, null);
          case AnchorEnum anchorEnum -> anchorEnum.generateSource(genSrcContext, namedType);
          case AnchorVector anchorVector -> {
            logger.log(System.Logger.Level.WARNING, "Ignoring defined vector type: " + anchorVector);
            yield null;
          }
          case null, default -> throw new IllegalStateException("Unexpected anchor defined type " + namedType);
        };
        if (sourceCode != null) {
          try {
            Files.writeString(typesDir.resolve(namedType.name() + ".java"), sourceCode, CREATE, TRUNCATE_EXISTING, WRITE);
          } catch (final IOException e) {
            throw new UncheckedIOException("Failed to write source code file.", e);
          }
        }
      } catch (final RuntimeException ex) {
        logger.log(ERROR, String.format("Failed to generate type %s source for %s.", namedType.name(), idl.name()));
        throw ex;
      }
    }

    for (final var event : idl.events()) {
      genSrcContext.clearImports();
      final var namedType = event.type() == null
          ? types.get(event.name())
          : event;
      try {
        if (namedType.type() instanceof AnchorStruct struct) {
          try {
            final var sourceCode = struct.generateSource(genSrcContext, genSrcContext.typePackage(), namedType, false, null);
            Files.writeString(typesDir.resolve(namedType.name() + ".java"), sourceCode, CREATE, TRUNCATE_EXISTING, WRITE);
          } catch (final RuntimeException ex) {
            logger.log(ERROR, String.format("Failed to generate event %s source for %s.", namedType.name(), idl.name()));
            throw ex;
          }
        } else {
          throw new IllegalStateException("Unexpected anchor defined event " + namedType);
        }
      } catch (final IOException e) {
        throw new UncheckedIOException("Failed to write Event source code file.", e);
      }
    }
  }

  public void addExports(final Set<String> exports) {
    if (exportPackages) {
      exports.add(String.format("exports %s;", packageName));
      if (!idl.accounts().isEmpty() || !idl.types().isEmpty() || !idl.events().isEmpty()) {
        exports.add(String.format("exports %s.types;", packageName));
      }
    }
  }
}
