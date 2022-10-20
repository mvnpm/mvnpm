import { LitElement, html, css} from 'lit';

/**
 * This component define a Nav Item
 */
 export class MvnpmNavItem extends LitElement {

    static styles = css`
      a {
        color: white;
        padding: 2px;
      }
      a:link, a:visited, a:active { 
        text-decoration: none; 
      }
      a:hover { 
        text-decoration: dashed; 
      }
    `;

    static properties = {
      path: {type: String},
      component: {type: String},
      text: {type: String},
    };

    constructor() {
        super();  
    }

    connectedCallback() {
      super.connectedCallback()
    }

    render() {
        return html`<a href='${this.path}'>${this.text}<a>`;
    }
    
 }
 customElements.define('mvnpm-navitem', MvnpmNavItem);