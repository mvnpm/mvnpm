import { LitElement, html, css } from 'lit';
import { customElement, state } from 'lit/decorators.js';
import '@vaadin/progress-bar';
import '@vaadin/message-list';

/**
 * This component shows the Sync Progress screen
 */
@customElement('mvnpm-progress')
export class MvnpmProgress extends LitElement {
    static styles = css`
    :host {
      display: flex;
      gap: 10px;
      width: 100%;
    }
    .lane {
      display: flex;
      gap: 10px;
      flex-direction: column;
      width: 100%;
      height: 100%;
      padding-left: 20px;
      padding-right: 20px;
      align-items: center;
    }
    .uploading {
      border-right: 2px solid var(--lumo-contrast-10pct);
      border-left: 2px solid var(--lumo-contrast-10pct);
    }
    .progressBar {
      width: 50%;
    }
  `;

    @state({ type: Array })
    private _initQueue: any[] | null = null;

    @state({ type: Array })
    private _uploadingQueue: any[] | null = null;

    @state({ type: Array })
    private _uploadedQueue: any[] | null = null;

    @state({ type: Array })
    private _closedQueue: any[] | null = null;

    @state({ type: Array })
    private _releasingQueue: any[] | null = null;

    @state({ type: Array })
    private _releasedQueue: any[] | null = null;

    private _centralSyncStateChange = (event: CustomEvent) => this._receiveStateChange(event.detail);

    constructor() {
        super();
    }

    connectedCallback() {
        super.connectedCallback();

        fetch("/api/sync/initQueue")
            .then(response => response.json())
            .then(initQueue => (this._initQueue = this._addMultipleToQueue(this._initQueue, initQueue, "Waiting...", 1)));
        fetch("/api/sync/uploadingQueue")
            .then(response => response.json())
            .then(uploadingQueue => (this._uploadingQueue = this._addMultipleToQueue(this._uploadingQueue, uploadingQueue, "Uploading to maven central...", 2)));
        fetch("/api/sync/uploadedQueue")
            .then(response => response.json())
            .then(uploadedQueue => (this._uploadedQueue = this._addMultipleToQueue(this._uploadedQueue, uploadedQueue, "Uploaded, validating...", 3)));
        fetch("/api/sync/closedQueue")
            .then(response => response.json())
            .then(closedQueue => (this._closedQueue = this._addMultipleToQueue(this._closedQueue, closedQueue, "Valid, preparing release...", 4)));
        fetch("/api/sync/releasingQueue")
            .then(response => response.json())
            .then(releasingQueue => (this._releasingQueue = this._addMultipleToQueue(this._releasingQueue, releasingQueue, "Releasing to maven central...", 5)));

        document.addEventListener('centralSyncStateChange', this._centralSyncStateChange, false);
    }

    disconnectedCallback() {
        super.disconnectedCallback();
        document.removeEventListener('centralSyncStateChange', this._centralSyncStateChange, false);
    }

    private _receiveStateChange(centralSyncItem: any) {
        if (centralSyncItem.stage === "INIT") {
            this._initQueue = this._addToQueue(this._initQueue, centralSyncItem, "Waiting...", 1);
        } else if (centralSyncItem.stage === "UPLOADING") {
            this._initQueue = this._removeFromQueue(this._initQueue, centralSyncItem);
            this._uploadingQueue = this._addToQueue(this._uploadingQueue, centralSyncItem, "Uploading to maven central", 2);
        } else if (centralSyncItem.stage === "UPLOADED") {
            this._uploadingQueue = this._removeFromQueue(this._uploadingQueue, centralSyncItem);
            this._uploadedQueue = this._addToQueue(this._uploadedQueue, centralSyncItem, "Uploaded, validating...", 3);
        } else if (centralSyncItem.stage === "CLOSED") {
            this._uploadedQueue = this._removeFromQueue(this._uploadedQueue, centralSyncItem);
            this._closedQueue = this._addToQueue(this._closedQueue, centralSyncItem, "Valid, preparing release...", 4);
        } else if (centralSyncItem.stage === "RELEASING") {
            this._closedQueue = this._removeFromQueue(this._closedQueue, centralSyncItem);
            this._releasingQueue = this._addToQueue(this._releasingQueue, centralSyncItem, "Releasing to maven central...", 5);
        } else if (centralSyncItem.stage === "RELEASED") {
            this._releasingQueue = this._removeFromQueue(this._releasingQueue, centralSyncItem);
            this._releasedQueue = this._addToQueue(this._releasedQueue, centralSyncItem, "Released.", 6);
        }
    }

    render() {
        return html`
      <div class="lane">
        <h3>Queue</h3>
        ${this._renderInitQueue()}
      </div>
      <div class="lane uploading">
        ${this._renderUploading()}
      </div>
      <div class="lane">
        <h3>Maven central</h3>
        ${this._renderMavenCentral()}
      </div>
    `;
    }

    private _renderInitQueue() {
        if (this._initQueue && this._initQueue.length > 0) {
            return html`<vaadin-message-list .items="${this._initQueue}"></vaadin-message-list>`;
        } else {
            return html`<p>Nothing in the sync queue</p>`;
        }
    }

    private _renderUploading() {
        if (this._uploadingQueue && this._uploadingQueue.length > 0) {
            return html`
        <vaadin-progress-bar class="progressBar" indeterminate></vaadin-progress-bar>
        <vaadin-message-list .items="${this._uploadingQueue}"></vaadin-message-list>
      `;
        }
    }

    private _renderMavenCentral() {
        return html`
      ${this._renderUploaded()}
      ${this._renderClosed()}
      ${this._renderReleasing()}
      ${this._renderReleased()}
    `;
    }

    private _renderUploaded() {
        if (this._uploadedQueue && this._uploadedQueue.length > 0) {
            return html`<vaadin-message-list .items="${this._uploadedQueue}"></vaadin-message-list>`;
        }
    }

    private _renderClosed() {
        if (this._closedQueue && this._closedQueue.length > 0) {
            return html`<vaadin-message-list .items="${this._closedQueue}"></vaadin-message-list>`;
        }
    }

    private _renderReleasing() {
        if (this._releasingQueue && this._releasingQueue.length > 0) {
            return html`<vaadin-message-list .items="${this._releasingQueue}"></vaadin-message-list>`;
        }
    }

    private _renderReleased() {
        if (this._releasedQueue && this._releasedQueue.length > 0) {
            return html`<vaadin-message-list .items="${this._releasedQueue}"></vaadin-message-list>`;
        }
    }

    private _addToQueue(queue: any[] | null, item: any, stagemessage: string, step: number) {
        let mle = this._toMessageListEntry(item, stagemessage, step);
        if (queue && queue.length > 0) {
            return [...queue, mle];
        } else {
            return [mle];
        }
    }

    private _addMultipleToQueue(queue: any[] | null, items: any[], stagemessage: string, step: number) {
        let mles = items.map(item => this._toMessageListEntry(item, stagemessage, step));
        if (queue && queue.length > 0) {
            return [...queue, ...mles];
        } else {
            return mles;
        }
    }

    private _removeFromQueue(queue: any[] | null, item: any) {
        if (queue && queue.length > 0) {
            var index = queue.findIndex(mle => {
                return mle.time == item.nameVersionType.name.mvnGroupId && mle.userName == item.nameVersionType.name.mvnArtifactId + " " + item.nameVersionType.version;
            });
            queue.splice(index, 1);
        }

        if (queue && queue.length == 0) {
            return null;
        } else {
            return [...queue];
        }
    }

    private _toMessageListEntry(item: any, stagemessage: string, step: number) {
        return {
            text: stagemessage,
            time: item.nameVersionType.name.mvnGroupId,
            userName: item.nameVersionType.name.mvnArtifactId + " " + item.nameVersionType.version,
            userColorIndex: step
        };
    }
}