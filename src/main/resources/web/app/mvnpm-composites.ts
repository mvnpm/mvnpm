import { LitElement, html, css } from 'lit';
import { customElement, state } from 'lit/decorators.js';
import type { TabsSelectedChangedEvent } from '@vaadin/tabs';
import '@vaadin/combo-box';
import '@vaadin/progress-bar';
import '@vaadin/tabs';
import '@qomponent/qui-code-block';
import '@vaadin/icon';
import '@vaadin/vaadin-lumo-styles/vaadin-iconset.js';
import '@vaadin/icons';
import { Router } from '@vaadin/router';

/**
 * This component shows the composites
 */
@customElement('mvnpm-composites')
export class MvnpmComposites extends LitElement {
    static styles = css`
    :host {
        display: flex;
        gap: 10px;
        width: 100%;
        padding: 20px;
        padding-top: 60px;
    }
    .selected {
        display: flex:
        flex-direction: column;
        width: 100%;
    }
    qui-code-block {
        width: 100%;
    }
    .left {
        display: flex;
        flex-direction: column;
        justify-content: space-between;
        width: 400px;
    }
    .header {
        display:flex;
        width: 100%;
        justify-content: space-between;
    }
    .version {
        display:flex;
        gap: 5px;
    }
    `;

    @state({ type: Array })
    private _compositesList: any[] | null = null;

    @state({ type: Array })
    private _selectedVersions: any[] | null = null;

    @state()
    private _selectedComposite: {};

    @state({ type: Boolean})
    private _isPublishing: boolean = false;
    
    connectedCallback() {
        super.connectedCallback();
        this._fetchCompositesList();
    }

    render() {
        return html`
            ${this._renderCompositesTable()}
        `;
    }

    private _renderCompositesTable() {
        if (this._compositesList && this._compositesList.length > 0) {

            return html`<div class="left">
                            <vaadin-tabs orientation="vertical" @selected-changed="${this.selectedChanged}">
                                ${this._compositesList.map((c) =>
                                    html`<vaadin-tab>${c.name.replaceAll(".xml", "")}</vaadin-tab>`
                                )}
                            </vaadin-tabs>
                            <vaadin-button theme="primary success" @onclick="${this._requestRefresh}">Reload</vaadin-button>
                        </div>
                        ${this._renderSelectedComposite()}
                        `;
        } else if (!this._compositesList) {
            return html`<vaadin-progress-bar class="progressBar" indeterminate></vaadin-progress-bar>`;
        } else {
            return html`<span>Nothing to display</span>`;
        }
    }

    _renderSelectedComposite(){
        return html`<div class="selected">
                            <div class="header">
                                <h3>${this._selectedComposite.name.replaceAll(".xml", "")} 
                                <vaadin-icon icon="vaadin:external-link" style="color: lightgrey;height:15px; width: 15px;" @click="${this._browse}"></vaadin-icon></h3>
                                ${this._renderVersions()}
                            </div>
                            <qui-code-block mode="xml" src="${this._selectedComposite.download_url}"></qui-code-block>
                        </div>`;
    }

    _browse(){
        let artifactId = this._selectedComposite.name.replaceAll(".xml", "");
        Router.go("/package/org.mvnpm.at.mvnpm:" + artifactId);
    }

    _renderVersions(){
        if(this._isPublishing){
            return html`<vaadin-progress-bar style="width: 200px;" class="progressBar" indeterminate></vaadin-progress-bar>`;
        }else{
            return html`<div class="version">
                        <vaadin-combo-box id="version"
                            allow-custom-value
                            .items="${this._selectedVersions}"
                        ></vaadin-combo-box>
                        <vaadin-button theme="secondary success" @click="${this._publish}">Publish</vaadin-button>
                    </div>`;
        }
    }
    
    _publish(){
        let v = this.shadowRoot.getElementById('version').value;
        if(v){
            this._isPublishing = true;
            let artifactId = this._selectedComposite.name.replaceAll(".xml", "");
            fetch("/maven2/org/mvnpm/at/mvnpm/" + artifactId + "/" + v + "/" + artifactId + "-" + v + ".jar")
                .then(response => {
                    this._isPublishing = false;
                    this._getVersions(artifactId);
                });
        }
    }

    selectedChanged(e: TabsSelectedChangedEvent) {
        this._selectedComposite = this._compositesList[e.detail.value];
        this._selectedVersions = this._getVersions(this._selectedComposite.name.replaceAll(".xml", ""));
    }

    private _requestRefresh() {
        fetch("/api/composite/refresh");
        this._fetchCompositesList();
    }

    private _getVersions(name){
        this._selectedVersions = null;
        fetch("/api/composite/versions/" + name)
            .then(response => response.json())
            .then(response => {
                this._selectedVersions = response;
            });
    }

    private _fetchCompositesList() {
        this._compositesList = null;
        fetch("/api/composite")
            .then(response => response.json())
            .then(response => {
                this._compositesList = response;
                this._selectedComposite = this._compositesList[0];
            });
    }
}