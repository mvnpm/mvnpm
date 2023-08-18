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
import '@vanillawc/wc-codemirror/mode/asciiarmor/asciiarmor.js';
import '@vanillawc/wc-codemirror/mode/powershell/powershell.js';
import '@vanillawc/wc-codemirror/mode/javascript/javascript.js';
import './mvnpm-loading.js';

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
            width: 100%;
            display: flex;
            flex-direction: column;
        }
    
        .fileList {
            display: flex;
            flex-direction: column;
            gap: 10px;
            width: 20%;
            padding-right: 5px;
        }
        
        .line a{
            color: rgb(98, 109, 124);
        }
        .line a:link{ 
            text-decoration: none; 
            color: #626d7c;
        }
        .line a:visited { 
            text-decoration: none; 
            color: #626d7c;
        }
        .line a:hover { 
            text-decoration: none; 
            color: #4b8ee6;
        }
        .line a:active { 
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
        .use {
            display: flex;
            gap: 10px;
        }
    
        .useBlock{
            display: flex;
            flex-direction: column;
            padding: 15px;
            gap: 10px;
            border: 1px solid rgba(102,179,67,0.96);
            border-radius: 15px;
            width: 50%;
        }
        .basiccode {
    
        }
        .fileBrower {
            display: flex;
            gap: 5px;
            padding-top: 10px;
        }
        .line{
            display: flex;
            justify-content: space-between;
        }
        .line .outIcon {
            font-size: 12px;
        }
        .line a {
            cursor: pointer;
        }
        .sync {
            display: flex;
            gap: 10px;
        }
        .codeView {
            width: 100%;
        }
        .nopreview {
            width: 100%;
            color: lightgray;
            font-size: 2rem;
            font-weight: bold;
            text-transform: uppercase;
            text-align: center;
        }
        .clearButton {
            align-self: end;
            margin-bottom: 14px;
        }
        mvnpm-loading {
            align-self: end;
        }
    `;

    static properties = {
        _coordinates:{state: true},
        _baseUrl:{state: true},
        _baseFile:{state: true},
        _disabled:{state: true},
        _usePom:{state: true},
        _useJson:{state: true},
        _versions:{state: true},
        _latestVersion:{state: true},
        _codeViewMode:{state: true},
        _codeViewSrc:{state: true},
        _codeViewSelection:{state: true},
        _loadingIcon:{state:true},
        _syncInfo:{state:true}
    };

    constructor() {
        super();
        this._clearCoordinates();
        this._disabled = "disabled";
        this._codeViewMode = "xml"; 
        this._codeViewSelection = ".pom";
        this._syncInfo = null;
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
            <vaadin-button class="clearButton" theme="secondary" @click="${this._clearCoordinates}">Clear</vaadin-button>
            <mvnpm-loading style="visibility: ${this._loadingIcon};"></mvnpm-loading>
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
                    <vaadin-tab id="pom-xml-tab">files</vaadin-tab>
                    <vaadin-tab id="usage-tab">usage</vaadin-tab>
                    <vaadin-tab id="sync-tab">sync</vaadin-tab>
                </vaadin-tabs>

                <div tab="pom-xml-tab">
                    ${this._loadPomTab()}
                </div>
            
                <div tab="usage-tab">
                    ${this._loadUsageTab()}
                </div>
            
                <div tab="sync-tab">
                    ${this._loadSyncTab()}
                </div>
            
            </vaadin-tabsheet>`;
        }
    }
    
    _loadSyncTab(){
        if(this._syncInfo){
            return html`<div class="use">
                    <div class="useBlock">
                        <span class="heading">Sync info</span>
                        <div class="sync">
                            Uploaded to Nexus OSS staging ${this._renderIcon(this._syncInfo.inStaging)}</br>
                        </div>
                        <div class="sync">    
                            Available in Maven central ${this._renderIcon(this._syncInfo.inCentral)}</br>
                        </div>    
                    </div>
                </div>
            `;
        }
    }
    
    _renderIcon(yes){
        if(yes){
            return html`<vaadin-icon style="color:green" icon="vaadin:check-circle"></vaadin-icon>`;
        }else{
            return html`<vaadin-icon style="color:red" icon="vaadin:close-circle"></vaadin-icon>`;
        }
    }
    
    _loadUsageTab(){
        if(this._usePom){
            return html`<div class="use">
                    <div class="useBlock">
                        <span class="heading">Pom dependency</span>
                        <pre lang="xml" class="basiccode">${this._usePom}</pre>
                    </div>
                    <div class="useBlock">
                        <span class="heading">Import map</span>
                        <pre lang="json" class="basiccode">${this._useJson}</pre>
                    </div>  
                </div>
            `;
        }
    }
    
    _loadPomTab(){
        if(this._baseUrl){
            return html`<div class="fileBrower">
                            ${this._renderCodeView()}
                            <div class="fileList">
                                ${this._renderFileGroup('pom', '.pom', 'file-code')}
                                ${this._renderFileGroup('jar', '.jar', 'file-zip')}
                                ${this._renderFileGroup('source', '-sources.jar', 'file-zip')}
                                ${this._renderFileGroup('javadoc', '-javadoc.jar', 'file-zip')}
                                ${this._renderFileGroup('original', '.tgz', 'file-zip')}
                                ${this._renderAnyFile('package', '.json','file-text-o')}
                            </div>    
                        </div>`;
        }
    }
    
    _renderFileGroup(heading,fileExt, icon){
        return html`
            <span class="heading">${heading}</span>
            ${this._renderLine(fileExt,icon)}
            ${this._renderLine(fileExt + '.sha1','file-text-o')}
            ${this._renderLine(fileExt + '.md5','file-text-o')}
            ${this._renderLine(fileExt + '.asc','file-text-o')}
        `;    
    }
    
    _renderLine(fileExt, icon){
        return this._renderAnyFile(this._baseFile, fileExt, icon);
    }
    
    _renderAnyFile(fileName, fileExt, icon){
        return html`
            <div class="line">
                <a @click="${this._showFile}" data-file="${this._baseUrl + fileName + fileExt}"><vaadin-icon icon="vaadin:${icon}"></vaadin-icon>${fileName + fileExt}</a>
                <a href="${this._baseUrl + fileName + fileExt}" target="_blank"><vaadin-icon class="outIcon" icon="vaadin:external-link"></vaadin-icon></a>
            </div>
        `;
    }
    
    _renderCodeView(){
        if(this._codeViewMode){
            return html`<wc-codemirror class="codeView"
                        mode='${this._codeViewMode}'
                        src='${this._codeViewSrc}'
                        readonly>
                    </wc-codemirror>`;
        }else{
            return html`<div class="codeView">
                            <div class="nopreview"> 
                                binary format - no preview
                            </div>
                        </div>`;
        }
    }
    
    _showFile(e){
        this._changeCodeView(e.target.dataset.file);
    }
    
    _changeCodeView(src){
        this._codeViewSrc = src;
        if(this._codeViewSrc.endsWith('.jar') || this._codeViewSrc.endsWith('.tgz')){
            this._codeViewMode = null;
        }else if(this._codeViewSrc.endsWith('.pom')){
            this._codeViewMode = "xml";
        }else if(this._codeViewSrc.endsWith('.asc')){
            this._codeViewMode = "asciiarmor";
        }else{
            this._codeViewMode = "powershell";
        }
        
        var n = this._baseUrl.length + this._baseFile.length;
        this._codeViewSelection = this._codeViewSrc.substring(n);
    }
    
    _findVersionsAndShowLatest(e){    
        if (e.which == 13 || e.which == 0) {
            this._loadingIcon = "visible";
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
        this._loadingIcon = "hidden";
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
        this._usePom = null;
        this._useJson = null;
        this._versions = null;
        this._codeViewSelection = ".pom";
        this._codeViewMode = "xml"; 
        this._loadingIcon = "hidden";
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
        this._loadingIcon = "visible";
        this._coordinates.version = version;
        
        var groupId = this._getGroupId(this._coordinates.groupId.trim());
        
        var artifactId = this._coordinates.artifactId.trim();
        
        if(!version){
            version = "latest";
        }
        
        groupId = groupId.replaceAll('/', '.');
        this._usePom = "<dependency>\n\t<groupId>" + groupId + "</groupId>\n\t<artifactId>" + artifactId + "</artifactId>\n\t<version>" + version + "</version>\n\t<scope>runtime</scope>\n</dependency>";
        var syncInfoUrl = "/sync/info/" + groupId + "/" + artifactId + "?version=" + version;
        
        groupId = groupId.replaceAll('.', '/');
        var importMapUrl = "/maven2/" + groupId + "/" + artifactId + "/" + version + "/importmap.json";

        fetch(importMapUrl)
            .then(response => response.json())
            .then(response => this._setUseJson(response));
            
        this._baseFile = artifactId + "-" + version;
        this._baseUrl = "/maven2/" + groupId + "/" + artifactId + "/" + version + "/";
        
        this._changeCodeView(this._baseUrl + this._baseFile + this._codeViewSelection);
        
        fetch(syncInfoUrl)
            .then(response => response.json())
            .then(response => this._syncInfo = response);
    }
    
    _setUseJson(response){
        this._useJson = JSON.stringify(response)
                .replaceAll(':{',':{\n\t\t')
                .replaceAll('","','",\n\t\t"')
                .replaceAll('"}','"\n\t}')
                .replaceAll('}}','}\n}')
                .replaceAll('{"','{\n\t"');
        this._loadingIcon = "hidden";
    }
 }
 customElements.define('mvnpm-home', MvnpmHome);