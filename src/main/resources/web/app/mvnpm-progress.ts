import { LitElement, html, css} from 'lit';
import { customElement, state } from 'lit/decorators.js';

/**
 * This component shows Progress of artifacts being processed
 */
@customElement('mvnpm-progress')
export class MvnpmProgress extends LitElement {

    static webSocket;
    static serverUri;

    static styles = css`
    `;

    @state() 
    private _uploadQueue?: string[];
    @state() 
    private _uploadingQueue?: string[];

    constructor() {
        super();
        if (!MvnpmProgress.webSocket) {
            if (window.location.protocol === "https:") {
                MvnpmProgress.serverUri = "wss:";
            } else {
                MvnpmProgress.serverUri = "ws:";
            }
            var currentPath = window.location.pathname;
            MvnpmProgress.serverUri += "//" + window.location.host + "/api/queue/";
            MvnpmProgress.connect();
        }
    }

    render() {
        return html``;
    }
    


    static connect() {
        MvnpmProgress.webSocket = new WebSocket(MvnpmProgress.serverUri);

        MvnpmProgress.webSocket.onopen = function (event) {
            console.log("ON OPEN " + event.data);
        };

        MvnpmProgress.webSocket.onmessage = function (event) {
            var centralSyncItem = JSON.parse(event.data);
            console.log("ON MESSAGE " + centralSyncItem);
        }

        MvnpmProgress.webSocket.onclose = function (event) {
            console.log("ON CLOSE " + event.data);
        };

        MvnpmProgress.webSocket.onerror = function (error) {
            console.log("ON ERROR " + error);
        }
    }
 }