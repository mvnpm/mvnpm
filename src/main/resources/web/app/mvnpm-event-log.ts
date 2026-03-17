import {LitElement, html, css} from 'lit';
import {customElement, state, property} from 'lit/decorators.js';

export const WEBSOCKET_BASE = `${window.location.protocol === "https:" ? "wss:" : "ws:"}//${window.location.host}`;

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
          background: var(--mvnpm-code-bg, #151722);
          border-radius: var(--mvnpm-radius-md, 10px);
          border: 1px solid var(--mvnpm-border, rgba(255,255,255,0.1));
      }

      .console {
          display: flex;
          flex-direction: column;
          width: 100%;
          height: 100%;
          padding: 16px 20px;
          font-family: var(--mvnpm-font-mono, 'Courier New', monospace);
          font-size: 13px;
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
        ${this.liveSync ? html` <p>Listening for new events
          <l-dot-stream
              size="20"
              speed="2.5"
              color="#F59E0B"
          ></l-dot-stream>
        </p>`: ''}
        ${this._renderInitEventLog()}
      </div>
    `;
  }

  private _renderInitEventLog() {
    if (this._initEventLog && this._initEventLog.length > 0) {
      return html`
        ${this._initEventLog.map((entry) => {
          return html`${this._renderLine(entry)}`
        })}
      `;
    } else {
      return html`<p style="color: gray">Nothing yet in the event log</p>`;
    }
  }

  private _renderLine(entry) {
    let formattedTime = entry.time.substring(0, entry.time.indexOf(".")).replace('T', ' ');

    return html`
      <div class="line">
        <span style="color: var(--mvnpm-log-time, grey)">${formattedTime}</span>
        <span style="color: var(--mvnpm-log-group, lightblue)">${entry.groupId}</span>
        <span style="color: var(--mvnpm-log-artifact, lightyellow)">${entry.artifactId}</span>
        <span style="color: var(--mvnpm-log-version, lightpink)">${entry.version}</span>
        <span style="color: var(--mvnpm-log-stage, lightgrey)">[${entry.stage}]</span>
        <span style="color: var(--mvnpm-log-${entry.color}, ${entry.color})">${entry.message}</span>
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