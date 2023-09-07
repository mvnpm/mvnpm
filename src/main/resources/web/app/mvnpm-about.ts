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
            border: 2px solid var(--lumo-contrast-60pct);
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
                <b>mvnpm</b> (Maven NPM) is a maven repository facade on top of the <a href="https://www.npmjs.com/" target="_blank">NPM Registry</a>
            </p>
            <p>
            When requesting a dependency, it will inspect the registry to see if it exist and if it does, convert of to a maven dependeny.
            </p>
            <p>Release notifications is available <a href="https://groups.google.com/g/mvnpm-releases" target="_blank">here</a></p>
            <p>
                To use this in your maven project add the following to your settings.xml (typically <span class="url">/home/your-username/.m2/settings.xml</span>)
            </p>
            <p>
                This will look for the artifact in maven central first, and if not found, will look at mvnpm, that will deliver the artifact and kick of the process 
                to sync this with maven central.
                <pre lang="xml" class="use">${this._use}</pre>
            </p>
        `;
    }
    
 }