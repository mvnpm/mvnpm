import { LitElement, html, css} from 'lit';
import { customElement } from 'lit/decorators.js';
import { Router } from '@vaadin/router';
import './mvnpm-home.js';
import './mvnpm-progress.js';
import './mvnpm-about.js';

const router = new Router(document.getElementById('outlet'));
router.setRoutes([
    {path: '/', component: 'mvnpm-home', name: 'Home'},
    {path: '/progress', component: 'mvnpm-progress', name: 'Sync'},
    {path: '/about', component: 'mvnpm-about', name: 'About'}
]);

/**
 * This component shows the navigation
 */
@customElement('mvnpm-nav')
export class MvnpmNav extends LitElement {

    static webSocket;
    static serverUri;

    constructor() {
      super();
      if (!MvnpmNav.webSocket) {
          if (window.location.protocol === "https:") {
            MvnpmNav.serverUri = "wss:";
          } else {
            MvnpmNav.serverUri = "ws:";
          }
          var currentPath = window.location.pathname;
          MvnpmNav.serverUri += "//" + window.location.host + "/api/queue/";
          MvnpmNav.connect();
      }
    }    

    render() {
        const routes = router.getRoutes();
        return html`<vaadin-tabs> 
                        ${routes.map((r) =>
                            html`<vaadin-tab>
                                  <a href="${r.path}">
                                    <span>${r.name}</span>
                                  </a>
                                </vaadin-tab>`
                        )}
                        <vaadin-tab>
                          <a href="https://groups.google.com/g/mvnpm-releases" target="_blank" vaadin-router-ignore><span>Releases</span></a>
                        </vaadin-tab>
                        <vaadin-tab>
                          <a href="https://github.com/mvnpm/mvnpm" target="_blank" vaadin-router-ignore><span>Source</span></a>
                        </vaadin-tab>
                    </vaadin-tabs>`;
    }

    static connect() {
        MvnpmNav.webSocket = new WebSocket(MvnpmNav.serverUri);
        MvnpmNav.webSocket.onmessage = function (event) {
            var centralSyncItem = JSON.parse(event.data);
            const centralSyncStateChangeEvent = new CustomEvent('centralSyncStateChange', {detail: centralSyncItem});
            document.dispatchEvent(centralSyncStateChangeEvent);
        }
    }
 }