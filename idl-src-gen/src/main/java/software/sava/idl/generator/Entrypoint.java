package software.sava.idl.generator;

import software.sava.anchor.AnchorUtil;
import software.sava.core.accounts.PublicKey;
import software.sava.core.tx.Instruction;
import software.sava.rpc.json.PublicKeyEncoding;
import software.sava.rpc.json.http.SolanaNetwork;
import software.sava.rpc.json.http.client.SolanaRpcClient;
import software.sava.rpc.json.http.response.AccountInfo;
import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongBinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.WARNING;
import static java.nio.file.StandardOpenOption.*;
import static java.util.Objects.requireNonNullElse;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static software.sava.idl.generator.AnchorSourceGenerator.resolveAndClearSourceDirectory;
import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

public final class Entrypoint extends Thread {

  private static final System.Logger logger = System.getLogger(Entrypoint.class.getName());
  private static final LongBinaryOperator MAX = Long::max;

  private final Semaphore semaphore;
  private final ConcurrentLinkedQueue<ProgramConfig> tasks;
  private final AtomicLong errorCount;
  private final long baseDelayMillis;
  private final AtomicLong latestCall;
  private final SolanaRpcClient rpcClient;
  private final Map<PublicKey, AccountInfo<byte[]>> idlAccounts;
  private final Path sourceDirectory;
  private final String basePackageName;
  private final String commonsPackage;
  private final Set<String> exports;
  private final int tabLength;

  private Entrypoint(final Semaphore semaphore,
                     final ConcurrentLinkedQueue<ProgramConfig> tasks,
                     final AtomicLong errorCount,
                     final long baseDelayMillis,
                     final AtomicLong latestCall,
                     final SolanaRpcClient rpcClient,
                     final Map<PublicKey, AccountInfo<byte[]>> idlAccounts,
                     final Path sourceDirectory,
                     final String basePackageName, final String commonsPackage,
                     final Set<String> exports,
                     final int tabLength) {
    this.semaphore = semaphore;
    this.tasks = tasks;
    this.errorCount = errorCount;
    this.baseDelayMillis = baseDelayMillis;
    this.latestCall = latestCall;
    this.rpcClient = rpcClient;
    this.idlAccounts = idlAccounts;
    this.sourceDirectory = sourceDirectory;
    this.basePackageName = basePackageName;
    this.commonsPackage = commonsPackage;
    this.exports = exports;
    this.tabLength = tabLength;
  }

  @Override
  public void run() {
    ProgramConfig task = null;
    AnchorIDL idl;
    for (long delayMillis, latestCall, now, sleep; ; ) {
      try {
        task = this.tasks.poll();
        if (task == null) {
          return;
        }
        if (task.remoteIDL()) {
          this.semaphore.acquire();
          delayMillis = this.baseDelayMillis * (this.errorCount.get() + 1);
          latestCall = this.latestCall.get();
          now = System.currentTimeMillis();
          sleep = (latestCall + delayMillis) - now;
          if (sleep > 0) {
            MILLISECONDS.sleep(sleep);
            now = System.currentTimeMillis();
          }
          this.latestCall.getAndAccumulate(now, MAX);
        }
        idl = task.fetchIDL(rpcClient.httpClient(), idlAccounts);
        if (idl == null) {
          continue;
        }
      } catch (final RuntimeException e) {
        logger.log(ERROR, "Failed to generate IDL for " + task, e);
        this.errorCount.getAndUpdate(x -> x < 100 ? x + 1 : x);
        this.tasks.add(task);
        return;
      } catch (final InterruptedException e) {
        throw new RuntimeException(e);
      } finally {
        this.semaphore.release();
      }
      this.errorCount.getAndUpdate(x -> x > 0 ? x - 1 : x);

      final var packageName = task.formatPackage(basePackageName);
      final var generator = new AnchorSourceGenerator(
          sourceDirectory,
          packageName,
          commonsPackage,
          task.exportPackages(),
          tabLength,
          idl
      );
      generator.run();
      generator.addExports(exports);
    }
  }

  private static String mandatoryProperty(final String key) {
    return Objects.requireNonNull(System.getProperty(key, "Must pass property "), key);
  }

  private static String propertyOrElse(final String key, final String orElse) {
    final var property = System.getProperty(key);
    return property == null || property.isBlank() ? orElse : property;
  }

  private record ProgramConfig(String name,
                               String packageName,
                               boolean exportPackages,
                               PublicKey programAddress,
                               PublicKey idlAddress,
                               URI idlURL,
                               Path idlFile) {

    String formatPackage(final String basePackageName) {
      return String.format("%s.%s.anchor", basePackageName, packageName);
    }

    boolean remoteIDL() {
      return idlURL != null;
    }

    AnchorIDL fetchIDL(final HttpClient httpClient, final Map<PublicKey, AccountInfo<byte[]>> idlAccounts) {
      if (idlURL != null) {
        return AnchorSourceGenerator.fetchIDL(httpClient, idlURL).join();
      } else if (idlFile != null) {
        try {
          return IDL.parseIDL(Files.readAllBytes(idlFile));
        } catch (final IOException e) {
          throw new UncheckedIOException(e);
        }
      } else {
        final var accountInfo = idlAccounts.get(idlAddress);
        if (accountInfo == null) {
          logger.log(WARNING, String.format(
                  "Failed to find an IDL for %s using a program address %s at the IDL address %s.",
                  name, programAddress, idlAddress
              )
          );
          return null;
        } else {
          final var onChainIDL = OnChainIDL.FACTORY.apply(accountInfo.pubKey(), accountInfo.data());
          return IDL.parseIDL(onChainIDL.json());
        }
      }
    }

    public static void parseConfigs(final boolean exportPackages,
                                    final Collection<ProgramConfig> configs,
                                    final JsonIterator ji) {
      while (ji.readArray()) {
        final var parser = new Parser(exportPackages);
        ji.testObject(parser);
        configs.add(parser.createConfig());
      }
    }

    private static final class Parser implements FieldBufferPredicate {

      private String name;
      private String packageName;
      private boolean exportPackages;
      private PublicKey programAddress;
      private URI idlURL;
      private Path idlFile;

      private Parser(boolean exportPackages) {
        this.exportPackages = exportPackages;
      }

      private ProgramConfig createConfig() {
        return new ProgramConfig(
            name,
            requireNonNullElse(packageName, name.toLowerCase(Locale.ENGLISH)),
            exportPackages,
            programAddress,
            AnchorUtil.createIdlAddress(programAddress),
            idlURL,
            idlFile
        );
      }

      @Override
      public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
        if (fieldEquals("name", buf, offset, len)) {
          name = ji.readString();
        } else if (fieldEquals("package", buf, offset, len)) {
          packageName = ji.readString();
        } else if (fieldEquals("exportPackages", buf, offset, len)) {
          exportPackages = ji.readBoolean();
        } else if (fieldEquals("program", buf, offset, len)) {
          programAddress = PublicKeyEncoding.parseBase58Encoded(ji);
        } else if (fieldEquals("idlURL", buf, offset, len)) {
          idlURL = java.net.URI.create(ji.readString());
        } else if (fieldEquals("idlFile", buf, offset, len)) {
          idlFile = Path.of(ji.readString());
        } else {
          ji.skip();
        }
        return true;
      }
    }
  }

  public static void main(final String[] args) throws InterruptedException, IOException {
    final var clas = Entrypoint.class;
    final var moduleName = clas.getModule().getName();
    final int tabLength = Integer.parseInt(propertyOrElse(
        moduleName + ".tabLength",
        "2"
    ));
    final var sourceDirectory = Path.of(propertyOrElse(moduleName + ".sourceDirectory", "anchor-programs/src/main/java")).toAbsolutePath();
    final var outputModuleName = propertyOrElse(moduleName + ".moduleName", moduleName);
    final var basePackageName = propertyOrElse(moduleName + ".basePackageName", clas.getPackageName());
    final var commonsPackage = basePackageName + "._commons";
    final var exportPackages = Boolean.parseBoolean(propertyOrElse(moduleName + ".exportPackages", "true"));
    final var rpcEndpoint = System.getProperty(moduleName + ".rpc");
    final var programsJsonFile = mandatoryProperty(moduleName + ".programs");
    final int numThreads = Integer.parseInt(propertyOrElse(moduleName + ".numThreads", "5"));
    final int baseDelayMillis = Integer.parseInt(propertyOrElse(moduleName + ".baseDelayMillis", "200"));

    final var tasks = new ConcurrentLinkedQueue<ProgramConfig>();
    try (final var ji = JsonIterator.parse(Files.readAllBytes(Path.of(programsJsonFile)))) {
      ProgramConfig.parseConfigs(exportPackages, tasks, ji);
    }

    final var idlAccountKeys = tasks.stream().<PublicKey>mapMulti((programConfig, downstream) -> {
      final var idlAddress = programConfig.idlAddress();
      if (idlAddress != null) {
        downstream.accept(idlAddress);
      }
    }).toList();

    try (final var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      try (final var httpClient = HttpClient.newBuilder().executor(executor).build()) {
        final var rpcClient = SolanaRpcClient.createClient(
            rpcEndpoint == null || rpcEndpoint.isBlank() ? SolanaNetwork.MAIN_NET.getEndpoint() : URI.create(rpcEndpoint),
            httpClient
        );

        final Map<PublicKey, AccountInfo<byte[]>> idlAccounts;
        if (idlAccountKeys.isEmpty()) {
          idlAccounts = Map.of();
        } else {
          idlAccounts = HashMap.newHashMap(idlAccountKeys.size());
          for (long errorCount = 0; ; ) {
            try {
              final var accountInfoList = rpcClient.getAccounts(idlAccountKeys).join();
              for (final var accountInfo : accountInfoList) {
                if (accountInfo != null) {
                  idlAccounts.put(accountInfo.pubKey(), accountInfo);
                }
              }
              break;
            } catch (final RuntimeException e) {
              final long delay = Math.max(21, ++errorCount);
              logger.log(ERROR, String.format(
                      "Failed to fetch %d accounts %d times, retrying in %d seconds.",
                      idlAccountKeys.size(), ++errorCount, delay
                  ), e
              );
              SECONDS.sleep(delay);
            }
          }
        }

        final var semaphore = new Semaphore(numThreads, false);
        final var errorCount = new AtomicLong();
        final var latestCall = new AtomicLong();
        final var exports = new ConcurrentSkipListSet<String>();
        final var threads = IntStream.range(0, numThreads).mapToObj(_ -> new Entrypoint(
            semaphore, tasks, errorCount, baseDelayMillis, latestCall,
            rpcClient,
            idlAccounts,
            sourceDirectory, basePackageName, commonsPackage,
            exports,
            tabLength
        )).toList();
        threads.forEach(Thread::start);

        final Path moduleFilePath;
        final StringBuilder moduleFileBuilder;
        if (outputModuleName.equals(moduleName)) {
          moduleFileBuilder = null;
          moduleFilePath = null;
        } else {
          moduleFileBuilder = new StringBuilder(2_048);
          exports.add(String.format("exports %s;", commonsPackage));
          exports.add(String.format("requires %s;", HttpClient.class.getModule().getName()));
          exports.add(String.format("requires transitive %s;", JsonIterator.class.getModule().getName()));
          exports.add(String.format("requires transitive %s;", Instruction.class.getModule().getName()));
          exports.add(String.format("requires transitive %s;", SolanaRpcClient.class.getModule().getName()));
          exports.add(String.format("requires %s;", System.class.getModule().getName()));
          moduleFilePath = sourceDirectory.resolve("module-info.java");
          if (Files.exists(moduleFilePath)) {
            try (final var moduleFileLines = Files.lines(moduleFilePath)) {
              moduleFileLines
                  .map(String::strip)
                  .filter(line -> !line.isBlank() && !line.startsWith("module") && !line.equals("}"))
                  .forEach(exports::add);
            }
          }

          moduleFileBuilder.append(String.format("module %s {%n", outputModuleName));
        }

        final var commonsSrcDir = resolveAndClearSourceDirectory(sourceDirectory, commonsPackage);
        Files.writeString(
            commonsSrcDir.resolve("ProgramError.java"), String.format("""
                package %s;
                
                public interface ProgramError {
                
                  int code();
                
                  String msg();
                }
                """, commonsPackage
            ), CREATE, TRUNCATE_EXISTING, WRITE
        );

        for (final var thread : threads) {
          thread.join();
        }

        if (moduleFileBuilder != null) {
          moduleFileBuilder.append(exports.stream().sorted(String::compareToIgnoreCase).collect(Collectors.joining("\n")).indent(tabLength));
          moduleFileBuilder.append('}').append('\n');
          Files.writeString(moduleFilePath, moduleFileBuilder.toString(), CREATE, TRUNCATE_EXISTING, WRITE);
        }
      }
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
