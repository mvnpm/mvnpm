import { LitElement, html, css} from 'lit';

/**
 * This component shows the About screen
 */
export class MvnpmAbout extends LitElement {

    static styles = css`
        :host {
            width: 100%;
            display: flex;
            flex-direction: column;
            align-items: center;
            padding-top: 15px;
            color: grey;
        }
        .warn {
            color: red;
            font-weight: bold;
            font-family: monospace;
        }
        .use {
            width: 800px;
            color: #2b2b2b;
            border: 2px solid var(--mvnpm-background);
            border-radius: 15px;
            padding-left: 20px;
            padding-right: 20px;
            padding-bottom: 20px;
        }
        .url {
            font-family: monospace;
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
        
            <p>
                To use this in your maven project add the following to your settings.xml (typically <span class="url">/home/your-username/.m2/settings.xml</span>)
                <pre lang="xml" class="use">${this._use}</pre>
            </p>
        
            <p class="warn"> 
                This project is still a work in progress.
            </p>
        `;
    }
    
 }
 customElements.define('mvnpm-about', MvnpmAbout);