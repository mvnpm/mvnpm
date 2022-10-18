# mvnpm
Maven for NPM

## Maven Repository

Add the mvnpm repository:

```
    <repositories>
        <repository>
            <id>mvnpm.org</id>
            <name>mvnpm</name>
            <url>https://repo.mvnpm.org/maven2</url>
        </repository>
    </repositories>
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

## Server install

`export MVNPM_LOCAL_USER_DIRECTORY=/home/pkruger/mvnpm`
`nohup ./mvnpm-1.0.1-runner > mvnpm.log &`

## TODO

- Handle maven-metadata.xml
