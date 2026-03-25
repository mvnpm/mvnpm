---
layout: doc
nav:
  order: 7
  title: About
  mobile: true
title: "About mvnpm"
description: "The story behind mvnpm, why it exists, and the ecosystem of tools that make frontend development in Java seamless."
image: og-image.png
---

# About mvnpm

Java projects needed NPM packages without Node.js tooling, so we built a Maven-native bridge.

## The story

[Phillip](https://github.com/phillip-kruger) and [Andy](https://github.com/ia3andy) kept needing NPM packages in Java without dealing with Node.js while working on [Quarkus](https://quarkus.io/). They wanted something seamless that wouldn't need two package managers (Andy had built [Quinoa](https://docs.quarkiverse.io/quarkus-quinoa/dev/index.html) before, but it still relied on Node.js). Couldn't find one, so they hacked together mvnpm to fix that.

## Why not WebJars?

[WebJars](https://www.webjars.org/) paved the way for using web libraries in Java and is still widely used. mvnpm goes further with:

- **Automatic publishing** - no need to wait for a maintainer, any NPM package is available on demand
- **Instant new versions** - versions appear as soon as they're published on NPM
- **Transitive dependency resolution** - all transitive deps in the given range are synced to ensure version resolution works correctly
- **Scoped package support** (`@org/package`)
- **Automatic version syncing to Maven Central**
- **Repository fallback** - use mvnpm as a fallback repository to fetch missing packages automatically

## The ecosystem

mvnpm is part of a stack of tools that make frontend development in Java seamless:

- **[mvnpm](https://mvnpm.org)** - the Maven-NPM bridge, converts NPM packages to Maven artifacts and syncs to Maven Central
- **[esbuild-java](https://github.com/mvnpm/esbuild-java)** - fast JavaScript/CSS bundling from Java, no Node.js required
- **[esbuild-maven-plugin](https://github.com/nickkaczmarek/esbuild-maven-plugin)** - Maven plugin wrapper for esbuild-java
- **[Quarkus Web Bundler](https://docs.quarkiverse.io/quarkus-web-bundler/dev/index.html)** - zero-config frontend bundling in Quarkus, uses esbuild-java + mvnpm under the hood
- **[mvnpm locker](https://github.com/mvnpm/locker)** - dependency version locking for `org.mvnpm` and `org.webjars`

## The vision

Building a seamless stack for developers and AI to build awesome UIs in Java. The goal is to make frontend development in Java as natural as it is in the JavaScript ecosystem. No context switching, no separate toolchains, just add a dependency and start building.

## The authors

- **Phillip Kruger** - [GitHub](https://github.com/phillip-kruger)
- **Andy Damevin** - [GitHub](https://github.com/ia3andy)

## Commonhaus Foundation

mvnpm is part of the [Commonhaus Foundation](https://www.commonhaus.org), an open-source foundation dedicated to supporting community-driven projects.
