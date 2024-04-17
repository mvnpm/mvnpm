import { LitElement, html, css } from 'lit';
import { customElement, state } from 'lit/decorators.js';
import '@vaadin/split-layout';
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
      gap: 10px;
      width: 100%;
      padding: 20px;
    }`;

    render() {
        return html`
            <vaadin-split-layout style="height: 100%;" orientation="vertical">
                <mvnpm-progress></mvnpm-progress>
                <mvnpm-event-log></mvnpm-event-log>
            </vaadin-split-layout>`;
    }

}