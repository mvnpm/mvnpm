import { LitElement, html, css} from 'lit';

// TODO: Waiting for Import Assertions. see https://chromestatus.com/feature/5765269513306112
// import footer from './_static/footer.json' assert { type: "json" };

/**
 * This component shows the Bottom Footer
 */
 export class MvnpmFooter extends LitElement {

    static styles = css`
        span { 
            font-size: smaller;
            color: grey;
        }`;

    static properties = {
      year: {type: String},
      version: {type: String},
    };

    constructor() {
        super();
    }

    connectedCallback() {
        super.connectedCallback()
        fetch("./_static/footer.json")
            .then((res) => res.json())
            .then((footer) => {
                this.year = footer.year;
                this.version = footer.version;
            });
      }

    render() {
        return html`<span>&copy; ${this.year} mvnpm.org | v${this.version}</span>`;
    }
    
 }
 customElements.define('mvnpm-footer', MvnpmFooter);