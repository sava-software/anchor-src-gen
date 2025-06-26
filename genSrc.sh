#!/usr/bin/env bash

set -e

readonly moduleName="software.sava.anchor_src_gen"
readonly package="software.sava.anchor.gen"
readonly mainClass="software.sava.anchor.Entrypoint"
projectDirectory="$(pwd)"
readonly projectDirectory

javaArgs=(
  '--enable-preview'
  '-XX:+UseZGC'
  '-Xms256M'
  '-Xmx1024M'
)

screen=0;
targetJavaVersion=24
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

for arg in "$@"
do
  if [[ "$arg" =~ ^--.* ]]; then
    key="${arg%%=*}"
    key="${key##*--}"
    val="${arg#*=}"

    case "$key" in
      l | log)
          case "$val" in
            INFO|WARN|DEBUG) logLevel="$val";;
            *)
              printf "'%slog=[INFO|WARN|DEBUG]' not '%s'.\n" "--" "$arg";
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
            printf "'%sscreen=[0|1]' or '%sscreen' not '%s'.\n" "--" "--" "$arg";
            exit 2;
          ;;
        esac
        ;;

      tjv | targetJavaVersion) targetJavaVersion="$val";;

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
          printf "Unsupported flag '%s' [key=%s] [val=%s].\n" "$arg" "$key" "$val";
          exit 1;
        ;;
    esac
  else
    printf "Unhandled argument '%s', all flags must begin with '%s'.\n" "$arg" "--";
    exit 1;
  fi
done

javaVersion=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | grep -oEi '^[0-9]+')
readonly javaVersion
if [[ "$javaVersion" -ne "$targetJavaVersion" ]]; then
  echo "Invalid Java version $javaVersion must be $targetJavaVersion."
  exit 3
fi

./gradlew --stacktrace clean :anchor-src-gen:image

readonly javaExe="$projectDirectory/anchor-src-gen/build/images/anchor-src-gen/bin/java"

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
  screen -S "anchor-src-gen" "$javaExe" "${javaArgs[@]}"
fi
