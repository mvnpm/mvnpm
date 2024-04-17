import { LitElement, html, css} from 'lit';
import { customElement, state } from 'lit/decorators.js';
import '@quarkus-webcomponents/codeblock';

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
            padding: 15px;
            gap: 30px;
        }

        @media (min-width: 900px) {
            :host {
                width: 800px;   
                margin: 0 auto;
            }
        }
        .use {
            width: 800px;
            border: 2px solid var(--lumo-contrast-10pct);
            border-radius: 15px;
            padding: 20px;
        }
        .url {
            font-family: monospace;
        }
        a{
            color:var(--lumo-contrast-60pct);
        }
        a:link{ 
            text-decoration: none; 
            color:var(--lumo-contrast-60pct);
        }
        a:visited { 
            text-decoration: none; 
            color:var(--lumo-contrast-60pct);
        }
        a:hover { 
            text-decoration: none; 
            color:var(--lumo-body-text-color);
        }
        .how{
            width: 100%;
        }
    `;
    @state() _dep: string = `
    <dependency>
        <groupId>org.mvnpm</groupId>
        <artifactId>{package-name}</artifactId>
        <version>{package-version}</version>
        <scope>{runtime/provided}</scope>
    </dependency>
    `;
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
            <section>
                <h3>Use npm like any other Maven dependency...</h3>
                <p><b>mvnpm</b> (Maven NPM) allows to consume the <a href="https://www.npmjs.com/" target="_blank">NPM
                    Registry</a> packages as dependencies directly from a Maven or Gradle project:
                </p>
                <p>
                    <qui-code-block mode="xml" content="${this._dep}"></qui-code-block>
                </p>
                <p>
                    <i>Use <code>org.mvnpm.at.{namespace}</code> as groupId for a particular
                        namespace (i.e. <code>@hotwired/stimulus</code> becomes <code>org.mvnpm.at.hotwired:stimulus</code>).</i>
                </p>
            </section>
            <section>
                <h3>How to consume?</h3>
                <ul>
                    <li>Packaged and served with the <a
                            href="https://docs.quarkiverse.io/quarkus-web-bundler/dev/index.html"
                            target="_blank">Quarkus Web Bundler extension</a> using scope "provided".
                    </li>
                    <li>Directly Served by Quarkus with scope "runtime"</li>
                    <li>In any Java application with <a href="https://github.com/mvnpm/importmap" target="_blank">importmaps</a>
                        or <a href="https://github.com/mvnpm/esbuild-java" target="_blank">esbuild-java</a></li>
                    <li>In any Java application like you would have done with <a href="https://www.webjars.org/"
                                                                                 target="_blank">webjars</a></li>
                </ul>
            </section>
            <section>
                <h3>How to sync a missing package?</h3>
                <p>
                    A lot of packages are already synced on Central, which mean they can directly be used from you
                    pom.xml or build.gradle.
                    You may check if a package version is available by looking at the "Maven central" badge on the <a
                        href="/">Browse page</a>.<br/>
                    <b>If it's not:</b>
                <ul>
                    <li>Click on the "Maven Central" badge to trigger a sync with Maven Central</li>
                    <li>Configure your local Maven settings to use the <a href="#configure-fallback-repo">MVNPM Maven
                        Repository as a fallback</a>. When a
                        package is
                        missing, it will fetch it from the fallback repository and automatically trigger a sync with
                        Maven Central.
                    </li>
                </ul>
                <b>You should use the Maven Central repository for production builds.</b>
                </p>
            </section>
            <section>
                <h3 id="configure-fallback-repo">How to configure the fallback repository?</h3>
                <p>
                    The <b>mvnpm</b> Maven repository is a facade on top of the <a href="https://www.npmjs.com/"
                                                                                   target="_blank">NPM Registry</a>, it
                    is
                    handy when starting (or updating versions) on a project with many non synchronised packages (which
                    will
                    become more and more unlikely in the future).<br/>
                    to use it in your local Maven settings add the following to your settings.xml (typically <span
                        class="url">/home/your-username/.m2/settings.xml</span>)<br/>
                </p>
                <qui-code-block mode="xml" content="${this._settings}"></qui-code-block>
            </section>
            <section>
                <h3>How does the mvnpm Maven repository work ?</h3>
                <img class="how" src="/static/how-does-mvnpm-work.png"/>

                <ul>
                    <li>Developer's Maven build requests an npm package from Maven Central.</li>
                    <li>Maven Central returns a 404 if the package does not exist.</li>
                    <li>The developer's Maven build continues to the next repository (as configured above) and requests
                        the npm package from mvnpm.
                    </li>
                    <li>mvnpm requests the NPM Registry for the package (tgz) and converts it to a JAR. It also
                        generates and includes an import map and pom for this package.
                    </li>
                    <li>mvnpm returns this JAR to the developer, and the developer can continue with the build.</li>
                    <li>In the background, mvnpm kicks off a process to create all the files needed to release this
                        package to Maven Central. This includes:<br/>
                        - source <br/>
                        - javadoc <br/>
                        - signatures for all of the files (sha1, md5, asc) <br/>
                        - bundling all the above to upload to Central.
                    </li>
                    <li>Once the bundle exists, it gets uploaded and released to Maven Central.</li>
                    <li>This means that by the time the CI/CD pipeline of the developer runs, the package is available
                        in Maven Central.
                    </li>
                </ul>
            </section>
            <section>
                <h3>How to update versions?</h3>
                <p>
                    <b>mvnpm</b> continuously monitor the NPM Registry for any previously synchronized packages.
                    When it detects a new version, a synchronization process will be initiated. So tools like dependabot
                    will be able to
                    propose the new version in your pull requests.
                </p>
            </section>
            <section>
                <h3 id="how-to-lock-dependencies-">How to lock dependencies?</h3>
                <p><strong>Locking with Maven</strong></p>
                <p>The <a href="https://github.com/mvnpm/locker" target="_blank">mvnpm locker Maven Plugin</a> will
                    create a version
                    locker profile for your org.mvnpm and org.webjars dependencies. Allowing you to mimick the
                    package-lock.json and yarn.lock files in a Maven world.</p>
                <p>It is essential as NPM dependencies are typically deployed using version ranges, without locking your
                    builds will use different versions of dependencies between builds if any of your transitive NPM
                    based dependencies are updated.</p>
                <p>In addition when using the locker, the number of files Maven need to download is considerably reduced
                    as it no longer need to check all possible version ranges (better for reproducibility, contributors
                    and CI).</p>
                <p><strong>Locking with Gradle</strong></p>
                <p>Gradle provides a native version locking system, to install it, add this:</p>
                <p>build.gradle</p>
                <p>
                    <qui-code-block mode="groovy" content="${this._gradleLocking}"></qui-code-block>
                </p>
                <p>Then run <code>gradle dependencies --write-locks</code> to generate the lockfile.</p>

            </section>

        `;
    }

 }