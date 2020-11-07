// <orders-panel>
class OrdersPanel extends HTMLElement {
	constructor() {
		super();

		this.divisions = 0;
		this.economyRowCount = 0;
		this.bribeRowCount = 0;
		this.giftRowCount = 0;
		this.letterCount = 0;
		this.plotCount = 0;
		this.plots = [];
		this.currentlySubmitting = false;
		this.submitQueued = false;
		this.syncDisabled = true;
		this.lastSync = "";

		let shadow = this.attachShadow({mode: "open"});
		this.shadow = shadow;
		shadow.innerHTML = `
			<style>
				:host {
					display: flex;
					flex-direction: column;
					background-color: #eee;
					font-family: sans-serif;
				}
				#tabs {
					display: flex;
				}
				#tabs label {
					flex: 1;
					cursor: pointer;
					transition: background-color 0.2s;
				}
				#tabs input {
					display: none;
				}
				#tabs svg {
					width: 2em;
					height: 2em;
				}
				#tabs > svg {
					border-bottom: 1px solid #666;
				}
				#tabs label svg {
					margin: auto;
					display: block;
				}
				#tabs label path {
					fill: #333;
					transition: fill 0.2s;
				}
				#tabs :checked + label path {
					fill: #fff;
				}
				#tabs :checked + label {
					border-bottom: 1px solid #aaa;
					background-color: #555;
				}
				#tabs label {
					border-bottom: 1px solid #666;
				}
				#tabpanel {
					height: calc(100% - 2em - 1px);
					padding-top: 1em;
					padding-left: 5px;
					padding-right: 5px;
					padding-bottom: 5px;
				}

				/* All tabs */
				table, input, select, button {
					font-size: inherit;
				}
				.alert::before {
					content: url('data:image/svg+xml; utf8, <svg viewBox="2 2 20 20" xmlns="http://www.w3.org/2000/svg"><path fill="red" d="M13,13H11V7H13M13,17H11V15H13M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2Z" /></svg>');
					width: 0.8em;
					height: 0.8em;
					margin-right: 0.2em;
					display: inline-block;
				}

				/* Home Tab */
				h1 {
					margin-top: 0;
					font-size: 150%;
					display: flex;
					align-items: center;
				}
				h1 img {
					height: 2em;
					filter: grayscale(100%) drop-shadow(0 0 5px #fff);
					margin-right: 0.2em;
				}
				#t_food label {
					width: 100%;
					display: flex;
					justify-content: space-between;
				}


				/* UNSORTED */
				form {
					margin: 0;
					height: calc(100% - 2em - 1px);
				}
				#tabpanel > div {
					height: 100%;
					overflow: auto;
				}
				input[type=text], input[type=number] {
					width: 5em;
				}
				#content_economy label {
					width: 100%;
				}
				input[type=range] {
					min-width: 60%;
				}
				#plot_newplot, #economy_newtransfer, #nations_newletter, #economy_newbribe, #nations_newgift {
					cursor: pointer;
					text-align: center;
					color: #00f;
				}
				#plot_rings label {
					display: block;
				}
				#plot_leverage kingdom-label {
					font-size: 60%;
				}
				table {
					width: 100%;
					margin-bottom: 1em;
				}
				table tr td:last-child {
					text-align: right;
				}
				#t_food_regional tr :nth-child(n + 2) {
					text-align: right;
				}
				#t_food_regional {
					margin-bottom: 1em;
				}
				#t_food_transferheader {
					display: none;
				}
				#doctrine_switches ul {
					margin-top: 0;
					font-size: 90%;
				}
				#nations_letters > div {
					margin-bottom: 0.5em;
					padding-bottom: 0.5em;
					border-bottom: 2px solid black;
				}
				#nations_letters > div > div:first-child {
					display: flex;
					flex-direction: row;
					flex-wrap: wrap;
					justify-content: space-between;
				}
				#nations_letters label, #plot_newplots label {
					background-color: #fff;
					border-radius: .5em;
					margin-left: .3em;
					margin-right: .3em;
					padding-left: .3em;
					padding-right: .3em;
				}
				#nations_letters label input[type=checkbox]:checked ~ span {
					text-decoration: underline;
				}
				#plot_newplots > div {
					margin-bottom: 1em;
					border-bottom: 1px solid #444;
				}
				#plot_newplots label select, #plot_newplots label input {
					margin-left: 1em;
				}
				#plot_newplots label {
					display: block;
				}
				textarea {
					width: 100%;
					height: 12em;
					resize: vertical;
				}
				button {
					width: 100%;
				}
				textarea:nth-child(2) {
					width: 100%;
					height: 2em;
					resize: vertical;
				}
				.warning {
					font-family: sans-serif;
					font-size: 80%;
					color: #830;
				}
				#nations_cede label {
					display: block;
				}
				#new_letters > div, #old_letters > div {
					background-color: #fff;
					border-radius: 0.5em;
					margin-bottom: 0.5em;
				}
				#plot_rings tr td:last-child {
					text-align: right;
				}
				.striped {
					display: grid;
					grid-template-columns: auto auto;
					margin-left: 0.7em;
					font-size: 90%;
				}
				.striped > :nth-child(4n + 3), .striped > :nth-child(4n + 4) {
					background: #ddd;
				}
				#economy_consequences {
					display: flex;
					flex-direction: row;
					justify-content: space-between;
					margin-bottom: 1em;
					margin-top: 1em;
				}
				#economy_consequences table {
					width: 40%;
				}
				#economy_consequences_budget td {
					text-align: right;
				}
				#economy_consequences_items td {
					text-align: left;
				}
				.negative {
					font-weight: bold;
					color: #a00;
				}
				#t_plots table {
					border-collapse: collapse;
				}
				#plot_leverage td {
					padding: 0.2em;
				}
				#plot_leverage td:nth-child(n + 1), #t_plots th:nth-child(n + 1) {
					text-align: right;
				}
				#plot_leverage > :nth-child(2n) {
					background: #ddd;
				}
				#t_gothi .voting {
					font-weight: bold;
					text-decoration: underline;
					color: #800;
				}
				#t_gothi td:nth-child(n + 2) {
					text-align: center;
				}
				#t_church .striped > :nth-child(2n) {
					text-align: right;
				}
				h2 {
					font-size: 100%;
					margin-top: 1em;
					margin-bottom: 0;
					font-weight: bold;
				}
				.highlight {
					background-color: #ff8;
					animation-duration: 0.3s;
					animation-iteration-count: 3;
					animation-name: highlightflash;
				}
				@keyframes highlightflash {
					from { background-color: #fff; }
					50% { background-color: #ff0; }
					to { background-color: #fff; }
				}
				#t_letters kingdom-label, #nations_nations_tbody kingdom-label {
					font-size: 50%;
					vertical-align: bottom;
					display: inline-block;
				}
				#tutorialLauncher {
					cursor: pointer;
					color: #00f;
				}
			</style>
			<div id="tabs">
				<input type="radio" name="selected_tab" id="r_home" checked/><label for="r_home"><svg viewBox="0 0 24 24"><path d="M10,20V14H14V20H19V12H22L12,3L2,12H5V20H10Z" /></label>
				<input type="radio" name="selected_tab" id="r_units"/><label for="r_units"><svg viewBox="0 0 24 24"><path d="M12,1L3,5V11C3,16.55 6.84,21.74 12,23C17.16,21.74 21,16.55 21,11V5L12,1Z" /></label>
				<input type="radio" name="selected_tab" id="r_letters"/><label for="r_letters"><svg viewBox="0 0 24 24"><path d="M15.54,3.5L20.5,8.47L19.07,9.88L14.12,4.93L15.54,3.5M3.5,19.78L10,13.31C9.9,13 9.97,12.61 10.23,12.35C10.62,11.96 11.26,11.96 11.65,12.35C12.04,12.75 12.04,13.38 11.65,13.77C11.39,14.03 11,14.1 10.69,14L4.22,20.5L14.83,16.95L18.36,10.59L13.42,5.64L7.05,9.17L3.5,19.78Z" /></label>
				<input type="radio" name="selected_tab" id="r_plots"/><label for="r_plots"><svg viewBox="0 0 24 24"><path d="M11.83,9L15,12.16C15,12.11 15,12.05 15,12A3,3 0 0,0 12,9C11.94,9 11.89,9 11.83,9M7.53,9.8L9.08,11.35C9.03,11.56 9,11.77 9,12A3,3 0 0,0 12,15C12.22,15 12.44,14.97 12.65,14.92L14.2,16.47C13.53,16.8 12.79,17 12,17A5,5 0 0,1 7,12C7,11.21 7.2,10.47 7.53,9.8M2,4.27L4.28,6.55L4.73,7C3.08,8.3 1.78,10 1,12C2.73,16.39 7,19.5 12,19.5C13.55,19.5 15.03,19.2 16.38,18.66L16.81,19.08L19.73,22L21,20.73L3.27,3M12,7A5,5 0 0,1 17,12C17,12.64 16.87,13.26 16.64,13.82L19.57,16.75C21.07,15.5 22.27,13.86 23,12C21.27,7.61 17,4.5 12,4.5C10.6,4.5 9.26,4.75 8,5.2L10.17,7.35C10.74,7.13 11.35,7 12,7Z" /></label>
				<input type="radio" name="selected_tab" id="r_food"/><label for="r_food"><svg viewBox="0 0 24 24"><path d="M7.33,18.33C6.5,17.17 6.5,15.83 6.5,14.5C8.17,15.5 9.83,16.5 10.67,17.67L11,18.23V15.95C9.5,15.05 8.08,14.13 7.33,13.08C6.5,11.92 6.5,10.58 6.5,9.25C8.17,10.25 9.83,11.25 10.67,12.42L11,13V10.7C9.5,9.8 8.08,8.88 7.33,7.83C6.5,6.67 6.5,5.33 6.5,4C8.17,5 9.83,6 10.67,7.17C10.77,7.31 10.86,7.46 10.94,7.62C10.77,7 10.66,6.42 10.65,5.82C10.64,4.31 11.3,2.76 11.96,1.21C12.65,2.69 13.34,4.18 13.35,5.69C13.36,6.32 13.25,6.96 13.07,7.59C13.15,7.45 13.23,7.31 13.33,7.17C14.17,6 15.83,5 17.5,4C17.5,5.33 17.5,6.67 16.67,7.83C15.92,8.88 14.5,9.8 13,10.7V13L13.33,12.42C14.17,11.25 15.83,10.25 17.5,9.25C17.5,10.58 17.5,11.92 16.67,13.08C15.92,14.13 14.5,15.05 13,15.95V18.23L13.33,17.67C14.17,16.5 15.83,15.5 17.5,14.5C17.5,15.83 17.5,17.17 16.67,18.33C15.92,19.38 14.5,20.3 13,21.2V23H11V21.2C9.5,20.3 8.08,19.38 7.33,18.33Z"/></label>
				<input type="radio" name="selected_tab" id="r_kingdoms"/><label for="r_kingdoms"><svg viewBox="0 0 24 24"><path d="M12,5.5A3.5,3.5 0 0,1 15.5,9A3.5,3.5 0 0,1 12,12.5A3.5,3.5 0 0,1 8.5,9A3.5,3.5 0 0,1 12,5.5M5,8C5.56,8 6.08,8.15 6.53,8.42C6.38,9.85 6.8,11.27 7.66,12.38C7.16,13.34 6.16,14 5,14A3,3 0 0,1 2,11A3,3 0 0,1 5,8M19,8A3,3 0 0,1 22,11A3,3 0 0,1 19,14C17.84,14 16.84,13.34 16.34,12.38C17.2,11.27 17.62,9.85 17.47,8.42C17.92,8.15 18.44,8 19,8M5.5,18.25C5.5,16.18 8.41,14.5 12,14.5C15.59,14.5 18.5,16.18 18.5,18.25V20H5.5V18.25M0,20V18.5C0,17.11 1.89,15.94 4.45,15.6C3.86,16.28 3.5,17.22 3.5,18.25V20H0M24,20H20.5V18.25C20.5,17.22 20.14,16.28 19.55,15.6C22.11,15.94 24,17.11 24,18.5V20Z" /></label>
				<input type="radio" name="selected_tab" id="r_pirates"/><label for="r_pirates"><svg viewBox="0 0 24 24"><path d="M8.2,12.1C7.9,12.3 7.7,12.7 7.8,13C7.8,13.7 8.5,14.2 9.1,14.2C9.7,14.2 10.3,13.7 10.3,13C9.7,12.6 9,12.3 8.2,12.1M22,11L23,7C23,7 21,7 18,5C15,3 15,1 12,1C9,1 9,3 6,5C3,7 1,7 1,7L2,11H2.1C2,11.3 2,11.7 2,12C2,15.5 3.8,18.6 6.5,20.4L6,21.3C12.4,25.4 18,21.3 18,21.3L17.5,20.4C20.2,18.6 22,15.5 22,12C22,11.7 22,11.3 22,11M11.3,4.5L9.9,3.1L10.6,2.4L12,3.8L13.4,2.4L14.1,3.1L12.7,4.5L14.1,5.9L13.4,6.6L12,5.2L10.6,6.6L9.9,5.9L11.3,4.5M9.3,8.5C10.3,8.2 11.3,8 12,8C14.2,8 17.9,9.6 19.8,10.4C19.9,10.7 19.9,11 19.9,11.3L9.3,8.5M13.6,19.1C12.9,19.5 12.2,19.8 11.4,19.9C10.9,19.5 10.9,18.7 11.4,18.3C11.8,17.9 12.7,17.7 13.2,18.2C13.5,18.3 13.6,18.8 13.6,19.1M20,13.4C19.5,15.5 18.2,17.4 16.5,18.6L15,16H9L7.5,18.6C5.4,17.2 4,14.8 4,12C4,11.5 4.1,11 4.2,10.5C4.7,10.3 5.3,10 6,9.7L13.1,11.6V14C13.1,14.5 13.5,15 14.1,15H16.1C16.6,15 17.1,14.6 17.1,14V12.6L20,13.4Z" /></label>
				<input type="radio" name="selected_tab" id="r_gothi"/><label for="r_gothi"><svg viewBox="0 0 24 24"><path d="M15,6.79C16.86,7.86 18,9.85 18,12C18,22 6,22 6,22C7.25,21.06 8.38,19.95 9.34,18.71C9.38,18.66 9.41,18.61 9.44,18.55C9.69,18.06 9.5,17.46 9,17.21C7.14,16.14 6,14.15 6,12C6,2 18,2 18,2C16.75,2.94 15.62,4.05 14.66,5.29C14.62,5.34 14.59,5.39 14.56,5.45C14.31,5.94 14.5,6.54 15,6.79M12,14A2,2 0 0,0 14,12A2,2 0 0,0 12,10A2,2 0 0,0 10,12A2,2 0 0,0 12,14Z" /></label>
				<input type="radio" name="selected_tab" id="r_church"/><label for="r_church"><svg viewBox="0 0 24 24"><path d="M11,2H13V4H15V6H13V9.4L22,13V15L20,14.2V22H14V17A2,2 0 0,0 12,15A2,2 0 0,0 10,17V22H4V14.2L2,15V13L11,9.4V6H9V4H11V2M6,20H8V15L7,14L6,15V20M16,20H18V15L17,14L16,15V20Z" /></label>
				<input type="radio" name="selected_tab" id="r_settings"/><label for="r_settings"><svg viewBox="0 0 24 24"><path d="M12,15.5A3.5,3.5 0 0,1 8.5,12A3.5,3.5 0 0,1 12,8.5A3.5,3.5 0 0,1 15.5,12A3.5,3.5 0 0,1 12,15.5M19.43,12.97C19.47,12.65 19.5,12.33 19.5,12C19.5,11.67 19.47,11.34 19.43,11L21.54,9.37C21.73,9.22 21.78,8.95 21.66,8.73L19.66,5.27C19.54,5.05 19.27,4.96 19.05,5.05L16.56,6.05C16.04,5.66 15.5,5.32 14.87,5.07L14.5,2.42C14.46,2.18 14.25,2 14,2H10C9.75,2 9.54,2.18 9.5,2.42L9.13,5.07C8.5,5.32 7.96,5.66 7.44,6.05L4.95,5.05C4.73,4.96 4.46,5.05 4.34,5.27L2.34,8.73C2.21,8.95 2.27,9.22 2.46,9.37L4.57,11C4.53,11.34 4.5,11.67 4.5,12C4.5,12.33 4.53,12.65 4.57,12.97L2.46,14.63C2.27,14.78 2.21,15.05 2.34,15.27L4.34,18.73C4.46,18.95 4.73,19.03 4.95,18.95L7.44,17.94C7.96,18.34 8.5,18.68 9.13,18.93L9.5,21.58C9.54,21.82 9.75,22 10,22H14C14.25,22 14.46,21.82 14.5,21.58L14.87,18.93C15.5,18.67 16.04,18.34 16.56,17.94L19.05,18.95C19.27,19.03 19.54,18.95 19.66,18.73L21.66,15.27C21.78,15.05 21.73,14.78 21.54,14.63L19.43,12.97Z" /></label>
			</div>
			<form>
				<div id="tabpanel">
					<div id="t_home">
						<h1 id="whoami"></h1>
						<div id="home_notifications"></div>
					</div>
					<div id="t_units">
						<table>
							<tbody id="units">
								<tr><th>Who</th><th></th><th>Action</th></tr>
								<tr id="table_nobles"><th colspan="2">Nobles</th></tr>
								<tr id="table_armies"><th colspan="2">Armies</th></tr>
								<tr id="table_navies"><th colspan="2">Navies</th></tr>
							</tbody>
						</table>
					</div>
					<div id="t_letters">
						<div id="nations_letters"></div>
						<div id="nations_newletter">Write Letter</div>
						<hr/>
						<div id="new_letters"></div>
						<zip-div id="old_letters" title="Older Letters"></zip-div>
					</div>
					<div id="t_plots">
						<div id="plot_newplots"></div>
						<div id="plot_newplot">Instigate New Plot</div>
						<table>
							<tr>
								<th></th>
								<th>Leverage</th>
								<th>Spending</th>
							</tr>
							<tbody id="plot_leverage"></tbody>
						</table>
					</div>
					<div id="t_food">
						<label>Taxation: <input id="economy_tax" name="economy_tax" type="range" min="0" max="200" step="25" value="100"/></label>
						<label>Shipbuilding: <input id="economy_ship" name="economy_ship" type="range" min="0" max="5" step="1" value="5"/></label>
						<label>Soldier Bonus Pay: <input id="economy_recruit_bonus" name="economy_recruit_bonus" type="range" min="-2" max="16" step="1" value="0"/></label>
						<label>Target Rations: <input id="economy_ration" name="economy_ration" type="range" min="0" max="200" step="1" value="100"/></label>
						<div id="economy_consequences">
							<table>
								<tbody id="economy_consequences_items">
								</tbody>
							</table>
							<table>
								<tr><th>Gold Source</th><th>+/-</th></th><th>Amount</th></tr>
								<tbody id="economy_consequences_budget">
								</tbody>
							</table>
						</div>
						<table id="t_food_regional">
							<tr><th>Region</th><th>Food</th><th>Rations</th><th><tooltip-element tooltip="Projected unrest as a result of these changes.">Unrest</tooltip-element></th><th>Death</th><th><tooltip-element tooltip="The duration until this region runs totally out of food, projecting based on current food and crops and upcoming harvests / planting.">Empty</tooltip-element></th></tr>
							<tbody id="food_regions">
							</tbody>
						</table>
						<table>
							<tr id="t_food_transferheader"><th>From</th><th>To</th><th>Amount</th><th>Cost (Gold)</th></tr>
							<tbody id="economy_transfers">
							</tbody>
							<tr><td colspan="4" id="economy_newtransfer">Add a Food Transfer</td></tr>
						</table>
						<expandable-snippet text="Food transfers are evaluated in order from top to bottom, after taxation income but before armies and civilians eat. If there are insufficient funds for a transfer, as much food as possible will be transferred with the funds available."></expandable-snippet>
					</div>
					<div id="t_kingdoms">
						<table>
							<tr><th rowspan="2"></th><th rowspan="2"><tooltip-element tooltip="Initiate battles with them under certain conditions. Of course, regardless of our position, they may still attack us.">Attack Them</tooltip-element></th><th rowspan="2"><tooltip-element tooltip="Pay a percentage of our income to them.">Tribute</tooltip-element></th><th colspan="4">Accept</th></tr>
							<tr><th><tooltip-element tooltip="If yes, accept regions they cede to us.">Land</tooltip-element></th><th><tooltip-element tooltip="If yes, accept population voluntarily emigrating from their regions into ours.">Refugees</tooltip-element></th><th><tooltip-element tooltip="If yes, allow their character to construct in our regions.">Building</tooltip-element></th><th><tooltip-element tooltip="If yes, accept armies and navies they transfer to us.">Armies</tooltip-element></th></tr>
							<tbody id="nations_nations_tbody">
							</tbody>
						</table>
						<table>
							<tr><th>Amount (Gold)</th><th>To</th></tr>
							<tbody id="nations_gifts">
							</tbody>
							<tr><td colspan="2" id="nations_newgift">Send Gold</td></tr>
						</table>
						<zip-div id="nations_cede" title="Cede Regions"></zip-div>
						<div id="t_kingdoms_warnings" class="warning"></div>
					</div>
					<div id="t_pirates">
						<div>
							<div>Pirate Threat: +<span id="pirate_threat"></span> pirates</div>
							<div id="threatshares" class="striped">
							</div>
							<div>Current Bribes:</div>
							<div id="bribes" class="striped">
							</div>
						</div>
						<table>
							<tr><th>Amount (Gold)</th><th>To</th><th>Who</th></tr>
							<tbody id="economy_bribes">
							</tbody>
							<tr><td colspan="3" id="economy_newbribe">Bribe Pirates</td></tr>
						</table>
					</div>
					<div id="t_gothi">
						<table id="votes">
							<tr>
								<th></th>
								<th><tooltip-element tooltip="The warwinds stop sea trade, destroy 25% of any army or navy at sea, and blow vessels in sea regions to random adjacent regions. They will start to destroy crops worldwide after 2 weeks of activity.">Warwinds (Alyrja)</tooltip-element></th>
								<th><tooltip-element tooltip="The veil makes all armies, navies, and characters hidden from other rulers, and plots become more successful. It will start to destroy crops worldwide after 2 weeks of activity.">Veil (Lyskr)</tooltip-element></th>
								<th><tooltip-element tooltip="Each construction has a 33% chance of being destroyed each week the quake is active. It will start to destroy crops worldwide after 2 weeks of activity.">Quake (Rjinku)</tooltip-element></th>
								<th><tooltip-element tooltip="The deluge destroys 25% of any army or navy travelling into a land region, allows navies to traverse land regions and participate in battles there, and prevents navies from being captured. It will start to destroy crops worldwide after 2 weeks of activity.">Deluge (Syrjen)</tooltip-element></th>
							</tr>
						</table>
						<div id="cult_join">
							<label><input type="checkbox" name="plot_cult"/>Swear loyalty to the Cult for +<span id="cult_profit"></span> soldiers</label>
							<expandable-snippet text="In exchange for loyalty, the Cult will give us undead soldiers. The Cult will gain access to any regions we control, and you should continue to expand their influence by annexing additional territory."></expandable-snippet>
						</div>
					</div>
					<div id="t_church">
						<h2>Opinions</h2>
						<div id="opinions" class="striped">
						</div>
						<h2>Dominant Ideology: <span id="church_dominant"></span></h2>
						<div id="ideologies" class="striped"></div>
						<h2 id="doctrine_header">Church Doctrine</h2>
						<div id="doctrine_switches"></div>
					</div>
					<div id="t_settings">
						<h2 id="tutorialLauncher">Launch Tutorial</h2>
						<h2><a href="https://docs.google.com/document/d/1Pa8rcAGnEBV_9BGtqNfXhAhXmQNoAUFAcTV80WoY6Nc/edit?usp=sharing" target="_blank">Rules</a></h2>
						<h2>Timeline</h2>
						<input id="timeline" type="range" min="1" step="1"/>
						<div id="weekdisplay"></div>
						<div id="timeleft"></div>
						<div id="end_vote_div">
							<h2 class="alert">Game Extension Vote</h2>
							<select name="end_vote">
								<option value="end">End the game</option>
								<option value="extend">Extend the game by 6 turns</option>
							</select>
						</div>
						<h2>Settings</h2>
						<label><input type="checkbox" name="send_email"></input>Email me on each turn.</label>
						<h2>Let's Play Notes</h2>
						<expandable-snippet text="After the game is over, the app will publish these notes to all players for inclusion in a Let's Play style blog. Feel free to record your thoughts, plans, and reactions, to add perspectives to that record."></expandable-snippet>
						<textarea name="letsplay_notes"></textarea>
					</div>
				</div>
			</form>
		`;

		let op = this;
		for (let t of shadow.querySelectorAll("#tabs input")) t.addEventListener("click", () => op.changeTab());
		this.changeTab();
	}

	changeTab(to) {
		if (to != undefined) {
			this.shadow.querySelector("#r_" + to).checked = true;
		}
		for (let t of this.shadow.querySelectorAll("#tabs input")) this.shadow.getElementById("t_" + t.id.substring(2, t.id.length)).style.display = t.checked ? "block" : "none";
	}


	loadData() {
		let op = this;
		let postLoadOps = [];
		let shadow = this.shadow;
		let form = shadow.querySelector("form");
		op.syncDisabled = true;

		// Utils
		let computeEconomyConsequences = function () {
			if (op.syncDisabled) return;
			let eTax = shadow.getElementById("economy_tax");
			let eShip = shadow.getElementById("economy_ship");
			let eRation = shadow.getElementById("economy_ration");
			let eBonus = shadow.getElementById("economy_recruit_bonus");
			let taxRate = parseInt(eTax.value) / 100.0;
			let recruitRate = 0;
			if (parseInt(eBonus.value) == -1) recruitRate = -.5;
			else if (parseInt(eBonus.value) == -2) recruitRate = -1;
			else if (parseInt(eBonus.value) > 0) recruitRate = Math.log2(parseInt(eBonus.value)) * .5 + .5;
			let rationing = parseInt(eRation.value) / 100.0;
			let happiness = Region.getUnrestMod(parseInt(eTax.value), parseInt(eRation.value), whoami);
			let taxParts = [];
			let recruitParts = [];
			for (let rid = 0; rid < g_data.regions.length; rid++) {
				let r = g_data.regions[rid];
				if (r.kingdom == whoami) {
					taxParts.push(r.calcTaxation(taxRate, rationing));
					recruitParts.push(r.calcRecruitment(recruitRate, rationing));
				}
			}
			let taxation = new Calc("+", taxParts).v;
			let recruitment = new Calc("+", recruitParts).v;
			let soldiers = 0;
			let consumptionRate = parseInt(eRation.value) - 100;
			let shipyardCount = 0;
			for (let region of g_data.regions) if (region.kingdom == whoami) shipyardCount += region.calcShipbuilding().v;
			let predictCosts = op.predictBudget(shadow);
			let armyCost = predictCosts.salary;
			for (let army of g_data.armies) if (army.kingdom == whoami) {
				if (!contains(army.tags, "Higher Power")) soldiers += army.size;
			}

			let items = [];
			if (shipyardCount > 0) items.push("+" + Math.round(shipyardCount * (parseInt(eShip.value) / 5.0)) + " new warships");
			items.push("+" + Math.round(recruitment) + " soldiers");

			let itemsEle = shadow.getElementById("economy_consequences_items");
			itemsEle.innerHTML = "";
			for (let item of items) {
				let tr = document.createElement("tr");
				let td = document.createElement("td");
				td.appendChild(document.createTextNode(item));
				tr.appendChild(td);
				itemsEle.appendChild(tr);
			}

			let budgetEle = shadow.getElementById("economy_consequences_budget");
			budgetEle.innerHTML = "";
			let goldTotal = g_data.kingdoms[whoami].gold;
			{ // Treasured Row
				let tr = document.createElement("tr");
				{
					let td = document.createElement("td");
					td.appendChild(document.createTextNode("Treasury"));
					tr.appendChild(td);
				}
				tr.appendChild(document.createElement("td"));
				{
					let td = document.createElement("td");
					td.appendChild(document.createTextNode(Math.round(goldTotal)));
					tr.appendChild(td);
				}
				budgetEle.appendChild(tr);
			}
			let addRow = function(source, amount) {
				if (amount == 0) return;
				let tr = document.createElement("tr");
				{
					let td = document.createElement("td");
					td.appendChild(document.createTextNode(source));
					tr.appendChild(td);
				}
				{
					let td = document.createElement("td");
					let amt = Math.round(amount);
					td.appendChild(document.createTextNode(amt > 0 ? "+" + amt : amt));
					if (goldTotal + amount < 0) td.className = "negative";
					tr.appendChild(td);
				}
				{
					let td = document.createElement("td");
					goldTotal = Math.max(0, goldTotal + amount);
					td.appendChild(document.createTextNode(Math.round(goldTotal)));
					tr.appendChild(td);
				}
				budgetEle.appendChild(tr);
			}
			addRow("Actions", -predictCosts.actions);
			addRow("Army/Navy Pay", -(Math.max(0, parseInt(eBonus.value) * (recruitment + soldiers) / 100) + armyCost));
			addRow("Tax Income", taxation);
			if (shipyardCount > 0) addRow("Shipyards", (shipyardCount * (5 - parseInt(eShip.value)) / 5.0));
			{ // Mystery Row
				let tr = document.createElement("tr");
				{
					let td = document.createElement("td");
					let tt = document.createElement("tooltip-element");
					tt.setAttribute("tooltip", "Other sources of gold are unpredictable and include plots, gold gifts, church donations, sea trade, and tribute.");
					tt.appendChild(document.createTextNode("Other"));
					td.appendChild(tt);
					tr.appendChild(td);
				}
				{
					let td = document.createElement("td");
					td.appendChild(document.createTextNode("+/- ?"));
					tr.appendChild(td);
				}
				{
					let td = document.createElement("td");
					tr.appendChild(td);
				}
				budgetEle.appendChild(tr);
			}

			let expectedFood = {};
			for (let rid = 0; rid < g_data.regions.length; rid++) {
				let r = g_data.regions[rid];
				if (r.kingdom == whoami) {
					let food = r.food;
					if (g_data.date % 4 == 0) food += r.calcHarvest().v;
					for (let ti = 0; ti < op.economyRowCount; ti++) {
						if (shadow.querySelector("[name=economy_from_" + ti + "]").value == r.name) food -= parseInt(shadow.querySelector("[name=economy_amount_" + ti + "]").value) * 1000;
						if (shadow.querySelector("[name=economy_to_" + ti + "]").value.replace(/\(.*\) /, "") == r.name) food += parseInt(shadow.querySelector("[name=economy_amount_" + ti + "]").value) * 1000;
					}
					let actualRations = Math.min(food / r.population, rationing);

					shadow.getElementById("food_amt_r" + r.id).textContent = Math.floor(food / 1000) + "k";

					let unrest = Math.min(100, Math.max(0, r.unrest_popular * 100 + Math.round(Region.getUnrestMod(parseInt(eTax.value), actualRations * 100, whoami))));
					if (r.noble.unrest != undefined && unrest < r.noble.unrest * 100) unrest = r.noble.unrest * 100;
					if (r.religion == "Iruhan (Sword of Truth)" || r.religion == "Iruhan (Chalice of Compassion)" || r.religion == "Iruhan (Tapestry of People)") {
						let wrath = 100 * Math.min(1, Math.max(0, -getNation(whoami).goodwill / 100.0));
						if (unrest < wrath) unrest = wrath;
					}
					shadow.getElementById("food_unrest_r" + r.id).textContent = Math.round(unrest) + "%";
					shadow.getElementById("food_death_r" + r.id).textContent = Math.max(0, Math.ceil((0.75 - actualRations) * 0.1 * 100)) + "%";

					shadow.getElementById("food_rations_r" + r.id).textContent = Math.floor(actualRations * 100) + "%";

					let weeks = 0;
					let foodProjection = food;
					let cropsProjection = r.crops;
					let harvestCap = r.calcHarvestCapacity().v;
					let consumption = r.calcConsumption(parseInt(eRation.value)).v;
					while (weeks < 48) {
						if (((g_data.date + weeks) % 4) == 0) {
							// The current harvest is acounted for above.
							if (weeks != 0) foodProjection += Math.min(cropsProjection, harvestCap);
							cropsProjection = r.calcPlanting(g_data.date + weeks).v;
						}
						foodProjection -= consumption;
						if (foodProjection <= 0) break;
						weeks++;
					}
					shadow.getElementById("food_starv_r" + r.id).textContent = weeks == 48 ? "> 1 year" : (weeks + " weeks");
				}
			}
		};

		{ // Home Tab
			let h1 = shadow.querySelector("#whoami");
			let kl = document.createElement("kingdom-label");
			kl.setAttribute("kingdom", whoami);
			h1.appendChild(kl);
			let notifications = this.shadow.querySelector("#home_notifications");
			for (let n of g_data.notifications) {
				let h2 = document.createElement("h2");
				h2.appendChild(document.createTextNode(n.title));
				notifications.appendChild(h2);
				let ex = document.createElement("expandable-snippet");
				ex.setAttribute("text", n.text);
				ex.setAttribute("max-length", 128);
				notifications.appendChild(ex);
			}
		}

		{ // Units Tab
			let getCharacterOptions = function(unit) {
				let opts = [];
				let r = g_data.regions[unit.location];
				opts.push("Stay in " + r.name);
				if (contains(unit.tags, "Cardinal") && g_data.regions[unit.location].name == "Sancta Civitate") opts.push("Inspire the Faithful");
				if (r.kingdom == unit.kingdom) {
					if (r.noble.name == undefined && !g_data.kingdoms[unit.kingdom].tags.includes("Republican")) {
						opts.push("Instate Noble");
					}
				}
				if (r.type == "land") {
					if (g_data.spy_rings.find(ring => ring.nation == unit.kingdom && ring.location == r.id) == undefined) {
						opts.push("Establish Spy Ring");
					}
					if (r.isCoastal()) opts.push("Build Shipyard");
					opts.push("Build Fortifications");
					for (let i of ["Chalice of Compassion", "Sword of Truth", "Tapestry of People", "Vessel of Faith"]) opts.push("Build Temple (" + i + ")");
					for (let i of ["Alyrja", "Lyskr", "Rjinku", "Syrjen"]) opts.push("Build Temple (" + i + ")");
					for (let i of ["Flame of Kith", "River of Kuun"]) opts.push("Build Temple (" + i + ")");
				}
				for (let a of g_data.armies) if (a.kingdom == unit.kingdom && a.location == unit.location) {
					opts.push("Lead " + a.type + " " + a.id);
				}
				for (let neighbor of r.getNeighbors()) opts.push("Travel to " + neighbor.name);
				opts.push("Hide in " + r.name);
				for (let neighbor of r.getNeighbors()) opts.push("Hide in " + neighbor.name);
				if (!contains(unit.tags, "Ruler")) {
					for (let k in g_data.kingdoms) {
						if (!g_data.kingdoms.hasOwnProperty(k) || k == unit.kingdom) continue;
						opts.push("Transfer character to " + k);
					}
				}
				return opts;
			};

			let addRow = function(ele, link, div, selec, before = undefined) {
				let r = document.createElement("tr");
				let t = document.createElement("td");
				t.appendChild(link);
				r.appendChild(t);
				t = document.createElement("td");
				if (div != undefined) t.appendChild(div);
				r.appendChild(t);
				t = document.createElement("td");
				t.appendChild(selec);
				r.appendChild(t);
				if (before == undefined) {
					ele.appendChild(r);
				} else {
					ele.insertBefore(r, before);
				}
			};
			let unitTable = shadow.getElementById("units");
			for (let unit of g_data.characters) {
				if (unit.kingdom == whoami) {
					let who = document.createElement("div");
					let whor = document.createElement("report-link");
					whor.setAttribute("href", "character/" + unit.name);
					whor.innerHTML = unit.name;
					who.appendChild(whor);
					let od = document.createElement("div");
					od.appendChild(this.select("action_" + unit.name.replace(/[ ']/g, "_"), getCharacterOptions(unit)));
					let wd = document.createElement("div");
					wd.setAttribute("id", "warning_character_" + unit.name.replace(/[ ']/g, "_"));
					wd.setAttribute("class", "warning");
					od.appendChild(wd);
					addRow(unitTable, who, undefined, od, shadow.getElementById("table_nobles"));
				}
			}
			let navyCount = 0;
			let armyCount = 0;
			let nobleCount = 0;
			for (let unit of g_data.armies) {
				if (unit.kingdom == whoami) {
					let tr = document.createElement("tr");
					tr.setAttribute("id", "army_row_" + unit.id);
					let who = document.createElement("div");
					let whor = document.createElement("report-link");
					whor.setAttribute("href", "army/" + unit.id);
					whor.innerHTML = (unit.type == "army" ? "Army " : "Navy ") + unit.id;
					who.appendChild(whor);
					who.appendChild(document.createTextNode(" (" + Math.floor(unit.size) + ")"));
					let t2 = undefined;
					t2 = document.createElement("button");
					t2.innerHTML = "÷";
					t2.addEventListener("click", this.getDivisionFunc(shadow, unit, tr, -1, form));
					if (unit.size < 2) t2.style.display = "none";
					let addCell = function(c) {
						let td = document.createElement("td");
						td.appendChild(c);
						tr.appendChild(td);
					}
					addCell(who);
					addCell(t2);
					let td3 = document.createElement("td");
					td3.appendChild(this.select("action_army_" + unit.id, this.getArmyOptions(unit)));
					let wd = document.createElement("div");
					wd.setAttribute("id", "warning_army_" + unit.id);
					wd.setAttribute("class", "warning");
					td3.appendChild(wd);
					tr.appendChild(td3);
					if (unit.type == "army") {
						unitTable.insertBefore(tr, shadow.getElementById("table_navies"));
						armyCount++;
					} else {
						unitTable.appendChild(tr);
						navyCount++;
					}
				}
			}
			for (let r of g_data.regions) {
				if (r.kingdom == whoami && r.noble.name != undefined && r.noble.unrest <= 0.5) {
					let tr = document.createElement("tr");
					let addCell = function(c) {
						let td = document.createElement("td");
						td.appendChild(c);
						tr.appendChild(td);
					}
					let who = document.createElement("report-link");
					who.setAttribute("href", "region/" + r.name);
					who.appendChild(document.createTextNode(r.noble.name));
					addCell(who);
					addCell(document.createElement("div"));
					let od = document.createElement("div");
					od.appendChild(this.select("action_noble_" + r.id, this.getNobleOptions(r)));
					let wd = document.createElement("div");
					wd.setAttribute("id", "warning_noble_" + r.id);
					wd.setAttribute("class", "warning");
					od.appendChild(wd);
					addCell(od);
					unitTable.insertBefore(tr, shadow.getElementById("table_armies"));
					nobleCount++;
				}
			}
			if (armyCount == 0) shadow.getElementById("table_armies").style.display = "none";
			if (navyCount == 0) shadow.getElementById("table_navies").style.display = "none";
			if (nobleCount == 0) shadow.getElementById("table_nobles").style.display = "none";
		}

		{ // Letters Tab
			let newLetDiv = shadow.getElementById("new_letters");
			let oldLetDiv = shadow.getElementById("old_letters");
			let comms = [];
			for (let l of g_data.communications) comms.push(l);
			comms.sort(function(a, b){
				let ddiff = b.post_date - a.post_date;
				if (ddiff != 0) return ddiff;
				let afrom = contains(a.to, whoami);
				let bfrom = contains(b.to, whoami);
				if (afrom && !bfrom) return -1;
				else if (!afrom && bfrom) return 1;
				return 0;
			});
			let last_date = g_data.date - 1;
			let sent = null;
			for (let l of comms) {
				let from = l.signed.replace(/.* /, "");
				let letDiv = (l.post_date < g_data.date - 1 || from == whoami) ? oldLetDiv : newLetDiv;
				if (l.post_date != last_date) {
					let h2 = document.createElement("h2");
					h2.appendChild(document.createTextNode("Week " + l.post_date));
					letDiv.appendChild(h2);
					last_date = l.post_date;
				}
				let lt = document.createElement("div");
				let fromClause = from == whoami ? "" : " from <kingdom-label kingdom=\"" + from + "\"></kingdom-label>";
				let toClause = "";
				if (l.to.length != 1 || l.to[0] != whoami) {
					l.to.sort();
					let comma = false;
					for (let to of l.to) {
						if (comma) toClause += ",";
						else toClause += " to";
						toClause += " <kingdom-label kingdom=\"" + to + "\"></kingdom-label>";
						comma = true;
					}
				}
				lt.innerHTML = (from == whoami ? "→" : "←") + fromClause + toClause;
				
				let snippet = document.createElement("expandable-snippet");
				snippet.setAttribute("text", l.text.replace(/</g, "&lt").replace(/>/g, "&gt") + "\n\n" + l.signed);
				snippet.setAttribute("max-length", 64);
				lt.appendChild(snippet);
				letDiv.appendChild(lt);
			}
		}

		{ // Intrigue Tab
			let leverage = shadow.getElementById("plot_leverage");
			let addRow = function(eles) {
				let r = document.createElement("tr");
				for (let e of eles) {
					let t = document.createElement("td");
					t.appendChild(e);
					r.appendChild(t);
				}
				leverage.appendChild(r);
			};
			for (let k in g_data.kingdoms) {
				let kl = document.createElement("kingdom-label");
				kl.setAttribute("kingdom", k);
				let l = g_data.kingdoms[whoami].leverage[k];
				if (l == undefined) l = 0;
				let spend = document.createElement("span");
				spend.setAttribute("id", "plot_costs_" + k);
				addRow([kl, document.createTextNode(Math.round(l)), spend]);
			}
			shadow.getElementById("plot_newplot").addEventListener("click", ()=>this.addPlot(shadow));
			postLoadOps.push(() => this.computePlotCosts());
		}

		{ // Food Tab
			let eTax = this.shadow.getElementById("economy_tax");
			let eShip = this.shadow.getElementById("economy_ship");
			let eRation = this.shadow.getElementById("economy_ration");
			let eBonus = this.shadow.getElementById("economy_recruit_bonus");
			postLoadOps.push(computeEconomyConsequences);

			let food = shadow.getElementById("food_regions");
			let rvals = [];
			for (let r of g_data.regions) {
				if (r.kingdom != whoami) continue;
				rvals.push(r);
			}
			rvals.sort((a, b) =>
					g_data.kingdoms[whoami].core_regions.includes(a.id)
							? g_data.kingdoms[whoami].core_regions.includes(b.id) ? a.name.localeCompare(b.name) : -1
							: g_data.kingdoms[whoami].core_regions.includes(b.id) ? 1 : a.name.localeCompare(b.name));
			for (let r of rvals) {
				let tr = document.createElement("tr");
				let td = document.createElement("td");
				let rl = document.createElement("report-link");
				rl.setAttribute("href", "region/" + r.name);
				rl.appendChild(document.createTextNode(r.name));
				if (g_data.kingdoms[whoami].profiles.includes("PROSPERITY") && g_data.kingdoms[whoami].core_regions.includes(r.id)) {
					let tt = document.createElement("tooltip-element");
					tt.setAttribute("tooltip", r.name + " is one of your historical regions. You gain points for feeding the people here, and lose points if they starve.");
					tt.appendChild(rl);
					td.appendChild(tt);
				} else {
					td.appendChild(rl);
				}
				tr.appendChild(td);

				td = document.createElement("td");
				td.setAttribute("id", "food_amt_r" + r.id);
				tr.appendChild(td);
				td = document.createElement("td");
				td.setAttribute("id", "food_rations_r" + r.id);
				tr.appendChild(td);
				td = document.createElement("td");
				td.setAttribute("id", "food_unrest_r" + r.id);
				tr.appendChild(td);
				td = document.createElement("td");
				td.setAttribute("id", "food_death_r" + r.id);
				tr.appendChild(td);
				td = document.createElement("td");
				td.setAttribute("id", "food_starv_r" + r.id);
				tr.appendChild(td);
				food.appendChild(tr);
			}
			shadow.getElementById("economy_newtransfer").addEventListener("click", ()=>op.addEconomyRowOrder(shadow));
		}

		{ // Relations Tab
			let o = this;
			let kingdoms = shadow.getElementById("nations_nations_tbody");
			let relationships = shadow.getElementById("nations_relationships");
			for (let k in g_data.kingdoms) if (g_data.kingdoms.hasOwnProperty(k)) {
				if (k == whoami) continue;
				let tr = document.createElement("tr");
				let tds = [
					`<kingdom-label kingdom="${k}"></kingdom-label>`,
					`<select name="rel_${k}_attack">
						<option value="ATTACK">Always</option>
						<option value="NEUTRAL">In our land</option>
						<option value="DEFEND">Never</option>
					</select>`,
					`<select name="rel_${k}_tribute">
						<option value="0">0%</option>
						<option value="0.25">25%</option>
						<option value="0.33">33%</option>
						<option value="0.5">50%</option>
						<option value="0.75">75%</option>
						<option value="1">100%</option>
					</select>`,
					`<select name="rel_${k}_cede">
						<option value="ACCEPT">Yes</option>
						<option value="REFUSE">No</option>
					</select>`,
					`<select name="rel_${k}_refugees">
						<option value="ACCEPT">Yes</option>
						<option value="REFUSE">No</option>
					</select>`,
					`<select name="rel_${k}_construct">
						<option value="PERMIT">Yes</option>
						<option value="FORBID">No</option>
					</select>`,
					`<select name="rel_${k}_fealty">
						<option value="ACCEPT">Yes</option>
						<option value="REFUSE">No</option>
					</select>`];
				for (let t of tds) {
					let td = document.createElement("td");
					td.innerHTML = t;
					tr.appendChild(td);
				}
				kingdoms.appendChild(tr);
			}
			shadow.getElementById("nations_newgift").addEventListener("click", ()=>this.addGift(shadow));
			let newletter = shadow.getElementById("nations_newletter");
			newletter.addEventListener("click", ()=>this.addLetter(shadow, []));
			let colocatedRulers = [];
			let rulerLocation = getNation(whoami).getRuler().location;
			for (let k in g_data.kingdoms) if (g_data.kingdoms.hasOwnProperty(k)) {
				let rules = g_data.kingdoms[k].getRuler();
				if (k != whoami && rules.location == rulerLocation)
				colocatedRulers.push(rules);
			}
			function sanitize(s) {
				return s.replace(/</g, "&lt").replace(/>/g, "&gt");
			}
			{ // Cede regions.
				let rr = [];
				for (let r of g_data.regions) if (r.kingdom == whoami && (r.noble == undefined || r.noble.name == undefined)) rr.push(r);
				rr.sort(function(a, b){ return a.name < b.name ? -1 : a.name > b.name ? 1 : 0});
				let cede = shadow.getElementById("nations_cede");
				let ks = [];
				for (let k in g_data.kingdoms) if (k != whoami) ks.push(k);
				ks.sort();
				ks.unshift("(Nobody)");
				for (let r of rr) {
					let l = document.createElement("label");
					l.appendChild(document.createTextNode("Cede "));
					let rl = document.createElement("report-link");
					rl.setAttribute("href", "region/" + r.name);
					rl.innerHTML = r.name;
					l.appendChild(rl);
					l.appendChild(document.createTextNode(" to "));
					l.appendChild(this.select("nations_cede_" + r.id, ks));
					cede.appendChild(l);
				}
			}
		}

		{ // Pirates Tab
			shadow.getElementById("pirate_threat").textContent = Math.round(g_data.pirates.threat * .25 * 100);
			shadow.getElementById("economy_newbribe").addEventListener("click", ()=>op.addBribe(shadow));
			let odiv = shadow.getElementById("threatshares");
			let kingdoms = [];
			for (let k in g_data.kingdoms) if (g_data.kingdoms.hasOwnProperty(k)) kingdoms.push({"k": k, "o": g_data.kingdoms[k].calcPirateThreat()});
			kingdoms.sort((a,b)=>(b.o.v - a.o.v));
			for (let i of kingdoms) {
				let d = document.createElement("report-link");
				d.innerHTML = i.k;
				d.setAttribute("href", "kingdom/" + i.k);
				odiv.appendChild(d);
				d = document.createElement("div");
				if (Number.isNaN(i.o.v)) d.innerHTML = "0%";
				else d.innerHTML = num(i.o, 1, 100) + "%";
				odiv.appendChild(d);
			}
			// Add bribes.
			let idiv = shadow.getElementById("bribes");
			let ideoWS = [];
			for (let i in g_data.pirates.bribes) if (g_data.pirates.bribes.hasOwnProperty(i)) ideoWS.push({"i": i, "v": g_data.pirates.bribes[i]});
			ideoWS.sort((a, b)=>(b.v - a.v));
			for (let i of ideoWS) {
				if (i.v == 0) continue;
				let d = document.createElement("report-link");
				d.innerHTML = i.i;
				d.setAttribute("href", "kingdom/" + i.i);
				idiv.appendChild(d);
				d = document.createElement("div");
				d.innerHTML = Math.round(i.v) + " gold";
				idiv.appendChild(d);
			}
		}

		{ // Gothi Tab
			let odiv = shadow.getElementById("votes");
			let kingdoms = [];
			for (let k in g_data.kingdoms) if (g_data.kingdoms.hasOwnProperty(k)) kingdoms.push(g_data.kingdoms[k]);
			kingdoms.sort((a,b)=>(b.name < a.name ? 1 : b.name > a.name ? -1 : 0));
			for (let k of kingdoms) {
				let votes = k.calcGothiVotes();
				if (votes["Alyrja"].v == 0 && votes["Lyskr"].v == 0 && votes["Rjinku"].v == 0 && votes["Syrjen"].v == 0) continue;
				let tr = document.createElement("tr");
				let td = document.createElement("td");
				td.innerHTML = "<report-link href=\"kingdom/" + k.name + "\">" + k.name + "</report-link>";
				tr.appendChild(td);
				for (let tivar of ["Alyrja", "Lyskr", "Rjinku", "Syrjen"]) {
					let td = document.createElement("td");
					if (votes[tivar].v > 0) {
						if (whoami == k.name) {
							let input = document.createElement("input");
							input.setAttribute("name", "gothi_" + tivar.toLowerCase());
							input.setAttribute("type", "checkbox");
							if (k.gothi[tivar]) input.checked = true;
							td.appendChild(input);
						}
						td.appendChild(document.createTextNode(votes[tivar].v));
					}
					if (k.gothi[tivar]) td.setAttribute("class", "voting");
					tr.appendChild(td);
				}
				odiv.appendChild(tr);
			}
			shadow.getElementById("cult_join").style.display = (g_data.kingdoms[whoami].loyal_to_cult || g_data.cult_triggered) ? "none" : "block";
			let undeadCount = 0;
			for (let c of g_data.cult_caches) {
				if (c.eligible_nation == whoami) undeadCount += c.size;
			}
			shadow.getElementById("cult_profit").textContent = Math.floor(undeadCount);
		}

		{ // Church Tab
			let ideologyTooltips = {
				"Chalice of Compassion": "Followers of the Chalice of Compassion ideology most strongly believe in charitable living and merciful conduct in war. While Chalice of Compassion is the dominant Iruhan ideology, refugees do not cause unrest in regions worshipping Iruhan.",
				"Sword of Truth": "Followers of the Sword of Truth ideology most strongly believe that the other religions of the world pose an existential threat to humanity. While Sword of Truth is the dominant Iruhan ideology, all armies and navies of nations with an Iruhan state religion are +15% as strong. Armies and navies of nations with the Sword of Truth state ideology are a further +10% as strong.",
				"Tapestry of People": "Followers of the Tapestry of People ideology believe most strongly in the value of a diverse multitude of points of view. While Tapestry of People is the dominant Iruhan ideology, all nations with an Iruhan state religion produce three percentage points more tax and recruits per unique ideology (including those of other religions) within that nation.",
				"Vessel of Faith": "Followers of the Vessel of Faith ideology do not recognize the Church of Iruhan as a legitimate authority, instead believing that Iruhan speaks to each individual personally. They strongly believe in spreading the word of Iruhan to all people. While Vessel of Faith is the dominant Iruhan ideology, the Tiecel is powerless and construction of Iruhan temples in non-Iruhan regions is free.",
			};
			let ideoW = {};
			for (let r of g_data.regions) {
				if (r.religion == undefined) continue;
				if (!r.religion.startsWith("Iruhan")) continue;
				let i = r.religion.substring(r.religion.indexOf("(") + 1, r.religion.indexOf(")"));
				if (!ideoW.hasOwnProperty(i)) ideoW[i] = 0;
				ideoW[i] += r.population;
			}
			let max = 0;
			let dominantIdeology = undefined;
			for (let i in ideoW) {
				if (!ideoW.hasOwnProperty(i)) continue;
				if (ideoW[i] > max) {
					max = ideoW[i];
					dominantIdeology = i;
				}
			}
			let domEle = shadow.getElementById("church_dominant");
			domEle.textContent = "";
			let tEle = document.createElement("tooltip-element");
			tEle.setAttribute("tooltip", ideologyTooltips[dominantIdeology]);
			tEle.appendChild(document.createTextNode(dominantIdeology));
			domEle.appendChild(tEle);

			let odiv = shadow.getElementById("opinions");
			let kingdoms = [];
			for (let k in g_data.kingdoms) if (g_data.kingdoms.hasOwnProperty(k)) kingdoms.push({"k": k, "o": g_data.kingdoms[k].goodwill});
			kingdoms.sort((a,b)=>(b.o - a.o));
			for (let i of kingdoms) {
				let dd = document.createElement("div");
				let d = document.createElement("report-link");
				d.innerHTML = i.k;
				d.setAttribute("href", "kingdom/" + i.k);
				dd.appendChild(d);
				odiv.appendChild(dd);
				d = document.createElement("div");
				d.innerHTML = Math.round(i.o);
				odiv.appendChild(d);
			}
			// Add ideologies.
			let idiv = shadow.getElementById("ideologies");
			let ideoWS = [];
			for (let i in ideoW) if (ideoW.hasOwnProperty(i)) ideoWS.push({"i": i, "v": ideoW[i]});
			ideoWS.sort((a, b)=>(b.v - a.v));
			for (let i of ideoWS) {
				let d = document.createElement("tooltip-element");
				d.innerHTML = i.i;
				d.setAttribute("tooltip", ideologyTooltips[i.i]);
				idiv.appendChild(d);
				d = document.createElement("div");
				d.innerHTML = Math.round(i.v) + " souls";
				idiv.appendChild(d);
			}

			// Add active doctrines.
			let amTiecel = false;
			for (let c of g_data.characters) {
				if (c.tags.includes("Tiecel") && c.kingdom == whoami) amTiecel = true;
			}

			let doctrines = shadow.getElementById("doctrine_switches");
			for (let doctrine in doctrineDescriptions) {
				let l = document.createElement("label");
				if (amTiecel) {
					let c = document.createElement("input");
					c.setAttribute("type", "checkbox");
					c.setAttribute("name", "church_" + doctrine);
					c.setAttribute("id", "church_" + doctrine);
					l.appendChild(c);
				}
				if (amTiecel || g_data.church.doctrines.includes(doctrine)) {
					l.appendChild(document.createTextNode(titleCase(doctrine)));
					let d = document.createElement("ul");
					for (let desc of doctrineDescriptions[doctrine]) {
						let li = document.createElement("li");
						li.appendChild(document.createTextNode(desc));
						d.appendChild(li);
					}
					doctrines.appendChild(l);
					doctrines.appendChild(d);
				}
			}
			let tiecel = undefined;
			for (let c of g_data.characters) {
				if (c.tags.includes("Tiecel")) tiecel = c;
			}
		}

		{ // Settings Tab
			shadow.querySelector("#tutorialLauncher").addEventListener("click", showTutorialPopup);
			if (g_data.date >= 26 && (g_data.date - 26) % 6 == 0) {
				shadow.querySelector("#r_settings").classList.add("alert");
			} else {
				shadow.querySelector("#end_vote_div").style.display = "none";
			}
			shadow.getElementById("timeline").setAttribute("max", g_turndata.length - 1);
			shadow.getElementById("timeline").value = g_data.date;
			shadow.getElementById("weekdisplay").textContent = "Week " + g_data.date;
			// Registers a new listener on each data load, not great. For now, worked-around by recreating the whole orders panel.
			shadow.getElementById("timeline").addEventListener("change", function() {
				op.syncDisabled = true;
				let newDate = parseInt(shadow.getElementById("timeline").value);
				shadow.getElementById("weekdisplay").innerHTML = "Week " + newDate + " (Loading)";
				loadTurnData(newDate, () => {});
			});
			function recalcTime() {
				let view = "";
				if (g_turndata[g_turndata.length - 1].gameover) {
					view = "This game is over.";
				} else {
					let msRemaining = g_turndata[g_turndata.length - 1].next_turn - Date.now();
					if (msRemaining < 0) {
						view = "Turn expired - waiting for new turn data from server...";
					} else if (msRemaining < 1000 * 60 * 2) { // 2 minutes
						view = "Orders due in " + Math.round(msRemaining / 1000) + " seconds";
						window.setTimeout(recalcTime, 500 - msRemaining % 1000);
					} else if (msRemaining < 1000 * 60 * 60 * 2) { // 2 hours
						view = "Orders due in " + Math.floor(msRemaining / 1000 / 60) + " minutes, " + Math.round(msRemaining / 1000) % 60 + " seconds";
						window.setTimeout(recalcTime, 500 - msRemaining % 1000);
					} else {
						view = "Orders due in " + Math.floor(msRemaining / 1000 / 60 / 60) + " hours, " + Math.floor(msRemaining / 1000 / 60) % 60 + " minutes";
						window.setTimeout(recalcTime, 1000 * 60 + 500 - msRemaining % 1000);
					}
				}
				shadow.getElementById("timeleft").innerHTML = view;
			}
			recalcTime();
		}

		let lastTime = 0;
		form.addEventListener("input", function(e) {
			if (e.srcElement.type != "textarea") {
				op.checkWarnings(shadow);
				op.plannedMotions(shadow);
				computeEconomyConsequences();
			}
			lastTime = Date.now();
			setTimeout(function() {
				if (lastTime < Date.now() - 1000) {
					lastTime = Date.now();
					form.dispatchEvent(new Event("change"));
				}
			}, 1500);
		});
		form.addEventListener("change", function() {
			if (op.syncDisabled) return;
			let req = new XMLHttpRequest();
			if (op.currentlySubmitting) {
				if (op.submitQueued) return;
				op.submitQueued = true;
				setTimeout(function() {
					form.dispatchEvent(new Event("change"));
				}, 50); // Wait 50 ms and retry.
			} else {
				op.submitQueued = false;
				let data = JSON.stringify({"orders": formToJSON(form.elements)});
				if (data == op.lastSync) {
					op.currentlySubmitting = false;
					return;
				}
				op.currentlySubmitting = true;
				req.open("post", g_server + "/entry/orders?gid=" + gameId + "&t=" + g_data.date, true);
				addAuth(req, g_player, g_password);
				req.onerror = function (e) {
					op.currentlySubmitting = false;
					window.alert("Failed to communicate with the server.");
				};
				req.onload = function (ev) {
					op.currentlySubmitting = false;
					if (req.status != 204) {
						window.alert("Failed to communicate with the server: " + req.status);
					}
				};
				req.send(data);
				op.lastSync = data;
			}
		});

		postLoadOps.push(() => {
			if (g_turndata.length - 1 != g_data.date || g_data.gameover) {
				for (let e of shadow.querySelectorAll("form input, form select, form button")) e.disabled = true;
				shadow.querySelector("#timeline").disabled = false;
			}
		});

		{ // Load Orders
			op.syncDisabled = true;
			let req = new XMLHttpRequest();
			req.open("get", g_server + "/entry/orders?gid=" + gameId + "&t=" + g_data.date, true);
			addAuth(req, g_player, g_password);
			req.onerror = function (e) {
				console.log(e);
				op.syncDisabled = false;
				for (let operation of postLoadOps) operation();
			};
			req.onload = function (ev) {
				if (req.status != 200) {
					for (let c of g_data.characters) {
						if (c.orderhint == "" || c.orderhint == undefined) continue;
						if (c.kingdom != whoami) continue;
						shadow.querySelector("[name=action_" + c.name.replace(/[ ']/g, "_") + "]").value = c.orderhint;
					}
					for (let c of g_data.armies) {
						if (c.orderhint == "" || c.orderhint == undefined) continue;
						if (c.kingdom != whoami) continue;
						shadow.querySelector("[name=action_army_" + c.id + "]").value = c.orderhint;
					}
					for (let r of g_data.regions) {
						if (r.noble == undefined || r.noble.orderhint == "" || r.noble.orderhint == undefined) continue;
						if (r.kingdom != whoami) continue;
						shadow.querySelector("[name=action_noble_" + r.id + "]").value = r.noble.orderhint;
					}
					if (g_data.kingdoms[whoami].send_email != undefined) shadow.querySelector("[name=send_email]").checked = g_data.kingdoms[whoami].send_email;
					if (g_data.kingdoms[whoami].taxratehint != undefined) shadow.querySelector("[name=economy_tax]").value = g_data.kingdoms[whoami].taxratehint;
					if (g_data.kingdoms[whoami].shipratehint != undefined) shadow.querySelector("[name=economy_ship]").value = g_data.kingdoms[whoami].shipratehint;
					if (g_data.kingdoms[whoami].signingbonushint != undefined) shadow.querySelector("[name=economy_recruit_bonus]").value = g_data.kingdoms[whoami].signingbonushint;
					if (g_data.kingdoms[whoami].rationhint != undefined) shadow.querySelector("[name=economy_ration]").value = g_data.kingdoms[whoami].rationhint;
					shadow.querySelector("[name=economy_recruit_bonus]").dispatchEvent(new CustomEvent("input"));
					for (let k in g_data.kingdoms) {
						if (k == whoami) continue;
						shadow.querySelector("[name=rel_" + k + "_attack]").value = g_data.kingdoms[whoami].relationships[k].battle; shadow.querySelector("[name=rel_" + k + "_refugees]").value = g_data.kingdoms[whoami].relationships[k].refugees;
						shadow.querySelector("[name=rel_" + k + "_construct]").value = g_data.kingdoms[whoami].relationships[k].construct;
						shadow.querySelector("[name=rel_" + k + "_tribute]").value = g_data.kingdoms[whoami].relationships[k].tribute;
						shadow.querySelector("[name=rel_" + k + "_cede]").value = g_data.kingdoms[whoami].relationships[k].cede;
						shadow.querySelector("[name=rel_" + k + "_fealty]").value = g_data.kingdoms[whoami].relationships[k].fealty;
					}
					for (let gothi in g_data.kingdoms[whoami].gothi) {
						if (g_data.kingdoms[whoami].gothi.hasOwnProperty(gothi) && g_data.kingdoms[whoami].gothi[gothi]) {
							shadow.querySelector("[name=gothi_" + gothi.toLowerCase() + "]").checked = true;
						}
					}
					for (let doctrine of g_data.church.doctrines) {
						let ele = shadow.querySelector("[name=church_" + doctrine + "]");
						if (ele != undefined) ele.checked = true;
					}
					for (let ring of g_data.spy_rings) {
						if (ring.nation != whoami) continue;
						if (ring.involvement_type == undefined) continue;
						shadow.querySelector("[name=spyring_" + ring.location + "]").value = ring.involved_in_plot_id;
						shadow.querySelector("[name=spyring_type_" + ring.location + "]").value = ring.involvement_type;
						shadow.querySelector("[name=spyring_" + ring.location + "]").dispatchEvent(new Event("change"));
					}
					op.checkWarnings(shadow);
					op.plannedMotions(shadow);
					op.syncDisabled = false;
					for (let operation of postLoadOps) operation();
					return;
				}
				let resp = JSON.parse(req.responseText).orders;
				for (let p in resp) {
					if (!resp.hasOwnProperty(p)) continue;
					if (p.startsWith("div_parent_")) {
						let dpid = parseInt(p.substr("div_parent_".length));
						let pid = parseInt(resp[p]);
						let chi = shadow.getElementById("army_row_" + pid);
						let e = undefined;
						for (let i = 0; i < g_data.armies.length; i++) {
							e = g_data.armies[i];
							if (e.id == pid && e.kingdom == whoami) break;
						}
						op.getDivisionFunc(shadow, e, chi, dpid, shadow.getElementById("form"), false)();
						if (op.divisions < dpid + 1) op.divisions = dpid + 1;
					} else if (p.startsWith("economy_amount_")) {
						op.addEconomyRowOrder(shadow);
					} else if (p.startsWith("economy_bribe_amount_")) {
						op.addBribe(shadow);
					} else if (p.startsWith("letter_") && p.endsWith("_sig")) {
						op.addLetter(shadow, []);
					} else if (p.startsWith("plot_type")) {
						op.addPlot(shadow);
					} else if (p.startsWith("nations_gift_target_")) {
						op.addGift(shadow);
					}
				}
				for (let p in resp) {
					if (resp.hasOwnProperty(p)) {
						let e = shadow.querySelectorAll("[name=" + p + "]");
						if (e.length == 1) {
							if (e[0].type == "checkbox") e[0].checked = resp[p] == "checked";
							else e[0].value = resp[p];
							e[0].dispatchEvent(new CustomEvent("change"));
							e[0].dispatchEvent(new CustomEvent("input"));
						} else if (e.length > 1) {
							for (let i = 0; i < e.length; i++) {
								if (e[i].type == "radio") {
									e[i].checked = e[i].value == resp[p];
									e[i].dispatchEvent(new CustomEvent("change"));
								} else {
									e[i].value = resp[p];
									e[i].dispatchEvent(new CustomEvent("input"));
									e[i].dispatchEvent(new CustomEvent("change"));
								}
							}
						}
					}
				}
				op.checkWarnings(shadow);
				op.plannedMotions(shadow);
				op.syncDisabled = false;
				for (let operation of postLoadOps) operation();
			};
			req.send();
		}
	}

	changeReport(href) {
		let ele = href.split("/");
		if (ele[0] == "character" || ele[0] == "army" || ele[0] == "region") {
			if (this.highlight(href)) this.changeTab("units");
		} else if (ele[0] == "church") {
			this.changeTab("church");
		} else if (ele[0] == "pirates" || ele[1] == "Pirate") {
			this.changeTab("pirates");
		} else if (ele[0] == "gothi") {
			this.changeTab("gothi");
		} else if (ele[0] == "international") {
			this.changeTab("relationships");
		} else if (ele[0] == "intrigue") {
			this.changeTab("plots");
		} else if (ele[0] == "timeline") {
			this.changeTab("settings");
		}
	}

	getNewDivisionId() {
		return this.divisions++;
	}

	getArmyOptions(unit) {
		let r = g_data.regions[unit.location];
		let opts = [];
		opts.push("Stay in " + r.name);
		if (unit.type == "army") {
			if (r.type == "land" && r.kingdom != unit.kingdom) {
				opts.push("Conquer");
				for (let k in g_data.kingdoms) {
					if (!g_data.kingdoms.hasOwnProperty(k) || k == unit.kingdom) continue;
					opts.push("Conquer for " + k);
				}
			}
			let constructions = {};
			if (r.constructions != undefined) {
				for (let c of r.constructions) {
					let key = c.type;
					if (c.type == "temple") {
						key = "temple " + c.religion;
					}
					if (constructions.hasOwnProperty(key)) constructions[key]++;
					else constructions[key] = 1;
				}
			}
			for (let c in constructions) {
				if (!constructions.hasOwnProperty(c)) continue;
				opts.push("Raze " + c);
			}
			if (r.kingdom == unit.kingdom && r.noble.name != undefined) opts.push("Oust Noble");
			for (let neighbor of r.getNeighbors()) opts.push("Travel to " + neighbor.name);
			if (r.population > 1) for (let neighbor of r.getNeighbors()) if (neighbor.type == "land") opts.push("Force civilians to " + neighbor.name);
		} else {
			let nopts = [];
			for (let neighbor of r.getNeighbors()) if (r.type == "water" || neighbor.type == "water" || g_data.tivar.deluge > 0) nopts.push(neighbor.name);
			for (let neighbor of nopts.sort()) {
				opts.push("Travel to " + neighbor);
			}
		}
		for (let a of g_data.armies) {
			if (a.kingdom == unit.kingdom && a.location == unit.location && a.type == unit.type && a != unit && (contains(unit.tags, "Undead") == contains(a.tags, "Undead"))) opts.push("Merge into " + a.type + " " + a.id);
		}
		for (let k in g_data.kingdoms) {
			if (!g_data.kingdoms.hasOwnProperty(k) || k == unit.kingdom) continue;
			opts.push("Transfer " + unit.type + " to " + k);
		}
		opts.push("Disband");
		return opts;
	};

	getNobleOptions(r) {
		let opts = [];
		opts.push("Train", "Relax");
		if (r.isCoastal()) opts.push("Build Shipyard");
		if (g_data.spy_rings.find(ring => ring.nation == r.kingdom && ring.location == r.id) == undefined) {
			opts.push("Establish Spy Ring");
		}
		opts.push("Build Fortifications");
		for (let i of ["Chalice of Compassion", "Sword of Truth", "Tapestry of People", "Vessel of Faith"]) opts.push("Build Temple (" + i + ")");
		for (let i of ["Alyrja", "Lyskr", "Rjinku", "Syrjen"]) opts.push("Build Temple (" + i + ")");
		for (let i of ["Flame of Kith", "River of Kuun"]) opts.push("Build Temple (" + i + ")");
		return opts;
	};

	// opts is a list of {name: string, value: string}.
	selectPretty(name, opts) {
		let sel = document.createElement("select");
		sel.setAttribute("name", name);
		for (let o of opts) {
			let oe = document.createElement("option");
			oe.setAttribute("value", o.value);
			oe.appendChild(document.createTextNode(o.name));
			sel.appendChild(oe);
		}
		return sel;
	}

	// opts is a list of option contents
	select(name, opts) {
		let sel = document.createElement("select");
		sel.setAttribute("name", name);
		for (let o of opts) {
			let oe = document.createElement("option");
			oe.innerHTML = o;
			sel.appendChild(oe);
		}
		return sel;
	}

	getDivisionFunc(shadow, entity, child, did=-1, form, markchange=true) {
		let max = entity.size - 1;
		let o = this;
		return function(event) {
			let id = did;
			if (did == -1) id = o.getNewDivisionId();
			if (event != undefined) event.preventDefault();
			let tr = document.createElement("tr");
			let td = document.createElement("td");
			td.appendChild(document.createTextNode("↳ Division " + id + " of size "));
			let size = document.createElement("input");
			size.setAttribute("type", "number");
			size.setAttribute("name", "div_size_" + id);
			size.setAttribute("min", 1);
			size.setAttribute("max", max);
			size.setAttribute("step", "1");
			size.setAttribute("value", 1);
			td.appendChild(size);
			let par = document.createElement("input");
			par.setAttribute("type", "hidden");
			par.setAttribute("name", "div_parent_" + id);
			par.setAttribute("value", entity.id);
			td.appendChild(par);
			tr.appendChild(td);
			td = document.createElement("td");
			let button = document.createElement("button");
			button.innerHTML = "×";
			button.addEventListener("click", function(e) {
				e.preventDefault();
				child.parentNode.removeChild(tr);
				for (let n of shadow.querySelectorAll("option[value=\"Lead division " + id + "\"]")) {
					n.remove();
				}
				form.dispatchEvent(new Event("change"));
			});
			td.appendChild(button);
			tr.appendChild(td);
			td = document.createElement("td");
			td.appendChild(o.select("action_div_" + id, o.getArmyOptions(entity)));
			let warn = document.createElement("div");
			warn.setAttribute("id", "warning_div_" + id);
			warn.setAttribute("class", "warning");
			td.appendChild(warn);
			tr.appendChild(td);
			child.parentNode.insertBefore(tr, child.nextSibling);
			for (let c of g_data.characters) if (c.location == entity.location) {
				for (let n of shadow.querySelectorAll("select[name=action_" + c.name.replace(/[ ']/g, "_"))) {
					let o = document.createElement("option");
					o.innerHTML = "Lead division " + id;
					o.setAttribute("value", o.innerHTML);
					n.appendChild(o);
				}
			}
			if (markchange) form.dispatchEvent(new Event("change"));
		}
	}

	addBribe(shadow) {
		let id = this.bribeRowCount;
		this.bribeRowCount++;
		let kingdoms = [];
		for (let k in g_data.kingdoms) if (g_data.kingdoms.hasOwnProperty(k)) kingdoms.push(k);
		kingdoms.sort();
		let tbody = shadow.getElementById("economy_bribes");
		let tr = document.createElement("tr");
		let td = document.createElement("td");
		let sel = this.select("economy_bribe_target_" + id, kingdoms);
		td.appendChild(sel);
		let td2 = document.createElement("td");
		let amount = document.createElement("input");
		amount.setAttribute("type", "number");
		amount.setAttribute("min", "0");
		amount.setAttribute("value", "0");
		amount.setAttribute("name", "economy_bribe_amount_" + id);
		td2.appendChild(amount);
		let td3 = document.createElement("td");
		td3.appendChild(this.select("economy_bribe_action_" + id, ["Attack", "Not Attack"]));
		tr.appendChild(td2);
		tr.appendChild(td3);
		tr.appendChild(td);
		tbody.appendChild(tr);
	}

	addGift(shadow) {
		let id = this.giftRowCount;
		this.giftRowCount++;
		let kingdoms = [];
		for (let k in g_data.kingdoms) if (g_data.kingdoms.hasOwnProperty(k) && k != whoami) kingdoms.push(k);
		kingdoms.sort();
		let tbody = shadow.getElementById("nations_gifts");
		let tr = document.createElement("tr");
		let td = document.createElement("td");
		td.appendChild(this.select("nations_gift_target_" + id, kingdoms));
		let td2 = document.createElement("td");
		let amount = document.createElement("input");
		amount.setAttribute("type", "number");
		amount.setAttribute("min", "0");
		amount.setAttribute("value", "0");
		amount.setAttribute("name", "nations_gift_amount_" + id);
		td2.appendChild(amount);
		tr.appendChild(td2);
		tr.appendChild(td);
		tbody.appendChild(tr);
	}

	computePlotCosts() {
		if (this.syncDisabled) return;
		let costs = {};
		for (let k in g_data.kingdoms) costs[k] = 0;
		for (let p of this.plots) {
			let pcost = p.cost(p.args.map(a => a.value));
			for (let ck in pcost) costs[ck] += pcost[ck];
		}
		for (let k in costs) {
			this.shadow.querySelector("#plot_costs_" + k).textContent = Math.ceil(costs[k]);
			this.shadow.querySelector("#plot_costs_" + k).className = costs[k] > g_data.kingdoms[whoami].leverage[k] ? "alert" : "";
		}
	}

	addEconomyRowOrder(shadow) {
		this.shadow.getElementById("t_food_transferheader").style.display = "table-row";
		let regions = [];
		let id = this.economyRowCount;
		this.economyRowCount++;
		for (let r of g_data.regions) if (r.kingdom == whoami) regions.push(r.name);
		regions.sort();
		let tbody = shadow.getElementById("economy_transfers");
		let tr = document.createElement("tr");
		let td = document.createElement("td");
		let td2 = document.createElement("td");
		let sel = this.select("economy_from_" + id, regions);
		let o = this;
		sel.addEventListener("change", function() {
			let region = undefined;
			for (let r of g_data.regions) if (r.name == sel.value) region = r;
			let dests = [];
			for (let r of region.getFoodTransferDestinations()) dests.push("(" + r.kingdom + ") " + r.name);
			dests.sort();
			td2.innerHTML = "";
			let d = o.select("economy_to_" + id, dests);
			td2.appendChild(d);
		});
		sel.dispatchEvent(new Event("change"));
		td.appendChild(sel);
		tr.appendChild(td);
		tr.appendChild(td2);
		td = document.createElement("td");
		let td4 = document.createElement("td");
		let amount = document.createElement("input");
		amount.setAttribute("type", "number");
		amount.setAttribute("min", "0");
		amount.setAttribute("value", "0");
		amount.setAttribute("name", "economy_amount_" + id);
		amount.addEventListener("input", function() {
			let cost = Math.round(amount.value / 50 * 10) / 10;
			if (getNation(whoami).calcStateReligion() == "Iruhan (Chalice of Compassion)") cost = 0;
			td4.innerHTML = cost;
		});
		td.appendChild(amount);
		td.appendChild(document.createTextNode("k"));
		td4.innerHTML = "0";
		tr.appendChild(td);
		tr.appendChild(td4);
		tbody.appendChild(tr);
	}

	addLetter(shadow, recipients) {
		let id = this.letterCount;
		this.letterCount++;
		let d = document.createElement("div");
		let to = document.createElement("div");
		to.appendChild(document.createTextNode("To: "));
		let textarea = document.createElement("textarea");
		let boxes = [];
		function updateWho() {
			let rulers = [];
			for (let b of boxes) if (b.checked) {
				let ruler = g_data.kingdoms[b.getAttribute("data-kingdom")].getRuler();
				rulers.push(ruler.honorific + " " + ruler.name);
			}
			textarea.value = rulers.join(", ") + (rulers.length == 0 ? "" : ",");
		}
		for (let k in g_data.kingdoms) if (g_data.kingdoms.hasOwnProperty(k)) {
			let label = document.createElement("label");
			let box = document.createElement("input");
			box.setAttribute("name", "letter_" + id + "_to_" + k);
			box.setAttribute("data-kingdom", k);
			box.setAttribute("type", "checkbox");
			if (recipients.includes(k)) box.checked = true;
			boxes.push(box);
			box.addEventListener("change", updateWho);
			label.appendChild(box);
			let sp = document.createElement("kingdom-label");
			sp.setAttribute("kingdom", k);
			label.appendChild(sp);
			to.appendChild(label);
		}
		d.appendChild(to);
		d.appendChild(textarea);
		let textarea2 = document.createElement("textarea");
		textarea.setAttribute("name", "letter_" + id + "_greeting");
		textarea2.setAttribute("name", "letter_" + id + "_text");
		textarea.setAttribute("placeholder", "Dear ruler,");
		textarea2.setAttribute("placeholder", "We have matters to discuss.");
		d.appendChild(textarea2);
		let sendAs = this.select("letter_" + id + "_sig", ["Signed, " + g_data.kingdoms[whoami].getRuler().honorific + " " + g_data.kingdoms[whoami].getRuler().name + " of " + whoami, "Anonymous"]);
		d.appendChild(sendAs);
		shadow.getElementById("nations_letters").appendChild(d);
		updateWho();
	}

	addPlot(shadow) {
		let id = this.plotCount;
		this.plotCount++;

		let characterSelect = {
			"name": "character",
			"type": "select",
			"label": "Who",
			"f": function() {
				return g_data.characters.map(c => { return {"name": "(" + c.kingdom + ") " + c.name, "value": c.name}});
			}
		};

		let nationSelect = {
			"name": "nation",
			"type": "select",
			"label": "Who",
			"f": function(args) {
				return Object.keys(g_data.kingdoms).filter(k => args.length == 0 || args[0] != k).map(k => { return {"name": k, "value": k}});
			}
		};

		let armySelect = {
			"name": "army",
			"type": "select",
			"label": "Who",
			"f": function(args) {
				return g_data.armies.filter(a => args.length == 0 || a.kingdom != args[0]).map(a => { return {"name": (a.type == "army" ? "Army " : "Navy ") + a.id, "value": a.id}});
			}
		};

		let ideologySelect = {
			"name": "ideology",
			"type": "select",
			"label": "To",
			"f": function(args) {
				let r = g_data.regions[args[0]];
				let templeCount = {
					"Iruhan (Vessel of Faith)": 0,
					"Iruhan (Sword of Truth)": 0,
					"Iruhan (Tapestry of People)": 0,
					"Iruhan (Chalice of Compassion)": 0,
					"Northern (Alyrja)": 0,
					"Northern (Lyskr)": 0,
					"Northern (Rjinku)": 0,
					"Northern (Syrjen)": 0,
					"Tavian (Flame of Kith)": 0,
					"Tavian (River of Kuun)": 0
				};
				for (let c of r.constructions) {
					if (c.type == "temple") templeCount[c.religion]++;
				}
				let possible = [];
				for (let i in templeCount) {
					if (i != r.religion && templeCount[i] == templeCount[r.religion]) possible.push(i);
				}
				return possible.map(i => { return {"name": i, "value": i}});
			}
		};

		let regionSelect = {
			"name": "region",
			"type": "select",
			"label": "Where",
			"f": function() {
				return g_data.regions.filter(r => r.type == "land").map(r => { return {"name": "(" + r.kingdom + ") " + r.name, "value": r.id}});
			}
		};

		let regionSelectConstruction = {
			"name": "region",
			"type": "select",
			"label": "Where",
			"f": function() {
				return g_data.regions.filter(r => r.type == "land").filter(r => r.constructions.length > 0).map(r => { return {"name": "(" + r.kingdom + ") " + r.name, "value": r.id}});
			}
		};

		let regionSelectNoble = {
			"name": "region",
			"type": "select",
			"label": "Where",
			"f": function() {
				return g_data.regions.filter(r => r.type == "land").filter(r => r.noble.name != undefined).map(r => { return {"name": "(" + r.kingdom + ") " + r.name, "value": r.id}});
			}
		};

		let constructionSelect = {
			"name": "construction",
			"type": "select",
			"label": "What",
			"f": function(args) {
				let types = new Set();
				for (let c of g_data.regions[args[0]].constructions) {
					types.add(c.type == "temple" ? c.type + " " + c.religion : c.type);
				}
				let t = [];
				for (let tt of types) t.push(tt);
				t.sort();
				return t.map(t => { return {"name": t, "value": t}});
			}
		};

		let destinationSelect = {
			"name": "destination",
			"type": "select",
			"label": "To",
			"f": function(args) {
				return g_data.regions[args[0]].getFoodTransferDestinations().map(r => { return {"name": r.name, "value": r.id}});
			}
		};

		let amountSelect = {
			"name": "amount",
			"type": "number",
			"label": "Amount",
			"f": function() {
				return Number.POSITIVE_INFINITY;
			}
		};

		let amountCropsSelect = {
			"name": "amount",
			"type": "number",
			"label": "Amount (k)",
			"f": function(args) {
				return g_data.regions[args[0]].crops;
			}
		};

		let amountFoodSelect = {
			"name": "amount",
			"type": "number",
			"label": "Amount (k)",
			"f": function(args) {
				return g_data.regions[args[0]].food;
			}
		};

		let plotTypes = [
			{"value": "", "name": "", "data": [], "cost": (args) => { return {}}},
			{"value": "assassinate", "name": "Assassinate", "data": [characterSelect], "cost": (args) => { let r = {}; r[Character.byName(args[0]).kingdom] = 50; return r;}},
			{"value": "conceal", "name": "Conceal an army", "data": [nationSelect, armySelect], "cost": (args) => { let r = {}; r[args[0]] = Army.byId(parseInt(args[1])).getEffectiveSize() / 100; return r;}},
			{"value": "convert", "name": "Convert a region", "data": [regionSelect, ideologySelect], "cost": (args) => { let r = {}; r[g_data.regions[parseInt(args[0])].kingdom] = 10; return r;}},
			{"value": "denounce", "name": "Denounce", "data": [nationSelect, amountSelect], "cost": (args) => { let r = {}; r[g_data.regions[g_geo.holycity].kingdom] = args[1] / 3; return r;}},
			{"value": "destroy", "name": "Destroy", "data": [regionSelectConstruction, constructionSelect], "cost": (args) => { let r = {}; r[g_data.regions[parseInt(args[0])].kingdom] = 12; return r;}},
			{"value": "hobble", "name": "Hobble", "data": [armySelect], "cost": (args) => { let r = {}; r[args[0]] = Army.byId(parseInt(args[1])).getEffectiveSize() / 100; return r;}},
			{"value": "incite_popular", "name": "Incite popular unrest", "data": [regionSelect, amountSelect], "cost": (args) => { let r = {}; r[g_data.regions[parseInt(args[0])].kingdom] = parseInt(args[1]) / 3; return r; }},
			{"value": "incite_noble", "name": "Incite noble unrest", "data": [regionSelectNoble, amountSelect], "cost": (args) => { let r = {}; r[g_data.regions[parseInt(args[0])].kingdom] = parseInt(args[1]) / 3; return r; }},
			{"value": "intercept", "name": "Intercept letters", "data": [nationSelect], "cost": (args) => { let r = {}; r[args[0]] = 15; return r; }},
			{"value": "praise", "name": "Praise", "data": [nationSelect, amountSelect], "cost": (args) => { let r = {}; r[g_data.regions[g_geo.holycity].kingdom] = args[1] / 5; return r;}},
			{"value": "ruin", "name": "Ruin crops", "data": [regionSelect, amountCropsSelect], "cost": (args) => {  let r = {}; r[g_data.regions[parseInt(args[0])].kingdom] = parseInt(args[1]) / 20; return r;}},
			{"value": "smuggle", "name": "Smuggle food", "data": [regionSelect, destinationSelect, amountFoodSelect], "cost": (args) => { let r = {}; r[g_data.regions[parseInt(args[0])].kingdom] = parseInt(args[2]) / 10; return r; }},
			{"value": "steal", "name": "Steal gold", "data": [nationSelect, amountSelect], "cost": (args) => { let r = {}; r[args[0]] = parseInt(args[1]) / 4; return r; }},
			{"value": "rebel", "name": "Trigger rebellion for", "data": [regionSelect, nationSelect], "cost": (args) => {  let r = {}; r[g_data.regions[parseInt(args[0])].kingdom] = 60 - g_data.regions[parseInt(args[0])].calcUnrest().v * 100 / 3; return r;}}];

		let op = this;
		let d = document.createElement("div");
		let sel = this.selectPretty("plot_type_" + id, plotTypes);
		d.appendChild(sel);
		let details = document.createElement("div");
		d.appendChild(details);
		op.plots.push({"args": [], "cost": (args) => { return {}}});
		sel.addEventListener("change", () => {
			while (details.firstChild) details.removeChild(details.firstChild);
			let plotType = undefined;
			for (let o of plotTypes) if (o.value == sel.value) plotType = o;
			let args = [];
			op.plots[id].args = args;
			op.plots[id].cost = plotType.cost;
			let num = 0;
			for (let data of plotType.data) {
				let did = num++;
				let dbox = document.createElement("label");
				dbox.appendChild(document.createTextNode(data.label));
				if (data.type == "select") {
					let sel = document.createElement("select");
					sel.setAttribute("name", "plot_" + data.name + "_" + id);
					for (let o of data.f(args.map(p => p.value)).sort((a, b) => a.name.localeCompare(b.name))) {
						let opt = document.createElement("option");
						opt.setAttribute("value", o.value);
						opt.appendChild(document.createTextNode(o.name));
						sel.appendChild(opt);
					}
					sel.addEventListener("change", () => {
						// Recreate further data element options.
						for (let i = did + 1; i < args.length; i++) {
							let inp = args[i];
							let subdata = plotType.data[i];
							if (subdata.type == "number") {
								inp.setAttribute("value", "0");
								inp.setAttribute("max", subdata.f(args.map(p => p.value)));
							} else if (subdata.type == "select") {
								while (inp.firstChild) inp.removeChild(inp.firstChild);
								for (let o of subdata.f(args.map(p => p.value)).sort((a, b) => a.name.localeCompare(b.name))) {
									let opt = document.createElement("option");
									opt.setAttribute("value", o.value);
									opt.appendChild(document.createTextNode(o.name));
									inp.appendChild(opt);
								}
							}
						}
						op.computePlotCosts();
					});
					dbox.append(sel);
					args.push(sel);
				} else if (data.type == "number") {
					let inp = document.createElement("input");
					inp.setAttribute("name", "plot_" + data.name + "_" + id);
					inp.setAttribute("type", "number");
					inp.setAttribute("min", "0");
					inp.setAttribute("value", "0");
					inp.setAttribute("max", data.f(args.map(p => p.value)));
					inp.addEventListener("change", () => { op.computePlotCosts(); });
					dbox.append(inp);
					args.push(inp);
				}
				details.appendChild(dbox);
			}
			op.computePlotCosts();
		});
		shadow.getElementById("plot_newplots").appendChild(d);
	}

	predictBudget(shadow) {
		let projectedConstructionCost = 0;
		let projectedSalaries = 0;
		for (let army of g_data.armies) if (army.kingdom == whoami) {
			let travelling = shadow.querySelector("[name=action_army_" + army.id + "]").value.startsWith("Travel");
			projectedSalaries += army.calcCost(travelling).v;
		}
		let capables = [];
		for (let c of g_data.characters) {
			if (c.kingdom == whoami) {
				let cname = c.name.replace(/[ ']/g, "_");
				capables.push({
						"who": c,
						"act": shadow.querySelector("select[name=action_" + cname + "]").value,
						"loc": c.location,
						"warn": shadow.getElementById("warning_character_" + cname)});
			}
		}
		for (let q of shadow.querySelectorAll("select")) {
			if (!q.name.startsWith("action_noble_")) continue;
			capables.push({
				"who": null,
				"act": q.value,
				"loc": parseInt(q.name.replace("action_noble_", "")),
				"warn": shadow.getElementById(q.name.replace("action", "warning"))});
		}
		for (let c of capables) c.warn.innerHTML = "";
		let fwarns = [];
		for (let c of capables) {
			if (!c.act.startsWith("Build ") && c.act != "Establish Spy Ring") continue;
			fwarns.push(c.warn);
			if (c.act == "Establish Spy Ring") {
				projectedConstructionCost += g_data.regions[c.loc].calcCostToEstablishSpyRing(whoami, c.who).v;
			} else {
				if (c.act.includes("Shipyard")) {
					projectedConstructionCost += g_data.regions[c.loc].calcCostToBuildShipyard(whoami, c.who).v;
				} else if (c.act.includes("Fortifications")) {
					projectedConstructionCost += g_data.regions[c.loc].calcCostToBuildFortifications(whoami, c.who).v;
				} else if (c.act.includes("Temple")) {
					let type = religionFromIdeology(c.act.replace("Build Temple ", ""));
					projectedConstructionCost += g_data.regions[c.loc].calcCostToBuildTemple(whoami, type, c.who).v;
				}
			}
		}
		return {
			"salary": projectedSalaries,
			"actions": projectedConstructionCost,
		};
	}

	checkWarnings(shadow) {
		let warmies = [];
		for (let a of g_data.armies) {
			let o = shadow.querySelector("select[name=action_army_" + a.id + "]");
			if (o == undefined) continue;
			warmies.push({"army": a, "o": o.value, "w": shadow.getElementById("warning_army_" + a.id)});
		}
		for (let i = 0; i < this.divisions; i++) {
			let o = shadow.querySelector("select[name=action_div_" + i + "]");
			if (o == undefined) continue;
			let source = undefined;
			let parentId = parseInt(shadow.querySelector("[name=div_parent_" + i + "]").value);
			for (let a of g_data.armies) if (a.id == parentId) source = a;
			let fakeArmy = {
				"type": source.type,
				"size": parseInt(shadow.querySelector("[name=div_size_" + i + "]").value),
				"kingdom": source.kingdom,
				"location": source.location,
				"preparation": [],
				"tags": source.tags,
				"orderhint": "",
				"gold": 0
			};
			warmies.push({"army": new Army(fakeArmy), "o": o.value, "w": shadow.getElementById("warning_div_" + i)});
		}
		for (let entry of warmies) {
			let a = entry.army;
			let o = entry.o;
			let warn = "";
			if (o.startsWith("Travel to ")) {
				let dest = undefined;
				for (let r of g_data.regions) if (r.name == o.replace("Travel to ", "")) dest = r;
				if (a.type == "navy" && dest.type == "land" && dest.kingdom != a.kingdom && (dest.kingdom == "Unruled" || g_data.kingdoms[dest.kingdom].relationships[a.kingdom].battle != "DEFEND") && g_data.tivar.deluge == 0) {
					warn += " (navies do not contribute to land battles except during the Deluge, and are vulnerable to capture)";
				}
			} else if (o.startsWith("Merge into army")) {
				let ot = undefined;
				for (let aa of g_data.armies) if (aa.id == parseInt(o.replace("Merge into army ", ""))) ot = aa;
				if (ot.tags[0] != a.tags[0] || ot.tags[1] != a.tags[1]) warn = "(67% of the army will merge, 33% will turn to piracy)";
			} else if (o.startsWith("Oust")) {
				if (a.calcStrength().v < g_data.regions[a.location].calcMinPatrolSize().v) {
					warn = " (army may be too small to oust)";
				}
			} else if (o.startsWith("Conquer")) {
				if (a.calcStrength().v < g_data.regions[a.location].calcMinConquestSize().v) {
					warn = " (army may be too small to conquer)";
				}
				if (g_data.regions[a.location].kingdom != "Unruled" && shadow.querySelector("[name=rel_" + g_data.regions[a.location].kingdom + "_attack]").value != "ATTACK") {
					warn += " (conquest requires being ordered to attack " + g_data.regions[a.location].kingdom + " armies/navies)";
				}
			} else if (o.startsWith("Raze")) {
				if (a.calcStrength().v < g_data.regions[a.location].calcMinConquestSize().v / 5) {
					warn = " (army may be too small to raze)";
				}
			}
			entry.w.innerHTML = warn;
		}
		{ // Warn about construction cost overruns.
			let spending = this.predictBudget(shadow);
			if (spending.actions > getNation(whoami).gold) {
				for (let w of fwarns) w.innerHTML += " (insufficient gold - spending " + Math.round(projectedConstructionCost) + " of " + Math.round(getNation(whoami).gold) + " gold on actions)";
			} else if (spending.actions + spending.salaries > getNation(whoami).gold) {
				for (let w of fwarns) w.innerHTML += " (low gold - spending " + Math.round(projectedConstructionCost + projectedSalaries) + " of " + Math.round(getNation(whoami).gold) + " gold on actions and troop salaries)";
			}
		}
		{ // Warn about attacking nations paying tribute.
			let hasNoble = false;
			for (let r of g_data.regions) if (r.kingdom == whoami && r.noble.name != undefined) {
				hasNoble = true;
				break;
			}
			if (hasNoble) {
				let w = shadow.getElementById("t_kingdoms_warnings");
				for (let k in g_data.kingdoms) {
					if (k == whoami) continue;
					if (g_data.kingdoms[k].relationships[whoami].tribute > 0 && shadow.querySelector("[name=rel_" + k + "_attack]").value == "ATTACK") {
						w.innerHTML += "<p>Attacking " + k + " while they pay us tribute will cause noble unrest.</p>"
					}
				}
			}
		}
	}

	plannedMotions(shadow) {
		let amotions = {};
		for (let a of g_data.armies) {
			let o = shadow.querySelector("select[name=action_army_" + a.id + "]");
			if (o != undefined) {
				if (o.value.startsWith("Travel to ")) {
					let dest = undefined;
					for (let i = 0; i < g_data.regions.length; i++) if (g_data.regions[i].name == o.value.replace("Travel to ", "")) dest = i;
					if (dest != undefined) amotions["a" + a.id] = dest;
				} else if (o.value.startsWith("Merge into ")) {
					let dest = undefined;
					for (let i = 0; i < g_data.armies.length; i++) if (g_data.armies[i].id == parseInt(o.value.replace("Merge into ", "").replace("army ", "").replace("navy ",""))) dest = g_data.armies[i];
					amotions["a" + a.id] = dest;
				} else {
					amotions["a" + a.id] = undefined;
				}
			}
		}
		for (let a of g_data.characters) {
			let o = shadow.querySelector("select[name=action_" + a.name.replace(/[ ']/g, "_") + "]");
			if (o != undefined) {
				if (o.value.startsWith("Travel to ") || o.value.startsWith("Hide in ")) {
					let dest = undefined;
					for (let i = 0; i < g_data.regions.length; i++) if (g_data.regions[i].name == o.value.replace("Travel to ", "").replace("Hide in ", "")) dest = i;
					if (dest != undefined && dest != a.location) amotions[a.name] = dest;
				} else if (o.value.startsWith("Lead ")) {
					let dest = undefined;
					for (let i = 0; i < g_data.armies.length; i++) if (g_data.armies[i].id == parseInt(o.value.replace("Lead ", "").replace("army ", "").replace("navy ",""))) dest = g_data.armies[i];
					amotions[a.name] = dest;
				} else {
					amotions[a.name] = undefined;
				}
			}
		}
		let dmotions = {};
		for (let i = 0; i < this.divisions; i++) {
			let o = shadow.querySelector("select[name=action_div_" + i + "]");
			if (o != undefined) {
				let paren = "a" + shadow.querySelector("[name=div_parent_" + i + "]").value;
				if (!dmotions.hasOwnProperty(paren)) dmotions[paren] = [];
				if (o.value.startsWith("Travel to ")) {
					let dest = undefined;
					for (let i = 0; i < g_data.regions.length; i++) if (g_data.regions[i].name == o.value.replace("Travel to ", "")) dest = i;
					if (dest != undefined) dmotions[paren].push(dest);
				} else if (o.value.startsWith("Merge into ")) {
					let dest = undefined;
					for (let i = 0; i < g_data.armies.length; i++) if (g_data.armies[i].id == parseInt(o.value.replace("Merge into ", "").replace("army ", "").replace("navy ",""))) dest = g_data.armies[i];
					dmotions[paren].push(dest);
				}
			}
		}
		updateMotions(amotions, dmotions); /* map2.html */
	}

	startLetter(recipients) {
		this.addLetter(this.shadow, recipients);
	}

	highlight(href) {
		this.clearHighlight();
		let s = href.split("/");
		let he = undefined;
		if (s[0] == "character") he = this.shadow.querySelector("select[name=action_" + s[1].replace(/[ ']/g, "_") + "]");
		else if (s[0] == "army") he = this.shadow.querySelector("select[name=action_army_" + s[1] + "]");
		else if (s[0] == "region") for (let i = 0; i < g_data.regions.length; i++) if (g_data.regions[i].name == s[1]) he = this.shadow.querySelector("select[name=action_noble_" + i + "]");
		if (he != undefined) he.classList.add("highlight");
		return he != undefined;
	}

	clearHighlight() {
		for (let e of this.shadow.querySelectorAll(".highlight")) e.classList.remove("highlight");
	}
}
customElements.define("orders-panel", OrdersPanel);
