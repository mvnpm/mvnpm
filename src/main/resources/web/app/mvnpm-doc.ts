import { LitElement, html, css} from 'lit';
import { customElement } from 'lit/decorators.js';
import { ThemeMixin } from './theme-mixin.js';
import '@qomponent/qui-code-block';

/**
 * This component shows the Doc screen
 */

@customElement('mvnpm-doc')
export class MvnpmDoc extends ThemeMixin(LitElement) {

    static styles = css`
        :host {
            width: 100%;
            display: flex;
            flex-direction: column;
            padding: 40px 30px;
            gap: 24px;
            line-height: 1.7;
        }

        @media (min-width: 1100px) {
            :host {
                width: 720px;
                margin: 0 auto;
            }
        }

        section {
            border-bottom: 1px solid var(--mvnpm-border-subtle, var(--lumo-contrast-5pct));
            padding-bottom: 32px;
        }

        section:last-of-type {
            border-bottom: none;
            padding-bottom: 0;
        }

        qui-code-block {
            width: 100%;
            height: auto;
            border-radius: var(--mvnpm-radius-sm, 6px);
            overflow: hidden;
            margin: 8px 0 16px;
        }

        h1 {
            margin: 0 0 8px;
            font-weight: 700;
            font-size: 1.8em;
            letter-spacing: -0.02em;
            color: var(--mvnpm-text-primary, var(--lumo-body-text-color));
        }

        .subtitle {
            color: var(--mvnpm-text-secondary, var(--lumo-secondary-text-color));
            font-size: 1.05em;
            margin: 0 0 16px;
        }

        h2 {
            margin: 0 0 12px;
            font-weight: 600;
            font-size: 1.25em;
            letter-spacing: -0.01em;
            color: var(--mvnpm-text-primary, var(--lumo-body-text-color));
        }

        p {
            margin: 0 0 12px;
            color: var(--mvnpm-text-secondary, var(--lumo-secondary-text-color));
        }

        .label {
            display: inline-block;
            font-weight: 600;
            font-size: 0.8em;
            text-transform: uppercase;
            letter-spacing: 0.05em;
            color: var(--mvnpm-text-tertiary, var(--lumo-tertiary-text-color));
            margin: 12px 0 4px;
        }

        .hint {
            font-size: 0.9em;
            color: var(--mvnpm-text-tertiary, var(--lumo-tertiary-text-color));
            margin: 4px 0 0;
        }

        code, .url {
            font-family: var(--mvnpm-font-mono, monospace);
            background-color: var(--mvnpm-code-bg, var(--lumo-contrast-5pct));
            padding: 2px 6px;
            border-radius: 4px;
            font-size: 0.85em;
        }

        a {
            color: var(--mvnpm-indigo-light, var(--lumo-primary-text-color));
            text-decoration: none;
            transition: color 0.15s ease;
        }

        a:hover {
            color: var(--mvnpm-text-link-hover, var(--lumo-body-text-color));
            text-decoration: underline;
        }

        ul, ol {
            margin: 8px 0 12px;
            padding-left: 20px;
            color: var(--mvnpm-text-secondary, var(--lumo-secondary-text-color));
        }

        li {
            margin-bottom: 8px;
        }

        li::marker {
            color: var(--mvnpm-text-tertiary, var(--lumo-tertiary-text-color));
        }

        .how {
            width: 100%;
            border-radius: var(--mvnpm-radius-md, 10px);
            margin-bottom: 16px;
        }

        .callout {
            background-color: var(--mvnpm-code-bg, var(--lumo-contrast-5pct));
            border-left: 3px solid var(--mvnpm-indigo-light, var(--lumo-primary-color));
            padding: 12px 16px;
            border-radius: 0 var(--mvnpm-radius-sm, 6px) var(--mvnpm-radius-sm, 6px) 0;
            margin: 12px 0;
            font-size: 0.95em;
        }

        .callout p {
            margin: 0;
        }

        @media (max-width: 768px) {
            :host {
                padding: 20px 16px;
                gap: 20px;
            }
            h1 {
                font-size: 1.4em;
            }
            h2 {
                font-size: 1.1em;
            }
            section {
                padding-bottom: 24px;
            }
            qui-code-block {
                font-size: 0.8rem;
            }
            code, .url {
                font-size: 0.8em;
                word-break: break-all;
            }
        }
    `;

    private _dep = `<dependency>
    <groupId>org.mvnpm</groupId>
    <artifactId>{package-name}</artifactId>
    <version>{package-version}</version>
    <scope>{runtime/provided}</scope>
</dependency>`;

    private _gradleDep = `implementation 'org.mvnpm:{package-name}:{package-version}'
// or compileOnly for bundled usage`;

    private _settings = `<settings>
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

    private _gradleRepo = `repositories {
    mavenCentral()
    maven { url "https://repo.mvnpm.org/maven2" }
}`;

    private _gradleLocking = `dependencyLocking {
    lockAllConfigurations()
}`;

    render() {
        return html`
            <header>
                <h1>Getting Started with mvnpm</h1>
                <p class="subtitle">Use NPM packages as standard Maven or Gradle dependencies.</p>
            </header>

            <section>
                <h2>Add an NPM dependency</h2>
                <p><b>mvnpm</b> lets you consume <a href="https://www.npmjs.com/" target="_blank">NPM Registry</a> packages directly from your Java build tool.</p>
                <span class="label">Maven</span>
                <qui-code-block mode="xml" theme="${this._theme}" content="${this._dep}"></qui-code-block>
                <span class="label">Gradle</span>
                <qui-code-block mode="groovy" theme="${this._theme}" content="${this._gradleDep}"></qui-code-block>
                <p class="hint">
                    For scoped packages, use <code>org.mvnpm.at.{namespace}</code> as groupId
                    (e.g. <code>@hotwired/stimulus</code> becomes <code>org.mvnpm.at.hotwired:stimulus</code>).
                </p>
            </section>

            <section>
                <h2>Ways to consume</h2>
                <ul>
                    <li>Bundled and served with the <a href="https://docs.quarkiverse.io/quarkus-web-bundler/dev/index.html" target="_blank">Quarkus Web Bundler</a> (scope <code>provided</code>)</li>
                    <li>Served directly using <a href="https://github.com/mvnpm/importmap" target="_blank">importmaps</a> (scope <code>runtime</code>)</li>
                    <li>Bundled with <a href="https://github.com/mvnpm/esbuild-java" target="_blank">esbuild-java</a></li>
                    <li>As a drop-in <a href="https://www.webjars.org/" target="_blank">WebJars</a> replacement</li>
                </ul>
            </section>

            <section>
                <h2>Syncing a missing package</h2>
                <p>Most popular packages are already on Maven Central. Check the "Maven Central" badge on the <a href="/">Browse page</a> to verify.</p>
                <ul>
                    <li>Click the "Maven Central" badge to trigger a sync.</li>
                    <li>Or configure the <a href="#configure-fallback-repo">fallback repository</a> to fetch missing packages automatically.</li>
                </ul>
                <p>Once a package is synced, mvnpm automatically syncs new versions as they're published on NPM. Tools like Dependabot and Renovate can then propose updates in your pull requests.</p>
                <div class="callout">
                    <p><strong>Use Maven Central for production builds.</strong> The fallback repository is for development and initial sync only.</p>
                </div>
            </section>

            <section>
                <h2>Fallback repository mode</h2>
                <img class="how" src="/static/how-does-mvnpm-work.svg" alt="Diagram showing how mvnpm converts NPM packages to Maven artifacts and syncs them to Maven Central"/>
                <ol>
                    <li>Your build requests a package from Maven Central.</li>
                    <li>If it's not there yet, the request falls through to the mvnpm repository.</li>
                    <li>mvnpm fetches the package from NPM and converts it into a Maven artifact (JAR + POM).</li>
                    <li>The artifact is returned to your build immediately and synced to Maven Central in the background.</li>
                </ol>
            </section>

            <section>
                <h2 id="configure-fallback-repo">Configuring the fallback repository</h2>
                <p>Add mvnpm as a fallback repository so missing packages are fetched automatically.</p>
                <span class="label">Maven</span>
                <p>Add to your <span class="url">~/.m2/settings.xml</span>:</p>
                <qui-code-block mode="xml" theme="${this._theme}" content="${this._settings}"></qui-code-block>
                <span class="label">Gradle</span>
                <p>Add to your <span class="url">build.gradle</span>:</p>
                <qui-code-block mode="groovy" theme="${this._theme}" content="${this._gradleRepo}"></qui-code-block>
            </section>

            <section>
                <h2 id="how-to-lock-dependencies-">Locking dependencies</h2>
                <span class="label">Maven</span>
                <p>The <a href="https://github.com/mvnpm/locker" target="_blank">mvnpm locker Maven Plugin</a> locks your <code>org.mvnpm</code> and <code>org.webjars</code> dependency versions, similar to <code>package-lock.json</code> or <code>yarn.lock</code>.</p>
                <span class="label">Gradle</span>
                <p>Enable native dependency locking in your <code>build.gradle</code>:</p>
                <qui-code-block mode="groovy" theme="${this._theme}" content="${this._gradleLocking}"></qui-code-block>
                <p>Then run <code>gradle dependencies --write-locks</code> to generate the lockfile.</p>
            </section>
        `;
    }

}
