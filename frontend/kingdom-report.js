// <kingdom-report>
// Attributes:
//   name      - the name of the kingdom

class KingdomReport extends HTMLElement {
	constructor() { super(); }
	connectedCallback() {
		let kname = this.getAttribute("kingdom");
		let kingdom = undefined;
		for (var k in g_data.kingdoms) {
			if (k == kname) kingdom = g_data.kingdoms[k];
		}
		let shadow = this.attachShadow({mode: "open"});

		let stateReligion = kingdom.calcStateReligion();
		let stateReligionShares = kingdom.calcStateReligionWeights();
		let stateReligionSharesText = [];
		for (let r in stateReligionShares) {
			if (!stateReligionShares.hasOwnProperty(r)) continue;
			stateReligionSharesText.push(r + ": " + Math.round(stateReligionShares[r]) + " souls");
		}
		let stateReligionTooltip = "State religion is the most popular religion and ideology among the regions ruled by a nation. " + kingdom.name + " rules: " + stateReligionSharesText.join(", ") + ". ";
		stateReligionTooltip += {
			"Iruhan (Sword of Truth)": "As a consequence of their widespread devotion to the Sword of Truth ideology, the people of " + kingdom.name + " will sabotage fortifications in adjacent enemy regions.",
			"Iruhan (Vessel of Faith)": "As a consequence of their widespread devotion to the Vessel of Faith ideology, the people of " + kingdom.name + " are made happy whenever any region anywhere converts to the Vessel of Faith ideology.",
			"Iruhan (Chalice of Compassion)": "As a consequence of their widespread devotion to the Chalice of Compassion ideology, the people of " + kingdom.name + " are careful how much they eat, and consume only 85% as much food.",
			"Iruhan (Tapestry of People)": "As a consequence of their widespread devotion to the Tapestry of Peoples ideology, the people of " + kingdom.name + " produce additional taxes and recruits for every unique ideology in the nation.",
			"Northern (Alyrja)": "As a consequence of their widespread devotion to Alyrja, the people of " + kingdom.name + " have eyes and ears in all ruled and adjacent regions who detect all hidden units.",
			"Northern (Rjinku)": "As a consequence of their widespread devotion to Rjinku, the people of " + kingdom.name + " more eagerly join armies any week their nation engages in battle.",
			"Northern (Lyskr)": "As a consequence of their widespread devotion to Lyskr, the people of " + kingdom.name + " are more skilled at plotting.",
			"Northern (Syrjen)": "As a consequence of their widespread devotion to Syrjen, the people of " + kingdom.name + " are skilled traders and profit more from dominance in sea regions.",
			"Tavian (Flame of Kith)": "As a consequence of their widespread adoption of the Flame of Kith, the people of " + kingdom.name + " abduct a percentage of the population from each neighboring region that is ruled by another nation each week.",
			"Tavian (River of Kuun)": "As a consequence of their widespread adoption of the River of Kuun, the people of " + kingdom.name + " trade more freely with neighboring regions that are ruled by a different nation, increasing taxation in those regions.",
			"Company": "This nation does not rule any territory, and focuses on efficiency, making their plots more effective and lowering the cost of their armies and navies.",
		}[stateReligion];
		let gothiVotes = kingdom.calcGothiVotes();
		let gothiVotesText = [];
		let spell = {
			"Alyrja": "<tooltip-element tooltip=\"Once summoned, the Warwind makes every sea region treacherous and pushes all occupants of each sea region to a random adjacent region each week. Like all gothi spells, the Warwind destroys 3% of crops worldwide each week, requires either 2/3rds of all gothi or five gothi total (whichever is more) to activate, and has a 50% chance of continuing each week after the gothi cease summoning it.\">Warwind</tooltip-element>",
			"Rjinku": "<tooltip-element tooltip=\"Once summoned, the Quake has a one-third chance to destroy each construction each week. Like all gothi spells, it destroys 3% of crops worldwide each week, requires either 2/3rds of all gothi or five gothi total (whichever is more) to activate, and has a 50% chance of continuing each week after the gothi cease summoning it.\">Quake</tooltip-element>",
			"Lyskr": "<tooltip-element tooltip=\"Once summoned, the Veil hides all navies, armies, and characters from other rulers, but does not otherwise obstruct their activities or prevent battles. Like all gothi spells, it destroys 3% of crops worldwide each week, requires either 2/3rds of all gothi or five gothi total (whichever is more) to activate, and has a 50% chance of continuing each week after the gothi cease summoning it.\">Veil</tooltip-element>",
			"Syrjen": "<tooltip-element tooltip=\"Once summoned, the Deluge makes every land region treacherous and allows navies to move between land regions, participate in battles in land regions, and prevents them from being captured. Like all gothi spells, it destroys 3% of crops worldwide each week, requires either 2/3rds of all gothi or five gothi total (whichever is more) to activate, and has a 50% chance of continuing each week after the gothi cease summoning it.\">Deluge</tooltip-element>",
		};
		for (let r in gothiVotes) if (gothiVotes.hasOwnProperty(r) && gothiVotes[r].v > 0) {
			gothiVotesText.push("<div>Controls " + gothiVotes[r].v + " (" + Math.round(gothiVotes[r].v / gothiVotes[r].total * 100) + "%) of " + r + "'s gothi" + (kingdom.gothi[r] ? " and votes to summon the " + spell[r] + "!" : "." ) + "</div>");
		}
		let html = `
			<div id="abspos">
				<div>${kingdom.name}</div>
				<div id="summary">
					<div>${kingdom.tags[0]}</div><div>${kingdom.tags[1]}</div>
					<div>${num(kingdom.calcPopulation(), 0, 1/1000)}k</div><div>citizens</div>
					<div>${num(kingdom.calcTaxation())}</div><div>tax</div>
					<div>${num(kingdom.calcFoodWeeks(), 1)}</div><div>weeks food</div>
					<div>${num(kingdom.calcRecruitment())}</div><div>recruits</div>
					<div><tooltip-element tooltip="${stateReligionTooltip}">${stateReligion}</tooltip></div>
				</div>
				${kingdom.gold != -1 ? "<h1>Treasury: " + (Math.round(10 * kingdom.gold) / 10) + " gold</h1>" : ""}
				<h1 id="heading_notifications">Notifications</h1>
				<div id="notifications"></div>
				<h1>Friends</h1>
				<div id="friendly"></div>
				<h1>Enemies</h1>
				<div id="enemy"></div>
				<h1>Characters</h1>
				<div id="characters"></div>
				<h1>Regions</h1>
				<div id="regions"></div>
				<h1>Factional Standing</h1>
				<div>
					<div><tooltip-element tooltip="Pirate threat is the probability that pirates will appear in a region controlled by this nation during the resolution of the current turn. It is decreased by lowered unrest, by paying off the pirates to avoid the nation (or paying them to prefer another nation), by followers of the Northern (Alyrja) ideology, and by the use of nobles."><report-link href="pirates/home">Pirate</report-link> Threat:</tooltip-element> ${num(kingdom.calcPirateThreat(), 0, 100)}%</div>
					<div><tooltip-element tooltip="Church opinion is improved by constructing temples to Iruhan (except for temples of the heretical Vessel of Faith ideology) or by fighting against excommunicated rulers. It is worsened by building heretical or heathen temples, by maintaining a non-Iruhan state religion, by moving food to cause starvation, executing characters, or slaying innocents. When standing drops below -75, a ruler is excommunicated. The Church will donate a substantial share of its income to nations with positive standing, weighted according to standing."><report-link href="church/home">Church Opinion:</report-link></tooltip-element> ${Math.round(kingdom.goodwill)}${kingdom.goodwill <= -75 ? " (Excommunicated!)" : ""}</div>
					<div>${kingdom.loyal_to_cult ? "<tooltip-element tooltip=\"This nation has cut a deal with the dangerous Cult of the Witness, acquiring additional food and command of undead armies in exchange for furthering the unknown and mysterious agenda of the Cult. If nations that have declared loyalty to the Cult conquer sufficient territory or if the undead occupy a sufficient number of regions over the course of the game, the Cult will trigger an event of apocalyptic proportions!\">Loyal to Cult!</tooltip-element>" : ""}</div>
					${gothiVotesText.join("\n")}
				</div>
				<h1>Past Correspondence</h1>
				<div id="letters"></div>
				<h1>Historical Regions</h1>
				<div id="historical"></div>
				<h1>Court</h1>
				<div id="court"></div>
				<h1 id="heading_score">Score</h1>
				<div id="score"></div>
			</div>
		`;
		// CSS
		let style = document.createElement("style");
		style.innerHTML = `
			:host {
				height: 100%;
				overflow: auto;
				box-shadow: 1em 0 1em rgba(0, 0, 50, .2);
				background-color: #fff;
			}
			#abspos {
				position: relative;
			}
			#abspos > div:first-child {		
				background: url(${"images/" + kingdom.name.toLowerCase() + "_strip.jpg"}) no-repeat center center;
				background-size: cover;
				padding-top: .2em;
				padding-bottom: .2em;
				font-size: 200%;
				color: #fff;
				text-shadow: 0 0 6px #000, 0 0 0 #000, 0 0 3px #000, 1px 1px 3px #000, -1px -1px 3px #000;
				text-align: center;
			}
			#abspos > div:nth-child(n+3) {
				margin-left: 0.3em;
				margin-right: 0.3em;
			}
			#summary {
				display: grid;
				grid-template-columns: 1fr 2fr 1fr 2fr;
				margin-top: 0.5em;
				margin-bottom: 0.5em;
				font-family: sans-serif;
			}
			#summary div:nth-child(odd) {
				text-align: right;
				padding-right: 0.5em;
			}
			#summary div:nth-child(n + 11) {
				text-align: center;
			  grid-column-start: 1;
			  grid-column-end: 5;
				margin-top: 0.7em;
			}
			#summary div:nth-child(1) {
				text-align: center;
				grid-column-start: 1;
				grid-column-end: 3;
				margin-bottom: 0.7em;
			}
			#summary div:nth-child(2) {
				text-align: center;
				grid-column-start: 3;
				grid-column-end: 5;
				margin-bottom: 0.7em;
			}
			#relationships {
				margin-left: 0.3em;
				margin-right: 0.3em;
			}
			#relationships .war {
				color: #c00;
			}
			#relationships .war_indirect {
				color: #700;
			}
			#relationships .openborders {
				color: #007;
			}
			#relationships .ally {
				color: #00f;
			}
			#characters .snippet {
				margin-left: 0.7em;
				color: #777;
				font-size: 75%;
				line-height: 0.9;
			}
			h1 {
				margin-top: 0.7em;
				margin-bottom: 0;
				margin-left: 0;
				font-size: 100%;
				font-weight: bold;
				font-family: sans-serif;
			}
			table {
				width: 100%;
			}
			tr td:nth-child(2), tr th:nth-child(2) {
				text-align: right;
			}
			`;
		shadow.appendChild(style);
		let div = document.createElement("div");
		div.innerHTML = html;
		shadow.appendChild(div);

		let rels = {"friendly": [], "enemy": [], "neutral": []}; 
		for (let k in g_data.kingdoms) {
			if (g_data.kingdoms.hasOwnProperty(k) && k != kingdom.name) {
				rels[kingdom.calcRelationship(g_data.kingdoms[k])].push(k);
			}
		}
		for (let r in rels) {
			if (r == "neutral") continue;
			if (!rels.hasOwnProperty(r)) continue;
			let d = shadow.getElementById(r);
			rels[r].sort();
			let comma = false;
			for (let k of rels[r]) {
				if (comma) d.appendChild(document.createTextNode(", "));
				let de = document.createElement("report-link");
				de.setAttribute("href", "kingdom/" + k);
				de.innerHTML = k;
				d.appendChild(de);
				comma = true;
			}
		}
		let charDiv = shadow.getElementById("characters");
		let characters = [];
		for (let c of g_data.characters) {
			if (c.kingdom == kingdom.name) characters.push(c);
		}
		characters.sort((a, b)=>(a.name > b.name ? 1 : a.name < b.name ? -1 : 0));
		for (let c of characters) {
			let cDiv = document.createElement("div");
			let d = document.createElement("report-link");
			d.setAttribute("href", "character/" + c.name);
			d.innerHTML = c.name;
			cDiv.appendChild(d);
			if (c.tags.length > 0) cDiv.appendChild(document.createTextNode(" (" + c.tags.join(", ") + ")"));
			cDiv.appendChild(document.createTextNode(" (in "));
			if (c.location == -1) {
				cDiv.appendChild(document.createTextNode("hiding"));
			} else {
				let dd = document.createElement("report-link");
				dd.innerHTML = g_data.regions[c.location].name;
				dd.setAttribute("href", "region/" + g_data.regions[c.location].name);
				cDiv.appendChild(dd);
			}
			cDiv.appendChild(document.createTextNode(")"));
			charDiv.appendChild(cDiv);
		}

		// Add letters.
		let letDiv = shadow.getElementById("letters");
		let comms = [];
		for (let l of g_data.communications) comms.push(l);
		comms.sort(function(a, b){
			let ddiff = b.post_date - a.post_date;
			if (ddiff != 0) return ddiff;
			let afrom = contains(a.to, kingdom.name);
			let bfrom = contains(b.to, kingdom.name);
			if (afrom && !bfrom) return -1;
			else if (!afrom && bfrom) return 1;
			return 0;
		});
		for (let l of comms) {
			let line = "";
			let date = l.post_date;
			if (contains(l.to, kingdom.name)) {
				line = "Received week " + (date + 1) + " from " + l.signed.replace(/.* /, "");
			} else if (l.signed.indexOf(kingdom.name) >= 0) {
				line = "Sent week " + date + " to " + l.to.join(", ");
			} else {
				continue;
			}
			let lt = document.createElement("div");
			lt.appendChild(document.createTextNode(line));
			let snippet = document.createElement("expandable-snippet");
			snippet.setAttribute("text", l.text.replace(/</g, "&lt").replace(/>/g, "&gt"));
			snippet.setAttribute("max-length", 64);
			lt.appendChild(snippet);
			letDiv.appendChild(lt);
		}

		// Add regions.
		let regions = [];
		for (let r of g_data.regions) {
			if (r.kingdom == kingdom.name) regions.push(r);
		}
		let table = document.createElement("table");
		let hr = document.createElement("tr");
		hr.appendChild(document.createElement("th"));
		let th = document.createElement("th");
		let sel = document.createElement("select");
		let opts = {
			"Food (Weeks)": (r)=>(Math.round(r.calcFoodWeeks().v * 10) / 10),
			"Population": (r)=>(Math.round(r.population)),
			"Recruitment": (r)=>(Math.round(r.calcRecruitment().v)),
			"Taxation": (r)=>(Math.round(r.calcTaxation().v)),
			"Unrest": (r)=>(Math.round(r.calcUnrest().v * 100)),
		};
		for (let opt in opts) {
			let optE = document.createElement("option");
			optE.innerHTML = opt;
			sel.appendChild(optE);
		}
		th.appendChild(sel);
		hr.appendChild(th);
		table.appendChild(hr);
		let tbody = document.createElement("tbody");
		table.appendChild(tbody);
		let setRegions = function () {
			let rvalues = [];
			for (let r of regions) rvalues.push({"r": r.name, "v": opts[sel.value](r)});
			rvalues.sort((a, b)=>(b.v - a.v));
			tbody.innerHTML = "";
			for (let r of rvalues) {
				let row = document.createElement("tr");
				let tda = document.createElement("td");
				let rlink = document.createElement("report-link");
				rlink.innerHTML = r.r;
				rlink.setAttribute("href", "region/" + r.r);
				tda.appendChild(rlink);
				let tdb = document.createElement("td");
				tdb.innerHTML = r.v;
				row.appendChild(tda);
				row.appendChild(tdb);
				tbody.appendChild(row);
			}
		}
		sel.addEventListener("change", setRegions);
		setRegions();
		shadow.getElementById("regions").appendChild(table);

		// Add notifications and court.
		let traitTooltips = {
			"Inspiring": "+50% recruitment",
			"Frugal": "+50% taxation",
			"Soothing": "-2 percentage points of popular unrest each turn",
			"Meticulous": "+15% harvest yield",
			"Loyal": "ruler's armies are 125% as strong in this region",
			"Policing": "pirates will not appear in this region",
			"Generous": "-20 percentage points of popular unrest on the turn of a harvest",
			"Pious": "the population of this region counts triple in determining national or global dominant ideologies",
			"Rationing": "regional food consumption decreased by 20%",
			"Patronizing": "construction in this region is 50% discounted",
			"Untrusting": "-35% recruitment",
			"Hoarding": "-35% taxation",
			"Wasteful": "regional food consumption increased by 20%",
			"Snubbed": "+2 noble unrest / turn",
			"Shady Connections": "pirates are 300% as likely to appear in this region",
			"Workaholic": "+1 popular unrest / turn",
			"Cultist": "the Cult scores this region as if its ruler were loyal to the Cult",
			"Tyrannical": "-50% recruitment in this region",
			"Desperate": "Fortifications in this region are only half as effective",
			"Broke": "Construction in this region costs 200% as much",
		};
		let notes = shadow.getElementById("notifications");
		let nList = [];
		for (let n of g_data.notifications) if (n.who == kingdom.name) nList.push(n);
		if (nList.length == 0) {
			notes.style.display = "none";
			shadow.getElementById("heading_notifications").style.display = "none";
		} else {
			for (let n of nList) {
				let lt = document.createElement("div");
				lt.appendChild(document.createTextNode(n.title));
				let snippet = document.createElement("expandable-snippet");
				snippet.setAttribute("text", n.text);
				snippet.setAttribute("max-length", 64);
				lt.appendChild(snippet);
				notes.appendChild(lt);
			}
		}
		let court = shadow.getElementById("court");
		for (let c of kingdom.court) {
			let cd = document.createElement("div");
			cd.innerHTML = c.name + " (" + c.tags.map(a=>("<tooltip-element tooltip=\"" + traitTooltips[a] + "\">" + a + "</tooltip-element>")).join(", ") + ")"; 
			court.appendChild(cd);
		}
		for (let ri of kingdom.core_regions) {
			let r = g_data.regions[ri];
			let d = document.createElement("div");
			let rl = document.createElement("report-link");
			rl.setAttribute("href", "region/" + r.name);
			rl.innerHTML = r.name;
			d.appendChild(rl);
			if (r.kingdom != kingdom.name) {
				rl = document.createElement("report-link");
				rl.setAttribute("href", "kingdom/" + r.kingdom);
				rl.innerHTML = r.kingdom;
				d.appendChild(document.createTextNode(" (Controlled by "));
				d.appendChild(rl);
				d.appendChild(document.createTextNode(")"));
			}
			shadow.getElementById("historical").appendChild(d);
		}
		if ("(Observer)" == whoami) {
			let score = shadow.getElementById("score");
			let d = document.createElement("div");
			d.innerHTML = "They care about " + kingdom.profiles.map(s => s.toLowerCase()).sort().join(", ") + ".";
			score.appendChild(d);
			if (kingdom.score != undefined) {
				for (let s in kingdom.score) if (kingdom.score.hasOwnProperty(s)) {
					let d = document.createElement("div");
					d.innerHTML = s.substring(0, 1).toUpperCase() + s.substring(1) + ": " + (kingdom.score[s] > 0 ? "+" : "") + Math.round(kingdom.score[s] * 10) / 10;
					score.appendChild(d);
				}
			}
		} else {
			shadow.getElementById("heading_score").style.display = "none";
		}
	}
}
customElements.define("kingdom-report", KingdomReport);

let contains = function(ar, val) {
	if (ar == undefined) return false;
	for (a of ar) {
		if (a == val) return true;
	}
	return false;
}
