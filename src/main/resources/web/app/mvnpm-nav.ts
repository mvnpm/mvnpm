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

  static styles = css`
    nav {
      display: flex;
      align-items: center;
      gap: 0;
    }
    a {
      display: flex;
      align-items: center;
      padding: 0.5rem 0.75rem;
      font-family: var(--lumo-font-family, var(--mvnpm-font-sans, sans-serif));
      font-size: var(--lumo-font-size-m, 0.875rem);
      font-weight: 500;
      color: var(--mvnpm-text-secondary, var(--lumo-secondary-text-color));
      text-decoration: none;
      cursor: pointer;
      position: relative;
      transition: color 0.15s ease;
      white-space: nowrap;
      -webkit-font-smoothing: antialiased;
    }
    a:hover {
      color: var(--mvnpm-text-primary, var(--lumo-body-text-color));
    }
    a[data-selected] {
      color: var(--mvnpm-indigo-light, var(--lumo-primary-text-color));
    }
    a[data-selected]::after {
      content: '';
      position: absolute;
      bottom: 0;
      left: 50%;
      width: var(--lumo-size-s, 1.5rem);
      height: 2px;
      background-color: var(--mvnpm-indigo-light, var(--lumo-primary-color));
      border-radius: 2px 2px 0 0;
      transform: translateX(-50%);
    }
    @media (max-width: 768px) {
      nav {
        flex-direction: column;
        gap: 2px;
        width: 100%;
      }
      a {
        padding: 0.5rem 1rem;
        border-radius: var(--mvnpm-radius-sm, 6px);
        justify-content: center;
      }
      a[data-selected] {
        background: rgba(99, 102, 241, 0.1);
      }
      a[data-selected]::after {
        display: none;
      }
      a.Live, a.Composites {
        display: none;
      }
    }
  `;

  @state() private _currentPath = window.location.pathname;

  constructor() {
    super();
    window.addEventListener('vaadin-router-location-changed', () => {
      this._currentPath = router.location.getUrl();
    });
    router.ready.then(() => {
      this._currentPath = router.location.getUrl();
    });
  }

  render() {
    const routes = router.getRoutes();
    return html`
      <nav>
        ${routes.map((r) => {
          if (r.path.includes(":")) return;
          const selected = this._isCurrentLocation(r);
          return html`<a href="${r.path}" ?data-selected=${selected} class="${r.name}">${r.name}</a>`;
        })}
        <a href="https://github.com/mvnpm/mvnpm" target="_blank" title="Source on GitHub">
          <svg viewBox="0 0 16 16" width="20" height="20" fill="currentColor"><path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27s1.36.09 2 .27c1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.01 8.01 0 0 0 16 8c0-4.42-3.58-8-8-8z"/></svg>
        </a>
      </nav>`;
  }

  private _isCurrentLocation = (route) => {
    return router.urlForPath(route.path) === this._currentPath;
  }


}