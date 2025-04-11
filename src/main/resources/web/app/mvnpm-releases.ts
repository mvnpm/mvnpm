import { LitElement, html, css } from 'lit';
import { customElement, state } from 'lit/decorators.js';
import '@vaadin/radio-group';
import '@vaadin/progress-bar';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';

/**
 * This component shows the Item stage screen
 */
@customElement('mvnpm-releases')
export class MvnpmReleases extends LitElement {
    static styles = css`
    :host {
        display: flex;
        flex-direction: column;
        gap: 10px;
        width: 100%;
        padding: 20px;
        padding-top: 60px;
    }
    
    .rotate {
        animation: rotation 1s infinite linear;
    }

    @keyframes rotation {
        from {
            transform: rotate(0deg);
        }
        to {
            transform: rotate(360deg);
        }
    }
    `;

    @state({ type: Array })
    private _itemList: any[] | null = null;

    @state() 
    private _selectedState?: string = "RELEASED";

    @state({ type: Boolean})
    private _disabled: boolean = false;

    connectedCallback() {
        super.connectedCallback();
        this._fetchSelectedItemList();
    }

    render() {
        return html`
            ${this._renderStageRadioBar()}
            ${this._renderItemTable()}
        `;
    }

    private _renderStageRadioBar() {
        return html`
            <vaadin-radio-group theme="horizontal" ?disabled=${this._disabled}>
                <vaadin-radio-button value="PACKAGING" label="Packaging" @change=${this._stageChange}></vaadin-radio-button>
                <vaadin-radio-button value="INIT" label="Pending" @change=${this._stageChange}></vaadin-radio-button>
                <vaadin-radio-button value="UPLOADING" label="Uploading" @change=${this._stageChange}></vaadin-radio-button>
                <vaadin-radio-button value="UPLOADED" label="Uploaded" @change=${this._stageChange}></vaadin-radio-button>
                <vaadin-radio-button value="CLOSED" label="Validating" @change=${this._stageChange}></vaadin-radio-button>
                <vaadin-radio-button value="RELEASING" label="Releasing" @change=${this._stageChange}></vaadin-radio-button>
                <vaadin-radio-button value="RELEASED" label="Released" @change=${this._stageChange} checked></vaadin-radio-button>
                <vaadin-radio-button value="ERROR" label="Error" @change=${this._stageChange}></vaadin-radio-button>
            </vaadin-radio-group>`;
    }

    private _renderItemTable() {
        if (this._itemList && this._itemList.length > 0) {
            return html`<vaadin-grid .items="${this._itemList}">
                <vaadin-grid-sort-column header="Group Id" path="groupId"></vaadin-grid-sort-column>
                <vaadin-grid-sort-column header="Artifact Id" path="artifactId"></vaadin-grid-sort-column>
                <vaadin-grid-sort-column header="Version" path="version"></vaadin-grid-sort-column>
                <vaadin-grid-sort-column header="Last modified" path="stageChangeTime"></vaadin-grid-sort-column>
                <vaadin-grid-sort-column header="Release Id" path="stagingRepoId"></vaadin-grid-sort-column>
                <vaadin-grid-sort-column header="Upload attemps" path="uploadAttempts"></vaadin-grid-sort-column>
                ${this._renderRetryCol()}
            </vaadin-grid>`;
        } else if(!this._itemList){
            return html`<vaadin-progress-bar class="progressBar" indeterminate></vaadin-progress-bar>`;
        } else {
            return html`<span>Nothing to display</span>`;
        }
    }

    private _renderRetryCol(){
        if(this._selectedState === "ERROR"){
            return html`<vaadin-grid-sort-column resizable
                                    header="Retry"
                                    ${columnBodyRenderer(this._retryRenderer, [])}>`;
        }
    }

    private _retryRenderer(item) {
        return html`<span style="cursor: pointer;" @click="${(e) => this._requestFullSync(e, item)}"><vaadin-icon style="color:var(--lumo-success-color)" icon="vaadin:refresh"></vaadin-icon></span>`;
    }

    private _requestFullSync(e, item){
        e.target.className = "rotate";
        var fullSyncRequest = "/api/sync/retry/" + item.groupId + "/" + item.artifactId + "?version=" + item.version;
        fetch(fullSyncRequest)
            .then(response => response.json())
            .then(response => this._fetchSelectedItemList());
    }

    private _stageChange(e){
        this._selectedState = e.target.value;
        this._fetchSelectedItemList();
    }
    
    private _fetchSelectedItemList(){
        this._itemList = null;
        this._disabled = true;
        fetch("/api/sync/item/" + this._selectedState)
            .then(response => response.json())
            .then(response => {
                this._itemList = response;
                this._disabled = false;
            });
    }
}