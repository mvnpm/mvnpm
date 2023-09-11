![banner](https://github.com/mvnpm/mvnpm/assets/6836179/787a3974-0b9a-4809-a74d-3710c6d08229)

Maven for NPM

## Maven Repository

Add the mvnpm repository:

```
    <settings>
    <profiles>
        <profile>
            <id>mvnpm</id>
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
        <activeProfile>mvnpm</activeProfile>
    </activeProfiles>

</settings>
```

see https://maven.apache.org/guides/mini/guide-multiple-repositories.html for more details on multiple repositories

## Include

```
    <dependency>
        <groupId>org.mvnpm</groupId>
        <artifactId>{ANY NPM PACKAGE NAME}</artifactId>
        <version>{ANY NPM PACKAGE VERSION}</version>
        <scope>runtime</scope>
    </dependency>
```

example (lit):

```
    <dependency>
        <groupId>org.mvnpm</groupId>
        <artifactId>lit</artifactId>
        <version>2.4.0</version>
        <scope>runtime</scope>
    </dependency>
```

TODO: Show advance example (with namespaced npm packages)

## Use 

TODO ... (import map etc)
