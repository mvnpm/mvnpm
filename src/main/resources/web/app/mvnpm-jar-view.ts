import { LitElement, html, css} from 'lit';
import { customElement, state, property } from 'lit/decorators.js';
import '@vaadin/tabsheet';
import '@vaadin/tabs';
import '@vaadin/grid';
import '@vaadin/icon';
import '@vaadin/button';
import '@vaadin/grid/vaadin-grid-tree-column.js';
import {columnBodyRenderer} from '@vaadin/grid/lit.js';
import { Notification } from '@vaadin/notification';
import '@vaadin/progress-bar';

/**
 * This component shows the Jar content screen
 */
@customElement('mvnpm-jar-view')
export class MvnpmJarView extends LitElement {

    static styles = css`
        :host {
            display: flex;
            flex:1;
        }
        .content {
            display: flex;
            flex: 1 1 0%;
            flex-direction: column;
        }
        .full-height {
            height: 100%;
        }
        
    `;

    @property({reflect: true})
    jarName?: string = '';
  
    @state() _jar: {};
    
    constructor() {
        super();
    }

    connectedCallback() {
        super.connectedCallback();   
    }

    updated(changedProperties) {
        if(changedProperties.has("jarName")){
            this._jar = null;
            // Fetch jar info
            let url = this.jarName.replace("/maven2/", "/api/");

            fetch(url)
                .then(response => response.json())
                .then(response => this._jar = response);    
        }
        
    }

    render() {
        if(this._jar){
            let j = this._jar;
            const dataProvider = function (params, callback) {
                if (params.parentItem === undefined) {
                    callback(j.rootAsset.children, j.rootAsset.children.length);
                } else {
                    callback(params.parentItem.children, params.parentItem.children.length)
                }
            };

            return html`<div class="content">
                    <h3>${this._jar.jarName}-${this._jar.version}${this._jar.type}</h3>
                    <vaadin-grid .itemHasChildrenPath="${'children'}" 
                                    .dataProvider="${dataProvider}"
                                    theme="compact no-border" 
                                    class="full-height">
                        <vaadin-grid-tree-column path="name"></vaadin-grid-tree-column>
                    </vaadin-grid>
                    </div>`;
        }else{
            
            return html`<vaadin-progress-bar indeterminate></vaadin-progress-bar>`;
        }
    }
 }