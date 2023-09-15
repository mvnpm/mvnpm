import { LitElement, html, css} from 'lit';
import { customElement } from 'lit/decorators.js';

/**
 * This component shows the About screen
 */
@customElement('mvnpm-about')
export class MvnpmAbout extends LitElement {

    static styles = css`
        :host {
            width: 100%;
            display: flex;
            flex-direction: column;
            align-items: center;
            padding-top: 15px;
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
    `;

    static properties = {
      _use:{state: true},
    };

    constructor() {
        super();
        this._use = `
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

</settings>`;
    }

    render() {
        return html` 
            <p>
                <b>mvnpm</b> (Maven NPM) is a maven repository facade on top of the <a href="https://www.npmjs.com/" target="_blank">NPM Registry</a><br/>
                To use this in your maven project add the following to your settings.xml (typically <span class="url">/home/your-username/.m2/settings.xml</span>)<br/>
                <pre lang="xml" class="use">${this._use}</pre>

                <h3>How does it work ?</h3>
                <img src="/static/mvnpm.png"/>

                <ul>
                    <li>Developer's Maven build requests an npm package from Maven Central.</li>
                    <li>Maven Central returns a 404 if the package does not exist.</li>
                    <li>The developer's Maven build continues to the next repository (as configured above) and requests the npm package from mvnpm.</li>
                    <li>mvnpm requests the NPM Registry for the package (tgz) and converts it to a JAR. It also generates and includes an import map and pom for this package.</li>
                    <li>mvnpm returns this JAR to the developer, and the developer can continue with the build.</li>
                    <li>In the background, mvnpm kicks off a process to create all the files needed to release this package to Maven Central. This includes:<br/>
                        - source <br/>
                        - javadoc <br/>
                        - signatures for all of the files (sha1, md5, asc) <br/>
                        - bundling all the above to upload to Central.
                    </li>
                    <li>Once the bundle exists, it gets uploaded and released to Maven Central.</li>
                    <li>This means that by the time the CI/CD pipeline of the developer runs, the package is available in Maven Central.</li>
                </ul>

                mvnpm will also continuously monitor the NPM Registry for any previously synchronized packages. When it detects a new version, a synchronization process will be initiated.
                <br/>  
                <h3>You can use this</h3>
                <ul>
                    <li>In Quarkus with the <a href="https://docs.quarkiverse.io/quarkus-web-bundler/dev/index.html" target="_blank">Quarkus Web Bundler</a></li>
                    <li>In any Java application with <a href="https://github.com/mvnpm/importmap" target="_blank">importmaps</a> or <a href="https://github.com/mvnpm/esbuild-java" target="_blank">esbuild-java</a></li>
                    <li>In any Java application like you would have done with <a href="https://www.webjars.org/" target="_blank">webjars</a></li>
                </ul>            
            </p>
        `;
    }
    
 }