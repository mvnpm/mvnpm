import { LitElement, html, css} from 'lit';
import '@vaadin/form-layout';
import '@vaadin/text-field';
import '@vaadin/button';

/**
 * This component shows the Home screen
 */
 export class MvnpmHome extends LitElement {

    static styles = css`
        :host {
            width: 100%;
            display: flex;
            justify-content: center;
        }
    
        .coordinates{
        }
    `;

    static properties = {
        _coordinates:{state: true},
    };

    constructor() {
        super();
        this._clearCoordinates();
    }

    render() {
        return html`<div class="coordinates" @keypress="${this._enter}">
            <vaadin-text-field label="Group Id" @input="${this._groupIdChanged}" clear-button-visible>
                <span slot="prefix">org.mvnpm</span>
            </vaadin-text-field>
            <vaadin-text-field label="Artifact Id" @input="${this._artifactIdChanged}" clear-button-visible></vaadin-text-field>
            <vaadin-text-field label="Version" @input="${this._versionIdChanged}" placeholder="latest" clear-button-visible></vaadin-text-field>
            <vaadin-button theme="primary" @click="${this._find}">Find</vaadin-button>
        </div>`;
    }
    
    _enter(e){
        
        if (e.which == 13) {
           this._find(); 
        }
    }
    
    _find(){
        var groupId = this._coordinates.groupId.trim();
        if(!groupId){
            groupId = "org.mvnpm";
        }else{
            if(!groupId.startsWith(".")){
                groupId = "." + groupId;
            }
            groupId = groupId.replace('@', 'at.')
            groupId = "org.mvnpm" + groupId;
        }
        
        var artifactId = this._coordinates.artifactId.trim();
        
        var version = this._coordinates.version.trim();
        if(!version){
            version = "latest";
        }
        
        console.log("groupId = [" + groupId + "]");
        console.log("artifactId = [" + artifactId + "]");
        console.log("version = [" + version + "]");
    }
    
    _clearCoordinates(){
        this._coordinates = { 
            groupId: '',
            artifactId: '',
            version: ''
        };
    }
    
    _groupIdChanged(e){
        this._coordinates.groupId = e.target.value;
    }

    _artifactIdChanged(e){
        this._coordinates.artifactId = e.target.value;
    }
    
    _versionChanged(e){
        this._coordinates.version = e.target.value;
    }
    
 }
 customElements.define('mvnpm-home', MvnpmHome);