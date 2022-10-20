import { LitElement, html, css} from 'lit';

/**
 * This component shows the Home screen
 */
 export class MvnpmHome extends LitElement {

    static styles = css`
    `;

    static properties = {
      
    };

    constructor() {
        super();
        
    }

    render() {
        return html`Hello Home !`;
    }
    
 }
 customElements.define('mvnpm-home', MvnpmHome);