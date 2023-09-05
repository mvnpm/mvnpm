import { LitElement, html} from 'lit';
import { customElement } from 'lit/decorators.js';

/**
 * This component shows the About screen
 */
@customElement('mvnpm-version')
export class MvnpmVersion extends LitElement {

    static properties = {
      _version:{state: true},
    };

    constructor() {
        super();
        this._version = null;
    }

    connectedCallback() {
        super.connectedCallback();
        
         var versionUrl = "/api/ui/version";

        fetch(versionUrl)
            .then(response => response.text())
            .then(v => this._version = v);
    }

    render() {
        if(this._version){
            return html`${this._version}`;
        }
    }
    
 }