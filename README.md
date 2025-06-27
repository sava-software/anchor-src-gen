![](https://github.com/sava-software/sava/blob/003cf88b3cd2a05279027557f23f7698662d2999/assets/images/solana_java_cup.svg)

# Anchor Source Generator [![Gradle Check](https://github.com/sava-software/anchor-src-gen/actions/workflows/build.yml/badge.svg)](https://github.com/sava-software/anchor-src-gen/actions/workflows/build.yml) [![Publish Release](https://github.com/sava-software/anchor-src-gen/actions/workflows/publish.yml/badge.svg)](https://github.com/sava-software/anchor-src-gen/actions/workflows/publish.yml)

## Documentation

User documentation lives at [sava.software](https://sava.software/).

* [Dependency Configuration](https://sava.software/quickstart)
* [IDL Source Generator](https://sava.software/utilities/anchor-src-gen)

## Build

[Generate a classic token](https://github.com/settings/tokens) with the `read:packages` scope needed to access
dependencies hosted on GitHub Package Repository.

#### ~/.gradle/gradle.properties

```properties
savaGithubPackagesUsername=GITHUB_USERNAME
savaGithubPackagesPassword=GITHUB_TOKEN
```

```shell
./gradlew check
```
