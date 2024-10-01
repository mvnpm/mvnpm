import {LitElement, html, css} from 'lit';
import {customElement, state} from 'lit/decorators.js';
import {Router} from '@vaadin/router';
import './mvnpm-home.js';
import './mvnpm-releases.js';
import './mvnpm-live.js';
import './mvnpm-doc.js';

const indicator = document.querySelectorAll('.js-loading-indicator');
if (indicator.length > 0) {
  indicator[0].remove();
}

const router = new Router(document.getElementById('outlet'));
router.setRoutes([
  {path: '/', component: 'mvnpm-home', name: 'Browse'},
  {path: '/package/:package', component: 'mvnpm-home', name: 'Browse'},
  {path: '/search/:name', component: 'mvnpm-home', name: 'Browse'},
  {path: '/doc', component: 'mvnpm-doc', name: 'Getting Started'},
  {path: '/releases', component: 'mvnpm-releases', name: 'Releases'},
  {path: '/live', component: 'mvnpm-live', name: 'Live'},
  {path: '/composites', component: 'mvnpm-composites', name: 'Composites'},
]);

/**
 * This component shows the navigation
 */
@customElement('mvnpm-nav')
export class MvnpmNav extends LitElement {




  @state({type: Object}) location = router.location;

  constructor() {
    super();


    router.ready.then(() => {
      this.location = router.location;
    });
  }


  render() {
    const routes = router.getRoutes();
    return html`
      <vaadin-tabs .selected=${undefined}>
        ${routes.map((r) => {
          let ignore = r.path.includes(":");
          if (!ignore) {
            const selected = this._isCurrentLocation(r);
            return html`
              <vaadin-tab .selected=${selected}>
                <a href="${r.path}">
                  <span>${r.name}</span>
                </a>
              </vaadin-tab>`;
          }
        })}
        <vaadin-tab>
          <a href="https://github.com/mvnpm/mvnpm" target="_blank" vaadin-router-ignore><span>Source</span></a>
        </vaadin-tab>
      </vaadin-tabs>`;
  }

  private _isCurrentLocation = (route) => {
    return router.urlForPath(route.path) === this.location.getUrl();
  }


}