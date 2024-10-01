import {LitElement, html, css} from 'lit';
import {customElement, state, property} from 'lit/decorators.js';

export const WEBSOCKET_BASE = window.location.protocol === "https:" ? "wss:" : "ws:" + "//" + window.location.host;

/**
 * This component shows the Event log
 */
@customElement('mvnpm-event-log')
export class MvnpmEventLog extends LitElement {
  static styles = css`
      :host {
          display: flex;
          gap: 10px;
          width: 100%;
          max-height: 40vh;
          background: black;
      }

      .console {
          display: flex;
          flex-direction: column;
          width: 100%;
          height: 100%;
          padding-left: 20px;
          padding-right: 20px;
          background: black;
          font-family: 'Courier New', monospace;
          font-size: small;
          filter: brightness(0.85);
      }

      .line {
          display: flex;
          flex-direction: row;
          gap: 10px;
      }

  `;

  private _logWebSocket = null;

  @state({type: Array})
  private _initEventLog: any[] | null = null;

  @property({type: Boolean})
  private liveSync;

  private _eventLogEntry = (event: CustomEvent) => this._receiveLogEntry(event.detail);

  constructor() {
    super();

  }

  connectedCallback() {
    super.connectedCallback();

    fetch("/api/eventlog/top")
      .then(response => response.json())
      .then(initEventLog => (this._initEventLog = this._addMultipleToLog(this._initEventLog, initEventLog)));

    document.addEventListener('eventLogEntryEvent', this._eventLogEntry, false);
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    this._stopLiveSync();
    document.removeEventListener('eventLogEntryEvent', this._eventLogEntry, false);
  }

  updated(changedProps) {
    if (this.liveSync) {
      this._startLiveSync();
    } else {
      this._stopLiveSync();
    }
  }

  private _startLiveSync() {
    if (!this._logWebSocket) {
      this._logWebSocket = new WebSocket(WEBSOCKET_BASE + "/api/stream/eventlog");
      this._logWebSocket.onmessage = function (event) {
        var eventLogEntry = JSON.parse(event.data);
        const eventLogEntryEvent = new CustomEvent('eventLogEntryEvent', {detail: eventLogEntry});
        document.dispatchEvent(eventLogEntryEvent);
      }
      this._logWebSocket.onclose = function (event) {
        setTimeout(function () {
          if (this.liveSync) {
            this._startLiveSync();
          }
        }, 100);
      };
    }
  }

  private _stopLiveSync() {
    if (this._logWebSocket) {
      this._logWebSocket.close();
      this._logWebSocket = null;
    }
  }


  private _receiveLogEntry(initEventLog: any) {
    this._initEventLog = this._addToLog(this._initEventLog, initEventLog);
  }

  render() {
    return html`
      <div class="console">
        ${this._renderInitEventLog()}
      </div>
    `;
  }

  private _renderInitEventLog() {
    if (this._initEventLog && this._initEventLog.length > 0) {
      return html`
        <l-dot-stream
            size="20"
            speed="2.5"
            color="#66a5b1"
        ></l-dot-stream><br/>
        ${this._initEventLog.map((entry) => {
          return html`${this._renderLine(entry)}`
        })}
      `;
    } else if (this.liveSync) {
      return html`
        <p>Nothing in the event log <br/>
          <l-dot-stream
              size="20"
              speed="2.5"
              color="#66a5b1"
          ></l-dot-stream>
        </p>`;
    } else {
      return html`<p>Nothing in the event log</p>`;
    }
  }

  private _renderLine(entry) {
    let formattedTime = entry.time.substring(0, entry.time.indexOf(".")).replace('T', ' ');

    return html`
      <div class="line">
        <span style="color: grey">${formattedTime}</span>
        <span style="color: lightblue">${entry.groupId}</span>
        <span style="color: lightyellow">${entry.artifactId}</span>
        <span style="color: lightpink">${entry.version}</span>
        <span style="color: lightgrey">[${entry.stage}]</span>
        <span style="color: ${entry.color}">${entry.message}</span>
      </div>`;
  }

  private _addToLog(queue: any[] | null, item: any) {
    if (queue && queue.length > 0) {
      return [item, ...queue];
    } else {
      return [item];
    }
  }

  private _addMultipleToLog(queue: any[] | null, items: any[]) {
    if (queue && queue.length > 0) {
      return [...items, ...queue];
    } else {
      return items;
    }
  }

}