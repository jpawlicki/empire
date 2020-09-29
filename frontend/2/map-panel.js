// <map-panel>

class MapPanel extends HTMLElement {
	constructor() {
		super();

		this.ViewMode = {
			POLITICAL: 1,
			CULTURE: 2,
			RELIGION: 3,
			POPULATION: 4,
			FOOD: 5,
			UNREST: 6,
			CULT: 7
		};

		// Create shadow.
		let shadow = this.attachShadow({mode: "open"});
		this.shadow = shadow;
		let style = document.createElement("style");
		style.textContent = `
			#controls {
				position: absolute;
				top: 0;
				left: 0;
				border-bottom-right-radius: 1em;
				background-color: rgba(220, 220, 220, 0.5);
				padding: 0.2em;
			}
			#controls #mapControls {
				padding-left: 1em;
				border-left: 1px solid #888;
				margin-left: 1em;
				display: inline-block;
			}
			#controls label {
				cursor: pointer;
				display: inline-block;
			}
			#controls input {
				display: none;
			}
			#controls input + svg {
				fill: #000;
				filter: drop-shadow(0px 0px 4px rgba(255,255,255,1));
				transition: filter 0.3s, fill 0.3s;
			}
			#controls input:checked + svg {
				fill: #fff;
				filter: drop-shadow(0px 3px 4px rgba(0,0,0,1));
			}

			/* mapDiv */
			#mapDiv {
				width: 100%;
				height: 100%;
				overflow: auto;
			}
			
			/* Map */
      #svg {
				width: 1750px;
      }
			#canals {
				stroke-dasharray: 3 3;
				fill: transparent;
				stroke: #aaaaff;
				stroke-width: 1;
			}
			#regions path:hover, #sea_regions path:hover, #regionContents g:hover {
				fill: #fc0 !important;
				transition: fill 0.3s;
			}
			#borders path {
				stroke: #000;
				stroke-width: 0.5;
				fill: transparent;
				pointer-events: none;
			}
			#regionContents line, #regionContents circle {
				pointer-events: none;
			}
			#regionContents g {
				filter: url(#shadow);
				transition: transform 1s;
			}
			#sea_regions path {
				fill: url(#circlefill);
			}
			#plannedMotions {
				pointer-events: none;
			}
			#regions path {
				transition: fill 0.25s;
			}
			#mapMouseTooltip {
				position: absolute;
				background: rgba(255, 255, 255, 0.3);
				font-family: sans-serif;
				border-radius: 5px;
			}
			#popup {
				font-family: sans-serif;
				background: rgba(255, 255, 255, 0.9);
				border-radius: 1em;
				border: 2px solid #000;
				display: none;
				position: fixed;
				top: 0;
				left: 0;
				max-height: 90%;
				max-width: 90%;
				min-width: 30%;
				min-height: 30%;
				overflow: auto;
			}
			#popupClose {
				float: right;
				cursor: pointer;
				border-radius: 0 0 0 calc(1em - 2px);
				background-color: rgba(255, 255, 255, 0.5);
			}
			#popupClose:hover path {
				fill: #600;
			}

			/* Table Panel */
			#tableDiv {
				overflow: auto;
				height: 100%;
				background: #fff;
			}
			#tableDiv td, #tableDiv th {
				padding: 0.4em;
			}
			#tableDiv table {
				border-spacing: 0;
				min-width: 100%;
				margin-bottom: 5em;
			}
			#tableDiv td img {
				height: 2.5em;
				filter: grayscale(100%) drop-shadow(0 0 5px #fff);
				margin-right: 0.2em;
			}
			#tableDivTable td:first-child, #tableDivTable td:nth-child(2) {
				font-size: 200%;
				font-weight: bold;
			}
			#tableDivTable tr :nth-child(5), #tableDivTable tr :nth-child(6), #tableDivTable tr :nth-child(7), #tableDivTable tr :nth-child(8) {
				text-align: right;
			}
			#tableDivTable td:nth-child(5), #tableDivTable td:nth-child(6), #tableDivTable td:nth-child(7), #tableDivTable td:nth-child(8) {
				font-size: 150%;
			}
			#tableDivTable ul {
				margin: 0;
				padding: 0;
				list-style-type: none;
			}
			#tableDivTableRegions kingdom-label {
				font-size: 50%;
			}
			#tableDivTableRegions td:nth-child(4), #tableDivTableRegions td:nth-child(n + 7) {
				text-align: right;
			}
			#tableDivTableRegions td:nth-child(5) {
				text-transform: capitalize;
			}

			/* Score Panel */
			#scoreDiv {
				background: #fff;
				overflow: auto;
				height: 100%;
			}
			h1 {
				margin-top: 1em;
				margin-bottom: 1em;
				margin-left: 0;
				font-size: 200%;
				font-weight: bold;
				font-family: sans-serif;
				text-align: center;
			}
			#scoretable {
				display: grid;
				grid-template-columns: 1fr 1fr;
				grid-template-rows: 6em 4em;
				text-align: center;
				font-family: sans-serif;
			}
			#scoretable > div > div:first-child {
				font-size: 200%;
			}
			#scoretable > div {
				font-size: 60%;
			}
			#scoretable > div:first-child {
				grid-column: 1 / 3;
				font-size: 150%;
			}
			.inactive {
				color: #777;
			}
			ul {
				font-size: 90%;
				margin-top: 0;
			}
		`;
		shadow.appendChild(style);

		let contentsStyle = document.createElement("style");
		contentsStyle.setAttribute("id", "contentsStyle");
		shadow.appendChild(contentsStyle);

		let controlDiv = document.createElement("div");

		controlDiv.setAttribute("id", "controls");
		controlDiv.appendChild(makeButton("Map", "topic", "M15,19L9,16.89V5L15,7.11M20.5,3C20.44,3 20.39,3 20.34,3L15,5.1L9,3L3.36,4.9C3.15,4.97 3,5.15 3,5.38V20.5A0.5,0.5 0 0,0 3.5,21C3.55,21 3.61,21 3.66,20.97L9,18.9L15,21L20.64,19.1C20.85,19 21,18.85 21,18.62V3.5A0.5,0.5 0 0,0 20.5,3Z", () => { this.showMap(); }, true));
		controlDiv.appendChild(makeButton("Table", "topic", "M5,4H19A2,2 0 0,1 21,6V18A2,2 0 0,1 19,20H5A2,2 0 0,1 3,18V6A2,2 0 0,1 5,4M5,8V12H11V8H5M13,8V12H19V8H13M5,14V18H11V14H5M13,14V18H19V14H13Z", () => { this.showTable(); }, false));
		controlDiv.appendChild(makeButton("International Relations", "topic", "M18,16.08C17.24,16.08 16.56,16.38 16.04,16.85L8.91,12.7C8.96,12.47 9,12.24 9,12C9,11.76 8.96,11.53 8.91,11.3L15.96,7.19C16.5,7.69 17.21,8 18,8A3,3 0 0,0 21,5A3,3 0 0,0 18,2A3,3 0 0,0 15,5C15,5.24 15.04,5.47 15.09,5.7L8.04,9.81C7.5,9.31 6.79,9 6,9A3,3 0 0,0 3,12A3,3 0 0,0 6,15C6.79,15 7.5,14.69 8.04,14.19L15.16,18.34C15.11,18.55 15.08,18.77 15.08,19C15.08,20.61 16.39,21.91 18,21.91C19.61,21.91 20.92,20.61 20.92,19A2.92,2.92 0 0,0 18,16.08Z", () => { this.showInternationalRelations(); }, false));
		controlDiv.appendChild(makeButton("Score", "topic", "M16,11.78L20.24,4.45L21.97,5.45L16.74,14.5L10.23,10.75L5.46,19H22V21H2V3H4V17.54L9.5,8L16,11.78Z", () => { this.showScore(); }, false));

		let mapControlDiv = document.createElement("div");
		mapControlDiv.setAttribute("id", "mapControls");
		controlDiv.appendChild(mapControlDiv);

		mapControlDiv.appendChild(makeButton("Political Map", "view", "M5,16L3,5L8.5,12L12,5L15.5,12L21,5L19,16H5M19,19A1,1 0 0,1 18,20H6A1,1 0 0,1 5,19V18H19V19Z", () => { this.viewMode = this.ViewMode.POLITICAL; this.repaintRegions(); }, true));
		this.viewMode = this.ViewMode.POLITICAL;
		mapControlDiv.appendChild(makeButton("Cultural Map", "view", "M20.71,4.63L19.37,3.29C19,2.9 18.35,2.9 17.96,3.29L9,12.25L11.75,15L20.71,6.04C21.1,5.65 21.1,5 20.71,4.63M7,14A3,3 0 0,0 4,17C4,18.31 2.84,19 2,19C2.92,20.22 4.5,21 6,21A4,4 0 0,0 10,17A3,3 0 0,0 7,14Z", () => { this.viewMode = this.ViewMode.CULTURE; this.repaintRegions(); }, false));
		mapControlDiv.appendChild(makeButton("Religious Map", "view", "M11,2H13V4H15V6H13V9.4L22,13V15L20,14.2V22H14V17A2,2 0 0,0 12,15A2,2 0 0,0 10,17V22H4V14.2L2,15V13L11,9.4V6H9V4H11V2M6,20H8V15L7,14L6,15V20M16,20H18V15L17,14L16,15V20Z", () => { this.viewMode = this.ViewMode.RELIGION; this.repaintRegions(); }, false));
		mapControlDiv.appendChild(makeButton("Population Map", "view", "M16,13C15.71,13 15.38,13 15.03,13.05C16.19,13.89 17,15 17,16.5V19H23V16.5C23,14.17 18.33,13 16,13M8,13C5.67,13 1,14.17 1,16.5V19H15V16.5C15,14.17 10.33,13 8,13M8,11A3,3 0 0,0 11,8A3,3 0 0,0 8,5A3,3 0 0,0 5,8A3,3 0 0,0 8,11M16,11A3,3 0 0,0 19,8A3,3 0 0,0 16,5A3,3 0 0,0 13,8A3,3 0 0,0 16,11Z", () => { this.viewMode = this.ViewMode.POPULATION; this.repaintRegions(); }, false));
		mapControlDiv.appendChild(makeButton("Food Map", "view", "M7.33,18.33C6.5,17.17 6.5,15.83 6.5,14.5C8.17,15.5 9.83,16.5 10.67,17.67L11,18.23V15.95C9.5,15.05 8.08,14.13 7.33,13.08C6.5,11.92 6.5,10.58 6.5,9.25C8.17,10.25 9.83,11.25 10.67,12.42L11,13V10.7C9.5,9.8 8.08,8.88 7.33,7.83C6.5,6.67 6.5,5.33 6.5,4C8.17,5 9.83,6 10.67,7.17C10.77,7.31 10.86,7.46 10.94,7.62C10.77,7 10.66,6.42 10.65,5.82C10.64,4.31 11.3,2.76 11.96,1.21C12.65,2.69 13.34,4.18 13.35,5.69C13.36,6.32 13.25,6.96 13.07,7.59C13.15,7.45 13.23,7.31 13.33,7.17C14.17,6 15.83,5 17.5,4C17.5,5.33 17.5,6.67 16.67,7.83C15.92,8.88 14.5,9.8 13,10.7V13L13.33,12.42C14.17,11.25 15.83,10.25 17.5,9.25C17.5,10.58 17.5,11.92 16.67,13.08C15.92,14.13 14.5,15.05 13,15.95V18.23L13.33,17.67C14.17,16.5 15.83,15.5 17.5,14.5C17.5,15.83 17.5,17.17 16.67,18.33C15.92,19.38 14.5,20.3 13,21.2V23H11V21.2C9.5,20.3 8.08,19.38 7.33,18.33Z", () => { this.viewMode = this.ViewMode.FOOD; this.repaintRegions(); }, false));
		mapControlDiv.appendChild(makeButton("Unrest Map", "view", "M20,12A8,8 0 0,0 12,4A8,8 0 0,0 4,12A8,8 0 0,0 12,20A8,8 0 0,0 20,12M22,12A10,10 0 0,1 12,22A10,10 0 0,1 2,12A10,10 0 0,1 12,2A10,10 0 0,1 22,12M15.5,8C16.3,8 17,8.7 17,9.5C17,10.3 16.3,11 15.5,11C14.7,11 14,10.3 14,9.5C14,8.7 14.7,8 15.5,8M10,9.5C10,10.3 9.3,11 8.5,11C7.7,11 7,10.3 7,9.5C7,8.7 7.7,8 8.5,8C9.3,8 10,8.7 10,9.5M12,14C13.75,14 15.29,14.72 16.19,15.81L14.77,17.23C14.32,16.5 13.25,16 12,16C10.75,16 9.68,16.5 9.23,17.23L7.81,15.81C8.71,14.72 10.25,14 12,14Z", () => { this.viewMode = this.ViewMode.UNREST; this.repaintRegions(); }, false));
		mapControlDiv.appendChild(makeButton("Cult Map", "view", "M12,1L9,9L1,12L9,15L12,23L15,15L23,12L15,9L12,1Z", () => { this.viewMode = this.ViewMode.CULT; this.repaintRegions(); }, false));
		shadow.appendChild(controlDiv);

		let mapDiv = document.createElement("div");
		mapDiv.setAttribute("id", "mapDiv");
		mapDiv.innerHTML = `
			<svg xmlns:svg="http://www.w3.org/2000/svg" xmlns="http://www.w3.org/2000/svg" viewBox="-10 -10 1020 1020" id="svg" version="1.1">
				<!-- Patterns -->
				<symbol id="character" viewBox="0 0 24 24">
					<circle fill="inherit" stroke="inherit" stroke-width="inherit" cx="12" cy="12" r="11.5"/>
					<path pointer-events="none" fill="inherit" stroke="inherit" stroke-width="inherit" d="M12,4A4,4 0 0,1 16,8A4,4 0 0,1 12,12A4,4 0 0,1 8,8A4,4 0 0,1 12,4M12,14C16.42,14 20,15.79 20,18V20H4V18C4,15.79 7.58,14 12,14Z"/>
				</symbol>
				<symbol id="army_seafaring" viewBox="-3 -3 30 30">
					<circle cx="12" cy="12" r="14" fill="inherit" stroke="inherit" stroke-width="1"></circle>
					<path pointer-events="none" fill="currentColor" d="M6.2,2.44L18.1,14.34L20.22,12.22L21.63,13.63L19.16,16.1L22.34,19.28C22.73,19.67 22.73,20.3 22.34,20.69L21.63,21.4C21.24,21.79 20.61,21.79 20.22,21.4L17,18.23L14.56,20.7L13.15,19.29L15.27,17.17L3.37,5.27V2.44H6.2M15.89,10L20.63,5.26V2.44H17.8L13.06,7.18L15.89,10M10.94,15L8.11,12.13L5.9,14.34L3.78,12.22L2.37,13.63L4.84,16.1L1.66,19.29C1.27,19.68 1.27,20.31 1.66,20.7L2.37,21.41C2.76,21.8 3.39,21.8 3.78,21.41L7,18.23L9.44,20.7L10.85,19.29L8.73,17.17L10.94,15Z" />
				</symbol>
				<symbol id="army_steel" viewBox="-3 -3 30 30">
					<rect x="-1" y="-1" width="26" height="26" fill="inherit" stroke="inherit" stroke-width="1"></rect>
					<path pointer-events="none" fill="currentColor" d="M6.2,2.44L18.1,14.34L20.22,12.22L21.63,13.63L19.16,16.1L22.34,19.28C22.73,19.67 22.73,20.3 22.34,20.69L21.63,21.4C21.24,21.79 20.61,21.79 20.22,21.4L17,18.23L14.56,20.7L13.15,19.29L15.27,17.17L3.37,5.27V2.44H6.2M15.89,10L20.63,5.26V2.44H17.8L13.06,7.18L15.89,10M10.94,15L8.11,12.13L5.9,14.34L3.78,12.22L2.37,13.63L4.84,16.1L1.66,19.29C1.27,19.68 1.27,20.31 1.66,20.7L2.37,21.41C2.76,21.8 3.39,21.8 3.78,21.41L7,18.23L9.44,20.7L10.85,19.29L8.73,17.17L10.94,15Z" />
				</symbol>
				<symbol id="army_pillagers" viewBox="-3 -3 30 30">
					<path fill="inherit" stroke="inherit" stroke-width="1" d="M-2,0L26,0L12,26Z"></path>
					<path pointer-events="none" fill="currentColor" d="M6.2,2.44L18.1,14.34L20.22,12.22L21.63,13.63L19.16,16.1L22.34,19.28C22.73,19.67 22.73,20.3 22.34,20.69L21.63,21.4C21.24,21.79 20.61,21.79 20.22,21.4L17,18.23L14.56,20.7L13.15,19.29L15.27,17.17L3.37,5.27V2.44H6.2M15.89,10L20.63,5.26V2.44H17.8L13.06,7.18L15.89,10M10.94,15L8.11,12.13L5.9,14.34L3.78,12.22L2.37,13.63L4.84,16.1L1.66,19.29C1.27,19.68 1.27,20.31 1.66,20.7L2.37,21.41C2.76,21.8 3.39,21.8 3.78,21.41L7,18.23L9.44,20.7L10.85,19.29L8.73,17.17L10.94,15Z" />
				</symbol>
				<symbol id="army_weathered" viewBox="-3 -3 30 30">
					<path fill="inherit" stroke="inherit" stroke-width="1" d="M2,-1L22,-1L27,25L-3,25Z"></path>
					<path pointer-events="none" fill="currentColor" d="M6.2,2.44L18.1,14.34L20.22,12.22L21.63,13.63L19.16,16.1L22.34,19.28C22.73,19.67 22.73,20.3 22.34,20.69L21.63,21.4C21.24,21.79 20.61,21.79 20.22,21.4L17,18.23L14.56,20.7L13.15,19.29L15.27,17.17L3.37,5.27V2.44H6.2M15.89,10L20.63,5.26V2.44H17.8L13.06,7.18L15.89,10M10.94,15L8.11,12.13L5.9,14.34L3.78,12.22L2.37,13.63L4.84,16.1L1.66,19.29C1.27,19.68 1.27,20.31 1.66,20.7L2.37,21.41C2.76,21.8 3.39,21.8 3.78,21.41L7,18.23L9.44,20.7L10.85,19.29L8.73,17.17L10.94,15Z" />
				</symbol>
				<symbol id="army_scheming" viewBox="-3 -3 30 30">
					<path fill="inherit" stroke="inherit" stroke-width="1" d="M3,-1L27,-1L21,24L-3,25Z"></path>
					<path pointer-events="none" fill="currentColor" d="M6.2,2.44L18.1,14.34L20.22,12.22L21.63,13.63L19.16,16.1L22.34,19.28C22.73,19.67 22.73,20.3 22.34,20.69L21.63,21.4C21.24,21.79 20.61,21.79 20.22,21.4L17,18.23L14.56,20.7L13.15,19.29L15.27,17.17L3.37,5.27V2.44H6.2M15.89,10L20.63,5.26V2.44H17.8L13.06,7.18L15.89,10M10.94,15L8.11,12.13L5.9,14.34L3.78,12.22L2.37,13.63L4.84,16.1L1.66,19.29C1.27,19.68 1.27,20.31 1.66,20.7L2.37,21.41C2.76,21.8 3.39,21.8 3.78,21.41L7,18.23L9.44,20.7L10.85,19.29L8.73,17.17L10.94,15Z" />
				</symbol>
				<symbol id="army_undead" viewBox="-3 -3 30 30">
					<path fill="inherit" stroke="inherit" stroke-width="1" d="M-2,12L12,-2L26,12L12,26Z"></path>
					<path pointer-events="none" fill="currentColor" d="M6.2,2.44L18.1,14.34L20.22,12.22L21.63,13.63L19.16,16.1L22.34,19.28C22.73,19.67 22.73,20.3 22.34,20.69L21.63,21.4C21.24,21.79 20.61,21.79 20.22,21.4L17,18.23L14.56,20.7L13.15,19.29L15.27,17.17L3.37,5.27V2.44H6.2M15.89,10L20.63,5.26V2.44H17.8L13.06,7.18L15.89,10M10.94,15L8.11,12.13L5.9,14.34L3.78,12.22L2.37,13.63L4.84,16.1L1.66,19.29C1.27,19.68 1.27,20.31 1.66,20.7L2.37,21.41C2.76,21.8 3.39,21.8 3.78,21.41L7,18.23L9.44,20.7L10.85,19.29L8.73,17.17L10.94,15Z" />
				</symbol>
				<symbol id="navy" viewBox="-3 -3 30 30">
					<circle cx="12" cy="12" r="14" fill="inherit" stroke="inherit" stroke-width="1"></circle>
					<path pointer-events="none" fill="currentColor" d="M12,2A3,3 0 0,0 9,5C9,6.27 9.8,7.4 11,7.83V10H8V12H11V18.92C9.16,18.63 7.53,17.57 6.53,16H8V14H3V19H5V17.3C6.58,19.61 9.2,21 12,21C14.8,21 17.42,19.61 19,17.31V19H21V14H16V16H17.46C16.46,17.56 14.83,18.63 13,18.92V12H16V10H13V7.82C14.2,7.4 15,6.27 15,5A3,3 0 0,0 12,2M12,4A1,1 0 0,1 13,5A1,1 0 0,1 12,6A1,1 0 0,1 11,5A1,1 0 0,1 12,4Z" />
				</symbol>
				<symbol id="crosshair_symbol" viewBox="0 0 20 20">
					<path fill="transparent" stroke="#fff" stroke-width="2" d="M0,10L20,10 M10,0L10,20" />
					<path fill="transparent" stroke="#000" stroke-width="1" d="M0,10L20,10 M10,0L10,20" />
				</symbol>
				<symbol id="holycity" viewBox="1.5 0.35 20 20">
					<circle fill="#ffffff" stroke="#000000" stroke-width="inherit" cx="11.75" cy="10.35" r="9.5"/>
					<path pointer-events="none" fill="inherit" d="M17.8,8C17.26,5.89 15.61,4.24 13.5,3.7V2H10.5V3.7C8.39,4.24 6.74,5.89 6.2,8H4V11H6.2C6.74,13.11 8.39,14.76 10.5,15.3V22H13.5V15.3C15.61,14.76 17.26,13.11 17.8,11H19.97V8H17.8M12.04,9.53L14.5,11H15.76C15.35,12.03 14.53,12.84 13.5,13.26V12L12.06,9.56L12,9.5L11.94,9.56L10.5,12V13.26C9.47,12.84 8.66,12.03 8.24,11H9.5L11.96,9.53L12,9.5H11.96L9.5,8H8.24C8.65,6.97 9.47,6.16 10.5,5.74V7L11.94,9.44L12,9.5L12.06,9.44L13.5,7V5.74C14.53,6.16 15.35,6.97 15.76,8H14.5L12.04,9.5H12L12.04,9.53Z" />
				</symbol>
				<marker id="arrowHead" markerWidth="10" markerHeight="10" refX="0" refY="1.5" orient="auto" markerUnits="strokeWidth">
					<path fill="#aaa" d="M0,0 L0,3 L6,1.5 z"/>
				</marker>
				<marker id="arrowHeadPlanned" markerWidth="10" markerHeight="10" refX="0" refY="1.5" orient="auto" markerUnits="strokeWidth">
					<path fill="#fa8" d="M0,0 L0,3 L6,1.5 z"/>
				</marker>
				<pattern id="circlefill" x="0" y="0" width="48" height="48" patternUnits="userSpaceOnUse">
					<rect width="48" height="48" fill="#ddddff"></rect>
					<path d="M2 3 q 0 0, 1 1 t 2 -1 t 3 1" stroke="#ccf" fill="transparent"></path>
					<path d="M12 9 q 0 0, 1 1 t 2 -1 t 3 1" stroke="#ccf" fill="transparent"></path>
					<path d="M24 30 q 0 0, 1 1 t 2 -1 t 3 1" stroke="#ccf" fill="transparent"></path>
					<path d="M32 20 q 0 0, 1 1 t 2 -1 t 3 1" stroke="#ccf" fill="transparent"></path>
					<path d="M29 6 q 0 0, 1 1 t 2 -1 t 3 1" stroke="#ccf" fill="transparent"></path>
					<path d="M37 36 q 0 0, 1 1 t 2 -1 t 3 1" stroke="#ccf" fill="transparent"></path>
					<path d="M7 32 q 0 0, 1 1 t 2 -1 t 3 1" stroke="#ccf" fill="transparent"></path>
				</pattern>
				<pattern id="starvation" x="0" y="0" width="24" height="24" patternUnits="userSpaceOnUse">
					<rect width="48" height="48" fill="#ff0000"></rect>
					<path transform="scale(0.5)" fill="#880000" d="M11.96,1.21C11.3,2.76 10.64,4.31 10.65,5.82C10.66,6.42 10.77,7 10.94,7.62C10.86,7.46 10.77,7.31 10.67,7.17C9.83,6 8.17,5 6.5,4C6.5,4.8 6.5,5.59 6.68,6.36L13,12.68V10.7C14.5,9.8 15.92,8.88 16.67,7.83C17.5,6.67 17.5,5.33 17.5,4C15.83,5 14.17,6 13.33,7.17C13.23,7.31 13.15,7.45 13.07,7.59C13.25,6.96 13.36,6.32 13.35,5.69C13.34,4.18 12.65,2.69 11.96,1.21M3.28,5.5L2,6.77L6.64,11.41C6.75,12 6.95,12.55 7.33,13.08C8.08,14.13 9.5,15.05 11,15.95V18.23L10.67,17.67C9.83,16.5 8.17,15.5 6.5,14.5C6.5,15.83 6.5,17.17 7.33,18.33C8.08,19.38 9.5,20.3 11,21.2V23H13V21.2C13.74,20.76 14.45,20.31 15.07,19.84L18.73,23.5L20,22.22C14,16.23 9.1,11.32 3.28,5.5M17.5,9.25C15.83,10.25 14.17,11.25 13.33,12.42L13.12,12.79L15,14.66C15.67,14.16 16.27,13.64 16.67,13.08C17.5,11.92 17.5,10.58 17.5,9.25M17.5,14.5C16.93,14.84 16.38,15.18 15.85,15.53L17.29,16.97C17.5,16.17 17.5,15.33 17.5,14.5Z" />
				</pattern>
				<pattern id="unruled" x="0" y="0" width="24" height="24" patternUnits="userSpaceOnUse">
					<rect width="48" height="48" fill="#cccccc"></rect>
					<path transform="scale(0.5)" fill="#999999" d="M 3 5 L 1.7207031 6.2695312 L 3.5664062 8.1152344 L 5 16 L 11.449219 16 L 13.449219 18 L 5 18 L 5 19 A 1 1 0 0 0 6 20 L 15.449219 20 L 18.449219 23 L 19.720703 21.720703 L 18 20 L 16 18 L 14 16 L 9 11 L 3 5 z M 12 5 L 9.8496094 9.3007812 L 16.550781 16 L 19 16 L 21 5 L 15.5 12 L 12 5 z M 18.550781 18 L 19 18.449219 L 19 18 L 18.550781 18 z"/>
				</pattern>
				<filter id="shadow" x="-100%" y="-100%" height="300%" width="300%">
					<feDropShadow dx="1.5" dy="1.5" stdDeviation="1" flood-opacity="0.5"/>
				</filter>
				<g id="content">
					<g id="regions" class="v_political">
					</g>
					<g id="sea_regions">
					</g>
					<g id="borders">
					</g>
					<g id="plannedMotions">
					</g>
					<g id="regionContents">
					</g>
					<use id="crosshair" x="-100" y="-100" width="30" height="30" href="#crosshair_symbol"></use>
				</g>
			</svg>
			<span id="mapMouseTooltip"></span>
			<div id="popup">
				<div id="popupClose">
					<svg style="width:24px;height:24px" viewBox="0 0 24 24" title="Close">
						<path fill="#000000" d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z" />
					</svg></div>
				<div id="popupContents"></div>
			</div>
			`;
		shadow.appendChild(mapDiv);

		shadow.getElementById("popupClose").addEventListener("click", () => {
			clearHighlight();
			shadow.getElementById("popup").style.display = "none";
		});

		// Panning and zooming controls.
		mapDiv.addEventListener("mousedown", (ev) => {
			this.totalScrollDelta = 0;
		});

		mapDiv.addEventListener("mousemove", (ev) => {
			if (ev.buttons & 0x1 == 0x1) {
				mapDiv.scrollBy(-ev.movementX, -ev.movementY);
				this.totalScrollDelta += Math.abs(ev.movementY) + Math.abs(ev.movementX);
			} else {
				let tt = shadow.getElementById("mapMouseTooltip");
				tt.style.bottom = "auto";
				tt.style.right = "auto";
				tt.style.top = ev.clientY + 10;
				tt.style.left = ev.clientX + 10;
				if (tt.getBoundingClientRect().bottom > window.innerHeight) {
					tt.style.top = "auto";
					tt.style.bottom = 10;
				}
			}
		});

		mapDiv.addEventListener("mouseout", (ev) => {
			let tt = shadow.getElementById("mapMouseTooltip");
			tt.innerHTML = "";
			tt.style.top = 0;
			tt.style.left = 0;
		});

		mapDiv.addEventListener("wheel", (ev) => {
			if (ev.shiftKey) {
				let svg = shadow.querySelector("#mapDiv > svg");
				svg.style.width = Math.max(500, Math.min(10000, svg.getBoundingClientRect().width * 1 - (ev.deltaY / 3))) + "px";
				ev.preventDefault();
			}
		}, {passive: false});

		let tableDiv = document.createElement("div");
		tableDiv.setAttribute("id", "tableDiv");
		tableDiv.style.display = "none";
		tableDiv.innerHTML = `
			<h1>Nations</h1>
			<table id="tableDivTable">
				<tr>
					<th></th>
					<th></th>
					<th>Traits</th>
					<th>Religion</th>
					<th>Population</th>
					<th>Tax</th>
					<th>Weeks Food</th>
					<th>Recruits</th>
					<th colspan="2">Ruler</th>
				</tr>
				<tbody id="tableDivTableTbody">
				</tbody>
			</table>
			<h1>Regions</h1>
			<table id="tableDivTableRegions">
				<tr>
					<th></th>
					<th>Contoller</th>
					<th>Historic Controller</th>
					<th>Population</th>
					<th>Culture</th>
					<th>Religion</th>
					<th>Tax</th>
					<th>Recruits</th>
					<th>Food</th>
					<th>Crops</th>
					<th>Unrest</th>
				</tr>
				<tbody id="tableDivTableTbodyRegions">
				</tbody>
			</table>
		`;
		shadow.appendChild(tableDiv);

		let scoreDiv = document.createElement("div");
		scoreDiv.setAttribute("id", "scoreDiv");
		scoreDiv.style.display = "none";
		shadow.appendChild(scoreDiv);
		scoreDiv.innerHTML = `
			<h1>Score</h1>
			<div id="scoretable">
				<div>
					<div id="score_kingdom"></div>
					<div>NATION</div>
				</div>
				<div>
					<div id="score_culture"></div>
					<div>CULTURE</div>
				</div>
				<div>
					<div id="score_global"></div>
					<div>ALL-TIME</div>
				</div>
			</div>
			<expandable-snippet text="Your score percentile (above) is based on the scores achieved by other players of your nation (in other games)."></expandable-snippet>
			<h2>Total: <span id="scorepoints"></span></h2>
			<div id="profiles">
				<expandable-snippet text="All of the below conditions can stack."></expandable-snippet>
			</div>
		`;

		let internationalRelationsDiv = document.createElement("div");
		internationalRelationsDiv.setAttribute("id", "internationalRelationsDiv");
		internationalRelationsDiv.style.display = "none";
		shadow.appendChild(internationalRelationsDiv);
		internationalRelationsDiv.innerHTML = `
			<h1>International Relations</h1>
			<div id="international_relations"></div>
		`;
	}

	loadGeography(geo) {
		let shadow = this.shadow;
		let regionElement = this.shadow.getElementById("regions");
		let seaRegionElement = this.shadow.getElementById("sea_regions");
		for (let i = 0; i < geo.regions.length; i++) {
			let region = geo.regions[i];
			let p = document.createElementNS("http://www.w3.org/2000/svg", "path");
			p.setAttribute("id", "region_" + i);
			p.setAttribute("d", shapeToPath(region.path));
			p.setAttribute("fill-rule", "evenodd");
			p.addEventListener("click", function () { changeReport("region/" + g_data.regions[i].name); });
			p.addEventListener("mouseover", () => { shadow.getElementById("mapMouseTooltip").textContent = g_data.regions[i].name; });
			if (region.type == "land") regionElement.appendChild(p);
			else seaRegionElement.appendChild(p);
		}

		let borderElement = this.shadow.getElementById("borders");
		for (let b of g_borders) {
			let pe = document.createElementNS("http://www.w3.org/2000/svg", "path");
			let path = [b.path];
			if (b.weight > 1) path.push(parallel(b.path, 1.5));
			if (b.weight > 2) path.push(parallel(b.path, -1.5));
			pe.setAttribute("d", shapeToPath(path, false));
			pe.setAttribute("id", "borderpath_" + b.a + "_" + b.b);
			borderElement.appendChild(pe);
		}

		let style = "";
		for (let ki of g_geo.kingdoms) {
			let n = ki.name;
			style += "#regionContents ." + n.toLowerCase() + "{ fill: " + getColor(n) + "; stroke: " + getForegroundColor(n) + "; color: " + getForegroundColor(n) + "; }\n";
			style += "#regionContents circle." + n.toLowerCase() + "{ fill: " + getForegroundColor(n) + "; stroke: transparent; }\n";
			style += "#regionContents line." + n.toLowerCase() + "{ stroke: " + getColor(n) + "; }\n";
		}
		style += "#regionContents .pirate { fill: #000; stroke: #fff; color: #fff; }\n";
		style += "#regionContents circle.pirate { fill: #fff; stroke: transparent; }\n";
		style += "#regionContents line.pirate { stroke: #000; }\n";

		this.shadow.getElementById("contentsStyle").textContent = style;

		this.pointCache = [];
	}

	repaintMap() {
		this.repaintRegions();
		this.repaintRegionContents();
		this.shadow.getElementById("plannedMotions").textContent = "";
		this.repaintScore();
		this.repaintTable();
		this.repaintInternationalRelations();
	}

	repaintRegions() {
		let cultureColors = {
			"eolsung": "#aaf",
			"anpilayn": "#faa",
			"hansa": "#fc4",
			"tyrgaetan": "#aaa",
			"tavian": "#a4c"
		};

		let religionColors = {
			"Iruhan (Sword of Truth)": "#f00",
			"Iruhan (Vessel of Faith)": "#f80",
			"Iruhan (Chalice of Compassion)": "#f88",
			"Iruhan (Tapestry of People)": "#f08",
			"Northern (Alyrja)": "#00f",
			"Northern (Rjinku)": "#80f",
			"Northern (Lyskr)": "#08f",
			"Northern (Syrjen)": "#88f",
			"Tavian (Flame of Kith)": "#8f0",
			"Tavian (River of Kuun)": "#0f8"
		}

		for (let i = 0; i < g_data.regions.length; i++) {
			let r = g_data.regions[i];
			if (r.type != "land") continue;
			let ele = this.shadow.getElementById("region_" + i);
			if (this.viewMode == this.ViewMode.POLITICAL) {
				if (r.kingdom == "Unruled") {
					ele.style.fill = "url(\"#unruled\")";
				} else {
					ele.style.fill = getColor(r.kingdom);
				}
			} else if (this.viewMode == this.ViewMode.CULTURE) {
				ele.style.fill = cultureColors[r.culture];
			} else if (this.viewMode == this.ViewMode.CULT) {
				ele.style.fill = r.cult_accessed ? "#a00" : "#fff";
			} else if (this.viewMode == this.ViewMode.RELIGION) {
				ele.style.fill = religionColors[r.religion];
			} else if (this.viewMode == this.ViewMode.POPULATION) {
				if (r.population < 400000) {
					// First gradient: pale to blue (400k).
					ele.style.fill = "rgb(" + Math.floor(255 - r.population / 400000 * 255) + ", " + Math.floor(255 - r.population / 400000 * 255) + ", 255)";
				} else if (r.population < 800000) {
					// Second gradient: blue to red (800k).
					ele.style.fill = "rgb(" + Math.floor((r.population - 400000) / 400000 * 255) + ", 0, " + Math.floor(255 - (r.population - 400000) / 400000 * 255) +  ")";
				} else {
					ele.style.fill = "#f00";
				}
			} else if (this.viewMode == this.ViewMode.UNREST) {
				let unrest = r.calcUnrest().v * 255;
				ele.style.fill = "rgb(255, " + Math.floor(255 - unrest) + ", " + Math.floor(255 - unrest) + ")";
			} else if (this.viewMode == this.ViewMode.FOOD) {
				let weeksToHarvest = 3 - (g_data.date + 3) % 4;
				let foodPoint = r.calcFoodWeeks().v;
				if (foodPoint < 0.75 && weeksToHarvest != 0) {
					ele.style.fill = "url(\"#starvation\")";
				} else if (foodPoint < weeksToHarvest * 0.75) {
					// First gradient: red to yellow.
					let gradientPoint = foodPoint / (weeksToHarvest * 0.75);
					ele.style.fill = "rgb(255, " + Math.floor(gradientPoint * 255) + ", 0)";
				} else if (foodPoint < weeksToHarvest) {
					// Second gradient: yellow to white.
					let gradientPoint = (foodPoint - weeksToHarvest * 0.75) / (weeksToHarvest * 0.25);
					ele.style.fill = "rgb(255, 255, " + Math.floor(gradientPoint * 255) + ")";
				} else if (foodPoint - weeksToHarvest < 4) {
					// Third gradient: white to green.
					let gradientPoint = (foodPoint - weeksToHarvest) / 4;
					ele.style.fill = "rgb(" + Math.floor((1 - gradientPoint) * 255) + ", 255, " + Math.floor((1 - gradientPoint) * 255) + ")";
				} else if (foodPoint - weeksToHarvest < 8) {
					// Third gradient: green to dark green.
					let gradientPoint = (foodPoint - weeksToHarvest - 4) / 4;
					ele.style.fill = "rgb(0, " + Math.floor((1 - gradientPoint / 2) * 255) + ", 0)";
				} else {
					// Deep green.
					ele.style.fill = "#080";
				}
			} else {
				ele.style.fill = "#fff"; // This case can only occur if there is a bug.
			}
		}
	}

	repaintRegionContents() {
		let shadow = this.shadow;
		let regionContentsEle = this.shadow.querySelector("#regionContents");
		if (this.ephemera != undefined) {
			for (let e of this.ephemera) e.parentNode.removeChild(e);
		}
		this.ephemera = [];
		let ephemera = this.ephemera;
		for (let r of this.pointCache) if (r != undefined) r.nextuse = 0;
		if (this.regionContents == undefined) {
			this.regionContents = [];
			for (let r of g_data.regions) this.regionContents.push([]);
		}
		let regionContents = this.regionContents;

		const ContentType = {
			HOLYCITY: 1,
			ARMY: 2,
			NAVY: 3,
			CHARACTER: 4,
		};

		let unownedRegionContentElements = {};
		for (let e of regionContentsEle.querySelectorAll("g")) {
			unownedRegionContentElements[e.getAttribute("id")] = true;
		}

		function hash(input) {
			return input.replace(/[_'" ]/g, "_");
		}

		function getPoint(where) {
			let p = {};
			outer: for (let t = 0; t < 30; t++) {
				let minD = 20 - t / 2;
				p = g_data.regions[where].getRandomPointInRegion(.33, regionContents[where].length == 0);
				for (let bbp of regionContents[where]) {
					if (length(minus(p, bbp)) < minD) continue outer;
				}
				break;
			}
			return p;
		}

		function addRegionContents(where, type, obj) {
			let id =
				type == ContentType.HOLYCITY ? "icon_holycity" :
				type == ContentType.ARMY ? "icon_army_" + obj.id :
				type == ContentType.CHARACTER ? "icon_character_" + hash(obj.name) :
				"_";
			let e = regionContentsEle.querySelector("#" + id);
			if (e != undefined) {
				delete unownedRegionContentElements[id];
				if (where != parseInt(e.getAttribute("data-location"))) {
					let p = getPoint(where);
					obj.mapPoint = p;
					e.setAttribute("data-location", where);
					e.setAttribute("data-map-point-x", p.x);
					e.setAttribute("data-map-point-y", p.y);
					e.setAttribute("transform", "translate(" + p.x + ", " + p.y + ")");
				} else {
					obj.mapPoint = {x: parseFloat(e.getAttribute("data-map-point-x")), y: parseFloat(e.getAttribute("data-map-point-y"))};
				}
				if (obj.hasOwnProperty("kingdom")) e.setAttribute("class", obj.kingdom.toLowerCase());
			} else {
				// Get point.
				let p = getPoint(where);
				obj.mapPoint = p;
				let g = document.createElementNS("http://www.w3.org/2000/svg", "g");
				g.setAttribute("id", id);
				g.setAttribute("data-location", where);
				g.setAttribute("data-map-point-x", p.x);
				g.setAttribute("data-map-point-y", p.y);
				g.setAttribute("transform", "translate(" + p.x + ", " + p.y + ")");
				let pe = document.createElementNS("http://www.w3.org/2000/svg", "use");
				pe.setAttribute("x", -6);
				pe.setAttribute("y", -6);
				pe.setAttribute("width", 12);
				pe.setAttribute("height", 12);
				if (type == ContentType.CHARACTER) {
					g.setAttribute("class", obj.kingdom.toLowerCase());
					pe.setAttribute("href", "#character");
					g.addEventListener('click', function() { changeReport("character/" + obj.name); });
					g.addEventListener("mouseover", () => { shadow.getElementById("mapMouseTooltip").textContent = obj.name; });
				}	else if (type == ContentType.ARMY) {
					g.setAttribute("class", obj.kingdom.toLowerCase());
					pe.setAttribute("href", obj.type == "army" ? "#army_" + obj.tags[0].toLowerCase().replace(/ .*/g, "") : "#navy");
					g.addEventListener('click', function() { changeReport("army/" + obj.id); });
					g.addEventListener("mouseover", () => { shadow.getElementById("mapMouseTooltip").textContent = (obj.type == "army" ? "Army " : "Navy ") + obj.id; });
				} else if (type == ContentType.HOLYCITY) {
					pe.setAttribute("href", "#holycity");
					g.addEventListener('click', function() { changeReport("church/main"); });
					g.addEventListener("mouseover", () => { shadow.getElementById("mapMouseTooltip").textContent = "Holy City"; });
				}
				g.appendChild(pe);
				regionContentsEle.appendChild(g);
				e = g;
			}
			let p = obj.mapPoint;
			if (type == ContentType.ARMY) {
				let power = obj.calcStrength().v;
				let lines = 12;
				for (let pp = Math.floor(power / 10 - 1); pp >= 0; pp--) {
					let r = parseInt(pp / 12) * 2.75 + 7.75;
					if (lines > 0) {
						let c2 = document.createElementNS("http://www.w3.org/2000/svg", "line");
						c2.setAttribute("x1", +r * Math.sin(pp / 6 * Math.PI));
						c2.setAttribute("y1", -r * Math.cos(pp / 6 * Math.PI));
						c2.setAttribute("x2", 0);
						c2.setAttribute("y2", 0);
						c2.setAttribute("stroke", "inherit");
						c2.setAttribute("stroke-width", "2");
						c2.setAttribute("class", obj.kingdom.toLowerCase());
						e.insertBefore(c2, e.lastChild);
						ephemera.push(c2);
						lines--;
					}
					let c = document.createElementNS("http://www.w3.org/2000/svg", "circle");
					c.setAttribute("cx", +r * Math.sin(pp / 6 * Math.PI));
					c.setAttribute("cy", -r * Math.cos(pp / 6 * Math.PI));
					c.setAttribute("r", 1);
					c.setAttribute("fill", "inherit");
					c.setAttribute("class", obj.kingdom.toLowerCase());
					e.insertBefore(c, e.lastChild);
					ephemera.push(c);
				}
			}
			regionContents[where].push(p);
		}

		for (let c of g_data.armies) addRegionContents(c.location, ContentType.ARMY, c);
		addRegionContents(g_geo.holycity, ContentType.HOLYCITY, {});
		for (let c of g_data.characters) if (c.location != -1) addRegionContents(c.location, ContentType.CHARACTER, c);

		for (let id in unownedRegionContentElements) {
			// TODO: This leaks the event listeners.
			regionContentsEle.removeChild(regionContentsEle.querySelector("#" + id));
		}
	}

	repaintMotions(plannedMotions, divisionMotions) {
		function travelCost(a, b) {
			for (let border of g_borders) {
				if ((border.a == a && border.b == b) || (border.b == a && border.a == b)) return border.weight;
			}
			return -1;
		}
		function interpolate(a, b, amount) {
			return {x: (1 - amount) * a.x + amount * b.x, y: (1 - amount) * a.y + amount * b.y};
		}
		if (this.motionMapPointCache == undefined) this.motionMapPointCache = {};
		let motionMapPointCache = this.motionMapPointCache;
		let pm = this.shadow.getElementById("plannedMotions");
		pm.innerHTML = "";
		let motions = {}; // Motions is keyed by [moverID][destinationID].
		let movers = [];
		for (let a of g_data.armies) movers.push(a);
		for (let a of g_data.characters) if (a.location != -1) movers.push(a);
		for (let a of movers) {
			let k = a.id != undefined ? "a" + a.id : a.name;
			if (motions[k] == undefined) motions[k] = {};
			let m = motions[k];
			for (let p of a.preparation) {
				if (m["r" + p.to] == undefined) {
					if (!motionMapPointCache.hasOwnProperty(k + "_r" + p.to)) motionMapPointCache[k + "_r" + p.to] = g_data.regions[p.to].getRandomPointInRegion(.75);
					m["r" + p.to] = {
						"sofar": p.amount,
						"p": motionMapPointCache[k + "_r" + p.to],
						"planned": false,
						"type": "travel"
					};
				}
			}
			if (plannedMotions != undefined && divisionMotions != undefined) {
				let subMotions = divisionMotions[k] == undefined ? [] : Array.from(divisionMotions[k]);
				if (plannedMotions[k] != undefined) subMotions.push(plannedMotions[k]);
				for (let subMotion of subMotions) {
					let mp = m["r" + subMotion];
					if (mp == undefined) {
						if (subMotion.hasOwnProperty("id")) {
							m["rmerge" + subMotion.id] = {
								"sofar": 0,
								"p": subMotion.mapPoint,
								"planned": true,
								"type": "merge"
							};
						} else {
							if (!motionMapPointCache.hasOwnProperty(k + "_r" + subMotion)) motionMapPointCache[k + "_r" + subMotion] = g_data.regions[subMotion].getRandomPointInRegion(.75);
							m["r" + subMotion] = {
								"sofar": 0,
								"p": motionMapPointCache[k + "_r" + subMotion],
								"planned": true,
								"type": "travel"
							};
						}
					} else {
						m["r" + subMotion].planned = true;
					}
				}
			}
			for (m in motions[k]) {
				if (!motions[k][m].planned && motions[k][m].sofar == 0) continue;
				if (motions[k][m].type == "merge") {
					let s = document.createElementNS("http://www.w3.org/2000/svg", "path");
					s.setAttribute("d", "M" + a.mapPoint.x + "," + a.mapPoint.y + " L" + motions[k][m].p.x + "," + motions[k][m].p.y);
					s.setAttribute("stroke", "#222");
					s.setAttribute("stroke-width", ".8");
					s.setAttribute("stroke-dasharray", "1 2");
					pm.appendChild(s);
				} else {
					let s = document.createElementNS("http://www.w3.org/2000/svg", "path");
					let midpoint = interpolate(a.mapPoint, motions[k][m].p, motions[k][m].sofar / travelCost(a.location, parseInt(m.replace("r", ""))));
					s.setAttribute("d", "M" + a.mapPoint.x + "," + a.mapPoint.y + " L" + midpoint.x + "," + midpoint.y);
					s.setAttribute("stroke", "#000");
					s.setAttribute("stroke-width", "1");
					pm.appendChild(s);
					s = document.createElementNS("http://www.w3.org/2000/svg", "path");
					s.setAttribute("d", "M" + midpoint.x + "," + midpoint.y + " L" + motions[k][m].p.x + "," + motions[k][m].p.y);
					s.setAttribute("stroke", motions[k][m].planned ? "#fa8" : "#aaa");
					s.setAttribute("stroke-width", "1");
					s.setAttribute("marker-end", "url(#arrowHead" + (motions[k][m].planned ? "Planned" : "") + ")");
					pm.appendChild(s);
				}
			}
		}
	}

	setCrosshair(href) {
		let x = this.shadow.getElementById("crosshair");
		x.setAttribute("x", -1000);
		x.setAttribute("y", -1000);
		if (href == undefined) return;
		let ele = href.split("/");
		if (ele[0] == "region") {
			let i = -1;
			for (let r = 0; r < g_data.regions.length; r++) if (g_data.regions[r].name == ele[1]) i = r;
			let p = g_data.regions[i].getRandomPointInRegion(.75);
			x.setAttribute("x", p.x-15);
			x.setAttribute("y", p.y-15);
		} else if (ele[0] == "character") {
			for (let c of g_data.characters) if (c.name == ele[1] && c.mapPoint != undefined) {
				x.setAttribute("x", c.mapPoint.x-15);
				x.setAttribute("y", c.mapPoint.y-15);
			}
		} else if (ele[0] == "army") {
			for (let c of g_data.armies) if (c.id == parseInt(ele[1]) && c.mapPoint != undefined) {
				x.setAttribute("x", c.mapPoint.x-15);
				x.setAttribute("y", c.mapPoint.y-15);
			}
		}
	}

	repaintScore() {
		let shadow = this.shadow;
		let totalScore = 0;
		let profiles = g_data.kingdoms[whoami].profiles;
		shadow.getElementById("profiles").innerHTML = "";
		for (let profile of Object.keys(g_scoreProfiles).sort((a, b) => {
			if (profiles.includes(a) && !profiles.includes(b)) return -1;
			if (profiles.includes(b) && !profiles.includes(a)) return 1;
			if (a < b) return -1;
			if (a > b) return 1;
			return 0;
		})) {
			if (!g_scoreProfiles[profile].selectable && !g_data.kingdoms[whoami].score.hasOwnProperty(profile) && !profiles.includes(profile)) continue;
			let p = document.createElement("div");
			p.appendChild(document.createTextNode(titleCase(profile)));
			if (g_data.kingdoms[whoami].score.hasOwnProperty(profile)) {
				p.appendChild(document.createTextNode(": " + Math.round(g_data.kingdoms[whoami].score[profile] * 10) / 10));
				totalScore += g_data.kingdoms[whoami].score[profile];
			}
			if (!profiles.includes(profile)) {
				if (!g_data.kingdoms[whoami].score.hasOwnProperty(profile)) continue;
				p.setAttribute("class", "inactive");
				p.appendChild(document.createTextNode(" (inactive)"));
			}
			let d = document.createElement("ul");
			for (let desc of g_scoreProfiles[profile].description) {
				let li = document.createElement("li");
				li.appendChild(document.createTextNode(desc));
				d.appendChild(li);
			}
			p.appendChild(d);
			shadow.getElementById("profiles").appendChild(p);
		}
		shadow.getElementById("scorepoints").innerHTML = "";
		shadow.getElementById("scorepoints").appendChild(document.createTextNode(Math.round(totalScore * 10) / 10));
		let percentiles = getHistoricalScore(g_data.date, whoami, totalScore);
		shadow.getElementById("score_kingdom").innerHTML = "";
		shadow.getElementById("score_kingdom").appendChild(document.createTextNode(Math.round(percentiles.nation * 100) + "%"));
		shadow.getElementById("score_culture").innerHTML = "";
		shadow.getElementById("score_culture").appendChild(document.createTextNode(Math.round(percentiles.culture * 100) + "%"));
		shadow.getElementById("score_global").innerHTML = "";
		shadow.getElementById("score_global").appendChild(document.createTextNode(Math.round(percentiles.global * 100) + "%"));
	}

	repaintTable() {
		let tableDiv = this.shadow.getElementById("tableDivTableTbody");
		tableDiv.innerHTML = "";
		let kingdoms = [];
		for (let k in g_data.kingdoms) kingdoms.push(k);
		kingdoms.sort();
		for (let k of kingdoms) {
			let kingdom = g_data.kingdoms[k];
			let tr = document.createElement("tr");
			tr.style.backgroundColor = mixColor(getColor(k), "#ffffff", 0.9);
			function addCell(e) {
				let td = document.createElement("td");
				td.appendChild(e);
				td.style.borderTop = "2px solid " + getColor(k);
				tr.appendChild(td);
				return td;
			}
			let img = document.createElement("img");
			img.setAttribute("src", "images/heraldry/" + k.toLowerCase() + ".png");
			addCell(img).style.borderLeft = "24px solid " + getColor(k);
			let c = document.createElement("span");
			c.appendChild(document.createTextNode(k));
			addCell(c);
			let ul = document.createElement("ul");
			for (let t of g_data.kingdoms[k].tags) {
				let li = document.createElement("li");
				let tt = document.createElement("tooltip-element");
				tt.setAttribute("tooltip", traitTooltips[t]);
				tt.appendChild(document.createTextNode(t));
				li.appendChild(tt);
				ul.appendChild(li);
			}
			document.createElement("li");
			addCell(ul);
			let religion = document.createElement("tooltip-element")
			let stateReligion = kingdom.calcStateReligion();
			let stateReligionShares = kingdom.calcStateReligionWeights();
			let stateReligionSharesText = [];
			for (let r in stateReligionShares) {
				if (!stateReligionShares.hasOwnProperty(r)) continue;
				stateReligionSharesText.push(r + ": " + Math.round(stateReligionShares[r]) + " souls");
			}
			religion.appendChild(document.createTextNode(stateReligion));
			religion.setAttribute(
					"tooltip",
					"State religion is the most popular religion and ideology among the regions ruled by a nation. "
					+ k + " rules: " + stateReligionSharesText.join(", ") + ". "
					+ {
							"Iruhan (Sword of Truth)": "As a consequence of their widespread devotion to the Sword of Truth ideology, the armies and navies of " + k + " are stronger.",
							"Iruhan (Vessel of Faith)": "As a consequence of their widespread devotion to the Vessel of Faith ideology, the people of " + k + " are made happy whenever any region anywhere converts to the Vessel of Faith ideology.",
							"Iruhan (Chalice of Compassion)": "As a consequence of their widespread devotion to the Chalice of Compassion ideology, the people of " + k + " are willing to subsidize the transfers of food.",
							"Iruhan (Tapestry of People)": "As a consequence of their widespread devotion to the Tapestry of Peoples ideology, the soldiers of " + k + " are stronger for every unique ideology in the nation.",
							"Northern (Alyrja)": "As a consequence of their widespread devotion to Alyrja, the people of " + k + " have eyes and ears in all ruled and adjacent regions who detect all hidden units.",
							"Northern (Rjinku)": "As a consequence of their widespread devotion to Rjinku, the people of " + k + " make excellent generals.",
							"Northern (Lyskr)": "As a consequence of their widespread devotion to Lyskr, the people of " + k + " are more skilled at plotting.",
							"Northern (Syrjen)": "As a consequence of their widespread devotion to Syrjen, the people of " + k + " are skilled traders and profit more from dominance in sea regions.",
							"Tavian (Flame of Kith)": "As a consequence of their widespread adoption of the Flame of Kith, the people of " + k + " make excellent governors.",
							"Tavian (River of Kuun)": "As a consequence of their widespread adoption of the River of Kuun, the people of " + k + " generate wealth by trade and tribute.",
							"Company": "This nation does not rule any territory, and focuses on efficiency, making their plots more effective and lowering the cost of their armies and navies.",
					}[stateReligion]);
			addCell(religion);
			let pop = kingdom.calcPopulation();
			let s = document.createElement("span");
			s.innerHTML = pop.v > 0 ? num(pop, 0, 1/1000) + "k" : "";
			addCell(s);
			s = document.createElement("span");
			s.innerHTML = pop.v > 0 ? num(kingdom.calcTaxation()) : "";
			addCell(s);
			s = document.createElement("span");
			s.innerHTML = pop.v > 0 ? num(kingdom.calcFoodWeeks(), 1) : "";
			addCell(s);
			s = document.createElement("span");
			s.innerHTML = pop.v > 0 ? num(kingdom.calcRecruitment()) : "";
			addCell(s);
			let ruler = kingdom.getRuler();
			s = document.createElement("div");
			s.style.width = "3em";
			let portrait = addCell(s)
			if (ruler.portrait == -1) ruler.portrait = parseInt(Math.random() * 2000);
			portrait.style.backgroundImage = "url(images/portraits/" + ruler.portrait + ".png)";
			portrait.style.backgroundSize = "cover";
			portrait.style.backgroundPosition = "center top";
			addCell(document.createTextNode(ruler.honorific + "\n" + ruler.name)).style.whiteSpace = "pre-line";
			tableDiv.appendChild(tr);
		}


		tableDiv = this.shadow.getElementById("tableDivTableTbodyRegions");
		tableDiv.innerHTML = "";
		let regions = [];
		let lastRuler = "";
		for (let r of g_data.regions) {
			if (r.type == "water") continue;
			let core = "ZZZ";
			for (let k in g_data.kingdoms) if (g_data.kingdoms[k].core_regions.includes(r.id)) {
				core = k;
				break;
			}
			regions.push({"r": r, "core": core});
		}
		regions.sort((a, b) => {
			let kcomp = a.r.kingdom.localeCompare(b.r.kingdom);
			if (kcomp != 0) return kcomp;
			return a.r.name.localeCompare(b.r.name);
		});
		for (let rboxed of regions) {
			let r = rboxed.r;
			let tr = document.createElement("tr");
			let border = r.kingdom != lastRuler ? "2px solid " + getColor(r.kingdom) : "";
			lastRuler = r.kingdom;
			tr.style.backgroundColor = mixColor(getColor(r.kingdom), "#ffffff", 0.9);
			function addCell(e) {
				let td = document.createElement("td");
				td.appendChild(e);
				td.style.borderTop = border;
				tr.appendChild(td);
				return td;
			}
			function addCellHTML(html) {
				let td = document.createElement("td");
				td.innerHTML = html;
				td.style.borderTop = border;
				tr.appendChild(td);
				return td;
			}
			function createTooltip(tip, contents) {
				let tt = document.createElement("tooltip-element");
				tt.setAttribute("tooltip", tip);
				tt.appendChild(document.createTextNode(contents));
				return tt;
			}
			addCell(document.createTextNode(r.name));
			let kl = document.createElement("kingdom-label");
			kl.setAttribute("kingdom", r.kingdom);
			addCell(kl);
			kl = document.createElement("kingdom-label");
			kl.setAttribute("kingdom", rboxed.core);
			addCell(kl);
			addCell(document.createTextNode(Math.round(r.population / 1000) + "k"));
			addCell(createTooltip(culture_tooltips[r.culture], r.culture));
			addCell(createTooltip(religion_tooltips[r.religion], r.religion));
			addCellHTML(num(r.calcTaxation()));
			addCellHTML(num(r.calcRecruitment()));
			addCell(document.createTextNode(Math.round(r.food / 1000) + "k"));
			addCell(document.createTextNode(Math.round(r.crops / 1000) + "k"));
			addCellHTML(num(r.calcUnrest(), 0, 100) + "%");
			tableDiv.appendChild(tr);
		}
	}

	repaintInternationalRelations() {
		let e = this.shadow.getElementById("international_relations");
		e.innerHTML = "";
		e.appendChild(document.createElement("international-report"));
	}

	showMap() {
		this.shadow.getElementById("mapDiv").style.display = "block";
		this.shadow.getElementById("mapControls").style.display = "inline-block";
		this.shadow.getElementById("tableDiv").style.display = "none";
		this.shadow.getElementById("scoreDiv").style.display = "none";
		this.shadow.getElementById("internationalRelationsDiv").style.display = "none";
	}

	showTable() {
		this.shadow.getElementById("mapDiv").style.display = "none";
		this.shadow.getElementById("mapControls").style.display = "none";
		this.shadow.getElementById("tableDiv").style.display = "block";
		this.shadow.getElementById("scoreDiv").style.display = "none";
		this.shadow.getElementById("internationalRelationsDiv").style.display = "none";
	}

	showScore() {
		this.shadow.getElementById("mapDiv").style.display = "none";
		this.shadow.getElementById("mapControls").style.display = "none";
		this.shadow.getElementById("tableDiv").style.display = "none";
		this.shadow.getElementById("scoreDiv").style.display = "block";
		this.shadow.getElementById("internationalRelationsDiv").style.display = "none";
	}

	showInternationalRelations() {
		this.shadow.getElementById("mapDiv").style.display = "none";
		this.shadow.getElementById("mapControls").style.display = "none";
		this.shadow.getElementById("tableDiv").style.display = "none";
		this.shadow.getElementById("scoreDiv").style.display = "none";
		this.shadow.getElementById("internationalRelationsDiv").style.display = "block";
	}

	popup(href) {
		if (this.totalScrollDelta > 20) {
			return;
		}
		let ele = href.split("/");
		if (ele[0] != "region" && ele[0] != "army" && ele[0] != "character") return;
		let popup = this.shadow.getElementById("popup");
		let mapDiv = this.shadow.getElementById("mapDiv");
		popup.style.display = "block";
		let popupContents = this.shadow.getElementById("popupContents");
		popupContents.textContent = "";
		let ap = undefined;
		if (ele[0] == "region") {
			ap = document.createElement("region-report");
			ap.setAttribute("region", ele[1]);
		} else if (ele[0] == "army") {
			ap = document.createElement("army-report");
			ap.setAttribute("army", ele[1]);
		} else if (ele[0] == "character") {
			ap = document.createElement("character-report");
			ap.setAttribute("character", ele[1]);
			popup.style.minWidth = "33em";
		}
		popupContents.appendChild(ap);
		popup.style.left = mapDiv.getBoundingClientRect().width * 0.5 - popup.getBoundingClientRect().width * 0.5 + "px";
		popup.style.top = mapDiv.getBoundingClientRect().height * 0.5 - popup.getBoundingClientRect().height * 0.5 + "px";
	}
}
customElements.define("map-panel", MapPanel);
