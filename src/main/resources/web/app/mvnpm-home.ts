import {LitElement, html, css} from 'lit';
import {customElement, state} from 'lit/decorators.js';
import {unsafeHTML} from 'lit/directives/unsafe-html.js';
import {compareVersions} from 'compare-versions';
import {ThemeMixin} from './theme-mixin.js';
import '@vaadin/form-layout';
import '@vaadin/text-field';
import '@vaadin/combo-box';
import '@vaadin/radio-group';
import '@vaadin/button';
import '@vaadin/tabs';
import '@vaadin/tabsheet';
import '@vaadin/icon';
import '@vaadin/vaadin-lumo-styles/vaadin-iconset';
import '@vaadin/icons';
import '@vaadin/progress-bar';
import '@qomponent/qui-code-block';
import '@qomponent/qui-card';
import '@qomponent/qui-badge';
import {Notification} from '@vaadin/notification';
import {marked} from 'marked';
import './mvnpm-jar-view.js';

interface Coordinates {
  name: string;
  version: string;
}

/**
 * This component shows the Home screen
 *
 */
@customElement('mvnpm-home')
export class MvnpmHome extends ThemeMixin(LitElement) {

  static styles = css`
      :host {
          width: 100%;
          display: flex;
          flex-direction: column;
          align-items: center;
      }

      input:-webkit-autofill,
      input:-webkit-autofill:focus {
          transition: background-color 0s 600000s, color 0s 600000s !important;
      }

      /* --- Coordinates Bar --- */
      .coordinates-pane {
          width: 100%;
          display: flex;
          justify-content: center;
          align-items: center;
          flex-direction: column;
          column-gap: 15px;
          padding-top: 32px;
          padding-bottom: 8px;
      }

      vaadin-progress-bar {
          margin: 8px;
          height: 4px;
      }

      vaadin-tab[selected] {
          --vaadin-tab-text-color: var(--lumo-primary-color);
      }

      .coordinates-name {
          width: 500px;
      }

      .coordinates-version {
          width: 100px;
      }

      .coordinates {
          width: 100%;
          display: flex;
          justify-content: center;
          align-items: baseline;
          column-gap: 15px;
      }

      /* --- Hero Section --- */
      .hero {
          display: flex;
          flex-direction: column;
          align-items: center;
          width: 100%;
          padding: 60px 24px 20px;
          gap: 16px;
      }

      .hero-title {
          font-size: 2.4rem;
          font-weight: 700;
          text-align: center;
          line-height: 1.2;
          margin: 0;
          letter-spacing: -0.02em;
      }

      .hero-title .accent {
          background: linear-gradient(135deg, var(--mvnpm-amber, #F59E0B), var(--mvnpm-indigo, #6366F1));
          -webkit-background-clip: text;
          -webkit-text-fill-color: transparent;
          background-clip: text;
      }

      .hero-subtitle {
          font-size: 1.1rem;
          color: var(--mvnpm-text-secondary, var(--lumo-secondary-text-color));
          text-align: center;
          max-width: 560px;
          margin: 0;
          line-height: 1.6;
      }

      /* --- How It Works --- */
      .how-it-works {
          display: flex;
          align-items: flex-start;
          justify-content: center;
          gap: 0;
          padding: 40px 24px 16px;
          width: 100%;
          max-width: 700px;
      }

      .how-step {
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: 10px;
          flex: 1;
          min-width: 0;
      }

      .how-step-icon {
          width: 56px;
          height: 56px;
          border-radius: var(--mvnpm-radius-md, 10px);
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 24px;
          font-weight: 700;
          border: 1px solid var(--mvnpm-border, var(--lumo-contrast-10pct));
          background: var(--mvnpm-bg-surface, var(--lumo-contrast-5pct));
          font-family: var(--mvnpm-font-mono, monospace);
          color: var(--mvnpm-text-primary);
      }

      .how-step-icon.npm {
          border-color: var(--mvnpm-text-primary, #18181B);
          color: var(--mvnpm-text-primary, #18181B);
      }

      .how-step-icon.mvnpm {
          border-image: linear-gradient(135deg, var(--mvnpm-amber, #F59E0B), var(--mvnpm-indigo, #6366F1)) 1;
          background: linear-gradient(135deg, var(--mvnpm-amber, #F59E0B), var(--mvnpm-indigo, #6366F1));
          -webkit-background-clip: text;
          -webkit-text-fill-color: transparent;
          background-clip: text;
      }

      .how-step-icon.maven {
          border-color: var(--mvnpm-amber, #F59E0B);
          color: var(--mvnpm-amber, #F59E0B);
      }

      .how-step-label {
          font-size: 0.85rem;
          font-weight: 600;
          color: var(--mvnpm-text-secondary, var(--lumo-secondary-text-color));
      }



      .how-step-desc {
          font-size: 0.75rem;
          color: var(--mvnpm-text-tertiary, var(--lumo-tertiary-text-color));
          text-align: center;
          max-width: 140px;
      }

      .how-arrow {
          font-size: 28px;
          font-weight: 700;
          color: var(--mvnpm-amber, #F59E0B);
          padding: 0 12px;
          height: 56px;
          display: flex;
          align-items: center;
      }

      /* --- Recently Synced --- */
      .recent-section {
          width: 100%;
          max-width: 700px;
          padding: 16px 24px 32px;
      }

      .recent-header {
          font-size: 0.8rem;
          font-weight: 600;
          text-transform: uppercase;
          letter-spacing: 0.08em;
          color: var(--mvnpm-text-tertiary, var(--lumo-tertiary-text-color));
          margin-bottom: 12px;
      }

      .recent-grid {
          display: grid;
          grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
          gap: 10px;
      }

      .recent-item {
          display: flex;
          flex-direction: column;
          gap: 4px;
          padding: 12px 14px;
          border-radius: var(--mvnpm-radius-sm, 6px);
          border: 1px solid var(--mvnpm-border, var(--lumo-contrast-10pct));
          background: var(--mvnpm-bg-surface, var(--lumo-contrast-5pct));
          cursor: pointer;
          transition: border-color 0.15s ease;
          overflow: hidden;
      }

      .recent-item:hover {
          border-color: var(--mvnpm-indigo, var(--lumo-primary-color));
      }

      .recent-item-name {
          font-size: 0.85rem;
          font-weight: 600;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
      }

      .recent-item-version {
          font-size: 0.75rem;
          color: var(--mvnpm-text-tertiary, var(--lumo-tertiary-text-color));
          font-family: var(--mvnpm-font-mono, monospace);
      }

      /* --- Tab/Package Detail --- */
      .tabpane {
          width: 100%;
          display: flex;
          flex-direction: column;
          padding: 0 16px;
      }

      .info {
          padding: 8px 0;
      }

      .info h1 {
          font-size: 1.6rem;
          font-weight: 700;
          margin: 8px 0 4px;
          letter-spacing: -0.01em;
      }

      .fileList {
          display: flex;
          flex-direction: column;
          gap: 8px;
          width: 220px;
          min-width: 220px;
          padding-right: 16px;
          border-right: 1px solid var(--mvnpm-border, var(--lumo-contrast-10pct));
          margin-right: 16px;
      }

      a {
          color: var(--mvnpm-text-link, var(--lumo-contrast-60pct));
          text-decoration: none;
          transition: color 0.15s ease;
      }

      a:hover {
          color: var(--mvnpm-text-link-hover, var(--lumo-body-text-color));
      }

      .block {
          display: flex;
          gap: 10px;
      }

      .heading {
          font-weight: 600;
          font-size: 0.75rem;
          text-transform: uppercase;
          letter-spacing: 0.06em;
          background-color: var(--mvnpm-bg-surface, var(--lumo-contrast-10pct));
          border-radius: var(--mvnpm-radius-sm, 6px);
          padding: 6px 10px;
          color: var(--mvnpm-text-tertiary, var(--lumo-tertiary-text-color));
          margin-top: 4px;
      }

      .use {
          display: flex;
          gap: 10px;
      }

      qui-code-block {
          width: 100%;
      }

      .fileBrowser {
          display: flex;
          gap: 0;
          padding-top: 10px;
      }

      .line {
          display: flex;
          justify-content: space-between;
          padding: 2px 0;
          font-size: 0.85rem;
      }

      .line .outIcon {
          font-size: 11px;
          opacity: 0.5;
      }

      .line .outIcon:hover {
          opacity: 1;
      }

      .line a {
          cursor: pointer;
      }

      .sync {
          display: flex;
          gap: 10px;
      }

      .codeView {
          width: 100%;
      }

      .nopreview {
          width: 100%;
          color: var(--mvnpm-text-tertiary, var(--lumo-tertiary-text-color));
          font-size: 2rem;
          font-weight: bold;
          text-transform: uppercase;
          text-align: center;
      }

      .clearButton {
          align-self: end;
      }

      .info-line {
          display: flex;
          justify-content: space-between;
          width: 100%;
          align-items: flex-start;
      }

      .info-buttons {
          display: flex;
          gap: 12px;
          font-size: 0.8rem;
          align-items: center;
          flex-wrap: wrap;
          justify-content: flex-end;
      }

      .info-buttons a, .info-buttons span {
          display: inline-flex;
          align-items: center;
          gap: 4px;
          padding: 4px 10px;
          border-radius: var(--mvnpm-radius-sm, 6px);
          border: 1px solid var(--mvnpm-border, var(--lumo-contrast-10pct));
          background: var(--mvnpm-bg-surface, var(--lumo-contrast-5pct));
          transition: border-color 0.15s ease, background-color 0.15s ease;
          white-space: nowrap;
      }

      .info-buttons a:hover {
          border-color: var(--mvnpm-text-tertiary);
          background: var(--mvnpm-bg-elevated, var(--lumo-contrast-10pct));
      }

      .badge {
          background-color: var(--mvnpm-indigo, #6366F1) !important;
          color: white !important;
          border-color: var(--mvnpm-indigo, #6366F1) !important;
          padding: 4px 10px;
          text-align: center;
          border-radius: var(--mvnpm-radius-sm, 6px);
          font-size: 12px;
          font-weight: 600;
          height: fit-content;
      }

      .description-line {
          color: var(--mvnpm-text-secondary, var(--lumo-secondary-text-color));
          font-size: 0.95rem;
      }

      .dependencies {
          padding-top: 20px;
          display: flex;
          width: 100%;
          justify-content: center;
          gap: 16px;
      }

      @media (max-width: 1000px) {
          .dependencies {
              flex-direction: column;
          }
      }

      .copy {
          width: 18px;
          cursor: pointer;
          opacity: 0.6;
          transition: opacity 0.15s ease;
      }

      .copy:hover {
          opacity: 1;
      }

      .gaveventlogconsole {
          display: flex;
          flex-direction: column;
          padding: 16px 20px;
          background: var(--mvnpm-code-bg, #151722);
          border: 1px solid var(--mvnpm-border, var(--lumo-contrast-10pct));
          border-radius: var(--mvnpm-radius-md, 10px);
          font-family: var(--mvnpm-font-mono, 'Courier New', monospace);
          font-size: 13px;
      }

      .gaveventlogline {
          display: flex;
          flex-direction: row;
          gap: 10px;
      }

      .dependencyTable {
          width: 100%;
          border-collapse: collapse;
      }

      .dependencyTable td {
          padding: 6px 8px;
          font-size: 0.85rem;
          font-family: var(--mvnpm-font-mono, monospace);
          border-bottom: 1px solid var(--mvnpm-border, var(--lumo-contrast-5pct));
          transition: background-color 0.1s ease;
      }

      .dependencyTable tr:hover td {
          background: var(--mvnpm-bg-surface, var(--lumo-contrast-5pct));
      }

      .dependencyTable tr:last-child td {
          border-bottom: none;
      }

      /* --- Search Results --- */
      .searchResults {
          display: flex;
          flex-direction: column;
          gap: 12px;
          width: 100%;
          max-width: 900px;
          padding: 16px 24px;
          box-sizing: border-box;
          align-self: center;
      }

      .searchResultName {
          font-size: 20px;
          font-weight: bold;
          margin-top: 0px;
          margin-bottom: 1px;
      }

      .searchResultCard {
          transition: border-color 0.15s ease, box-shadow 0.15s ease;
      }

      .searchResultCard:hover {
          border-color: var(--mvnpm-indigo, var(--lumo-primary-color));
          box-shadow: 0 2px 8px rgba(99, 102, 241, 0.08);
          cursor: pointer;
      }

      .searchResultContent:hover {
          cursor: unset;
      }

      .searchResultDescription {
          padding-right: 10px;
          padding-left: 10px;
          font-size: 0.9rem;
          color: var(--mvnpm-text-secondary, var(--lumo-secondary-text-color));
      }

      .searchResultKeywords {
          display: flex;
          gap: 6px;
          padding: 10px;
          flex-wrap: wrap;
      }

      .searchResultDetails {
          display: flex;
          justify-content: space-between;
      }

      .searchResultLinks {
          padding: 15px;
          display: flex;
          gap: 12px;
          font-size: 0.85rem;
      }

      .infoCard {
          width: 100%;
      }

      .infoCardHeader {
          display: flex;
          width: 100%;
          justify-content: space-between;
          align-items: center;
      }

      /* --- Desktop SPA layout --- */
      @media (min-width: 769px) {
          :host {
              overflow: hidden;
          }
          .searchResults {
              flex: 1;
              min-height: 0;
              overflow-y: auto;
              max-width: none;
              padding-left: max(24px, calc((100% - 852px) / 2));
              padding-right: max(24px, calc((100% - 852px) / 2));
          }
          .fileBrowser {
              height: calc(100vh - var(--mvnpm-header-height) - var(--mvnpm-footer-height) - 180px);
          }
          .fileList {
              height: 100%;
              box-sizing: border-box;
              overflow-y: auto;
          }
          .fileBrowser > qui-code-block,
          .fileBrowser > mvnpm-jar-view {
              flex: 1;
              min-width: 0;
              height: 100%;
              overflow: auto;
          }
          .gaveventlogconsole {
              height: calc(100vh - var(--mvnpm-header-height) - var(--mvnpm-footer-height) - 180px);
              overflow-y: auto;
              box-sizing: border-box;
          }
      }

      @media (max-width: 768px) {
          .hero {
              padding: 32px 16px 16px;
          }
          .hero-title {
              font-size: 1.8rem;
          }
          .hero-subtitle {
              font-size: 0.95rem;
          }
          .coordinates-pane {
              padding: 16px 12px 0;
              box-sizing: border-box;
          }
          .coordinates {
              flex-wrap: wrap;
              column-gap: 8px;
              row-gap: 0;
          }
          .coordinates-name {
              width: 100%;
          }
          .coordinates-version {
              width: auto;
              flex: 1;
              min-width: 0;
          }
          .how-it-works {
              padding: 24px 16px 8px;
          }
          .how-step-desc {
              display: none;
          }
          .how-arrow {
              font-size: 22px;
              padding: 0 6px;
          }
          .recent-section {
              padding: 12px 16px 24px;
          }
          .recent-grid {
              grid-template-columns: 1fr 1fr;
          }
          .info-line {
              flex-direction: column;
              gap: 8px;
          }
          .info-buttons {
              gap: 6px;
          }
          .fileBrowser {
              flex-direction: column;
          }
          .fileList {
              width: 100%;
              min-width: unset;
              border-right: none;
              border-bottom: 1px solid var(--mvnpm-border, var(--lumo-contrast-10pct));
              padding-right: 0;
              padding-bottom: 12px;
              margin-right: 0;
              margin-bottom: 12px;
          }
          .searchResults {
              padding: 16px 12px;
          }
          .dependencies {
              flex-direction: column;
          }
      }

      @media (max-width: 480px) {
          .hero {
              padding: 20px 12px 12px;
          }
          .hero-title {
              font-size: 1.4rem;
          }
          .hero-subtitle {
              font-size: 0.85rem;
          }
          .how-step-icon {
              width: 44px;
              height: 44px;
              font-size: 18px;
          }
          .how-step-label {
              font-size: 0.75rem;
          }
          .how-arrow {
              font-size: 18px;
              padding: 0 4px;
              height: 44px;
          }
          .recent-grid {
              grid-template-columns: 1fr;
          }
          .info h1 {
              font-size: 1.2rem;
          }
          .tabpane {
              padding: 0 8px;
          }
          .gaveventlogline {
              flex-wrap: wrap;
              gap: 4px;
          }
      }
  `;

  @state()
  private _coordinates = {};
  @state()
  private _info = null;
  @state()
  private _baseUrl?: string;
  @state()
  private _baseFile?: string;
  @state()
  private _disabled = "disabled";
  @state()
  private _usePom?: string;
  @state()
  private _useJson?: string;
  @state()
  private _versions?: string[];
  @state()
  private _latestVersion?: string;
  @state()
  private _codeViewMode?: string;
  @state()
  private _codeViewSrc?: string;
  @state()
  private _codeViewSelection = ".pom";
  @state()
  private _loadingIcon = "hidden";
  @state()
  private _searchResults: string;
  @state()
  private _centralSyncItem?: object;
  @state()
  private _gavEventLog?: object;
  @state()
  private _scope = "provided";

  @state()
  private _recentReleases?: any[];

  private _onPopState: () => void;

  constructor() {
    super();
    this._clearCoordinates();
    this._disabled = "disabled";
    this._codeViewSelection = ".pom";
    this._centralSyncItem = null;
    this._gavEventLog = null;

    var currentPath = window.location.pathname;
    if (currentPath.startsWith("/package/")) {
      this._coordinates.name = currentPath.substring(9);
      this._showGA(this._coordinates.name);
    } else if (currentPath.startsWith("/search/")) {
      this._showGA(currentPath.substring(8));
    }
  }

  connectedCallback() {
    super.connectedCallback();
    this._fetchRecentReleases();
    this._onPopState = this._handlePopState.bind(this);
    window.addEventListener('popstate', this._onPopState);
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    window.removeEventListener('popstate', this._onPopState);
  }

  private _handlePopState() {
    var currentPath = window.location.pathname;
    if (currentPath.startsWith("/package/")) {
      this._searchResults = null;
      this._info = null;
      this._coordinates = {};
      this._coordinates.name = currentPath.substring(9);
      this._showGA(this._coordinates.name);
    } else if (currentPath.startsWith("/search/")) {
      this._coordinates.version = null;
      this._info = null;
      this._search(currentPath.substring(8), false);
    } else {
      this._clearCoordinates();
    }
  }

  private _fetchRecentReleases() {
    fetch("/api/sync/item/RELEASED")
      .then(response => response.json())
      .then(items => {
        this._recentReleases = items.slice(0, 12);
      })
      .catch(() => {});
  }

  render() {
    return html`
      ${this._renderCoordinatesPane()}
      ${this._renderMiddlePane()}
    `;
  }

  _renderCoordinatesPane() {
    return html`
      <div class="coordinates-pane">
        ${this._renderCoordinates()}
      </div>`;
  }

  _renderCoordinates() {
    return html`
      <div class="coordinates">
        <vaadin-text-field id="coordinates-field" label="Name (Package or Coordinates)" class="coordinates-name"
                           @keypress="${this._findVersionsAndShowLatest}"
                           @input="${this._coordinatesNameChanged}"
                           value="${this._coordinates.name}" clear-button-visible></vaadin-text-field>
        ${this._renderVersionForm()}
        <vaadin-button class="clearButton" theme="secondary" @click="${this._clearCoordinates}">Clear</vaadin-button>
      </div>
      <div class="coordinates">
        <vaadin-progress-bar class="progress" style="visibility: ${this._loadingIcon};" indeterminate></vaadin-progress-bar>
      </div>`;
  }

  _renderVersionForm() {
    return html`
      <vaadin-combo-box ?disabled=${this._disabled}
                        label="Version"
                        item-label-path="value"
                        item-value-path="value"
                        .items="${this._versions}"
                        value="${this._latestVersion}"
                        @change="${this._versionChanged}"
                        class="coordinates-version"
      ></vaadin-combo-box>`

  }

  _renderMiddlePane() {
    if (this._coordinates && this._coordinates.version) {
      return this._renderTabPane();
    } else if (this._searchResults) {
      return this._renderSearchResults();
    } else {
      return this._renderHero();
    }
  }

  _renderHero() {
    return html`
      <div class="hero">
        <h1 class="hero-title">NPM packages as <span class="accent">Maven dependencies</span></h1>
        <p class="hero-subtitle">Search any NPM package and get Maven/Gradle coordinates instantly. Packages are automatically synced to Maven Central.</p>
      </div>

      <div class="how-it-works">
        <div class="how-step">
          <div class="how-step-icon npm">{ }</div>
          <div class="how-step-label">NPM Registry</div>
          <div class="how-step-desc">Source packages from npmjs.com</div>
        </div>
        <div class="how-arrow">&#10230;</div>
        <div class="how-step">
          <div class="how-step-icon mvnpm">{/&gt;</div>
          <div class="how-step-label">mvnpm</div>
          <div class="how-step-desc">Converts to JARs, POMs &amp; signs</div>
        </div>
        <div class="how-arrow">&#10230;</div>
        <div class="how-step">
          <div class="how-step-icon maven">&lt;/&gt;</div>
          <div class="how-step-label">Maven Central</div>
          <div class="how-step-desc">Use like any Maven dependency</div>
        </div>
      </div>

      ${this._renderRecentReleases()}
    `;
  }

  _renderRecentReleases() {
    if (this._recentReleases && this._recentReleases.length > 0) {
      return html`
        <div class="recent-section">
          <div class="recent-header">Recently synced</div>
          <div class="recent-grid">
            ${this._recentReleases.map(item => html`
              <div class="recent-item" @click="${() => this._browseRecent(item)}">
                <div class="recent-item-name">${item.artifactId}</div>
                <div class="recent-item-version">${item.version}</div>
              </div>
            `)}
          </div>
        </div>
      `;
    }
    return html``;
  }

  _browseRecent(item) {
    this._coordinates.name = item.groupId + ":" + item.artifactId;
    this._showGA(this._coordinates.name);
  }

  _renderTabPane() {
    if (this._coordinates.version) {
      return html`
        <vaadin-tabsheet class="tabpane">
          <vaadin-tabs slot="tabs">
            <vaadin-tab id="info-tab">info</vaadin-tab>
            <vaadin-tab id="files-tab">files</vaadin-tab>
            <vaadin-tab id="event-log-tab">event log</vaadin-tab>
          </vaadin-tabs>

          <div tab="info-tab">
            ${this._loadInfoTab()}
          </div>

          <div tab="files-tab">
            ${this._loadFilesTab()}
          </div>

          <div tab="event-log-tab">
            ${this._loadEventLogTab()}
          </div>

        </vaadin-tabsheet>`;
    }
  }

  _renderSearchResults() {
    if (this._searchResults.objects) {
      const objects = this._searchResults.objects;
      return html`
        <div class="searchResults">
          ${objects.map((result) =>
              html`
                <qui-card class="searchResultCard" header="${result.package.name} : ${result.package.version}"
                          data-package="${result.package.name}" @click="${this._selectSearchResult}">
                  <div class="searchResultContent" slot="content">
                    <div class="searchResultDetails">
                      <div class="searchResultDescription">
                        ${this._renderPackageDescription(result)}
                      </div>
                      <div class="searchResultLinks">
                        <a href="${result.package.links.npm}" target="_blank">
                          <vaadin-icon title="Go to NPM registry page" icon="vaadin:tag"></vaadin-icon>
                          npm registry
                        </a>
                        <a href="${result.package.links.homepage}" target="_blank">
                          <vaadin-icon icon="vaadin:external-link"></vaadin-icon>
                          page
                        </a>
                      </div>
                    </div>
                    ${this._renderSearchKeywords(result.package.keywords)}

                  </div>
                </qui-card>`
          )}
        </div>`;
    }
  }

  _renderPackageDescription(result) {
    if (result.package.description) {
      return html`
        ${unsafeHTML(marked(result.package.description))}
        ${this._renderBy(result)}`;
    }
  }

  _renderBy(result) {
    if (result.package.author && result.package.author.name) {
      return html` - by ${result.package.author.name}`;
    } else if (result.package.publisher) {
      if (result.package.publisher.name) {
        return html` - by ${result.package.publisher.name}`;
      } else if (result.package.publisher.username) {
        return html` - by ${result.package.publisher.username}`;
      }
    }
  }

  _selectSearchResult(e) {
    this._coordinates.version = null;
    this._coordinates.name = e.currentTarget.dataset.package;
    this._showGA(this._coordinates.name, false);
  }

  _renderSearchKeywords(keywords) {
    if (keywords) {
      return html`
        <div class="searchResultKeywords">
          ${keywords.map((keyword) =>
              html`
                <qui-badge level="contrast" small><span>${keyword}</span></qui-badge>`
          )}
        </div>`;
    }
  }

  _showNpmjsLink() {
    if (this._centralSyncItem) {
      var npmUrl = "/api/info/npm/" + this._centralSyncItem.groupId + "/" + this._centralSyncItem.artifactId + "?version=" + this._centralSyncItem.version;

      return html`<a href="${npmUrl}" target="_blank">
        <vaadin-icon title="Go to NPM registry page" icon="vaadin:tag"></vaadin-icon>
        npm registry
      </a>`;
    }
  }

  _loadSyncIcon() {
    if (this._centralSyncItem) {
      if (this._centralSyncItem.stage === "RELEASED") {
        return html`<span><vaadin-icon title="Stage: ${this._centralSyncItem.stage}" style="color:var(--lumo-success-color)"
                                       icon="vaadin:check-circle"></vaadin-icon> Maven central</span>`;
      } else if (this._centralSyncItem.started) {
        return html`<span><vaadin-icon title="Stage: ${this._centralSyncItem.stage}" style="color:var(--lumo-warning-color)"
                                       icon="vaadin:progressbar"></vaadin-icon> Maven central</span>`;
      } else {
        return html`<span style="cursor: pointer;" @click="${this._requestFullSync}"><vaadin-icon
            title="Stage: ${this._centralSyncItem.stage}" style="color:var(--lumo-error-color)"
            icon="vaadin:close-circle"></vaadin-icon> Maven central</span>`;
      }
    }
    return html`<span><vaadin-icon title="Checking..." style="color:var(--lumo-warning-color)"
                                   icon="vaadin:question-circle-o"></vaadin-icon> Maven central</span>`;
  }

  _requestFullSync() {
    var fullSyncRequest = "/api/sync/request/" + this._centralSyncItem.groupId + "/" + this._centralSyncItem.artifactId + "?version=" + this._centralSyncItem.version;

    fetch(fullSyncRequest)
      .then(response => response.json())
      .then(response => this._centralSyncItem = response);
  }

  _scopeChanged(e) {
    this._scope = e.target.value;
    this._changeVersion(this._coordinates.version);
  }

  _loadInfoTab() {
    if (this._info) {
      return html`
        <div class="info">
          <div class="info-line">
            <h1>${this._coordinates.name} ${this._coordinates.version}</h1>
            <div class="info-buttons">
              ${this._loadSyncIcon()}
              ${this._showNpmjsLink()}
              <a href="${this._info.scmUrl}" target="_blank">
                <vaadin-icon icon="vaadin:code"></vaadin-icon>
                code</a>
              <a href="${this._info.issueUrl}" target="_blank">
                <vaadin-icon icon="vaadin:bug-o"></vaadin-icon>
                issues</a>
              <a href="${this._info.url}" target="_blank">
                <vaadin-icon icon="vaadin:external-link"></vaadin-icon>
                page</a>
              <span class="badge">${this._info.licenseName}</span>
            </div>
          </div>
          <div class="info-line">
            <div class="description-line">${unsafeHTML(marked(this._info.description))}</div>
            <div>by <a href="${this._info.organizationUrl}">${this._info.organizationName}</a></div>
          </div>
          <div class="dependencies">
            <qui-card class="infoCard">
              <div slot="header" style="width:100%;">
                <div class="infoCardHeader">
                  <span>Pom dependency</span>
                  <vaadin-icon class="copy" title="copy to clipboard" icon="vaadin:copy-o"
                               @click=${this._pomToClipboard}></vaadin-icon>
                </div>
              </div>
              <div slot="content">
                <qui-code-block id="pom-dependency-code" mode="xml" theme="${this._theme}" content="${this._usePom}"></qui-code-block>
              </div>
              <div slot="footer">
                <vaadin-radio-group @change="${this._scopeChanged}">
                  <vaadin-radio-button value="runtime" label="runtime"></vaadin-radio-button>
                  <vaadin-radio-button value="provided" label="provided" checked></vaadin-radio-button>
                </vaadin-radio-group>
              </div>
            </qui-card>
            <qui-card class="infoCard" header="Import map (Runtime)">
              <div slot="content">
                <qui-code-block id="import-map-code" mode="json" theme="${this._theme}" content="${this._useJson}"></qui-code-block>
              </div>
            </qui-card>
            <qui-card class="infoCard" header="Dependencies">
              <div slot="content">
                <table class="dependencyTable">
                  ${this._info.dependencies.map((dependency) =>
                      html`
                        <tr>
                          <td style="cursor: pointer;" @click="${() => this._viewDependency(dependency)}">${dependency}</td>
                        </tr>`
                  )}
                </table>
              </div>
            </qui-card>
          </div>
        </div>
      `;
    }
  }

  _pomToClipboard() {
    navigator.clipboard.writeText(this._usePom);
  }

  _viewDependency(dependency) {
    const gav = dependency.split(":");
    this._clearCoordinates()
    this._coordinates.name = gav[0] + ":" + gav[1];
    window.history.pushState({/* State */}, "", "/package/" + this._coordinates.name);
    this._showGA(this._coordinates.name);
  }

  _loadFilesTab() {
    if (this._baseUrl) {
      let released = this._centralSyncItem?.stage === 'RELEASED';
      return html`
        <div class="fileBrowser">
          <div class="fileList">
            ${this._renderFileGroup('pom', '.pom', 'file-code')}
            ${this._renderFileGroup('jar', '.jar', 'file-zip')}
            ${this._renderFileGroup('source', '-sources.jar', 'file-zip')}
            ${this._renderFileGroup('javadoc', '-javadoc.jar', 'file-zip')}
            ${!released ? this._renderFileGroup('original', '.tgz', 'file-zip') : ''}
            ${this._renderAnyFile('package', '.json', 'file-text-o')}
          </div>
          ${this._renderCodeView()}
        </div>`;
    }
  }

  _loadEventLogTab() {
    if (this._gavEventLog) {
      return html`
        <div class="gaveventlogconsole">
          ${this._renderGavEventLog()}
        </div>`;
    }
  }

  private _renderGavEventLog() {
    if (this._gavEventLog && this._gavEventLog.length > 0) {
      return html`
        ${this._gavEventLog.map((entry) => {
          return html`${this._renderGavEventLogLine(entry)}`
        })}
      `;
    } else {
      return html`<p>Nothing in the event log</p>`;
    }
  }

  private _renderGavEventLogLine(entry) {
    let formattedTime = entry.time.substring(0, entry.time.indexOf(".")).replace('T', ' ');

    return html`
      <div class="gaveventlogline">
        <span style="color: var(--mvnpm-log-time, grey)">${formattedTime}</span>
        <span style="color: var(--mvnpm-log-group, lightblue)">${entry.groupId}</span>
        <span style="color: var(--mvnpm-log-artifact, lightyellow)">${entry.artifactId}</span>
        <span style="color: var(--mvnpm-log-version, lightpink)">${entry.version}</span>
        <span style="color: var(--mvnpm-log-stage, lightgrey)">[${entry.stage}]</span>
        <span style="color: var(--mvnpm-log-${entry.color}, ${entry.color})">${entry.message}</span>
      </div>`;
  }


  _renderFileGroup(heading, fileExt, icon) {
    return html`
      <span class="heading">${heading}</span>
      ${this._renderLine(fileExt, icon)}
      ${this._renderLine(fileExt + '.sha1', 'file-text-o')}
      ${this._renderLine(fileExt + '.md5', 'file-text-o')}
      ${this._renderLine(fileExt + '.asc', 'file-text-o')}
    `;
  }

  _renderLine(fileExt, icon) {
    return this._renderAnyFile(this._baseFile, fileExt, icon);
  }

  _renderAnyFile(fileName, fileExt, icon) {
    let url =  this._baseUrl + fileName + fileExt;
    return html`
      <div class="line">
        <a @click="${this._showFile}" data-file="${url}">
          <vaadin-icon icon="vaadin:${icon}"></vaadin-icon>
          ${fileName + fileExt}</a>
        <a href="${url + "?redirect=true"}" target="_blank">
          <vaadin-icon class="outIcon" icon="vaadin:external-link"></vaadin-icon>
        </a>
      </div>
    `;
  }

  _renderCodeView() {
    if (this._codeViewMode) {
      return html`
        <qui-code-block mode="${this._codeViewMode}" theme="${this._theme}" src='${this._codeViewSrc}?proxy=true'></qui-code-block>`;
    } else {
      return html`
        <mvnpm-jar-view jarName="${this._codeViewSrc}?proxy=true"></mvnpm-jar-view>`;
    }
  }

  _showFile(e) {
    this._changeCodeView(e.target.dataset.file);
  }

  _changeCodeView(src) {
    this._codeViewSrc = src;
    if (this._codeViewSrc.endsWith('.jar') || this._codeViewSrc.endsWith('.tgz')) {
      this._codeViewMode = null;
    } else if (this._codeViewSrc.endsWith('.pom')) {
      this._codeViewMode = "xml";
    } else if (this._codeViewSrc.endsWith('.asc')) {
      this._codeViewMode = "asc";
    } else {
      this._codeViewMode = "default";
    }

    var n = this._baseUrl.length + this._baseFile.length;
    this._codeViewSelection = this._codeViewSrc.substring(n);
  }

  _findVersionsAndShowLatest(e) {
    if ((e.which == 13 || e.which == 0)) {
      this._showGA(this._coordinates.name.trim());
    } else {

    }
  }

  _showGA(name, fallbackToSearch = true) {
    if (name && name.length > 0) {
      let groupPath: string;
      let artifactPath: string;
      if (name.match(/^[^:]+:[^:]+$/)) {
        this._loadingIcon = "visible";
        const ga = name.split(":");
        groupPath = ga[0].trim().replaceAll('.', '/');
        if (!groupPath.startsWith("org/mvnpm")) {
          groupPath = `org/mvnpm/${groupPath}`;
        }
        artifactPath = ga[1].trim();
      } else {
        this._loadingIcon = "visible";
        groupPath = "org/mvnpm";
        artifactPath = name.replaceAll('@', 'at/');
      }

      const metadataUrl = `/maven2/${groupPath}/${artifactPath}/maven-metadata.xml`;

      fetch(metadataUrl)
        .then((response) => {
          if (response.ok) {
            let contentLength = response.headers.get('Content-Length');
            if (contentLength == null || parseInt(contentLength, 10) > 0) {
              this._stopLoading();
              return response.text();
            }
          }
          throw new Error();
        })
        .then(xmlDoc => new window.DOMParser().parseFromString(xmlDoc, "text/xml"))
        .then(metadata => this._inspectMetadata(metadata))
        .catch(error => {
          if (fallbackToSearch) {
            this._search(name);
          } else {
            this._stopLoading();
            Notification.show(name + ' is being synced, please try again shortly', {
              position: 'top-center',
              duration: 3000,
            });
          }
        });

    }
  }

  _search(name, updateHistory = true) {
    var searchUrl = "/api/info/search/" + name;
    fetch(searchUrl)
      .then((response) => {
        this._stopLoading();
        if (response.ok) {
          return response.json();
        } else if (response.status === 404) {
          const notification = Notification.show(name + ' not found', {
            position: 'top-center',
            duration: 5000,
          });
        } else {
          const notification = Notification.show('Error: ' + response.status + ' - ' + response.statusText, {
            position: 'top-center',
            duration: 5000,
          });
        }
      })
      .then(jsonResult => {
        if (updateHistory) {
          window.history.pushState({/* State */}, "", "/search/" + name);
        }
        this._coordinates.version = null;
        this._searchResults = jsonResult;
      });
  }

  _inspectMetadata(metadata) {
    this._coordinates.groupId = metadata.getElementsByTagName("groupId")[0].childNodes[0].nodeValue.substring(9);
    this._coordinates.artifactId = metadata.getElementsByTagName("artifactId")[0].childNodes[0].nodeValue;

    if (this._coordinates.groupId) {
      window.history.pushState({/* State */}, "", "/package/org.mvnpm" + this._coordinates.groupId + ":" + this._coordinates.artifactId);
    } else {
      window.history.pushState({/* State */}, "", "/package/" + this._coordinates.artifactId);
    }


    var latestTags = metadata.getElementsByTagName("latest");
    var versionTags = metadata.getElementsByTagName("version");

    this._latestVersion = latestTags[0].childNodes[0].nodeValue;
    var s = [...new Set(Array.from(versionTags).map(e => e.childNodes[0].nodeValue))].sort(compareVersions).reverse();
    this._versions = s.map(e => ({value: e}));

    this._changeVersion(this._latestVersion);
  }

  _stopLoading() {
    this._disabled = null;
    this._loadingIcon = "hidden";
  }

  _inspectModel(projectModel) {
    let model = new Object();

    model.description = this._getElementValue(projectModel, "description");
    model.url = this._getElementValue(projectModel, "url");
    model.organizationName = this._getElementX2Value(projectModel, "organization", "name");
    model.organizationUrl = this._getElementX2Value(projectModel, "organization", "url");
    model.licenseName = this._getElementX3Value(projectModel, "licenses", "license", "name");
    model.scmUrl = this._getElementX2Value(projectModel, "scm", "url");
    model.issueUrl = this._getElementX2Value(projectModel, "issueManagement", "url");

    let propertiesMap = new Map();
    let properties = [];
    let propertiesElement = projectModel.getElementsByTagName("properties");
    if (propertiesElement && propertiesElement.length > 0) {
      properties = propertiesElement[0].children;
    }

    for (let i = 0; i < properties.length; i++) {
      let prop = properties[i];
      propertiesMap.set(prop.tagName, prop.textContent);
    }

    model.dependencies = [];
    let dependenciesElement = projectModel.getElementsByTagName("dependencies");
    if (dependenciesElement && dependenciesElement.length > 0) {
      let dependencies = dependenciesElement[0].getElementsByTagName("dependency");

      for (let i = 0; i < dependencies.length; i++) {
        let dependency = dependencies[i];
        let groupId = dependency.getElementsByTagName("groupId")[0].childNodes[0].nodeValue;
        let artifactId = dependency.getElementsByTagName("artifactId")[0].childNodes[0].nodeValue;
        let version = dependency.getElementsByTagName("version")[0].childNodes[0].nodeValue;

        if (version.startsWith("$")) {
          version = version.substring(2).slice(0, -1);
          version = propertiesMap.get(version);
        }

        model.dependencies.push(groupId + ":" + artifactId + ":" + version);
      }
    }

    this._info = model;

  }

  _getElementValue(model, elementName) {
    let element = model.getElementsByTagName(elementName);
    if (element && element.length > 0) {
      let children = element[0].childNodes;
      if (children && children.length > 0) {
        return element[0].childNodes[0].nodeValue;
      }
    }
    return "";
  }

  _getElementX2Value(model, elementName1, elementName2) {
    let element1 = model.getElementsByTagName(elementName1);
    if (element1 && element1.length > 0) {
      let element2 = element1[0].getElementsByTagName(elementName2);
      if (element2 && element2.length > 0 && element2[0].childNodes?.length > 0) {
        return element2[0].childNodes[0].nodeValue;
      }
    }
    return "";
  }

  _getElementX3Value(model, elementName1, elementName2, elementName3) {
    let element1 = model.getElementsByTagName(elementName1);
    if (element1 && element1.length > 0) {
      let element2 = element1[0].getElementsByTagName(elementName2);
      if (element2 && element2.length > 0) {
        let element3 = element2[0].getElementsByTagName(elementName3);
        if (element3 && element3.length > 0) {
          return element3[0].childNodes[0].nodeValue;
        }
      }
    }
    return "";
  }

  _getGroupId(groupId) {
    if (!groupId) {
      groupId = "org.mvnpm";
    } else {
      if (!groupId.startsWith(".")) {
        groupId = "." + groupId;
      }
      groupId = groupId.replace('@', 'at.')
      if (!groupId.startsWith("org.mvnpm")) {
        groupId = "org.mvnpm" + groupId;
      }
    }
    return groupId;
  }

  _clearCoordinates() {
    this._info = null;
    this._baseUrl = null;
    this._baseFile = null;
    this._usePom = null;
    this._useJson = null;
    this._versions = null;
    this._latestVersion = null;
    this._codeViewSelection = ".pom";
    this._codeViewMode = "xml";
    this._loadingIcon = "hidden";
    this._searchResults = null;
    this._disabled = "disabled";
    this._coordinates = {};
  }

  _coordinatesNameChanged(e) {
    if (e.target.value.trim() === '') {
      this._clearCoordinates();
    } else {
      this._coordinates.name = e.target.value;
      this._disabled = "disabled";
    }
  }

  _versionChanged(e) {
    this._changeVersion(e.target.value.trim());
  }

  _changeVersion(version) {
    this._loadingIcon = "visible";
    this._coordinates.version = version;

    var groupId = this._getGroupId(this._coordinates.groupId.trim());

    var artifactId = this._coordinates.artifactId.trim();

    if (!version) {
      version = "latest";
    }

    groupId = groupId.replaceAll('/', '.');
    this._usePom = "<dependency>\n\t<groupId>" + groupId + "</groupId>\n\t<artifactId>" + artifactId + "</artifactId>\n\t<version>" + version + "</version>\n\t<scope>" + this._scope + "</scope>\n</dependency>";
    var getCentralSyncItemUrl = "/api/sync/info/" + groupId + "/" + artifactId + "?version=" + version;
    var eventLogUrl = `/api/eventlog/gav/${groupId}/${artifactId}/${version}`;
    fetch(eventLogUrl)
      .then(response => response.json())
      .then(response => this._gavEventLog = response);

    groupId = groupId.replaceAll('.', '/');
    var importMapUrl = "/maven2/" + groupId + "/" + artifactId + "/" + version + "/importmap.json";

    fetch(importMapUrl)
      .then(response => response.json())
      .then(response => this._setUseJson(response));

    this._baseFile = artifactId + "-" + version;
    this._baseUrl = "/maven2/" + groupId + "/" + artifactId + "/" + version + "/";

    this._changeCodeView(this._baseUrl + this._baseFile + this._codeViewSelection);

    fetch(getCentralSyncItemUrl)
      .then(response => response.json())
      .then(response => {
        this._centralSyncItem = response;
      });

    const pomUrl = `/maven2/${groupId}/${artifactId}/${version}/${artifactId}-${version}.pom?proxy=true`;
    fetch(pomUrl)
      .then(response => response.text())
      .then(xmlDoc => new window.DOMParser().parseFromString(xmlDoc, "application/xml"))
      .then(projectModel => this._inspectModel(projectModel));
  }

  _setUseJson(response) {
    this._useJson = JSON.stringify(response)
      .replaceAll(':{', ':{\n\t\t')
      .replaceAll('","', '",\n\t\t"')
      .replaceAll('"}', '"\n\t}')
      .replaceAll('}}', '}\n}')
      .replaceAll('{"', '{\n\t"');
    this._loadingIcon = "hidden";
  }
}