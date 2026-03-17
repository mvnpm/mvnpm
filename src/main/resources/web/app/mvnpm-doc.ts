import { LitElement, html, css} from 'lit';
import { customElement, state } from 'lit/decorators.js';
import '@qomponent/qui-code-block';

/**
 * This component shows the Doc screen
 */

@customElement('mvnpm-doc')
export class MvnpmDoc extends LitElement {

    static styles = css`
        :host {
            width: 100%;
            display: flex;
            flex-direction: column;
            padding: 30px;
            gap: 30px;
        }

        @media (min-width: 1100px) {
            :host {
                width: 960px;
                margin: 0 auto;
            }
        }

        section {
            background-color: var(--mvnpm-bg-surface, var(--lumo-contrast-5pct));
            border: 1px solid var(--mvnpm-border, var(--lumo-contrast-10pct));
            border-radius: var(--mvnpm-radius-md, 10px);
            padding: 24px;
            position: relative;
            z-index: 1;
        }

        qui-code-block {
            width: 100%;
        }

        h1 {
            margin-top: 0;
            font-weight: 700;
            font-size: 1.6em;
        }

        h2 {
            margin-top: 0;
            font-weight: 600;
        }

        .use {
            width: 100%;
            border: 1px solid var(--mvnpm-border, var(--lumo-contrast-10pct));
            border-radius: var(--mvnpm-radius-md, 10px);
            padding: 20px;
        }
        .url {
            font-family: var(--mvnpm-font-mono, monospace);
            background-color: var(--mvnpm-code-bg, var(--lumo-contrast-5pct));
            padding: 2px 6px;
            border-radius: 4px;
            font-size: 0.9em;
        }
        code {
            font-family: var(--mvnpm-font-mono, monospace);
            background-color: var(--mvnpm-code-bg, var(--lumo-contrast-5pct));
            padding: 2px 6px;
            border-radius: 4px;
            font-size: 0.9em;
        }
        a {
            color: var(--mvnpm-text-link, var(--lumo-contrast-60pct));
            text-decoration: none;
            transition: color 0.15s ease;
        }
        a:hover {
            color: var(--mvnpm-text-link-hover, var(--lumo-body-text-color));
        }
        .how {
            width: 100%;
            border-radius: var(--mvnpm-radius-md, 10px);
        }
    `;
    @state() _dep: string = `<dependency>
    <groupId>org.mvnpm</groupId>
    <artifactId>{package-name}</artifactId>
    <version>{package-version}</version>
    <scope>{runtime/provided}</scope>
</dependency>`;

    @state() _gradleDep: string = `implementation 'org.mvnpm:{package-name}:{package-version}'`;
    @state() _settings: string = `
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

</settings>`;

    @state() _gradleLocking: string = `
     dependencyLocking {
        lockAllConfigurations()
     }   
    `;

    render() {
        return html`
            <h1>Getting Started with mvnpm</h1>
            <section>
                <h2>Add an NPM dependency</h2>
                <p><b>mvnpm</b> lets you consume <a href="https://www.npmjs.com/" target="_blank">NPM Registry</a> packages as dependencies directly from a Maven or Gradle project.</p>
                <p><strong>Maven</strong></p>
                <qui-code-block mode="xml" content="${this._dep}"></qui-code-block>
                <p><strong>Gradle</strong></p>
                <qui-code-block mode="groovy" content="${this._gradleDep}"></qui-code-block>
                <p>
                    <i>For scoped packages, use <code>org.mvnpm.at.{namespace}</code> as groupId
                    (e.g. <code>@hotwired/stimulus</code> becomes <code>org.mvnpm.at.hotwired:stimulus</code>).</i>
                </p>
            </section>
            <section>
                <h2>Ways to consume</h2>
                <ul>
                    <li>Packaged and served with the <a href="https://docs.quarkiverse.io/quarkus-web-bundler/dev/index.html" target="_blank">Quarkus Web Bundler extension</a> using scope "provided"</li>
                    <li>Directly served by Quarkus with scope "runtime"</li>
                    <li>In any Java application with <a href="https://github.com/mvnpm/importmap" target="_blank">importmaps</a> or <a href="https://github.com/mvnpm/esbuild-java" target="_blank">esbuild-java</a></li>
                    <li>In any Java application as you would with <a href="https://www.webjars.org/" target="_blank">webjars</a></li>
                </ul>
            </section>
            <section>
                <h2>How it works</h2>
                <img class="how" src="/static/how-does-mvnpm-work.png" alt="Diagram showing how mvnpm converts NPM packages to Maven artifacts and syncs them to Maven Central"/>
                <ol>
                    <li>Your Maven/Gradle build requests an NPM package from Maven Central.</li>
                    <li>If the package doesn't exist on Central yet, the build falls through to the mvnpm repository (if configured as a fallback).</li>
                    <li>mvnpm fetches the package from the NPM Registry, converts the tgz into a JAR, and generates a POM and import map.</li>
                    <li>The JAR is returned to your build immediately so you can continue working.</li>
                    <li>In the background, mvnpm creates all the files needed for Maven Central (source, javadoc, SHA1/MD5/ASC signatures) and uploads a release bundle.</li>
                    <li>By the time your CI/CD pipeline runs, the package is available on Maven Central.</li>
                </ol>
            </section>
            <section>
                <h2>Syncing a missing package</h2>
                <p>Most popular packages are already synced to Maven Central and can be used directly. Check the "Maven Central" badge on the <a href="/">Browse page</a> to see if a package version is available.</p>
                <p>If it's not synced yet:</p>
                <ul>
                    <li>Click the "Maven Central" badge on the Browse page to trigger a sync.</li>
                    <li>Or configure your Maven settings to use the <a href="#configure-fallback-repo">mvnpm repository as a fallback</a>. Missing packages will be fetched automatically and synced to Central.</li>
                </ul>
                <p><strong>Use Maven Central for production builds.</strong> The fallback repository is for development and initial sync only.</p>
            </section>
            <section>
                <h2 id="configure-fallback-repo">Configuring the fallback repository</h2>
                <p>The mvnpm Maven repository is a facade on top of the <a href="https://www.npmjs.com/" target="_blank">NPM Registry</a>. It's useful when starting a project or updating versions with many unsynced packages.</p>
                <p>Add the following to your <span class="url">~/.m2/settings.xml</span>:</p>
                <qui-code-block mode="xml" content="${this._settings}"></qui-code-block>
            </section>
            <section>
                <h2>Version updates</h2>
                <p>mvnpm continuously monitors the NPM Registry for previously synchronized packages. When a new version is detected, it's automatically synced to Maven Central. Tools like Dependabot and Renovate will be able to propose version updates in your pull requests.</p>
            </section>
            <section>
                <h2 id="how-to-lock-dependencies-">Locking dependencies</h2>
                <p><strong>Maven</strong></p>
                <p>The <a href="https://github.com/mvnpm/locker" target="_blank">mvnpm locker Maven Plugin</a> creates a version locker profile for your <code>org.mvnpm</code> and <code>org.webjars</code> dependencies, similar to <code>package-lock.json</code> or <code>yarn.lock</code>.</p>
                <p>This is essential because NPM dependencies use version ranges. Without locking, your builds may pull different versions between runs. The locker also reduces the number of files Maven needs to download (better for reproducibility and CI).</p>
                <p><strong>Gradle</strong></p>
                <p>Gradle has native dependency locking. Enable it in your <code>build.gradle</code>:</p>
                <qui-code-block mode="groovy" content="${this._gradleLocking}"></qui-code-block>
                <p>Then run <code>gradle dependencies --write-locks</code> to generate the lockfile.</p>
            </section>
        `;
    }

 }