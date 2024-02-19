![banner](https://github.com/mvnpm/mvnpm/assets/6836179/787a3974-0b9a-4809-a74d-3710c6d08229)

Consume the NPM packages directly from Maven and Gradle projects.

A lot of packages are already synced on Central, you may check this from [mvnpm.org](mvnpm.org): <img height="30" alt="image" src="https://github.com/mvnpm/mvnpm/assets/2223984/e1c9b820-d9f9-43f5-a61d-6b0514f0efe1">

If it's not:
- Configure your project to use the MVNPM Maven Repository as a fallback. When a package is missing, it will fetch it from the fallback repository and automatically trigger a sync with Maven Central.
- Click to trigger a sync with Maven Central: <img height="30" alt="image" src="https://github.com/mvnpm/mvnpm/assets/2223984/32a30bc6-2d4a-47a8-bd4d-d245b03f1422">



## Configure the MVNPM Maven Repository as a fallback


In your settings.xml:

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

see https://maven.apache.org/guides/mini/guide-multiple-repositories.html for more details on multiple repositories

In your project pom.xml:
```xml
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
</profiles
```

## Include dependencies

```
    <dependency>
        <groupId>org.mvnpm[.at.namespace]</groupId>
        <artifactId>{ANY NPM PACKAGE NAME}</artifactId>
        <version>{ANY NPM PACKAGE VERSION}</version>
        <scope>runtime</scope>
    </dependency>
```

The scope depends on the usage of the dependencies, for example with the [Quarkus Web Bundler](https://docs.quarkiverse.io/quarkus-web-bundler/dev/advanced-guides.html#mvnpm), use `provided` scope instead.

Examples:

Lit
```
    <dependency>
        <groupId>org.mvnpm</groupId>
        <artifactId>lit</artifactId>
        <version>2.4.0</version>
        <scope>runtime</scope>
    </dependency>
```

@hotwired/stimulus
```
    <dependency>
        <groupId>org.mvnpm.at.hotwired</groupId>
        <artifactId>stimulus</artifactId>
        <version>3.2.2</version>
        <scope>runtime</scope>
    </dependency>
```

For dependency locking (similar to package-lock.json in the npm world), have a look to the https://github.com/mvnpm/locker.


