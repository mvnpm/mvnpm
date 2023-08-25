import { LitElement, html, css} from 'lit';
import { customElement, property, query } from 'lit/decorators.js';
import { EditorView, basicSetup } from "codemirror"
import { xml } from '@codemirror/lang-xml'
import { javascript } from '@codemirror/lang-javascript'
import { json } from '@codemirror/lang-json'
import { asciiArmor } from "@codemirror/legacy-modes/mode/asciiarmor"
import { powerShell } from "@codemirror/legacy-modes/mode/powershell"
import { StreamLanguage } from "@codemirror/language"

@customElement('codemirror-viewer')
export class CodemirrorViewer extends LitElement {

    @property({type: String})
    src: string;

    async updated(changedProperties: Map<string | number | symbol, unknown>) {
        super.updated(changedProperties);

        if (changedProperties.has('src')) {
            await this._loadContent();
        }
    }


    async _loadContent() {
        const el = this.shadowRoot?.querySelector('div');
        if (!el) return;
        el.innerHTML = '';

        const response = await fetch(this.src);
        const content = await response.text();
        this._view = new EditorView({
            doc: content,
            readonly: true,
            extensions: [basicSetup, this._detectLanguage(this.src)],
            parent: el
        });
    }

    private _detectLanguage(src: string): any {
        const ext = src.split('.').pop()?.toLowerCase();
        switch (ext) {
            case 'js':
                return javascript();
            case 'pom':
            case 'xml':
                return xml();
            case 'json':
                return json();
            case 'asc':
                return StreamLanguage.define(asciiArmor)
            default:
                return StreamLanguage.define(powerShell);
        }
    }


    render() {
        return html`<div class="codeView"></div>`;
    }
}