package io.mvnpm.nexus.mvn;

import java.nio.file.Path;

import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;

public class MavenUploadForm {

    @RestForm("maven2.groupId")
    String groupId;

    @RestForm("maven2.artifactId")
    String artifactId;

    @RestForm("maven2.version")
    String version;

    @RestForm("maven2.generate-pom")
    String generatePom;

    @RestForm("maven2.asset1")
    @PartType("application/java-archive")
    Path artifact;

    @RestForm("maven2.asset1.extension")
    String artifactExtension;

    @RestForm("maven2.asset2")
    @PartType("application/java-archive")
    Path sources;

    @RestForm("maven2.asset2.extension")
    String sourcesExtension;

    @RestForm("maven2.asset2.classifier")
    String sourcesClassifier;

    @RestForm("maven2.asset3")
    @PartType("application/java-archive")
    Path javadoc;

    @RestForm("maven2.asset3.extension")
    String javadocExtension;

    @RestForm("maven2.asset3.classifier")
    String javadocClassifier;

    @RestForm("maven2.asset4")
    @PartType("application/xml")
    Path pom;

    @RestForm("maven2.asset4.extension")
    String pomExtension;
}
