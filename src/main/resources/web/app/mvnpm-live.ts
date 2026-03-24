import { LitElement, html, css } from 'lit';
import { customElement, state } from 'lit/decorators.js';
import '@vaadin/split-layout';
import '@vaadin/checkbox';
import './mvnpm-event-log.js';
import './mvnpm-progress.js';

/**
 * This component shows the live server processes
 */
@customElement('mvnpm-live')
export class MvnpmLive extends LitElement {
    static styles = css`
        :host {
            display: flex;
            flex-direction: column;
            gap: 16px;
            width: 100%;
            padding: 24px;
            box-sizing: border-box;
        }

        @media (min-width: 769px) {
            :host {
                overflow: hidden;
            }
        }

        .live-sync {
            display: flex;
            justify-content: center;
            padding: 8px;
        }

        vaadin-checkbox[checked] {
            --vaadin-checkbox-background: var(--vaadin-selection-color, var(--lumo-primary-color));
        }
    `;

    @state({type: Boolean})
    private _liveSync;

    render() {

        return html`
                <div class="live-sync">
                    <vaadin-checkbox label="Live sync" @checked-changed="${this._liveSyncChange}"></vaadin-checkbox>
                </div>
                <vaadin-split-layout style="height: 100%;" orientation="vertical">
                    <mvnpm-progress .liveSync=${this._liveSync}></mvnpm-progress>
                    <mvnpm-event-log .liveSync=${this._liveSync}></mvnpm-event-log>
                </vaadin-split-layout>
         `;
    }

    private _liveSyncChange = (event) => {
        this._liveSync = event.target.checked;
    }

}