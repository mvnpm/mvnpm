import { LitElement, html, css} from 'lit';

/**
 * This component shows the navigation
 */
 export class MvnpmNav extends LitElement {

    static styles = css`
      a {
        color: white;
        padding: 2px;
      }

      a:link { 
        text-decoration: none; 
      }
      a:visited { 
        text-decoration: none; 
      }
      a:hover { 
        text-decoration: none; 
      }
      a:active { 
        text-decoration: none; 
      }
    `;

    static properties = {
      year: {type: String},
      version: {type: String},
    };

    constructor() {
        super();
        
    }

    render() {
        return html`<a href="/">Home<a><a href="/about">About<a>`;
    }
    
 }
 customElements.define('mvnpm-nav', MvnpmNav);