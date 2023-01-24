import { LitElement, html, css} from 'lit';
import {Router} from '@vaadin/router';
import './mvnpm-home.js';
import './mvnpm-about.js';

const router = new Router(document.getElementById('outlet'));
router.setRoutes([
    {path: '/', component: 'mvnpm-home', name: 'Home'},
    {path: '/about', component: 'mvnpm-about', name: 'About'}
]);

/**
 * This component shows the navigation
 */
export class MvnpmNav extends LitElement {

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
      
    };

    render() {
        var routes = router.getRoutes();
        return html`${routes.map((r) =>
                html`<a href='${r.path}'>${r.name}<a>`
            )}`;
    }
 }
 customElements.define('mvnpm-nav', MvnpmNav);