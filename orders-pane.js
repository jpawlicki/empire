class OrdersPane extends HTMLElement {
	constructor() {
		super();
		this.divisions = 0;
		this.economyRowCount = 0;
		this.bribeRowCount = 0;
		this.giftRowCount = 0;
		this.letterCount = 0;
		this.currentlySubmitting = false;
		this.submitQueued = false;
		this.syncDisabled = true;
	}
	connectedCallback() {
		let kingdom = g_data.kingdoms[this.getAttribute("kingdom")];
		let shadow = this.attachShadow({mode: "open"});
		let html = `
			<div id="tabs">
				<div id="tab_units" class="tab_units">units</div>
				<div id="tab_plots" class="tab_plots">plots</div>
				<div id="tab_nations" class="tab_nations">nations</div>
				<div id="tab_economy" class="tab_economy">economy</div>
				<div id="tab_economy" class="tab_game">game</div>
			</div>
			<form id="form">
				<table id="content_units">
					<tbody id="units">
						<tr><th>Who</th><th></th><th>Action</th></tr>
						<tr id="table_armies"><th colspan="2">Armies</th></tr>
						<tr id="table_navies"><th colspan="2">Navies</th></tr>
					</tbody>
				</table>
				<div id="content_plots">
					<h1>Objective</h1>
					<div>
						<select name="plot_type" id="plot_type">
							<option value="defend">Discover / Defend From Plots</option>
							<option value="CHARACTER">Hatch a Character Plot</option>
							<option value="REGION">Hatch a Regional Plot</option>
							<option value="CHURCH">Hatch a Church Plot</option>
							<option value="INTERNATIONAL">Hatch a International Plot</option>
						</select>
					</div>
					<h1>Details</h1>
					<div id="plot_details"></div>
					<h1>Risk</h1>
					<div id="plot_risks">(No risks.)</div>
					<hr/>
					<label><input type="checkbox" name="plot_cult" ${kingdom.loyal_to_cult ? "checked=\"true\" disabled=\"true\"" : ""}/>Swear loyalty to the Cult</label>
					<expandable-snippet text="In exchange for loyalty, the Cult will give us an army of 5000 undead soldiers and 2 weeks of food in every region we control. However, the Cult will gain access to any regions we control, and we do not fully understand their objectives."></expandable-snippet>
					<hr/>
					<label id="gothi_Alyrja"><input type="checkbox" name="gothi_Alyrja"/>Vote to summon the <tooltip-element tooltip="The warwinds make all sea regions treacherous, blows vessels in sea regions to random adjacent regions, and destroy 3% of all crops each week they are active.">Warwinds</tooltip-element></label>
					<label id="gothi_Rjinku"><input type="checkbox" name="gothi_Rjinku"/>Vote to summon the <tooltip-element tooltip="Each construction has a 33% chance of being destroyed each week the quake is active. The quake destroys 3% of all crops each week it is active.">Quake</tooltip-element></label>
					<label id="gothi_Syrjen"><input type="checkbox" name="gothi_Syrjen"/>Vote to summon the <tooltip-element tooltip="The deluge makes all land regions treacherous, allows navies to traverse land regions and participate in battles there (and prevents them from being captured). It also destroys 3% of all crops each week while active.">Deluge</tooltip-element></label>
					<label id="gothi_Lyskr"><input type="checkbox" name="gothi_Lyskr"/>Vote to summon the <tooltip-element tooltip="The veil makes all armies, navies, and characters hidden from other rulers. It also destroys 3% of crops worldwide each week while active.">Veil</tooltip-element></label>
				</div>
				<div id="content_nations">
					<h1><tooltip-element tooltip="These communications are usually delayed by 0 to 30 seconds.">Real-Time</tooltip-element> Communications</h1>
					<div id="nations_rtc">
						<expandable-snippet text="Only colocated rulers not in hiding can engage in real-time communications."></expandable-snippet>
					</div>
					<h1>Relationships</h1>
					<div id="nations_nations"></div>
					<expandable-snippet text="All changes will be announced to all rulers."></expandable-snippet>
					<h1>Letters</h1>
					<div id="nations_letters">
					</div>
					<div id="nations_newletter">Add Letter</div>
					<h1>Gold</h1>
					<table>
						<tr><th>Amount (Gold)</th><th>To</th></tr>
						<tbody id="nations_gifts">
						</tbody>
						<tr><td colspan="2" id="nations_newgift">Send Gold</td></tr>
					</table>
					<zip-div id="nations_cede" title="Cede Regions"></zip-div>
				</div>
				<div id="content_economy">
					<h1>Economic Controls</h1>
					<label>Taxation: <input id="economy_tax" name="economy_tax" type="range" min="0" max="200" step="25" value="100"/></label>
					<label>Rationing: <input id="economy_ration" name="economy_ration" type="range" min="75" max="125" step="25" value="100"/></label>
					<label>Recruit Signing Bonus: <input id="economy_recruit_bonus" name="economy_recruit_bonus" type="range" min="-2" max="16" step="1" value="0"/></label>
					<div id="economy_consequences">
					</div>
					<expandable-snippet text="Recruit signing bonuses will automatically scale down in the event we cannot pay the signing bonuses."></expandable-snippet>
					<table>
						<tr><th>From</th><th>To</th><th>Amount (Measures)</th><th>Cost (Gold)</th></tr>
						<tbody id="economy_transfers">
						</tbody>
						<tr><td colspan="4" id="economy_newtransfer">Add a Food Transfer</td></tr>
					</table>
					<expandable-snippet text="Food transfers are evaluated in order from top to bottom, after taxation income but before armies and civilians eat. If there are insufficient funds for a transfer, as much food as possible will be transferred with the funds available."></expandable-snippet>
					<hr/>
					<table>
						<tr><th>Amount (Gold)</th><th>To</th><th>Who</th></tr>
						<tbody id="economy_bribes">
						</tbody>
						<tr><td colspan="3" id="economy_newbribe">Bribe Pirates</td></tr>
					</table>
				</div>
				<div id="content_game">
					<h1>Score</h1>
					<div id="score_switches">
						<label><input type="checkbox" id="score_food" name="score_food"></input>Food</label>
						<label><input type="checkbox" id="score_prosperity" name="score_prosperity"></input>Prosperity</label>
						<label><input type="checkbox" id="score_happiness" name="score_happiness"></input>Happiness</label>
						<label><input type="checkbox" id="score_unity" name="score_unity"></input>Unity</label>
						<label><input type="checkbox" id="score_riches" name="score_riches"></input>Riches</label>
						<label><input type="checkbox" id="score_conquest" name="score_conquest"></input>Conquest</label>
						<label><input type="checkbox" id="score_glory" name="score_glory"></input>Glory</label>
						<label><input type="checkbox" id="score_supremacy" name="score_supremacy"></input>Supremacy</label>
						<label><input type="checkbox" id="score_religion" name="score_religion"></input>Religion</label>
						<label><input type="checkbox" id="score_security" name="score_security"></input>Security</label>
						<label><input type="checkbox" id="score_culture" name="score_culture"></input>Culture</label>
						<label><input type="checkbox" id="score_ideology" name="score_ideology"></input>Ideology</label>
						<expandable-snippet text="Your score profiles can be changed on every 7th turn.">
					</div>
					<h1>Final Actions</h1>
					<select id="final_action" name="final_action">
						<option value="continue_ruling">Continue Ruling</option>
						<option value="exodus">Exodus</option>
						<option value="abdicate">Gracefully Abdicate</option>
						<option value="last_stand">Last Stand</option>
						<option value="salt_the_earth">Salt the Earth</option>
					</select>
					<expandable-snippet text="Final Actions are powerful actions that end the story of your nation and remove you from the game."></expandable-snippet>
					<div id="final_action_details"></div>
				</div>
			</form>
			<div id="clock">Week ${g_data.date} (${(g_data.date % 52 < 13 || g_data.date % 52 >= 39) ? "Winter" : "Summer"})</div>
		`;
		// CSS
		let style = document.createElement("style");
		style.innerHTML = `
			:host {
				height: 100%;
				overflow-y: auto;
				box-shadow: 1em 0 1em rgba(0, 0, 50, .2);
				background-color: #fff;
				z-index: 1;
				font-size: 90%;
				font-family: sans-serif;
			}
			table, input, select, button {
				font-size: inherit;
			}
			#tabs > div::first-letter {
				text-transform: capitalize;
			}
			#tabs {
				display: flex;
				justify-content: space-around;
				font-family: sans-serif;
				border-left: 0;
				border-right: 0;
			}
			#tabs > div {
				cursor: pointer;
				transition: padding-bottom 0.2s, padding-top 0.2s, color 0.2s;
			}
			.tab_units {
				border-bottom: 3px solid red;
				border-left: 1.5px solid white;
				border-right: 1.5px solid white;
			}
			.tab_plots {
				border-bottom: 3px solid blue;
				border-left: 1.5px solid white;
				border-right: 1.5px solid white;
			}
			.tab_nations {
				border-bottom: 3px solid gold;
				border-left: 1.5px solid white;
				border-right: 1.5px solid white;
			}
			.tab_economy {
				border-bottom: 3px solid green;
				border-left: 1.5px solid white;
				border-right: 1.5px solid white;
			}
			h1 {
				margin-top: 0.7em;
				margin-bottom: 0;
				margin-left: 0;
				font-size: 100%;
				font-weight: bold;
				font-family: sans-serif;
			}
			input[type=text], input[type=number] {
				width: 5em;
			}
			#content_economy label {
				width: 100%;
			}
			input[type=range] {
				width: 90%;
			}
			#economy_newtransfer, #nations_newletter, #economy_newbribe, #nations_newgift {
				cursor: pointer;
				text-align: center;
				color: #00f;
			}
			table {
				width: 100%;
			}
			table tr td:last-child {
				text-align: right;
			}
			#nations_nations > div {
				font-family: sans-serif;
				margin: 0.2em;
				border: 1px solid black;
				padding: 0.1em;
				cursor: pointer;
			}
			#nations_nations select {
				display: block;
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
			#nations_letters label {
				margin-left: .3em;
				margin-right: .3em;
			}
			#nations_letters label input[type=checkbox]:checked ~ span {
				text-decoration: underline;
			}
			textarea {
				width: 100%;
				height: 7em;
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
			#clock {
				width: 100%;
				text-align: center;
				font-size: 115%;
			}
			#nations_cede label {
				display: block;
			}
			`;
		shadow.appendChild(style);
		let content = document.createElement("div");
		content.innerHTML = html;
		shadow.appendChild(content);
		let form = shadow.getElementById("form");
		for (let t of ["units", "plots", "nations", "economy"]) shadow.getElementById("content_" + t).style.display = "none";
		let changeTab = function (tab) {
			shadow.getElementById("tabs").className = "tab_" + tab;
			for (let t of ["units", "plots", "nations", "economy"]) {
				shadow.getElementById("content_" + t).style.display = "none";
				shadow.getElementById("tab_" + t).style.paddingBottom = "0.5em";
				shadow.getElementById("tab_" + t).style.color = "#777";
				shadow.getElementById("tab_" + t).style.paddingTop = "0";
			}
			shadow.getElementById("content_" + tab).style.display = "block";
			shadow.getElementById("tab_" + tab).style.paddingBottom = "0";
			shadow.getElementById("tab_" + tab).style.paddingTop = "0.5em";
			shadow.getElementById("tab_" + tab).style.color = "#000";
		};
		shadow.getElementById("tab_units").addEventListener("click", ()=>(changeTab("units")));
		shadow.getElementById("tab_plots").addEventListener("click", ()=>(changeTab("plots")));
		shadow.getElementById("tab_nations").addEventListener("click", ()=>(changeTab("nations")));
		shadow.getElementById("tab_economy").addEventListener("click", ()=>(changeTab("economy")));
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
		let getCharacterOptions = function(unit) {
			let opts = [];
			let r = g_data.regions[unit.location];
			if (unit.captor == "") {
				if (contains(unit.tags, "Cardinal") && g_data.regions[unit.location].name == "Sancta Civitate") opts.push("Inspire the Faithful");
				if (r.kingdom == unit.kingdom) {
					opts.push("Govern " + r.name);
					if (r.noble.name == undefined) {
						for (let n of g_data.kingdoms[unit.kingdom].court) {
							opts.push("Instate Noble (" + n.tags.join(", ") + ")");
						}
					}
				}
				if (r.type == "land") {
					if (r.isCoastal()) opts.push("Build Shipyard");
					opts.push("Build Fortifications");
					for (let i of ["Chalice of Compassion", "Sword of Truth", "Tapestry of People", "Vessel of Faith"]) opts.push("Build Temple (" + i + ")");
					for (let i of ["Alyrja", "Lyskr", "Rjinku", "Syrjen"]) opts.push("Build Temple (" + i + ")");
					for (let i of ["Flame of Kith", "River of Kuun"]) opts.push("Build Temple (" + i + ")");
				}
			}
			opts.push("Stay in " + r.name);
			if (unit.captor == "") {
				for (let a of g_data.armies) if (a.kingdom == unit.kingdom && a.location == unit.location) {
					opts.push("Lead " + a.type + " " + a.id);
				}
			}
			for (let neighbor of r.getNeighbors()) opts.push("Travel to " + neighbor.name);
			if (unit.captor != "") {
				opts.push("Execute");
				opts.push("Set Free");
			}
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

		let unitTable = shadow.getElementById("units");
		for (let unit of g_data.characters) {
			if (unit.kingdom == kingdom.name || unit.captor == kingdom.name) {
				let who = document.createElement("div");
				let whor = document.createElement("report-link");
				whor.setAttribute("href", "character/" + unit.name);
				whor.innerHTML = unit.name;
				who.appendChild(whor);
				if (unit.captor == kingdom.name) {
					who.appendChild(document.createTextNode(" (Our Captive)"));
				} else if (unit.captor != "") {
					who.appendChild(document.createTextNode(" (Captive of " + unit.captor + ")"));
				}
				addRow(unitTable, who, undefined, this.select("action_" + unit.name.replace(/ /g, "_"), getCharacterOptions(unit)), shadow.getElementById("table_armies"));
			}
		}
		for (let unit of g_data.armies) {
			if (unit.kingdom == kingdom.name) {
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
				if (unit.type == "army") unitTable.insertBefore(tr, shadow.getElementById("table_navies"));
				else unitTable.appendChild(tr);
			}
		}
		changeTab("units");

		// PLOT TAB
		let plotTypeSel = shadow.getElementById("plot_type");
		let details = shadow.getElementById("plot_details");
		let risks = shadow.getElementById("plot_risks");
		plotTypeSel.addEventListener("change", function() {
			details.innerHTML = {
				"defend": "Our characters will have +50% strength for investigating and defending against plots.",
				"CHARACTER": `
					<div>
						Find <select id="plot_target_character" name="plot_target"></select>
						and <select id="plot_action" name="plot_action">
							<option value="find">that's all</option>
							<option value="arrest">capture them if they are trespassing in our territory</option>
							<option value="capture">capture them</option>
							<option value="rescue">rescue them</option>
							<option value="kill">kill them</option>
						</select>.
					</div>
				`,
				"REGION": `
					<div>
						<select id="plot_action" name="plot_action">
							<option value="rebel">Incite rebellion</option>
							<option value="burn">Burn food</option>
						</select>
						in <select id="plot_target_region" name="plot_target"></select>.
					</div>
				`,
				"CHURCH": `
					<div>
						<select id="plot_action" name="plot_action">
							<option value="praise">Sing the praises of</option>
							<option value="denounce">Denounce and condemn</option>
						</select>
						<select id="plot_target_kingdom" name="plot_target"></select> among the elite members of the Church of Iruhan.
					</div>
				`,
				"INTERNATIONAL": `
					<div>
						<select id="plot_action" name="plot_action">
							<option value="eavesdrop">Intercept the communications of</option>
						</select>
						<select id="plot_target_kingdom" name="plot_target"></select>.
					</div>
				`,
			}[plotTypeSel.value];
			let stateDetailsAndRisk = function() {
				let action = shadow.getElementById("plot_action");
				if (action == undefined) {
				  shadow.getElementById("plot_risks").innerHTML = "(No risks.)";
					return;
				} else {
					action = action.value;
				}
				let ptk = shadow.getElementById("plot_target_kingdom");
				let ptc = shadow.getElementById("plot_target_character");
				let ptr = shadow.getElementById("plot_target_region");
				let targetRegion = -1;
				let defender = "";
				if (plotTypeSel.value == "CHARACTER") {
					for (let c of g_data.characters) if (c.name == ptc.value) {
						targetRegion = c.location;
						defender = c.captor == "" ? c.kingdom : c.captor;
					}
				} else if (plotTypeSel.value == "CHURCH") {
					for (let r of g_data.regions) if (r.name == "Sancta Civitate") targetRegion = r.id;
					defender = g_data.regions[targetRegion].kingdom;
				} else if (plotTypeSel.value == "REGION") {
					for (let r of g_data.regions) if (r.name == ptr.value) targetRegion = r.id;
					defender = g_data.regions[targetRegion].kingdom;
				} else if (plotTypeSel.value == "INTERNATIONAL") {
					targetRegion = g_data.kingdoms[ptk.value].getRuler().location;
					defender = ptk.value;
				}
				let riskText = "<p>All nations will learn the details of the plot, but not necessarily of our involvement.</p>";
				if (targetRegion == -1) {
					let tc = ptc == undefined ? g_data.kingdoms[ptk.value].getRuler().name : ptc.value;
					riskText += "<p>We do not know where " + tc + " is, so our spymasters cannot predict our chances of success.</p>";
				} else {
					let strengths = g_data.regions[targetRegion].calcPlotPowersInRegion();
					let ourStrength = strengths[kingdom.name];
					let unknown = [];
					let inferior = [];
					let inferiorWithDefense = [];
					let superior = [];
					for (let k in strengths) if (strengths.hasOwnProperty(k)) {
						if (k == kingdom.name) continue;
						let positionsKnown = true;
						for (let c of g_data.characters) if (c.kingdom == k && c.location == -1) positionsKnown = false;
						if (strengths[k] >= ourStrength) superior.push(k);
						else if (!positionsKnown) unknown.push(k);
						else if (strengths[k] * 1.5 * 1.5 < ourStrength) inferiorWithDoubleDefense.push(k);
						else if (strengths[k] * 1.5 < ourStrength) inferiorWithDefense.push(k);
						else if (strengths[k] < ourStrength) inferior.push(k);
					}
					unknown.sort();
					inferior.sort();
					inferiorWithDefense.sort();
					inferiorWithDoubleDefense.sort();
					superior.sort();
					if (defender == kingdom.name) riskText += "<p>Our plot will obviously succeed, as we are the \"defenders\".</p>";
					else if (plotTypeSel.value == "CHURCH"
						&& ((action == "praise" && g_data.kingdoms[defender].calcRelationship(g_data.kingdoms[ptk.value]) == "friendly")
						|| (action == "denounce" && g_data.kingdoms[defender].calcRelationship(g_data.kingdoms[ptk.value]) == "enemy"))) riskText += "<p>Our plot will succeed, because the defenders will not oppose us.</p>";
					else if (contains(superior, defender)) riskText += "<p>Our plot will fail.</p>";
					else if (contains(unknown, defender)) riskText += "<p>Our plot may succeed or fail.</p>";
					else if (contains(inferior, defender)) riskText += "<p>Our plot will succeed unless " + defender + " spies devote this week to defense " + (plotTypeSel.value == "CHARCTER" ? " or the character leads an army or navy this turn" : "") + ".</p>";
					else if (contains(inferiorWithDefense, defender)) riskText += "<p>Our plot will succeed" + (plotTypeSel.value == "CHARCTER" ? " the enemy devotes their spies to defense this week and the character leads an army or navy this turn" : "") + ".</p>";
					else if (contains(inferiorWithDoubleDefense, defender)) riskText += "<p>Our plot will succeed.</p>";
					if (superior.length > 0) riskText += "<p>" + superior.map(x => "<report-link href=\"kingdom/" + x + "\">" + x + "</report-link>").join(", ") + " will know of our involvement.</p>";
					if (unknown.length > 0) riskText += "<p>" + unknown.map(x => "<report-link href=\"kingdom/" + x + "\">" + x + "</report-link>").join(", ") + " may know of our involvement, depending on the location of their hidden characters.</p>";
					if (inferior.length > 0) riskText += "<p>" + inferior.map(x => "<report-link href=\"kingdom/" + x + "\">" + x + "</report-link>").join(", ") + " will know of our involvement only if they devote their spies to discovering plots this week" + (superior.length == 0 ? " or if we are the only nation with greater plot strength than them" : "") + ".</p>";
					if (inferiorWithDefense.length > 0) riskText += "<p>" + inferiorWithDefense.map(x => "<report-link href=\"kingdom/" + x + "\">" + x + "</report-link>").join(", ") + " will not know of our involvement" + (superior.length == 0 ? " unless we are the only nation with greater plot strength than them" : "") + ".</p>";
					if (superior.length == 0) riskText += "<p>At least one nation will know of our involvement.</p>";
				}
				riskText += "<expandable-snippet text=\"If international friendships shift this turn, theses estimations of risk may be invalid.\"></expandable-snippet>";
				shadow.getElementById("plot_risks").innerHTML = riskText;
			};
			let setVal = function (vals, element) {
				if (element != undefined) {
					vals.sort();
					for (let v of vals) {
						let opt = document.createElement("option");
						opt.innerHTML = v;
						opt.setAttribute("value", v.replace(/\(.*\) /, ""));
						element.appendChild(opt);
					}
					element.addEventListener("change", stateDetailsAndRisk);
				}
			}
			let kingdoms = [];
			for (let k in g_data.kingdoms) if (g_data.kingdoms.hasOwnProperty(k) && k != kingdom.name) kingdoms.push(k);
			let characters = [];
			for (let k of g_data.characters) characters.push("(" + k.kingdom + ") " + k.name);
			let regions = [];
			for (let k of g_data.regions) if (k.type == "land") regions.push("(" + k.kingdom + ") " + k.name);
			setVal(kingdoms, shadow.getElementById("plot_target_kingdom"));
			setVal(characters, shadow.getElementById("plot_target_character"));
			setVal(regions, shadow.getElementById("plot_target_region"));
			stateDetailsAndRisk();
		});
		plotTypeSel.dispatchEvent(new Event("change"));

		// NATIONS
		let o = this;
		let kingdoms = shadow.getElementById("nations_nations");
		let relationships = shadow.getElementById("nations_relationships");
		for (let k in g_data.kingdoms) if (g_data.kingdoms.hasOwnProperty(k)) {
			if (k == kingdom.name) continue;
			let kdiv = document.createElement("zip-div");
			kdiv.innerHTML = `
				<select name="rel_${k}_attack">
					<option value="ATTACK">Attack their armies / navies.</option>
					<option value="NEUTRAL">Attack their armies / navies only in our regions.</option>
					<option value="DEFEND">Never attack their armies / navies.</option>
				</select>
				<select name="rel_${k}_refugees">
					<option value="ACCEPT">Accept their refugees.</option>
					<option value="REFUSE">Refuse their refugees.</option>
				</select>
				<select name="rel_${k}_construct">
					<option value="FORBID">Forbid them from constructing in our regions.</option>
					<option value="PERMIT">Permit them to construct in our regions.</option>
				</select>
				<select name="rel_${k}_tribute">
					<option value="0">Offer no tribute.</option>
					<option value="0.25">Offer 25% of our income as tribute.</option>
					<option value="0.33">Offer 33% of our income as tribute.</option>
					<option value="0.5">Offer 50% of our income as tribute.</option>
				</select>
				<select name="rel_${k}_cede">
					<option value="ACCEPT">Allow them to cede us regions.</option>
					<option value="REFUSE">Refuse regions they cede.</option>
				</select>
				<select name="rel_${k}_fealty">
					<option value="ACCEPT">Allow them to transfer us troops.</option>
					<option value="REFUSE">Refuse troops they transfer to us.</option>
				</select>
			`;
			kdiv.setAttribute("title", k);
			kdiv.setAttribute("background", g_data.kingdoms[k].color_bg);
			kdiv.setAttribute("color", g_data.kingdoms[k].color_fg);
			kingdoms.appendChild(kdiv);
		}
		shadow.getElementById("nations_newgift").addEventListener("click", ()=>this.addGift(shadow));
		let newletter = shadow.getElementById("nations_newletter");
		newletter.addEventListener("click", ()=>this.addLetter(shadow));
		let colocatedRulers = [];
		let rulerLocation = kingdom.getRuler().location;
		for (let k in g_data.kingdoms) if (g_data.kingdoms.hasOwnProperty(k)) {
			let rules = g_data.kingdoms[k].getRuler();
			if (k != kingdom.name && rules.location == rulerLocation)
			colocatedRulers.push(rules);
		}
		function sanitize(s) {
			return s.replace(/</g, "&lt").replace(/>/g, "&gt");
		}
		if (colocatedRulers.length > 0) {
			let rtc = shadow.getElementById("nations_rtc");
			rtc.innerHTML = "";
			let d = document.createElement("div");
			rtc.appendChild(d);
			let to = document.createElement("div");
			to.appendChild(document.createTextNode("To: "));
			let boxes = [];
			for (let r of colocatedRulers) {
				let label = document.createElement("label");
				let box = document.createElement("input");
				box.setAttribute("data-kingdom", r.kingdom);
				box.setAttribute("type", "checkbox");
				boxes.push(box);
				label.appendChild(box);
				label.appendChild(document.createTextNode(r.kingdom));
				to.appendChild(label);
			}
			rtc.appendChild(to);
			let textarea = document.createElement("textarea");
			rtc.appendChild(textarea);
			let submit = document.createElement("button");
			let checkForUpdate = function() {
				let req = new XMLHttpRequest();
				req.open("get", "https://empire-189013.appspot.com/entry/world?k=" + whoami + "&gid=" + gameId + "&password=" + password + "&t=" + g_data.date, true);
				req.onerror = function (e) {
					window.alert("Failed to communicate with the server.");
				};
				req.onload = function (ev) {
					if (req.status != 200) {
						window.alert("Failed to communicate with the server: " + req.status);
					} else {
						d.innerHTML = "";
						for (let msg of JSON.parse(req.response).rtc) {
							let m = document.createElement("div");
							msg.to.push(msg.from);
							msg.to.sort();
							m.innerHTML = "<b>(" + sanitize(msg.to.join(", ")) + ") " + sanitize(msg.from) + ": </b>" + sanitize(msg.text).replace(/\n/g, "<br/>");
							m.style.background = "linear-gradient(90deg, #fff -50%, " + g_data.kingdoms[msg.from].color_bg + " 350%)";
							d.appendChild(m);
						}
					}
				};
				req.send();
			}
			checkForUpdate();
			setInterval(checkForUpdate, 30000);
			submit.innerHTML = "Send";
			submit.addEventListener("click", function(e) {
				e.preventDefault();
				let data = {
					"to": [],
					"text": textarea.value
				};
				for (let b of boxes) {
					if (b.checked) data.to.push(b.getAttribute("data-kingdom"));
				}
				if (data.to.length > 0 && data.text.length > 0) {
					let req = new XMLHttpRequest();
					req.open("post", "https://empire-189013.appspot.com/entry/rtc?k=" + whoami + "&gid=" + gameId + "&password=" + password + "&t=" + g_data.date, true);
					req.onerror = function (e) {
						window.alert("Failed to communicate with the server.");
					};
					req.onload = function (ev) {
						if (req.status != 204) {
							window.alert("Failed to communicate with the server: " + req.status);
						}
						checkForUpdate();
					};
					req.send(JSON.stringify(data));
					textarea.value = "";
				}
				return false;
			});
			rtc.appendChild(submit);
		}
		{ // Cede regions.
			let rr = [];
			for (let r of g_data.regions) if (r.kingdom == kingdom.name && (r.noble == undefined || r.noble.name == undefined)) rr.push(r);
			rr.sort(function(a, b){ return a.name < b.name ? -1 : a.name > b.name ? 1 : 0});
			let cede = shadow.getElementById("nations_cede");
			let ks = [];
			for (let k in g_data.kingdoms) if (k != kingdom.name) ks.push(k);
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

		// ECONOMY
		let eTax = shadow.getElementById("economy_tax");
		let eRation = shadow.getElementById("economy_ration");
		let eBonus = shadow.getElementById("economy_recruit_bonus");
		let economyConsequences = shadow.getElementById("economy_consequences");
		let computeEconomyConsequences = function () {
			let taxRate = parseInt(eTax.value) / 100.0 - 1;
			let recruitRate = 0;
			if (parseInt(eBonus.value) == -1) recruitRate = -.5;
			else if (parseInt(eBonus.value) == -2) recruitRate = -1;
			else if (parseInt(eBonus.value) > 0) recruitRate = Math.log2(parseInt(eBonus.value)) * .5 + .5;
			let happiness = 0;
			if (parseInt(eTax.value) <= 100) happiness = (parseInt(eTax.value) - 125) / 25;
			else happiness = ((parseInt(eTax.value) - 100) / 25) * ((parseInt(eTax.value) - 100) / 25 + 1) / 2;
			if (parseInt(eRation.value) == 75) happiness -= 15;
			else if (parseInt(eRation.value) == 125) happiness += 10;
			economyConsequences.innerHTML = "";
			let baseRecruits = kingdom.calcRecruitment().v;
			let baseTaxation = kingdom.calcTaxation().v;
			let newRecruits = kingdom.calcRecruitment(recruitRate).v;
			let newTaxation = kingdom.calcTaxation(taxRate).v;
			let soldiers = 0;
			for (let army of g_data.armies) if (army.kingdom == kingdom.name && !contains(army.tags, "Higher Power")) soldiers += army.size;
			if (taxRate != 0) economyConsequences.innerHTML += "<p>" + ((taxRate > 0 ? "+" : "") + Math.round(taxRate * 100)) + "% Tax Income (~ " + (newTaxation > baseTaxation ? "+" : "") + Math.round(newTaxation - baseTaxation) + " gold)</p>";
			if (happiness != 0) economyConsequences.innerHTML += "<p>Popular unrest " + (happiness < 0 ? "decreases " + (-happiness) : "increases " + happiness) + " percentage points in our regions.</p>";
			if (recruitRate != 0) economyConsequences.innerHTML += "<p>" + ((recruitRate > 0 ? "+" : "") + Math.round(recruitRate * 100)) + "% Recruitment (~ " + (newRecruits > baseRecruits ? "+" : "") + Math.round(newRecruits - baseRecruits) + " recruits)</p>";
			if (recruitRate > 0) economyConsequences.innerHTML += "<p>Spend " + eBonus.value + " gold per 100 soldiers (~ " + Math.round(parseInt(eBonus.value) * (newRecruits + soldiers) / 100) + " gold total)</p>";
		}
		eTax.addEventListener("input", computeEconomyConsequences);	
		eBonus.addEventListener("input", computeEconomyConsequences);
		computeEconomyConsequences();
		let op = this;
		shadow.getElementById("economy_newtransfer").addEventListener("click", ()=>op.addEconomyRowOrder(shadow));
		shadow.getElementById("economy_newbribe").addEventListener("click", ()=>op.addBribe(shadow));
		let lastTime = 0;
		form.addEventListener("input", function() {
			op.checkWarnings(shadow);
			op.plannedMotions(shadow);
			lastTime = Date.now();
			setTimeout(function() {
				if (lastTime < Date.now() - 1000) {
					lastTime = Date.now();
					form.dispatchEvent(new Event("change"));
				}
			}, 1500);
		});
		form.addEventListener("change", function() {
			let req = new XMLHttpRequest();
			if (op.syncDisabled) return;
			if (op.currentlySubmitting) {
				if (op.submitQueued) return;
				op.submitQueued = true;
				setTimeout(function() {
					form.dispatchEvent(new Event("change"));
				}, 50); // Wait 50 ms and retry.
			} else {
				op.submitQueued = false;
				op.currentlySubmitting = true;
				req.open("post", "https://empire-189013.appspot.com/entry/orders?k=" + whoami + "&gid=" + gameId + "&password=" + password + "&t=" + g_data.date, true);
				//req.open("post",          "http://localhost:8080/entry/orders?k=" + whoami + "&gid=" + gameId + "&password=" + password + "&t=" + g_data.date, true);
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
				req.send(JSON.stringify({"orders": formToJSON(form.elements)}));
			}
		});
		let gothiVotes = g_data.kingdoms[whoami].calcGothiVotes();
		if (gothiVotes["Alyrja"].v == 0) shadow.getElementById("gothi_Alyrja").style.display = "none";
		if (gothiVotes["Rjinku"].v == 0) shadow.getElementById("gothi_Rjinku").style.display = "none";
		if (gothiVotes["Lyskr"].v == 0) shadow.getElementById("gothi_Lyskr").style.display = "none";
		if (gothiVotes["Syrjen"].v == 0) shadow.getElementById("gothi_Syrjen").style.display = "none";

		// GAME TAB
		shadow.getElementById("final_action").addEventListener("change", () => {
			let final_act_desc = {
				"continue_ruling": "",
				"salt_the_earth": "You lay waste to your own lands, decreasing their value to your enemies. All regions you control become treacherous, lose half their food, and become unruled. All your armies and navies become pirates. Your heroes are removed from the game. Your people overthrow you and you are removed from the game.",
				"graceful_abdication": "You set the affairs of your nation in order, increasing the value of your people. Your regions become unruled and your heroes are removed from the game. Popular and noble unrest in your core regions reverts to 10%. Your armies and navies disband. You step down gracefully from your ruling position and are removed from the game.",
				"exodus": "You gather those loyal to you and flee across the great sea to lands unknown. Your heroes are removed from the map and your regions become unruled. A fraction of population from your core regions goes with you, depending on your naval strength relative to your enemies. You depart from this region of the world, removing you from the game.",
				"last_stand": "You inspire your troops to make a heroic final stand. Your armies and navies fight with +400% efficacy this turn, and then become pirates. Your regions become unruled and your heroes are removed from the game. You are either killed in battle or slip away to live out a quiet life far from politics, removing you from the game.",
			};
			shadow.getElementById("final_action_details").innerHTML = final_act_desc[shadow.getElementById("final_action").value];
		});
		if (g_data.date % 7 != 0) for (let e of shadow.querySelectorAll("#score_switches input")) e.disabled = true;
		for (let value of g_data.kingdoms[whoami].getRuler().values) {
			shadow.getElementById("score_" + value).checked = true;
		}

		// Load Old Orders
		let req = new XMLHttpRequest();
		req.open("get", "https://empire-189013.appspot.com/entry/orders?k=" + whoami + "&gid=" + gameId + "&password=" + password + "&t=" + g_data.date, true);
		//req.open("get", "http://localhost:8080/entry/orders?k=" + whoami + "&gid=" + gameId + "&password=" + password + "&t=" + g_data.date, true);
		req.onerror = function (e) {
			console.log(e);
			op.syncDisabled = false;
		};
		req.onload = function (ev) {
			if (req.status != 200) {
				for (let c of g_data.characters) {
					if (c.orderhint == "" || c.orderhint == undefined) continue;
					if (c.captor == "" && c.kingdom != whoami) continue;
					else if (c.captor != "" && c.captor != whoami) continue;
					shadow.querySelector("[name=action_" + c.name.replace(/ /g, "_") + "]").value = c.orderhint;
				}
				for (let c of g_data.armies) {
					if (c.orderhint == "" || c.orderhint == undefined) continue;
					if (c.kingdom != whoami) continue;
					shadow.querySelector("[name=action_army_" + c.id + "]").value = c.orderhint;
				}
				if (g_data.kingdoms[whoami].taxratehint != undefined) shadow.querySelector("[name=economy_tax]").value = g_data.kingdoms[whoami].taxratehint;
				if (g_data.kingdoms[whoami].signingbonushint != undefined) shadow.querySelector("[name=economy_recruit_bonus]").value = g_data.kingdoms[whoami].signingbonushint;
				shadow.querySelector("[name=economy_recruit_bonus]").dispatchEvent(new CustomEvent("input"));
				if (g_data.kingdoms[whoami].rationhint != undefined) shadow.querySelector("[name=economy_rations]").value = g_data.kingdoms[whoami].rationhint;
				for (let k in g_data.kingdoms) {
					if (k == whoami) continue;
					shadow.querySelector("[name=rel_" + k + "_attack]").value = g_data.kingdoms[whoami].relationships[k].battle;
					shadow.querySelector("[name=rel_" + k + "_refugees]").value = g_data.kingdoms[whoami].relationships[k].refugees;
					shadow.querySelector("[name=rel_" + k + "_construct]").value = g_data.kingdoms[whoami].relationships[k].construct;
					shadow.querySelector("[name=rel_" + k + "_tribute]").value = g_data.kingdoms[whoami].relationships[k].tribute;
					shadow.querySelector("[name=rel_" + k + "_cede]").value = g_data.kingdoms[whoami].relationships[k].cede;
					shadow.querySelector("[name=rel_" + k + "_fealty]").value = g_data.kingdoms[whoami].relationships[k].fealty;
				}
				for (let gothi in g_data.kingdoms[whoami].gothi) {
					if (g_data.kingdoms[whoami].gothi.hasOwnProperty(gothi) && g_data.kingdoms[whoami].gothi[gothi]) {
						shadow.querySelector("[name=gothi_" + gothi + "]").checked = true;
					}
				}
				op.checkWarnings(shadow);
				op.plannedMotions(shadow);
				op.syncDisabled = false;
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
					op.addLetter(shadow);
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
							e[i].checked = e[i].value == resp[p];
							e[i].dispatchEvent(new CustomEvent("change"));
						}
					}
				}
			}
			op.checkWarnings(shadow);
			op.plannedMotions(shadow);
			op.syncDisabled = false;
		};
		req.send();
		if (g_turndata.length - 1 != g_data.date) {
			for (let e of shadow.querySelectorAll("input, select, button")) e.disabled = true;
		}
	}

	getNewDivisionId() {
		return this.divisions++;
	}

	getArmyOptions(unit) {
		let r = g_data.regions[unit.location];
		let opts = [];
		if (unit.type == "army") {
			if (r.type == "land") {
				if (unit.calcStrength() > r.calcMinPatrolSize().v) {
					opts.push("Patrol " + r.name);
					opts.push("Stay in " + r.name);
				} else {
					opts.push("Stay in " + r.name);
					opts.push("Patrol " + r.name);
				}
			} else {
				opts.push("Stay in " + r.name);
			}
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
			if (r.type == "land") opts.push("Slay Civilians");
			for (let neighbor of r.getNeighbors()) opts.push("Travel to " + neighbor.name);
			if (r.population > 1) for (let neighbor of r.getNeighbors()) if (neighbor.type == "land") opts.push("Force civilians to " + neighbor.name);
		} else {
			opts.push("Stay in " + r.name);
			let nopts = [];
			for (let neighbor of r.getNeighbors()) nopts.push(neighbor.name);
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
		if (!contains(unit.tags, "Higher Power")) opts.push("Disband");
		return opts;
	};

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
			tr.appendChild(td);
			child.parentNode.insertBefore(tr, child.nextSibling);
			for (let c of g_data.characters) if (c.location == entity.location && c.captor == "") {
				for (let n of shadow.querySelectorAll("select[name=action_" + c.name.replace(/ /g, "_"))) {
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

	addEconomyRowOrder(shadow) {
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
			td2.appendChild(o.select("economy_to_" + id, dests));
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
			td4.innerHTML = Math.round(amount.value / 50 * 10) / 10;
		});
		td.appendChild(amount);
		td.appendChild(document.createTextNode("k"));
		td4.innerHTML = "0";
		tr.appendChild(td);
		tr.appendChild(td4);
		tbody.appendChild(tr);
	}

	addLetter(shadow) {
		let id = this.letterCount;
		this.letterCount++;
		let d = document.createElement("div");
		let to = document.createElement("div");
		to.appendChild(document.createTextNode("To: "));
		let textarea = document.createElement("textarea");
		let boxes = [];
		for (let k in g_data.kingdoms) if (g_data.kingdoms.hasOwnProperty(k)) {
			if (k == whoami) continue;
			let label = document.createElement("label");
			let box = document.createElement("input");
			box.setAttribute("name", "letter_" + id + "_to_" + k);
			box.setAttribute("data-kingdom", k);
			box.setAttribute("type", "checkbox");
			boxes.push(box);
			box.addEventListener("change", function() {
				let rulers = [];
				for (let b of boxes) if (b.checked) {
					let ruler = g_data.kingdoms[b.getAttribute("data-kingdom")].getRuler();
					rulers.push(ruler.honorific + " " + ruler.name);
				}
				textarea.innerHTML = rulers.join(", ") + ",";
			});
			label.appendChild(box);
			let sp = document.createElement("span");
			sp.appendChild(document.createTextNode(k));
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
	}

	checkWarnings(shadow) {
		for (let a of g_data.armies) {
			let o = shadow.querySelector("select[name=action_army_" + a.id + "]");
			if (o == undefined) continue;
			let warn = "";
			if (o.value.startsWith("Travel to ")) {
				let dest = undefined;
				for (let r of g_data.regions) if (r.name == o.value.replace("Travel to ", "")) dest = r;
				if (dest != undefined && (dest.type != "land" || g_data.kingdoms[dest.kingdom].calcRelationship(g_data.kingdoms[a.kingdom]) != "friendly") && !contains(a.tags, "Weathered") && (dest.climate == "treacherous" || (dest.climate == "seasonal" && (g_data.date % 52 < 13 || g_data.date % 52 >= 39)))) {
					warn = "(will suffer 25% attrition due to climate)";
				}
				if (a.type == "navy" && dest.type == "land" && g_data.regions[a.location].type == "land" && !g_data.tivar.deluge) {
					warn += "(navies can only move between land regions during the Deluge)";
				}
				if (a.type == "navy" && dest.type == "land" && dest.kingdom != a.kingdom && g_data.kingdoms[dest.kingdom].relationships[a.kingdom].battle != "DEFEND" && !g_data.tivar.deluge) {
					warn += "(navies do not contribute to land battles except during the Deluge, and are vulnerable to capture)";
				}
			} else if (o.value.startsWith("Merge into army")) {
				let ot = undefined;
				for (let aa of g_data.armies) if (aa.id == parseInt(o.value.replace("Merge into army ", ""))) ot = aa;
				if (ot.tags[0] != a.tags[0] || ot.tags[1] != a.tags[1]) warn = "(67% of the army will merge, 33% will turn to piracy)";
			} else if (o.value.startsWith("Slay")) {
				warn = "(will anger the Church of Iruhan)";
			} else if (o.value.startsWith("Patrol")) {
				if (a.calcStrength().v < g_data.regions[a.location].calcMinPatrolSize().v) {
					warn = "(army may be too small to patrol)";
				}
			} else if (o.value.startsWith("Oust")) {
				if (a.calcStrength().v < g_data.regions[a.location].calcMinPatrolSize().v) {
					warn = "(army may be too small to oust)";
				}
			} else if (o.value.startsWith("Conquer")) {
				if (a.calcStrength().v < g_data.regions[a.location].calcMinConquestSize().v) {
					warn = "(army may be too small to conquer)";
				}
				if (g_data.kingdoms[whoami].relationships[g_data.regions[a.location].kingdom].battle != "ATTACK") {
					warn += "(conquest requires being ordered to attack " + g_data.regions[a.location].kingdom + " armies/navies)";
				}
			} else if (o.value.startsWith("Raze")) {
				if (a.calcStrength().v < g_data.regions[a.location].calcMinConquestSize().v / 2) {
					warn = "(army may be too small to raze)";
				}
			}
			shadow.getElementById("warning_army_" + a.id).innerHTML = warn;
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
			let o = shadow.querySelector("select[name=action_" + a.name.replace(/ /g, "_") + "]");
			if (o != undefined) {
				if (o.value.startsWith("Travel to ") || o.value.startsWith("Hide in ")) {
					let dest = undefined;
					for (let i = 0; i < g_data.regions.length; i++) if (g_data.regions[i].name == o.value.replace("Travel to ", "").replace("Hide in ", "")) dest = i;
					if (dest != undefined && dest != a.location) amotions[a.name] = dest;
				} else {
					amotions[a.name] = undefined;
				}
			}
		}
		updateMotions(amotions); /* map1.html */
	}
}
customElements.define("orders-pane", OrdersPane);
