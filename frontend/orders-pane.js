class OrdersPane extends HTMLElement {
	constructor() {
		super();
		this.divisions = 0;
		this.economyRowCount = 0;
		this.bribeRowCount = 0;
		this.giftRowCount = 0;
		this.letterCount = 0;
		this.plotCount = 0;
		this.currentlySubmitting = false;
		this.submitQueued = false;
		this.syncDisabled = true;
	}
	connectedCallback() {
		let kingdom = g_data.kingdoms[this.getAttribute("kingdom")];
		let shadow = this.attachShadow({mode: "open"});
		let undeadCount = 0;
		for (let c of g_data.cult_caches) {
			if (c.eligible_nations.includes(whoami)) undeadCount += c.size;
		}
		let html = `
			<div id="tabs">
				<div id="tab_units" class="tab_units">Units</div>
				<div id="tab_plots" class="tab_plots">Plots</div>
				<div id="tab_tiecel" class="tab_tiecel">Tiecel</div>
				<div id="tab_nations" class="tab_nations">Nations</div>
				<div id="tab_economy" class="tab_economy">Economy</div>
				<div id="tab_game" class="tab_game">Game</div>
			</div>
			<form id="form">
				<table id="content_units">
					<tbody id="units">
						<tr><th>Who</th><th></th><th>Action</th></tr>
						<tr id="table_nobles"><th colspan="2">Nobles</th></tr>
						<tr id="table_armies"><th colspan="2">Armies</th></tr>
						<tr id="table_navies"><th colspan="2">Navies</th></tr>
					</tbody>
				</table>
				<div id="content_plots">
					<h1>Known Plots</h1>
					<div id="plot_plots"></div>
					<div id="plot_newplots"></div>
					<div id="plot_newplot">Instigate New Plot</div>
					<h1>Spy Rings</h1>
					<div id="plot_rings"></div>
					<h1>Cult</h1>
					<label><input type="checkbox" name="plot_cult" ${kingdom.loyal_to_cult ? "checked=\"true\" disabled=\"true\"" : ""}/>Swear loyalty to the Cult</label>
					<expandable-snippet text="In exchange for loyalty, the Cult will give us ${Math.round(undeadCount)} undead soldiers. The Cult will gain access to any regions we control, and you should continue to expand their influence by annexing additional territory."></expandable-snippet>
					<hr/>
					<h1>Gothi Votes</h1>
					<label id="gothi_alyrja"><input type="checkbox" name="gothi_alyrja"/>Vote to summon the <tooltip-element tooltip="The warwinds stops sea trade, destroys 25% of any army or navy at sea, and blows vessels in sea regions to random adjacent regions. It will start to destroy crops worldwide after 2 weeks of activity.">Warwinds</tooltip-element></label>
					<label id="gothi_rjinku"><input type="checkbox" name="gothi_rjinku"/>Vote to summon the <tooltip-element tooltip="Each construction has a 33% chance of being destroyed each week the quake is active. It will start to destroy crops worldwide after 2 weeks of activity.">Quake</tooltip-element></label>
					<label id="gothi_syrjen"><input type="checkbox" name="gothi_syrjen"/>Vote to summon the <tooltip-element tooltip="The deluge destroys 25% of any army or navy travelling into a land region, allows navies to traverse land regions and participate in battles there, and prevents navies from being captured. It will start to destroy crops worldwide after 2 weeks of activity.">Deluge</tooltip-element></label>
					<label id="gothi_lyskr"><input type="checkbox" name="gothi_lyskr"/>Vote to summon the <tooltip-element tooltip="The veil makes all armies, navies, and characters hidden from other rulers. It will start to destroy crops worldwide after 2 weeks of activity.">Veil</tooltip-element></label>
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
				<div id="content_tiecel">
					<h1 id="doctrine_header">Church Doctrine</h1>
					<div id="doctrine_switches"></div>
				</div>
				<div id="content_economy">
					<h1>Economic Controls</h1>
					<label>Taxation: <input id="economy_tax" name="economy_tax" type="range" min="0" max="200" step="25" value="100"/></label>
					<label>Shipbuilding: <input id="economy_ship" name="economy_ship" type="range" min="0" max="5" step="1" value="5"/></label>
					<label>Rationing: <input id="economy_ration" name="economy_ration" type="range" min="75" max="125" step="25" value="100"/></label>
					<label>Soldier Bonus Pay: <input id="economy_recruit_bonus" name="economy_recruit_bonus" type="range" min="-2" max="16" step="1" value="0"/></label>
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
					<div id="end_vote_div">
						<h1 class="alert">Game Extension Vote</h1>
						<select name="end_vote">
							<option value="end">End the game</option>
							<option value="extend">Extend the game by 6 turns</option>
						</select>
					</div>
					<h1>Final Actions</h1>
					<expandable-snippet text="Final Actions are powerful actions that end the story of your nation and remove you from the game."></expandable-snippet>
					<select id="final_action" name="final_action">
						<option value="continue_ruling">Continue Ruling</option>
						<option value="exodus">Exodus</option>
						<option value="abdicate">Gracefully Abdicate</option>
						<option value="last_stand">Last Stand</option>
						<option value="salt_the_earth">Salt the Earth</option>
					</select>
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
			.tab_tiecel {
				border-bottom: 3px solid purple;
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
			.tab_game {
				border-bottom: 3px solid grey;
				border-left: 1.5px solid white;
				border-right: 1.5px solid white;
			}
			.alert::before {
				content: url('data:image/svg+xml; utf8, <svg viewBox="2 2 20 20" xmlns="http://www.w3.org/2000/svg"><path fill="red" d="M13,13H11V7H13M13,17H11V15H13M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2Z" /></svg>');
				width: 0.8em;
				height: 0.8em;
				margin-right: 0.2em;
				display: inline-block;
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
			#plot_newplot, #economy_newtransfer, #nations_newletter, #economy_newbribe, #nations_newgift {
				cursor: pointer;
				text-align: center;
				color: #00f;
			}
			#plot_rings label {
				display: block;
			}
			table {
				width: 100%;
			}
			table tr td:last-child {
				text-align: right;
			}
			#doctrine_switches ul {
				margin-top: 0;
				font-size: 90%;
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
			#nations_letters > div > div:first-child, #plot_newplots > div > div {
				display: flex;
				flex-direction: row;
				flex-wrap: wrap;
				justify-content: space-between;
			}
			#nations_letters label, #plot_newplots label {
				background-color: #eee;
				border-radius: .5em;
				margin-left: .3em;
				margin-right: .3em;
				padding-left: .3em;
				padding-right: .3em;
			}
			#nations_letters label input[type=checkbox]:checked ~ span {
				text-decoration: underline;
			}
			#plot_plots h2 {
				font-size: 110%;
				text-align: center;
				border-top: 1px solid black;
				margin-top: 1em;
				background-color: #fee;
			}
			#plot_plots .objective {
				font-weight: bold;
				text-align: center;
			}
			#plot_plots .trigger {
				font-size: 120%;
			}
			#plot_plots .conspirators {
				display: flex;
				flex-direction: row;
				flex-wrap: wrap;
				justify-content: space-between;
			}
			#plot_plots .conspirators label {
				background-color: #eee;
				border-radius: .5em;
				margin-left: .3em;
				margin-right: .3em;
				padding-left: .3em;
				padding-right: .3em;
			}
			#plot_plots .conspirators label.selected {
				font-weight: bold;
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
		const tabList = ["units", "plots", "tiecel", "nations", "economy", "game"];
		let changeTab = function (tab) {
			shadow.getElementById("tabs").className = "tab_" + tab;
			for (let t of tabList) {
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
		for (let t of tabList) {
			shadow.getElementById("content_" + t).style.display = "none";
			shadow.getElementById("tab_" + t).addEventListener("click", ((tt)=>()=>changeTab(tt))(t));
		}
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
			if (contains(unit.tags, "Cardinal") && g_data.regions[unit.location].name == "Sancta Civitate") opts.push("Inspire the Faithful");
			if (r.kingdom == unit.kingdom) {
				opts.push("Govern " + r.name);
				if (r.noble.name == undefined && !g_data.kingdoms[unit.kingdom].tags.includes("Republic")) {
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
			if (r.type == "land") {
				if (r.isCoastal()) opts.push("Build Shipyard");
				opts.push("Build Fortifications");
				for (let i of ["Chalice of Compassion", "Sword of Truth", "Tapestry of People", "Vessel of Faith"]) opts.push("Build Temple (" + i + ")");
				for (let i of ["Alyrja", "Lyskr", "Rjinku", "Syrjen"]) opts.push("Build Temple (" + i + ")");
				for (let i of ["Flame of Kith", "River of Kuun"]) opts.push("Build Temple (" + i + ")");
			}
			opts.push("Stay in " + r.name);
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
			} else {
				if (!g_data.kingdoms[whoami].score_profiles_locked) {
					for (let profile of Object.keys(g_scoreProfiles).sort()) {
						if (!g_scoreProfiles[profile].selectable) continue;
						if (g_data.kingdoms[whoami].profiles.includes(profile)) {
							opts.push("Reflect on " + profile.toLowerCase() + " (remove)");
						} else {
							opts.push("Reflect on " + profile.toLowerCase() + " (add)");
						}
					}
				}
			}
			return opts;
		};

		let unitTable = shadow.getElementById("units");
		for (let unit of g_data.characters) {
			if (unit.kingdom == kingdom.name) {
				let who = document.createElement("div");
				let whor = document.createElement("report-link");
				whor.setAttribute("href", "character/" + unit.name);
				whor.innerHTML = unit.name;
				who.appendChild(whor);
				addRow(unitTable, who, undefined, this.select("action_" + unit.name.replace(/[ ']/g, "_"), getCharacterOptions(unit)), shadow.getElementById("table_nobles"));
			}
		}
		let navyCount = 0;
		let armyCount = 0;
		let nobleCount = 0;
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
				addCell(this.select("action_noble_" + r.id, this.getNobleOptions(r)));
				unitTable.insertBefore(tr, shadow.getElementById("table_armies"));
				nobleCount++;
			}
		}
		if (armyCount == 0) shadow.getElementById("table_armies").style.display = "none";
		if (navyCount == 0) shadow.getElementById("table_navies").style.display = "none";
		if (nobleCount == 0) shadow.getElementById("table_nobles").style.display = "none";
		changeTab("units");

		// PLOT TAB
		{
			let computePlotSuccessChances = () => {
				for (let plot of g_data.plots) {
					let support_mods = 0;
					let sabotage_mods = 0;
					let targetRegion = plot.getTargetRegion();
					if (targetRegion != undefined) {
						for (let ring of shadow.querySelectorAll("#plot_rings select")) {
							if (ring.name.startsWith("spyring_type_")) continue;
							let realRing = g_data.spy_rings.find(r => r.location == parseInt(ring.name.replace("spyring_", "")));
							let strength = realRing.calcStrengthIn(targetRegion.id)
							if (realRing.involved_in_plot_id == plot.plot_id) {
								// Discount the strength of the ring from the apparent success strength. Add it back based on whether the ring is currently ordered to support or sabotage, etc.
								support_mods -= strength;
							}
							if (parseInt(ring.value) != plot.plot_id) continue;
							if (shadow.querySelector("[name=" + ring.name.replace("spyring_", "spyring_type_") + "]").value == "SUPPORTING") {
								support_mods += strength;
							} else {
								sabotage_mods += strength * 2;
							}
						}
					}
					let minSuccess = Math.floor(100 * Math.max(0, (plot.power_hint * 0.9 + support_mods) / (plot.power_hint_total * 1.1 + sabotage_mods + support_mods)));
					let maxSuccess = Math.ceil(100 * Math.min(1, (plot.power_hint * 1.1 + support_mods) / (plot.power_hint_total * 0.9 + sabotage_mods + support_mods)));
					shadow.getElementById("power_" + plot.plot_id).innerHTML = minSuccess + "% to " + maxSuccess + "% chance of success." + (targetRegion == undefined ? " Because the target's location is unknown, it is not possible to update these numbers interactively." : "");
				}
			}
			let plots = shadow.getElementById("plot_plots");
			for (let plot of g_data.plots) {
				let pr = document.createElement("div");
				pr.innerHTML = `
					<h2>Operation ${rainbowCode(plot.plot_id)}</h2>
					<div class="objective">${plot.getObjective()}</div>
					<div id="power_${plot.plot_id}"></div>
					<label class="trigger"><input type="checkbox" name="plot_execute_${plot.plot_id}"></input>Trigger Operation!</label>
					<div class="conspirators"></div>`
				let cdiv = pr.querySelector(".conspirators");
				let invitees = [];
				for (let k in g_data.kingdoms) invitees.push(k);
				invitees.sort();
				for (let k of invitees) {
					let l = document.createElement("label");
					let i = document.createElement("input");
					i.setAttribute("type", "checkbox");
					i.setAttribute("name", "plot_invite_" + plot.plot_id + "_" + k);
					l.appendChild(i);
					l.appendChild(document.createTextNode(k));
					if (plot.conspirators.includes(k)) {
						i.setAttribute("disabled", "true");
						i.setAttribute("checked", "true");
						l.className = "selected";
					}
					cdiv.appendChild(l);
				}
				plots.appendChild(pr);
			}
			let rings = shadow.getElementById("plot_rings");
			for (let r of g_data.spy_rings) if (whoami == r.nation) {
				let l = document.createElement("label")
				let link = document.createElement("report-link");
				link.setAttribute("href", "region/" + g_data.regions[r.location].name);
				link.appendChild(document.createTextNode(g_data.regions[r.location].name));
				l.appendChild(link);
				l.appendChild(document.createTextNode(": "));
				let s1 = this.selectPretty("spyring_type_" + r.location, [{"name": "Support", "value": "SUPPORTING"}, {"name": "Sabotage", "value": "SABOTAGING"}]);
				s1.addEventListener("change", computePlotSuccessChances);
				l.appendChild(s1);
				let plotOpts = [{"name": "[Nothing]", "value": -1}];
				for (let p of g_data.plots) plotOpts.push({"name": "Operation " + rainbowCode(p.plot_id), "value": p.plot_id});
				let s2 = this.selectPretty("spyring_" + r.location, plotOpts);
				s2.addEventListener("change", computePlotSuccessChances);
				l.appendChild(s2);
				rings.appendChild(l);
			}
			shadow.getElementById("plot_newplot").addEventListener("click", ()=>this.addPlot(shadow));
			computePlotSuccessChances();
		}
		if (g_data.cult_triggered) shadow.querySelector("[name=plot_cult]").disabled = true;

		// TIECEL TAB
		{
			let doctrines = shadow.getElementById("doctrine_switches");
			for (let doctrine in doctrineDescriptions) {
				let l = document.createElement("label");
				let c = document.createElement("input");
				c.setAttribute("type", "checkbox");
				c.setAttribute("name", "church_" + doctrine);
				c.setAttribute("id", "church_" + doctrine);
				l.appendChild(c);
				l.appendChild(document.createTextNode(titleCase(doctrine)));
				doctrines.appendChild(l);
				let d = document.createElement("ul");
				for (let desc of doctrineDescriptions[doctrine]) {
					let li = document.createElement("li");
					li.appendChild(document.createTextNode(desc));
					d.appendChild(li);
				}
				doctrines.appendChild(d);
			}
			let tiecel = undefined;
			for (let c of g_data.characters) {
				if (c.tags.includes("Tiecel")) tiecel = c;
			}
			if (tiecel == undefined || tiecel.kingdom != whoami) shadow.getElementById("tab_tiecel").style.display = "none";
		}

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
			kdiv.setAttribute("background", getColor(k));
			kdiv.setAttribute("color", getForegroundColor(k));
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
				req.open("get", g_server + "/entry/world?k=" + whoami + "&gid=" + gameId + "&password=" + password + "&t=" + g_data.date, true);
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
							m.style.background = "linear-gradient(90deg, #fff -50%, " + getColor(msg.from) + " 350%)";
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
					req.open("post", g_server + "/entry/rtc?k=" + whoami + "&gid=" + gameId + "&password=" + password + "&t=" + g_data.date, true);
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
		let eShip = shadow.getElementById("economy_ship");
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
			if (parseInt(eTax.value) <= 100) happiness = (parseInt(eTax.value) - 125) / 25 * 4;
			else happiness = ((parseInt(eTax.value) - 100) / 25) * ((parseInt(eTax.value) - 100) / 25 + 1) * 2;
			if (parseInt(eRation.value) == 75) happiness += 15;
			else if (parseInt(eRation.value) == 125) happiness -= 10;
			economyConsequences.innerHTML = "";
			let baseRecruits = kingdom.calcRecruitment().v;
			let baseTaxation = kingdom.calcTaxation().v;
			let newRecruits = kingdom.calcRecruitment(recruitRate).v;
			let newTaxation = kingdom.calcTaxation(taxRate).v;
			let soldiers = 0;
			let consumptionRate = parseInt(eRation.value) - 100;
			let shipyardCount = 0;
			for (let region of g_data.regions) if (region.kingdom == kingdom.name) for (let c of region.constructions) if (c.type == "shipyard") shipyardCount++;
			for (let army of g_data.armies) if (army.kingdom == kingdom.name && !contains(army.tags, "Higher Power")) soldiers += army.size;
			if (taxRate != 0) economyConsequences.innerHTML += "<p>" + ((taxRate > 0 ? "+" : "") + Math.round(taxRate * 100)) + "% Tax Income (~ " + (newTaxation > baseTaxation ? "+" : "") + Math.round(newTaxation - baseTaxation) + " gold)</p>";
			economyConsequences.innerHTML += "<p>" + (shipyardCount * parseInt(eShip.value)) + " new warships and +" + (shipyardCount * (5 - parseInt(eShip.value))) + " gold.</p>";
			if (happiness != 0) economyConsequences.innerHTML += "<p>Popular unrest " + (happiness < 0 ? "decreases " + (-happiness) : "increases " + happiness) + " percentage points in our regions.</p>";
			if (recruitRate != 0) economyConsequences.innerHTML += "<p>" + ((recruitRate > 0 ? "+" : "") + Math.round(recruitRate * 100)) + "% Recruitment (~ " + (newRecruits > baseRecruits ? "+" : "") + Math.round(newRecruits - baseRecruits) + " recruits)</p>";
			if (recruitRate > 0) economyConsequences.innerHTML += "<p>Spend " + eBonus.value + " gold per 100 soldiers (~ " + Math.round(parseInt(eBonus.value) * (newRecruits + soldiers) / 100) + " gold total)</p>";
			if (consumptionRate != 0) economyConsequences.innerHTML += "<p>Regions consume " + (consumptionRate > 0 ? "+" : "") + consumptionRate + "% food.</p>";
		}
		eTax.addEventListener("input", computeEconomyConsequences);	
		eShip.addEventListener("input", computeEconomyConsequences);	
		eRation.addEventListener("input", computeEconomyConsequences);	
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
				req.open("post", g_server + "/entry/orders?k=" + whoami + "&gid=" + gameId + "&password=" + password + "&t=" + g_data.date, true);
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
		if (gothiVotes["Alyrja"].v == 0) shadow.getElementById("gothi_alyrja").style.display = "none";
		if (gothiVotes["Rjinku"].v == 0) shadow.getElementById("gothi_rjinku").style.display = "none";
		if (gothiVotes["Lyskr"].v == 0) shadow.getElementById("gothi_lyskr").style.display = "none";
		if (gothiVotes["Syrjen"].v == 0) shadow.getElementById("gothi_syrjen").style.display = "none";

		// GAME TAB
		shadow.getElementById("final_action").addEventListener("change", () => {
			let final_act_desc = {
				"continue_ruling": "",
				"salt_the_earth": "You lay waste to your own lands, decreasing their value to your enemies. All regions you control become treacherous, lose half their food, and become unruled. All your armies and navies become pirates. Your heroes are removed from the game. Your people overthrow you and you are removed from the game.",
				"abdicate": "You set the affairs of your nation in order, increasing the value of your people. Your regions become unruled and your heroes are removed from the game. Popular and noble unrest in your core regions reverts to 10%. Your armies and navies disband. You step down gracefully from your ruling position and are removed from the game.",
				"exodus": "You gather those loyal to you and flee across the great sea to lands unknown. Your heroes are removed from the map and your regions become unruled. A fraction of population from your core regions goes with you, depending on your naval strength relative to your enemies. You depart from this region of the world, removing you from the game.",
				"last_stand": "You inspire your troops to make a heroic final stand. Your armies and navies fight with +400% efficacy this turn, and then become pirates. Your regions become unruled and your heroes are removed from the game. You are either killed in battle or slip away to live out a quiet life far from politics, removing you from the game.",
			};
			shadow.getElementById("final_action_details").innerHTML = final_act_desc[shadow.getElementById("final_action").value];
		});
		if (g_data.date >= 26 && (g_data.date - 26) % 6 == 0) {
			shadow.querySelector("#tab_game").classList.add("alert");
		} else {
			shadow.querySelector("#end_vote_div").style.display = "none";
		}

		// Load Old Orders
		let req = new XMLHttpRequest();
		req.open("get", g_server + "/entry/orders?k=" + whoami + "&gid=" + gameId + "&password=" + password + "&t=" + g_data.date, true);
		//req.open("get", "http://localhost:8080/entry/orders?k=" + whoami + "&gid=" + gameId + "&password=" + password + "&t=" + g_data.date, true);
		req.onerror = function (e) {
			console.log(e);
			op.syncDisabled = false;
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
				if (g_data.kingdoms[whoami].taxratehint != undefined) shadow.querySelector("[name=economy_tax]").value = g_data.kingdoms[whoami].taxratehint;
				if (g_data.kingdoms[whoami].shipratehint != undefined) shadow.querySelector("[name=economy_ship]").value = g_data.kingdoms[whoami].shipratehint;
				if (g_data.kingdoms[whoami].signingbonushint != undefined) shadow.querySelector("[name=economy_recruit_bonus]").value = g_data.kingdoms[whoami].signingbonushint;
				if (g_data.kingdoms[whoami].rationhint != undefined) shadow.querySelector("[name=economy_ration]").value = g_data.kingdoms[whoami].rationhint;
				shadow.querySelector("[name=economy_recruit_bonus]").dispatchEvent(new CustomEvent("input"));
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
						shadow.querySelector("[name=gothi_" + gothi.toLowerCase() + "]").checked = true;
					}
				}
				for (let doctrine of g_data.church.doctrines) {
					shadow.querySelector("[name=church_" + doctrine + "]").checked = true;
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
				op.calcPlo
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
				} else if (p.startsWith("plot_new_type")) {
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
			for (let neighbor of r.getNeighbors()) opts.push("Travel to " + neighbor.name);
			if (r.population > 1) for (let neighbor of r.getNeighbors()) if (neighbor.type == "land") opts.push("Force civilians to " + neighbor.name);
		} else {
			opts.push("Stay in " + r.name);
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
		if (!contains(unit.tags, "Higher Power")) opts.push("Disband");
		return opts;
	};

	getNobleOptions(r) {
		let opts = [];
		opts.push("Relax", "Soothe Population", "Levy Tax", "Conscript Recruits");
		if (r.isCoastal()) opts.push("Build Shipyard");
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

	addPlot(shadow) {
		let id = this.plotCount;
		this.plotCount++;
		if (this.plotCount >= 5) {
			shadow.getElementById("plot_newplot").style.display = "none";
		}
		let d = document.createElement("div");
		let targetProviderNone = () => [];
		let targetProviderCharacter = () => g_data.characters.map(c => { return {"name": "(" + c.kingdom + ") " + c.name, "value": c.name}});
		let targetProviderRegion = () => g_data.regions.map(r => { return {"name": "(" + r.kingdom + ") " + r.name, "value": r.name}});
		let targetProviderRegionNoble = () => g_data.regions.filter(r => r.noble.name != undefined).map(r => { return {"name": "(" + r.kingdom + ") " + r.name, "value": r.name}});
		let targetProviderNation = () => Object.keys(g_data.kingdoms).map(k => { return {"name": k, "value": k}});
		let plotTypes = [
			{"name": "", "value": "", "targetType": targetProviderNone},
			{"name": "Assassinate", "value": "ASSASSINATE", "targetType": targetProviderCharacter},
			{"name": "Burn a shipyard in", "value": "BURN_SHIPYARD", "targetType": targetProviderRegion},
			{"name": "Sabotage fortifications in", "value": "SABOTAGE_FORTIFICATIONS", "targetType": targetProviderRegion},
			{"name": "Spoil food in", "value": "SPOIL_FOOD", "targetType": targetProviderRegion},
			{"name": "Spoil crops in", "value": "SPOIL_CROPS", "targetType": targetProviderRegion},
			{"name": "Incite popular unrest in", "value": "INCITE_UNREST", "targetType": targetProviderRegion},
			{"name": "Prevent food transfers in", "value": "PIN_FOOD", "targetType": targetProviderRegion},
			{"name": "Murder the noble of", "value": "MURDER_NOBLE", "targetType": targetProviderRegionNoble},
			{"name": "Poison noble relations in", "value": "POISON_RELATIONS", "targetType": targetProviderRegionNoble},
			{"name": "Sing the praises of", "value": "PRAISE", "targetType": targetProviderNation},
			{"name": "Denounce the deeds of", "value": "DENOUNCE", "targetType": targetProviderNation},
			{"name": "Intercept communications of", "value": "INTERCEPT_COMMUNICATIONS", "targetType": targetProviderNation},
			{"name": "Gather intelligence on", "value": "SURVEY_NATION", "targetType": targetProviderNation},
		];
		let sel = this.selectPretty("plot_new_type_" + id, plotTypes);
		d.appendChild(sel);
		let targetSel = document.createElement("select");
		targetSel.setAttribute("name", "plot_new_target_" + id);
		d.appendChild(targetSel);
		sel.addEventListener("change", () => {
			while (targetSel.firstChild) targetSel.removeChild(targetSel.firstChild);
			let plotType = undefined;
			for (let o of plotTypes) if (o.value == sel.value) plotType = o;
			for (let o of plotType.targetType().sort((a, b) => a.name.localeCompare(b.name))) {
				let opt = document.createElement("option");
				opt.setAttribute("value", o.value);
				opt.appendChild(document.createTextNode(o.name));
				targetSel.appendChild(opt);
			}
		});
		let to = document.createElement("div");
		to.appendChild(document.createTextNode("Conspirators: "));
		let boxes = [];
		for (let k in g_data.kingdoms) if (g_data.kingdoms.hasOwnProperty(k)) {
			if (k == whoami) continue;
			let label = document.createElement("label");
			let box = document.createElement("input");
			box.setAttribute("name", "plot_new_involve_" + k + "_" + id);
			box.setAttribute("type", "checkbox");
			boxes.push(box);
			label.appendChild(box);
			let sp = document.createElement("span");
			sp.appendChild(document.createTextNode(k));
			label.appendChild(sp);
			to.appendChild(label);
		}
		d.appendChild(to);
		d.appendChild(document.createElement("hr"));
		shadow.getElementById("plot_newplots").appendChild(d);
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
			if (o == undefined) conitinue;
			let source = g_data.armies[parseInt(shadow.querySelector("[name=div_parent_" + i + "]").value)];
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
			} else if (o.startsWith("Patrol")) {
				if (a.calcStrength().v < g_data.regions[a.location].calcMinPatrolSize().v) {
					warn += " (army may be too small to patrol)";
				}
				if (getNation(a.kingdom).calcRelationship(getNation(g_data.regions[a.location].kingdom)) != "friendly") {
					warn += " (armies can only patrol friendly regions)";
				}
			} else if (o.startsWith("Oust")) {
				if (a.calcStrength().v < g_data.regions[a.location].calcMinPatrolSize().v) {
					warn = " (army may be too small to oust)";
				}
			} else if (o.startsWith("Conquer")) {
				if (a.calcStrength().v < g_data.regions[a.location].calcMinConquestSize().v) {
					warn = " (army may be too small to conquer)";
				}
				if (g_data.regions[a.location].kingdom != "Unruled" && g_data.kingdoms[whoami].relationships[g_data.regions[a.location].kingdom].battle != "ATTACK") {
					warn += " (conquest requires being ordered to attack " + g_data.regions[a.location].kingdom + " armies/navies)";
				}
			} else if (o.startsWith("Raze")) {
				if (a.calcStrength().v < g_data.regions[a.location].calcMinConquestSize().v / 2) {
					warn = " (army may be too small to raze)";
				}
			}
			entry.w.innerHTML = warn;
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
		updateMotions(amotions, dmotions); /* map1.html */
	}
}
customElements.define("orders-pane", OrdersPane);
