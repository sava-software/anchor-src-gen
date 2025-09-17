#!/usr/bin/env bash

set -euo pipefail

# Ensure we execute from the repository root regardless of caller location
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

readonly moduleName="software.sava.idl_src_gen"
readonly package="software.sava.anchor.gen"
readonly mainClass="software.sava.idl.generator.Entrypoint"
projectDirectory="$(pwd)"
readonly projectDirectory

print_help() {
  cat <<'EOF'
Usage: ./genSrc.sh [options]

Generates Java sources for Anchor programs using the idl-src-gen tool.

Options:
  --baseDelayMillis=N, --bdm=N
      Base backoff delay in milliseconds for network calls. Default: 200

  --basePackageName=PKG, --bp=PKG
      Java base package where generated code will be placed. Default: software.sava.anchor.gen

  --exportPackages=true|false, --ep=true|false
      Whether to export generated packages in the JPMS module-info. Default: true

  --help, -h
      Show this help message and exit.

  --javaArgs=ARG1,ARG2,..., --ja=ARG1,ARG2,...
      Override the initial JVM arguments used to launch the generator. Provide a comma-separated list.
      Examples:
        --javaArgs=-Xms512M,-Xmx2G,-XX:+UseZGC
        --javaArgs=           (clears all initial args)

  --log=LEVEL, --l=LEVEL
      Log level passed to the generator. One of: INFO, WARN, DEBUG. Default: INFO

  --moduleName=org.your.module, --mn=org.your.module
      Override the output module name for the generated sources. Default: software.sava.idl_src_gen

  --numThreads=N, --nt=N
      Number of worker threads used by the generator. Default: 5

  --programs=PATH, --p=PATH
      Path to a JSON file listing on-chain programs to generate from. Default: ./main_net_programs.json

  --rpc=URL
      RPC endpoint URL to use when fetching IDLs or on-chain data. Default: https://api.mainnet-beta.solana.com

  --screen[=screen]|--screen=0
      When provided as "--screen" (or "--screen=screen"), runs the generator inside a GNU screen session named "idl-src-gen".
      Use "--screen=0" to run in the foreground (default).

  --sourceDirectory=PATH, --sd=PATH
      Source directory relative to the project root where Java will be written. Default: src/main/java

  --tabLength=N, --tl=N
      Number of spaces for indentation in generated sources. Default: 2

Example:
  - ./genSrc.sh --bp=com.example.gen --programs=./programs.json --rpc=https://api.mainnet-beta.solana.com
EOF
}

javaArgs=(
  '-Xms256M'
  '-Xmx1024M'
)
screen=0;
logLevel="INFO";
tabLength=2;
sourceDirectory="src/main/java";
outputModuleName="";
basePackageName="$package";
exportPackages="true";
rpc="";
programs="./main_net_programs.json";
numThreads=5;
baseDelayMillis=200;

for arg in "$@"; do
  if [[ "$arg" == "--help" || "$arg" == "-h" ]]; then
    print_help
    exit 0
  fi
done

for arg in "$@"
do
  if [[ "$arg" =~ ^--.* ]]; then
    key="${arg%%=*}"
    key="${key##*--}"
    val="${arg#*=}"

    case "$key" in
      ja | javaArgs)
        # Override initial JVM args with a comma-separated list provided by the user
        IFS=',' read -r -a __userJavaArgs <<< "$val"
        javaArgs=()
        for __arg in "${__userJavaArgs[@]}"; do
          # Trim leading/trailing whitespace
          __trimmed="${__arg#"${__arg%%[![:space:]]*}"}"
          __trimmed="${__trimmed%"${__trimmed##*[![:space:]]}"}"
          [[ -n "$__trimmed" ]] && javaArgs+=("$__trimmed")
        done
        ;;
      l | log)
          case "$val" in
            INFO|WARN|DEBUG) logLevel="$val";;
            *)
              printf "'%slog=[INFO|WARN|DEBUG]' not '%s'. Use --help for details.\n" "--" "$arg";
              exit 2;
            ;;
          esac
          javaArgs+=("-D$moduleName.logLevel=$logLevel")
        ;;

      screen)
        case "$val" in
          1|*screen) screen=1 ;;
          0) screen=0 ;;
          *)
            printf "'%sscreen=[0|1]' or '%sscreen' not '%s'. Use --help for details.\n" "--" "--" "$arg";
            exit 2;
          ;;
        esac
        ;;

      bdm | baseDelayMillis) baseDelayMillis="$val";;
      bp | basePackageName) basePackageName="$val";;
      ep | exportPackages) exportPackages="$val";;
      mn | moduleName) outputModuleName="$val";;
      nt | numThreads) numThreads="$val";;
      p | programs) programs="$val";;
      rpc) rpc="$val";;
      sd | sourceDirectory) sourceDirectory="$val";;
      tl | tabLength) tabLength="$val";;

      *)
          printf "Unsupported flag '%s' [key=%s] [val=%s]. Use --help for usage.\n" "$arg" "$key" "$val";
          exit 1;
        ;;
    esac
  else
    printf "Unhandled argument '%s', all flags must begin with '%s'. Use --help for usage.\n" "$arg" "--";
    exit 1;
  fi
done

readonly javaExe="$projectDirectory/idl-src-gen/build/images/idl-src-gen/bin/java"

# Build the image only if the java executable doesn't exist yet
if [[ ! -x "$javaExe" ]]; then
  if [[ -x "$projectDirectory/compile.sh" ]]; then
    ./compile.sh
  else
    echo "Missing generator image and compile.sh is not executable. Please run './gradlew :idl-src-gen:image' or make compile.sh executable." >&2
    exit 4
  fi
fi

javaArgs+=(
  "-D$moduleName.baseDelayMillis=$baseDelayMillis"
  "-D$moduleName.basePackageName=$basePackageName"
  "-D$moduleName.exportPackages=$exportPackages"
  "-D$moduleName.moduleName=$outputModuleName"
  "-D$moduleName.numThreads=$numThreads"
  "-D$moduleName.programs=$programs"
  "-D$moduleName.rpc=$rpc"
  "-D$moduleName.sourceDirectory=$sourceDirectory"
  "-D$moduleName.tabLength=$tabLength"
  '-m' "$moduleName/$mainClass"
)

if [[ "$screen" == 0 ]]; then
  set -x
  "$javaExe" "${javaArgs[@]}"
else
  set -x
  screen -S "idl-src-gen" "$javaExe" "${javaArgs[@]}"
fi
