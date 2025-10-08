package software.sava.idl.generator.anchor;

import software.sava.anchor.AnchorUtil;
import software.sava.core.accounts.PublicKey;
import software.sava.idl.generator.src.NamedType;

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

import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.WARNING;
import static java.nio.file.StandardOpenOption.*;

public record AnchorSourceGenerator(Path sourceDirectory,
                                    String packageName,
                                    String commonsPackage,
                                    boolean exportPackages,
                                    int tabLength,
                                    boolean accountsHaveDiscriminators,
                                    AnchorIDL idl,
                                    Map<String, String> externalTypes) implements Runnable {

  private static final System.Logger logger = System.getLogger(AnchorSourceGenerator.class.getName());

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

  public static Path resolveAndClearSourceDirectory(final Path sourceDirectory, final String packageName) {
    final var fullSrcDir = sourceDirectory.resolve(packageName.replace(".", "/"));
    clearAndReCreateDirectory(fullSrcDir);
    return fullSrcDir;
  }

  @Override
  public void run() {
    final var fullSrcDir = resolveAndClearSourceDirectory(sourceDirectory, packageName);

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

    final var genSrcContext = new AnchorSrcGenContext(
        idl.type(),
        accountsHaveDiscriminators,
        idl.accounts().keySet(),
        externalTypes,
        definedTypes,
        imports,
        staticImports,
        tab,
        packageName,
        commonsPackage,
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
      final var accountName = account.name();
      if (genSrcContext.isExternalType(accountName)) {
        continue;
      }
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
      final var typeName = namedType.name();
      if (accounts.contains(typeName) || genSrcContext.isExternalType(typeName)) {
        continue;
      }
      genSrcContext.clearImports();
      try {
        final var sourceCode = switch (namedType.type()) {
          case AnchorStruct struct ->
              struct.generateSource(genSrcContext, genSrcContext.typePackage(), namedType, false, null);
          case AnchorEnum anchorEnum -> anchorEnum.generateSource(genSrcContext, namedType);
          case AnchorVector anchorVector -> {
            logger.log(WARNING, "Ignoring defined vector type: " + anchorVector);
            yield null;
          }
          case null, default -> throw new IllegalStateException("Unexpected anchor defined type " + namedType);
        };
        if (sourceCode != null) {
          try {
            Files.writeString(typesDir.resolve(typeName + ".java"), sourceCode, CREATE, TRUNCATE_EXISTING, WRITE);
          } catch (final IOException e) {
            throw new UncheckedIOException("Failed to write source code file.", e);
          }
        }
      } catch (final RuntimeException ex) {
        logger.log(ERROR, String.format("Failed to generate type %s source for %s.", typeName, idl.name()));
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

  private void exportTypePackage(final Set<String> exports) {
    exports.add(String.format("exports %s.types;", packageName));
  }

  public void addExports(final Set<String> exports) {
    if (exportPackages) {
      exports.add(String.format("exports %s;", packageName));
      if (!idl.events().isEmpty()) {
        exportTypePackage(exports);
      } else {
        for (final var account : idl.accounts().values()) {
          if (!externalTypes.containsKey(account.name())) {
            exportTypePackage(exports);
            return;
          }
        }
        for (final var type : idl.types().values()) {
          if (!externalTypes.containsKey(type.name())) {
            exportTypePackage(exports);
            return;
          }
        }
      }
    }
  }
}
