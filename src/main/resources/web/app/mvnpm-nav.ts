import { LitElement, html, css} from 'lit';
import { customElement } from 'lit/decorators.js';
import { Router } from '@vaadin/router';
import './mvnpm-home.js';
import './mvnpm-releases.js';
import './mvnpm-live.js';
import './mvnpm-about.js';

const indicator = document.querySelectorAll('.js-loading-indicator');
if (indicator.length > 0) {
  indicator[0].remove();
}

const router = new Router(document.getElementById('outlet'));
router.setRoutes([
    {path: '/', component: 'mvnpm-home', name: 'Home'},
    {path: '/releases', component: 'mvnpm-releases', name: 'Releases'},
    {path: '/live', component: 'mvnpm-live', name: 'Live'},
    {path: '/about', component: 'mvnpm-about', name: 'About'},
    
]);

/**
 * This component shows the navigation
 */
@customElement('mvnpm-nav')
export class MvnpmNav extends LitElement {

    static syncWebSocket;
    static syncServerUri;

    static logWebSocket;
    static logServerUri;

    constructor() {
      super();
      if (!MvnpmNav.syncWebSocket) {
          if (window.location.protocol === "https:") {
            MvnpmNav.syncServerUri = "wss:";
          } else {
            MvnpmNav.syncServerUri = "ws:";
          }
          var currentPath = window.location.pathname;
          MvnpmNav.syncServerUri += "//" + window.location.host + "/api/queue/";
          MvnpmNav.connectSync();
      }
      if (!MvnpmNav.logWebSocket) {
          if (window.location.protocol === "https:") {
            MvnpmNav.logServerUri = "wss:";
          } else {
            MvnpmNav.logServerUri = "ws:";
          }
          var currentPath = window.location.pathname;
          MvnpmNav.logServerUri += "//" + window.location.host + "/api/stream/eventlog";
          MvnpmNav.connectLog();
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
                          <a href="https://github.com/mvnpm/mvnpm" target="_blank" vaadin-router-ignore><span>Source</span></a>
                        </vaadin-tab>
                    </vaadin-tabs>`;
    }

    static connectSync() {
        MvnpmNav.syncWebSocket = new WebSocket(MvnpmNav.syncServerUri);
        MvnpmNav.syncWebSocket.onmessage = function (event) {
            var centralSyncItem = JSON.parse(event.data);
            const centralSyncStateChangeEvent = new CustomEvent('centralSyncStateChange', {detail: centralSyncItem});
            document.dispatchEvent(centralSyncStateChangeEvent);
        }
    }
    
    static connectLog() {
        MvnpmNav.logWebSocket = new WebSocket(MvnpmNav.logServerUri);
        MvnpmNav.logWebSocket.onmessage = function (event) {
            var eventLogEntry = JSON.parse(event.data);
            const eventLogEntryEvent = new CustomEvent('eventLogEntryEvent', {detail: eventLogEntry});
            document.dispatchEvent(eventLogEntryEvent);
        }
    }
 }