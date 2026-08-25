// package org.mvnpm.nexus.mvn;

// import java.io.File;

// import org.eclipse.aether.DefaultRepositorySystemSession;
// import org.eclipse.aether.RepositorySystem;
// import org.eclipse.aether.RepositorySystemSession;
// import org.eclipse.aether.artifact.Artifact;
// import org.eclipse.aether.artifact.DefaultArtifact;
// import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
// import org.eclipse.aether.deployment.DeployRequest;
// import org.eclipse.aether.impl.DefaultServiceLocator;
// import org.eclipse.aether.repository.Authentication;
// import org.eclipse.aether.repository.LocalRepository;
// import org.eclipse.aether.repository.LocalRepositoryManager;
// import org.eclipse.aether.repository.RemoteRepository;
// import org.eclipse.aether.repository.RepositoryPolicy;
// import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
// import org.eclipse.aether.spi.connector.transport.TransporterFactory;
// import org.eclipse.aether.transport.apache.ApacheTransporterFactory;
// import org.eclipse.aether.util.artifact.SubArtifact;
// import org.eclipse.aether.util.repository.AuthenticationBuilder;

// public final class SnapshotUploader {

//     private SnapshotUploader() {
//     }

//     public static void deploy(String nexusUrl, String username, String password, String groupId, String artifactId,
//             String version, File pom, File jar, File sourcesJar, File javadocJar) throws Exception {

//         RepositorySystem system = newRepositorySystem();
//         RepositorySystemSession session = newSession(system);

//         Authentication authentication = new AuthenticationBuilder().addUsername(username).addPassword(password).build();

//         RemoteRepository repository = new RemoteRepository.Builder("nexus", "default", nexusUrl)
//                 .setAuthentication(authentication).setSnapshotPolicy(new RepositoryPolicy(true,
//                         RepositoryPolicy.UPDATE_POLICY_ALWAYS, RepositoryPolicy.CHECKSUM_POLICY_FAIL))
//                 .build();

//         Artifact main = new DefaultArtifact(groupId, artifactId, "", "jar", version, null, jar);

//         Artifact sources = new DefaultArtifact(groupId, artifactId, "sources", "jar", version, null, sourcesJar);

//         Artifact javadoc = new DefaultArtifact(groupId, artifactId, "javadoc", "jar", version, null, javadocJar);

//         Artifact pomArtifact = new SubArtifact(main, "", "pom", pom);

//         DeployRequest request = new DeployRequest();

//         request.addArtifact(main);
//         request.addArtifact(sources);
//         request.addArtifact(javadoc);
//         request.addArtifact(pomArtifact);

//         request.setRepository(repository);

//         system.deploy(session, request);
//     }

//     private static RepositorySystem newRepositorySystem() {

//         DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();

//         locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);

//         locator.addService(TransporterFactory.class, ApacheTransporterFactory.class);

//         return locator.getService(RepositorySystem.class);
//     }

//     private static RepositorySystemSession newSession(RepositorySystem system) {

//         DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

//         LocalRepository localRepository = new LocalRepository("target/local-repo");

//         LocalRepositoryManager manager = system.newLocalRepositoryManager(session, localRepository);

//         session.setLocalRepositoryManager(manager);

//         return session;
//     }
// }
