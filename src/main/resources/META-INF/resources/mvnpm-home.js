import { LitElement, html, css} from 'lit';
import '@vaadin/form-layout';
import '@vaadin/text-field';
import '@vaadin/combo-box';
import '@vaadin/button';
import '@vaadin/tabs';
import '@vaadin/tabsheet';
import '@vaadin/icon';
import '@vaadin/vaadin-lumo-styles/vaadin-iconset.js';
import '@vaadin/icons';
import '@vanillawc/wc-codemirror';
import '@vanillawc/wc-codemirror/mode/xml/xml.js';

/**
 * This component shows the Home screen
 * 
 */
 export class MvnpmHome extends LitElement {

    static styles = css`
        :host {
            width: 100%;
            display: flex;
            flex-direction: column;
            align-items: center;
        }
        .coordinates{
            width: 100%;
            display: flex;
            justify-content: center;
            align-items: baseline;
            column-gap: 15px;
        }
        .tabpane {
            width: 90%;
            display: flex;
            flex-direction: column;
        }
        .files {
            display: flex;
            flex-direction: column;
            padding: 15px;
            gap: 10px;
            border: 1px solid rgba(102,179,67,0.96);
            border-radius: 15px;
            width: 20%;
        }
        .files a:link { 
            text-decoration: none; 
            color: #626d7c;
        }
        .files a:visited { 
            text-decoration: none; 
            color: #626d7c;
        }
        .files a:hover { 
            text-decoration: none; 
            color: #4b8ee6;
        }
        .files a:active { 
            text-decoration: none; 
            color: #626d7c;
        }
        .block {
            display: flex;
            gap: 10px;
        }
        .heading{
            color: #66b343;
            font-weight: bold;
            border-bottom: 1px dotted #66b343;
        }
    `;

    static properties = {
        _coordinates:{state: true},
        _baseUrl:{state: true},
        _baseFile:{state: true},
        _disabled:{state: true},
        _use:{state: true},
        _versions:{state: true},
        _latestVersion:{state: true},
    };

    constructor() {
        super();
        this._clearCoordinates();
        this._disabled = "disabled";
    }

    render() {
        return html`<div class="coordinates">
            <vaadin-text-field label="Group Id" @input="${this._groupIdChanged}" value="${this._coordinates.groupId}" clear-button-visible>
                <span slot="prefix">org.mvnpm</span>
            </vaadin-text-field>
            <vaadin-text-field label="Artifact Id" 
                    @focusout="${this._findVersionsAndShowLatest}" 
                    @keypress="${this._findVersionsAndShowLatest}" 
                    @input="${this._artifactIdChanged}" 
                    value="${this._coordinates.artifactId}" clear-button-visible></vaadin-text-field>
            ${this._renderVersionForm()}
            <vaadin-button theme="secondary" @click="${this._clearCoordinates}">Clear</vaadin-button>
        </div>
        
        ${this._renderTabPane()}
        `;
    }
    
    _renderVersionForm(){
        return html`<vaadin-combo-box ?disabled=${this._disabled}
            label="Version"
            item-label-path="value"
            item-value-path="value"
            .items="${this._versions}"
            value="${this._latestVersion}"
            @change="${this._versionChanged}"
        ></vaadin-combo-box>`
        
    }
    
    _renderTabPane(){
        if(this._coordinates.version) {
            return html`<vaadin-tabsheet class="tabpane">
                <vaadin-tabs slot="tabs">
                    <vaadin-tab id="pom-xml-tab">pom</vaadin-tab>
                    <vaadin-tab id="files-tab">files</vaadin-tab>
                    <vaadin-tab id="usage-tab">usage</vaadin-tab>
                </vaadin-tabs>

                <div tab="pom-xml-tab">
                    ${this._loadPomTab()}
                </div>
                
                <div tab="files-tab">
                    ${this._loadFilesTab()}
                </div>
            
                <div tab="usage-tab">
                    ${this._loadUsageTab()}
                </div>
            </vaadin-tabsheet>`;
        }
    }
    
    _loadUsageTab(){
        if(this._use){
            return html`<pre lang="xml">${this._use}</pre>`;
        }
    }
    
    _loadPomTab(){
        if(this._baseUrl){
            return html`<wc-codemirror
                                mode='xml'
                                src='${this._baseUrl + this._baseFile + ".pom"}'
                                readonly>
                            </wc-codemirror>`;
        }
    }
    
    _loadFilesTab(){
        if(this._baseUrl){
            return html`<div class="block">
                            <div class="files">
                                <span class="heading">pom</span>
                                <a href="${this._baseUrl + this._baseFile}.pom" target="_blank"><vaadin-icon icon="vaadin:file-code"></vaadin-icon> ${this._baseFile}.pom</a>
                                <a href="${this._baseUrl + this._baseFile}.pom.sha1" target="_blank"><vaadin-icon icon="vaadin:file-text-o"></vaadin-icon> ${this._baseFile}.pom.sha1</a>
                                <a href="${this._baseUrl + this._baseFile}.pom.md5" target="_blank"><vaadin-icon icon="vaadin:file-text-o"></vaadin-icon> ${this._baseFile}.pom.md5</a>
                                <a href="${this._baseUrl + this._baseFile}.pom.asc" target="_blank"><vaadin-icon icon="vaadin:file-text-o"></vaadin-icon> ${this._baseFile}.pom.asc</a>
                            </div>
                            <div class="files">
                                <span class="heading">jar</span>
                                <a href="${this._baseUrl + this._baseFile}.jar" target="_blank"><vaadin-icon icon="vaadin:file-zip"></vaadin-icon> ${this._baseFile}.jar</a>
                                <a href="${this._baseUrl + this._baseFile}.jar.sha1" target="_blank"><vaadin-icon icon="vaadin:file-text-o"></vaadin-icon> ${this._baseFile}.jar.sha1</a>
                                <a href="${this._baseUrl + this._baseFile}.jar.md5" target="_blank"><vaadin-icon icon="vaadin:file-text-o"></vaadin-icon> ${this._baseFile}.jar.md5</a>
                                <a href="${this._baseUrl + this._baseFile}.jar.asc" target="_blank"><vaadin-icon icon="vaadin:file-text-o"></vaadin-icon> ${this._baseFile}.jar.asc</a>
                            </div>
                            <div class="files">
                                <span class="heading">source</span>
                                <a href="${this._baseUrl + this._baseFile}-sources.jar" target="_blank"><vaadin-icon icon="vaadin:file-zip"></vaadin-icon> ${this._baseFile}-sources.jar</a>
                                <a href="${this._baseUrl + this._baseFile}-sources.jar.sha1" target="_blank"><vaadin-icon icon="vaadin:file-text-o"></vaadin-icon> ${this._baseFile}-sources.jar.sha1</a>
                                <a href="${this._baseUrl + this._baseFile}-sources.jar.md5" target="_blank"><vaadin-icon icon="vaadin:file-text-o"></vaadin-icon> ${this._baseFile}-sources.jar.md5</a>
                                <a href="${this._baseUrl + this._baseFile}-sources.jar.asc" target="_blank"><vaadin-icon icon="vaadin:file-text-o"></vaadin-icon> ${this._baseFile}-sources.jar.asc</a>
                            </div>
                            <div class="files">
                                <span class="heading">javadoc</span>
                                <a href="${this._baseUrl + this._baseFile}-javadoc.jar" target="_blank"><vaadin-icon icon="vaadin:file-zip"></vaadin-icon> ${this._baseFile}-javadoc.jar</a>
                                <a href="${this._baseUrl + this._baseFile}-javadoc.jar.sha1" target="_blank"><vaadin-icon icon="vaadin:file-text-o"></vaadin-icon> ${this._baseFile}-javadoc.jar.sha1</a>
                                <a href="${this._baseUrl + this._baseFile}-javadoc.jar.md5" target="_blank"><vaadin-icon icon="vaadin:file-text-o"></vaadin-icon> ${this._baseFile}-javadoc.jar.md5</a>
                                <a href="${this._baseUrl + this._baseFile}-javadoc.jar.asc" target="_blank"><vaadin-icon icon="vaadin:file-text-o"></vaadin-icon> ${this._baseFile}-javadoc.jar.asc</a>
                            </div>
                            <div class="files">
                                <span class="heading">original</span>
                                <a href="${this._baseUrl + this._baseFile}.tgz" target="_blank"><vaadin-icon icon="vaadin:file-zip"></vaadin-icon> ${this._baseFile}.tgz</a>
                                <a href="${this._baseUrl + this._baseFile}.tgz.sha1" target="_blank"><vaadin-icon icon="vaadin:file-text-o"></vaadin-icon> ${this._baseFile}.tgz.sha1</a>
                                <a href="${this._baseUrl + this._baseFile}.tgz.md5" target="_blank"><vaadin-icon icon="vaadin:file-text-o"></vaadin-icon> ${this._baseFile}.tgz.md5</a>
                                <a href="${this._baseUrl + this._baseFile}.tgz.asc" target="_blank"><vaadin-icon icon="vaadin:file-text-o"></vaadin-icon> ${this._baseFile}.tgz.asc</a>
                            </div>
                        </div>`;
        }
    }
    
    _findVersionsAndShowLatest(e){    
        if (e.which == 13 || e.which == 0) {
            var groupId = this._getGroupId(this._coordinates.groupId.trim());
            var artifactId = this._coordinates.artifactId.trim();

            if(artifactId) {
                groupId = groupId.replaceAll('.', '/');

                var metadataUrl = "/maven2/" + groupId + "/" + artifactId + "/maven-metadata.xml";

                fetch(metadataUrl)
                    .then(response => response.text())
                    .then(xmlDoc => new window.DOMParser().parseFromString(xmlDoc, "text/xml"))
                    .then(metadata => this._inspectMetadata(metadata));
            }
        }
    }
    
    _inspectMetadata(metadata){
        var latestTags = metadata.getElementsByTagName("latest");
        var versionTags = metadata.getElementsByTagName("version");

        this._latestVersion = latestTags[0].childNodes[0].nodeValue;
        var s = new Set();
        Array.from(versionTags).forEach(function (element) {
            var selectEntry = {
                value: element.childNodes[0].nodeValue
            };
            s.add(selectEntry);
        });
        this._versions = Array.from(s).reverse();
        
        this._changeVersion(this._latestVersion);
        this._disabled = null;
    }
    
    _getGroupId(groupId){
        if(!groupId){
            groupId = "org.mvnpm";
        }else{
            if(!groupId.startsWith(".")){
                groupId = "." + groupId;
            }
            groupId = groupId.replace('@', 'at.')
            groupId = "org.mvnpm" + groupId;
        }
        return groupId;
    }
    
    _clearCoordinates(){
        this._coordinates = { 
            groupId: '',
            artifactId: '',
            version: ''
        };
        this._disabled = "disabled";
        this._baseUrl = null;
        this._baseFile = null;
        this._use = null;
        this._versions = null;
    }
    
    _groupIdChanged(e){
        this._coordinates.groupId = e.target.value;
    }

    _artifactIdChanged(e){
        this._coordinates.artifactId = e.target.value;
        this._disabled = "disabled";
    }
    
    _versionChanged(e){
        this._changeVersion(e.target.value.trim());
    }
    
    _changeVersion(version){
        this._coordinates.version = version;
        
        var groupId = this._getGroupId(this._coordinates.groupId.trim());
        var artifactId = this._coordinates.artifactId.trim();
        
        if(!version){
            version = "latest";
        }
        
        this._use = "<dependency>\n\t<groupId>" + groupId + "</groupId>\n\t<artifactId>" + artifactId + "</artifactId>\n\t<version>" + version + "</version>\n\t<scope>runtime</scope>\n</dependency>";
            
        groupId = groupId.replaceAll('.', '/');
        this._baseFile = artifactId + "-" + version;
        this._baseUrl = "/maven2/" + groupId + "/" + artifactId + "/" + version + "/";
    }
    
 }
 customElements.define('mvnpm-home', MvnpmHome);