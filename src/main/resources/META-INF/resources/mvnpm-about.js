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
            padding: 15px;
        }
        .use {
            width: 800px;
            color: #2b2b2b;
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
                    <id>mvnpm.org</id>
                    <name>mvnpm</name>
                    <url>https://repo.mvnpm.org/maven2</url>
                    <releases>
                        <enabled>true</enabled>
                        <updatePolicy>daily</updatePolicy>
                        <checksumPolicy>warn</checksumPolicy>
                    </releases>
                    <snapshots>
                        <enabled>false</enabled>
                        <updatePolicy>never</updatePolicy>
                        <checksumPolicy>fail</checksumPolicy>
                    </snapshots>
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
            <img src="logo.png" alt="mvnpm" width="200px"/>
            <p>
                mvnpm (Maven NPM) is a maven repository facade on top of the <a href="https://www.npmjs.com/" target="_blank">NPM Registry</a>
            </p>
            <p>
            When requesting a dependency, it will inspect the registry to see if it exist and if it does, convert of to a maven dependeny.
            </p>
        
            <p>
                To use this in your maven project add the following to your settings.xml (typically /home/your-username/.m2/settings.xml)
                <pre lang="xml" class="use">${this._use}</pre>
            </p>
        
            <p class="warn"> 
                This project is still a work in progress.
            </p>
        `;
    }
    
 }
 customElements.define('mvnpm-about', MvnpmAbout);