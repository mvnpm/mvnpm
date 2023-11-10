import { LitElement, html, css } from 'lit';
import { customElement, state } from 'lit/decorators.js';
import '@vaadin/radio-group';
import '@vaadin/progress-bar';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-sort-column.js';

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
    }`;

    @state({ type: Array })
    private _itemList: any[] | null = null;


    constructor() {
        super();
        this._itemList = null;
    }

    connectedCallback() {
        super.connectedCallback();

        fetch("/api/sync/item/RELEASED")
            .then(response => response.json())
            .then(response => this._itemList = response);
    }

    disconnectedCallback() {
        super.disconnectedCallback();
    }

    render() {
        return html`
            ${this._renderStageRadioBar()}
            ${this._renderItemTable()}
        `;
    }

    private _renderStageRadioBar() {
        return html`
            <vaadin-radio-group label="Stage" theme="horizontal">
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
                <vaadin-grid-sort-column header="Staging Repo Id" path="stagingRepoId"></vaadin-grid-sort-column>
                <vaadin-grid-sort-column header="Upload attemps" path="uploadAttempts"></vaadin-grid-sort-column>
                <vaadin-grid-sort-column header="Promotion attemps" path="promotionAttempts"></vaadin-grid-sort-column>
            </vaadin-grid>`;
        } else if(!this._itemList){
            return html`<vaadin-progress-bar class="progressBar" indeterminate></vaadin-progress-bar>`;
        } else {
            return html`<span>Nothing to display</span>`;
        }
    }

    private _stageChange(e){
        this._itemList = null;
        fetch("/api/sync/item/" + e.target.value)
            .then(response => response.json())
            .then(response => this._itemList = response);
    }
}