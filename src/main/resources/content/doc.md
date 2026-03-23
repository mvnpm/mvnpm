---
layout: doc
title: "Getting Started with mvnpm"
description: "Use NPM packages as standard Maven or Gradle dependencies. Learn how to add NPM dependencies, configure the fallback repository, and lock versions."
image: og-image.png
---

# Getting Started with mvnpm

Use NPM packages as standard Maven or Gradle dependencies.

## Add an NPM dependency

**mvnpm** lets you consume [NPM Registry](https://www.npmjs.com/) packages directly from your Java build tool.

**Maven**

```xml
<dependency>
    <groupId>org.mvnpm</groupId>
    <artifactId>\{package-name}</artifactId>
    <version>\{package-version}</version>
    <scope>\{runtime/provided}</scope>
</dependency>
```

**Gradle**

```groovy
implementation 'org.mvnpm:\{package-name}:\{package-version}'
// or compileOnly for bundled usage
```

For scoped packages, use `org.mvnpm.at.\{namespace}` as groupId
(e.g. `@hotwired/stimulus` becomes `org.mvnpm.at.hotwired:stimulus`).

## Ways to consume

- Bundled and served with the [Quarkus Web Bundler](https://docs.quarkiverse.io/quarkus-web-bundler/dev/index.html) (scope `provided`)
- Served directly using [importmaps](https://github.com/mvnpm/importmap) (scope `runtime`)
- Bundled with [esbuild-java](https://github.com/mvnpm/esbuild-java)
- As a drop-in [WebJars](https://www.webjars.org/) replacement

## Syncing a missing package

Most popular packages are already on Maven Central. Check the "Maven Central" badge on the [Browse page](/) to verify.

- Click the "Maven Central" badge to trigger a sync.
- Or configure the [fallback repository](#configuring-the-fallback-repository) to fetch missing packages automatically.

Once a package is synced, mvnpm automatically syncs new versions as they're published on NPM. Tools like Dependabot and Renovate can then propose updates in your pull requests.

> **Use Maven Central for production builds.** The fallback repository is for development and initial sync only.

## Fallback repository mode

![Diagram showing how mvnpm converts NPM packages to Maven artifacts and syncs them to Maven Central](/static/how-does-mvnpm-work.svg)

1. Your build requests a package from Maven Central.
2. If it's not there yet, the request falls through to the mvnpm repository.
3. mvnpm fetches the package from NPM and converts it into a Maven artifact (JAR + POM).
4. The artifact is returned to your build immediately and synced to Maven Central in the background.

## Configuring the fallback repository

Add mvnpm as a fallback repository so missing packages are fetched automatically.

**Maven**

Add to your `~/.m2/settings.xml`:

```xml
<settings>
    <profiles>
        <profile>
            <id>mvnpm-repo</id>
            <repositories>
                <repository>
                    <id>central</id>
                    <name>central</name>
                    <url>https://repo.maven.apache.org/maven2</url>
                </repository>
                <repository>
                    <snapshots>
                        <enabled>false</enabled>
                    </snapshots>
                    <id>mvnpm.org</id>
                    <name>mvnpm</name>
                    <url>https://repo.mvnpm.org/maven2</url>
                </repository>
            </repositories>
        </profile>
    </profiles>

    <activeProfiles>
        <activeProfile>mvnpm-repo</activeProfile>
    </activeProfiles>
</settings>
```

**Gradle**

Add to your `build.gradle`:

```groovy
repositories {
    mavenCentral()
    maven { url "https://repo.mvnpm.org/maven2" }
}
```

## Locking dependencies

**Maven**

The [mvnpm locker Maven Plugin](https://github.com/mvnpm/locker) locks your `org.mvnpm` and `org.webjars` dependency versions, similar to `package-lock.json` or `yarn.lock`.

**Gradle**

Enable native dependency locking in your `build.gradle`:

```groovy
dependencyLocking {
    lockAllConfigurations()
}
```

Then run `gradle dependencies --write-locks` to generate the lockfile.
