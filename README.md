![Logo](brand/fulllogo_transparent_nobuffer.png)

Consume the NPM packages directly from Maven and Gradle projects.

A lot of packages are already synced on Central, you may check this from [mvnpm.org](mvnpm.org): <img height="25" alt="image" src="https://github.com/mvnpm/mvnpm/assets/2223984/60aa898d-73e2-4a5e-83ec-fb7e0a7d22c3">


If it's not:
- Configure your project to use the MVNPM Maven Repository as a fallback. When a package is missing, it will fetch it from the fallback repository and automatically trigger a sync with Maven Central.
- Click to trigger a sync with Maven Central: <img height="25" alt="image" src="https://github.com/mvnpm/mvnpm/assets/2223984/923f09ff-9631-4c11-aa61-8f6a9ded73d8">




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

**Examples:**

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


