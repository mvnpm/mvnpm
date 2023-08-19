import { LitElement, html, css} from 'lit';
import { customElement, property, query } from 'lit/decorators.js';
import {EditorView, basicSetup} from 'codemirror'
import { javascript } from '@codemirror/lang-javascript'
import { xml } from '@codemirror/lang-xml'

@customElement('code-mirror')
export class CodeMirror extends LitElement {



    @property({type: String})
    id: string;


    @property({type: String})
    src: string;

    get _editor() {
        return this.renderRoot?.querySelector(this.id) ?? null;
    }

    render() {
        let editor = new EditorView({
            src: this.src,
            extensions: [basicSetup, javascript(), xml()],
            parent: (this._editor)
        })
        return html`
          <div id="${this.id}"></div>
        `;
    }
}