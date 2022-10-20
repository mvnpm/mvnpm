import { LitElement, html, css} from 'lit';
import {Router} from '@vaadin/router';

/**
 * This component shows the navigation
 */
 export class MvnpmNav extends LitElement {

    static styles = css`
      
    `;

    static properties = {
      
    };

    constructor() {
        super();  
    }

    connectedCallback() {
      super.connectedCallback()

      var routes = [];
      

      for (const child of this.children) {

        if(child.tagName.toLowerCase() === "mvnpm-navitem"){
          var route = {};

          let nodeMap = child.attributes;
          
          for (let i = 0; i < nodeMap.length; i++) {
            if(nodeMap[i].name === "path"){
              route.path = nodeMap[i].value;
            }
            if(nodeMap[i].name === "component"){
              route.component = nodeMap[i].value;
            }
          }

          routes.push({...route});
        }

      }

      const router = new Router(document.getElementById('outlet'));
      router.setRoutes(routes);
    }

    render() {
        return html`<slot></slot>`;
    }
    
 }
 customElements.define('mvnpm-nav', MvnpmNav);