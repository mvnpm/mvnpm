import { LitElement, html, css} from 'lit';
import { customElement, state } from 'lit/decorators.js';
import '@vaadin/form-layout';
import '@vaadin/text-field';
import '@vaadin/combo-box';
import '@vaadin/button';
import '@vaadin/tabs';
import '@vaadin/tabsheet';
import '@vaadin/icon';
import '@vaadin/vaadin-lumo-styles/vaadin-iconset.js';
import '@vaadin/icons';
import '@vaadin/progress-bar';
import '@vaadin/tabs';
import '@vaadin/tabsheet';
import { Notification } from '@vaadin/notification';

interface Coordinates {
    name: string;
    version: string;
}

const DEFAULT_COORDS = {
    name: '',
    version: '',
};

/**
 * This component shows the Home screen
 * 
 */
@customElement('mvnpm-home')
export class MvnpmHome extends LitElement {

    static styles = css`
        :host {
            width: 100%;
            display: flex;
            flex-direction: column;
            align-items: center;
        }
        .coordinates-pane {
            width: 100%;
            display: flex;
            justify-content: center;
            align-items: center;
            flex-direction: column;
            column-gap: 15px;
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
            padding-right: 10px;
        }
        
        a{
            color:var(--lumo-contrast-60pct);
        }
        a:link{ 
            text-decoration: none; 
            color:var(--lumo-contrast-60pct);
        }
        a:visited { 
            text-decoration: none; 
            color:var(--lumo-contrast-60pct);
        }
        a:hover { 
            text-decoration: none; 
            color:var(--lumo-body-text-color);
        }
        a:active { 
            text-decoration: none; 
            color:var(--lumo-contrast-60pct);
        }
        .block {
            display: flex;
            gap: 10px;
        }
        .heading{
            font-weight: bold;
            background-color: var(--lumo-contrast-10pct);
            padding: 10px;
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
            border-left: 1px solid var(--lumo-contrast-10pct);
            width: 50%;
        }
        codemirror-viewer {
            width: 100%;
        }
        .fileBrowser {
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
            color: var(--lumo-contrast-60pct);
            font-size: 2rem;
            font-weight: bold;
            text-transform: uppercase;
            text-align: center;
        }
        .clearButton {
            align-self: end;
        }
        .info-line {
            display: flex;
            justify-content: space-between;
            width: 100%;
        }
        .info-buttons {
            display: flex;
            gap: 20px;
            font-size: smaller;
        }

        .badge {
            background-color: gray;
            color: white;
            padding: 2px 4px;
            text-align: center;
            border-radius: 5px;
            font-size: small;
            height: fit-content;
        }
        .dependencies {
            padding-top: 20px;
            display: flex;
            justify-content: space-evenly;
            width: 100%;
        }
        @media (max-width: 1000px) {
            .dependencies {
                flex-direction: column;
            }
        }
        .copy {
            width: 20px;
            cursor: pointer;
            align-self: end;
        }
        .copy:hover { 
            color:var(--lumo-success-color);
        }

        .gaveventlogconsole {
            display: flex;
            flex-direction: column;
            height: 100%;
            padding-left: 20px;
            padding-right: 20px;
            background: black;
            font-family: 'Courier New', monospace;
            font-size: small;
            filter: brightness(0.85);
            }
        .gaveventlogline {
            display: flex;
            flex-direction: row;
            gap: 10px;
        }
    `;

    @state() 
    private _coordinates = DEFAULT_COORDS;
    @state() 
    private _info = null;
    @state() 
    private _baseUrl?: string;
    @state() 
    private _baseFile?: string;
    @state() 
    private _disabled = "disabled";
    @state() 
    private _usePom?: string;
    @state() 
    private _useJson?: string;
    @state() 
    private _versions?: string[];
    @state() 
    private _latestVersion?: string;
    @state() 
    private _codeViewMode?: string;
    @state() 
    private _codeViewSrc?: string;
    @state() 
    private _codeViewSelection = ".pom";
    @state() 
    private _loadingIcon = "hidden";
    @state() 
    private _centralSyncItem?: object;
    @state() 
    private _gavEventLog?: object;
    

    constructor() {
        super();
        this._clearCoordinates();
        this._disabled = "disabled";
        this._codeViewSelection = ".pom";
        this._centralSyncItem = null;
        this._gavEventLog = null;
    }

    render() {
        return html`
                ${this._renderCoordinatesPane()}
                ${this._renderTabPane()}
                `;
    }
    
    _renderCoordinatesPane(){
        return html`<div class="coordinates-pane"> 
            ${this._renderCoordinates()}
        </div>`;
    }

    _renderCoordinates(){
        return html`<div class="coordinates">
            <vaadin-text-field id="coordinates-field" label="Name (Package or Coordinates)" style="width: 500px"
                    @focusout="${this._findVersionsAndShowLatest}" 
                    @keypress="${this._findVersionsAndShowLatest}" 
                    @input="${this._coordinatesNameChanged}" 
                    value="${this._coordinates.name}" clear-button-visible></vaadin-text-field>
            ${this._renderVersionForm()}
            <vaadin-button class="clearButton" theme="secondary" @click="${this._clearCoordinates}">Clear</vaadin-button>
        </div>
        <div class="coordinates">
            <vaadin-progress-bar class="progress" style="visibility: ${this._loadingIcon};" indeterminate></vaadin-progress-bar>
        </div>`;
    }

    _renderVersionForm(){
        return html`<vaadin-combo-box ?disabled=${this._disabled}
            label="Version"
            item-label-path="value"
            item-value-path="value"
            .items="${this._versions}"
            value="${this._latestVersion}"
            @change="${this._versionChanged}"
            style="width: 100px"
        ></vaadin-combo-box>`
        
    }
    
    _renderTabPane(){
        if(this._coordinates.version) {
            return html`<vaadin-tabsheet class="tabpane">
                <vaadin-tabs slot="tabs">
                    <vaadin-tab id="info-tab">info</vaadin-tab>
                    <vaadin-tab id="files-tab">files</vaadin-tab>
                    <vaadin-tab id="event-log-tab">event log</vaadin-tab>
                </vaadin-tabs>

                <div tab="info-tab">
                    ${this._loadInfoTab()}
                </div>

                <div tab="files-tab">
                    ${this._loadFilesTab()}
                </div>

                <div tab="event-log-tab">
                    ${this._loadEventLogTab()}
                </div>

            </vaadin-tabsheet>`;
        }
    }
    
    _showNpmjsLink(){
        if(this._centralSyncItem){
            var npmUrl = "https://www.npmjs.com/package/" + this._centralSyncItem.name.npmFullName + "/v/" + this._centralSyncItem.version;

            return html`<a href="${npmUrl}" target="_blank">
                            <vaadin-icon title="${this._centralSyncItem.name.npmFullName}" icon="vaadin:tag"></vaadin-icon> npm registry
                        </a>`;
        }
    }

    _loadSyncIcon(){
        if(this._centralSyncItem){
            if(this._centralSyncItem.stage === "RELEASED"){
                return html`<span><vaadin-icon title="Stage: ${this._centralSyncItem.stage}" style="color:var(--lumo-success-color)" icon="vaadin:check-circle"></vaadin-icon> Maven central</span>`;
            }else if(this._centralSyncItem.inProgress || this._centralSyncItem.stage === "INIT"){
                return html`<span><vaadin-icon title="Stage: ${this._centralSyncItem.stage}" style="color:var(--lumo-warning-color)" icon="vaadin:progressbar"></vaadin-icon> Maven central</span>`;
            }else {
                return html`<span style="cursor: pointer;" @click="${this._requestFullSync}"><vaadin-icon title="Stage: ${this._centralSyncItem.stage}" style="color:var(--lumo-error-color)" icon="vaadin:close-circle"></vaadin-icon> Maven central</span>`;
            }
        }
        return html`<span><vaadin-icon title="Checking..." style="color:var(--lumo-warning-color)" icon="vaadin:question-circle-o"></vaadin-icon> Maven central</span>`;
    }
    
    _requestFullSync(){
        var fullSyncRequest = "/api/sync/request/" + this._centralSyncItem.name.mvnGroupId + "/" + this._centralSyncItem.name.mvnArtifactId + "?version=" + this._centralSyncItem.version;
        
        fetch(fullSyncRequest)
            .then(response => response.json())
            .then(response => this._centralSyncItem = response);
    }

    _loadInfoTab(){
        if(this._info){
            return html`<div class="info">
                            <div class="info-line">
                                <h1>${this._coordinates.name} ${this._coordinates.version}</h1>
                                <div class="info-buttons">
                                    ${this._loadSyncIcon()}
                                    ${this._showNpmjsLink()}
                                    <a href="${this._info.scmUrl}" target="_blank"><vaadin-icon icon="vaadin:code"></vaadin-icon> code</a>
                                    <a href="${this._info.issueUrl}" target="_blank"><vaadin-icon icon="vaadin:bug-o"></vaadin-icon> issues</a>
                                    <a href="${this._info.url}" target="_blank"><vaadin-icon icon="vaadin:external-link"></vaadin-icon> page</a>
                                    <span class="badge">${this._info.licenseName}</span>
                                </div>
                            </div>
                            <div class="info-line">
                                ${this._info.description}
                                <div>by <a href="${this._info.organizationUrl}">${this._info.organizationName}</a></div>
                            </div>
                            <div class="info-line">
                                <div class="dependencies">
                                    <div class="useBlock">
                                        <span class="heading">Pom dependency</span>
                                        <pre lang="xml" class="basiccode" id="pom-dependency-code">${this._usePom}</pre>
                                        <vaadin-icon class="copy" title="copy to clipboard" icon="vaadin:copy-o" @click=${this._pomToClipboard}></vaadin-icon>
                                    </div>
                                    
                                    <div class="useBlock">
                                        <span class="heading">Import map</span>
                                        <pre lang="json" class="basiccode" id="import-map-code">${this._useJson}</pre>
                                    </div>

                                    <div class="useBlock">
                                        <span class="heading">Dependencies</span>

                                        <table class="dependencyTable">
                                            ${this._info.dependencies.map((dependency) =>
                                                html`<tr>
                                                        <td style="cursor: pointer;" @click="${()=>this._viewDependency(dependency)}">${dependency}</td>
                                                    </tr>`
                                            )}
                                        </table>
                                    </div>
                                    
                                </div>
                            </div>
                        </div>
                        `;
        }
    }
    
    _pomToClipboard(){
       navigator.clipboard.writeText(this._usePom); 
    }

    _viewDependency(dependency){
        const gav = dependency.split(":");
        this._clearCoordinates()
        this._coordinates.name = gav[0]+":"+gav[1];
        this._showGA(this._coordinates.name);
    }

    _loadFilesTab(){
        if(this._baseUrl){
            return html`<div class="fileBrowser">
                            <div class="fileList">
                                ${this._renderFileGroup('pom', '.pom', 'file-code')}
                                ${this._renderFileGroup('jar', '.jar', 'file-zip')}
                                ${this._renderFileGroup('source', '-sources.jar', 'file-zip')}
                                ${this._renderFileGroup('javadoc', '-javadoc.jar', 'file-zip')}
                                ${this._renderFileGroup('original', '.tgz', 'file-zip')}
                                ${this._renderAnyFile('package', '.json','file-text-o')}
                            </div>
                            ${this._renderCodeView()}    
                        </div>`;
        }
    }
    
    _loadEventLogTab(){
        if(this._gavEventLog){
            return html`<div class="gaveventlogconsole">
            ${this._renderGavEventLog()}
          </div>`;
        }
    }

    private _renderGavEventLog() {
        if (this._gavEventLog && this._gavEventLog.length > 0) {
            return html`
                ${this._gavEventLog.map((entry) => {
                    return html`${this._renderGavEventLogLine(entry)}`
                })}
              `;
        } else {
            return html`<p>Nothing in the event log</p>`;
        }
    }

    private _renderGavEventLogLine(entry){
        let formattedTime = entry.time.substring(0, entry.time.indexOf(".")).replace('T',' ');
        
        return html`<div class="gaveventlogline">
                        <span style="color: grey">${formattedTime}</span>
                        <span style="color: lightblue">${entry.groupId}</span>
                        <span style="color: lightyellow">${entry.artifactId}</span>
                        <span style="color: lightpink">${entry.version}</span>
                        <span style="color: lightgrey">[${entry.stage}]</span>
                        <span style="color: ${entry.color}">${entry.message}</span>
                    </div>`;
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
            return html`<codemirror-viewer src='${this._codeViewSrc}'></codemirror-viewer>`;
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
        const name = this._coordinates.name.trim();
        
        if ((e.which == 13 || e.which == 0)){
            this._showGA(name);    
        }
    }
    
    _showGA(name){ 
        if(name && name.length>0){
            let groupPath: string;
            let artifactPath: string;
            if(name.match(/^[^:]+:[^:]+$/)) {
                this._loadingIcon = "visible";
                const ga = name.split(":");
                groupPath = ga[0].trim().replaceAll('.', '/');
                if(!groupPath.startsWith("org/mvnpm")) {
                    groupPath = `org/mvnpm/${groupPath}`;
                }
                artifactPath = ga[1].trim();

            } else {
                // TODO: This should do a search...
                this._loadingIcon = "visible";
                groupPath = "org/mvnpm";
                artifactPath =  name.replaceAll('@', 'at/');
            }

            const metadataUrl = `/maven2/${groupPath}/${artifactPath}/maven-metadata.xml`;
            fetch(metadataUrl)
                .then((response) => {
                    this._stopLoading();
                    if(response.ok){
                        return response.text();
                    }else if(response.status === 404){
                        const notification = Notification.show(groupPath + '/' + artifactPath + ' not found', {
                            position: 'top-center',
                            duration: 5000,
                        });
                    }else{
                        const notification = Notification.show('Error: ' + response.status + ' - ' + response.statusText, {
                            position: 'top-center',
                            duration: 5000,
                        });
                    }
                })
                .then(xmlDoc => new window.DOMParser().parseFromString(xmlDoc, "text/xml"))
                .then(metadata => this._inspectMetadata(metadata));
        }
    }

    _inspectMetadata(metadata){
        this._coordinates.groupId = metadata.getElementsByTagName("groupId")[0].childNodes[0].nodeValue.substring(9);
        this._coordinates.artifactId = metadata.getElementsByTagName("artifactId")[0].childNodes[0].nodeValue;
        
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
    }
    
    _stopLoading(){
        this._disabled = null;
        this._loadingIcon = "hidden";
    }

    _inspectModel(projectModel){
        let model = new Object();

        model.description = this._getElementValue(projectModel, "description");
        model.url = this._getElementValue(projectModel, "url");
        model.organizationName = this._getElementX2Value(projectModel, "organization", "name");
        model.organizationUrl = this._getElementX2Value(projectModel, "organization", "url");
        model.licenseName = this._getElementX3Value(projectModel, "licenses", "license", "name");
        model.scmUrl = this._getElementX2Value(projectModel, "scm", "url");
        model.issueUrl = this._getElementX2Value(projectModel, "issueManagement", "url");
        
        let propertiesMap = new Map();
        let properties = [];
        let propertiesElement = projectModel.getElementsByTagName("properties");
        if(propertiesElement && propertiesElement.length>0){
            properties = propertiesElement[0].children;
        }
        
        for (let i = 0; i < properties.length; i++) {
            let prop = properties[i];
            propertiesMap.set(prop.tagName, prop.textContent);
        }

        model.dependencies = [];
        let dependenciesElement = projectModel.getElementsByTagName("dependencies");
        if(dependenciesElement && dependenciesElement.length>0){
            let dependencies = dependenciesElement[0].getElementsByTagName("dependency");
        
            for (let i = 0; i < dependencies.length; i++) {
                let dependency = dependencies[i];
                let groupId = dependency.getElementsByTagName("groupId")[0].childNodes[0].nodeValue;
                let artifactId = dependency.getElementsByTagName("artifactId")[0].childNodes[0].nodeValue;
                let version = dependency.getElementsByTagName("version")[0].childNodes[0].nodeValue;
            
                if(version.startsWith("$")){
                    version = version.substring(2).slice(0, -1);
                    version = propertiesMap.get(version);
                }
            
                model.dependencies.push(groupId + ":" + artifactId + ":" + version);
            }
        }

        this._info = model;

    }

    _getElementValue(model, elementName){
        let element = model.getElementsByTagName(elementName);
        if(element && element.length>0){
            let children = element[0].childNodes;
            if(children && children.length>0){
                return element[0].childNodes[0].nodeValue;
            }
        }
        return "";
    }

    _getElementX2Value(model, elementName1, elementName2){
        let element1 = model.getElementsByTagName(elementName1);
        if(element1 && element1.length>0){
            let element2 = element1[0].getElementsByTagName(elementName2);
            if(element2 && element2.length>0){
                return element2[0].childNodes[0].nodeValue; 
            }
        }
        return "";
    }

    _getElementX3Value(model, elementName1, elementName2, elementName3){
        let element1 = model.getElementsByTagName(elementName1);
        if(element1 && element1.length>0){
            let element2 = element1[0].getElementsByTagName(elementName2);
            if(element2 && element2.length>0){
                let element3 = element2[0].getElementsByTagName(elementName3);
                if(element3 && element3.length>0){
                    return element3[0].childNodes[0].nodeValue; 
                }
            }
        }
        return "";
    }

    _getGroupId(groupId){
        if(!groupId){
            groupId = "org.mvnpm";
        }else{
            if(!groupId.startsWith(".")){
                groupId = "." + groupId;
            }
            groupId = groupId.replace('@', 'at.')
            if(!groupId.startsWith("org.mvnpm")){
                groupId = "org.mvnpm" + groupId;
            }
        }
        return groupId;
    }

    _clearCoordinates(){
        this._coordinates = DEFAULT_COORDS;
        this._info = null;
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
    
    _coordinatesNameChanged(e){
        this._coordinates.name = e.target.value;
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
        var getCentralSyncItemUrl = "/api/sync/info/" + groupId + "/" + artifactId + "?version=" + version;
        var eventLogUrl = `/api/eventlog/gav/${groupId}/${artifactId}/${version}`;
        fetch(eventLogUrl)
            .then(response => response.json())
            .then(response => this._gavEventLog = response);

        groupId = groupId.replaceAll('.', '/');
        var importMapUrl = "/maven2/" + groupId + "/" + artifactId + "/" + version + "/importmap.json";

        fetch(importMapUrl)
            .then(response => response.json())
            .then(response => this._setUseJson(response));
            
        this._baseFile = artifactId + "-" + version;
        this._baseUrl = "/maven2/" + groupId + "/" + artifactId + "/" + version + "/";
        
        this._changeCodeView(this._baseUrl + this._baseFile + this._codeViewSelection);
        
        fetch(getCentralSyncItemUrl)
            .then(response => response.json())
            .then(response => this._centralSyncItem = response);

        const pomUrl = `/maven2/${groupId}/${artifactId}/${version}/${artifactId}-${version}.pom`;
            fetch(pomUrl)
              .then(response => response.text())
              .then(xmlDoc => new window.DOMParser().parseFromString(xmlDoc, "application/xml"))
              .then(projectModel => this._inspectModel(projectModel));
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