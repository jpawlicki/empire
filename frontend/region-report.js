class RegionReport extends HTMLElement {
	constructor() { super(); }
	connectedCallback() {
		let r = undefined;
		let rname = this.getAttribute("region");
		for (let ro of g_data.regions) {
			if (ro.name == rname) r = ro;
		}
		let religion = r.religion;

		let shadow = this.attachShadow({mode: "open"});

		let html = "";
		if (r.type == "land") {
			html = `
				<div id="name">${r.name}</div>
				<kingdom-label kingdom="${r.kingdom}"></kingdom-label>
				<div id="cct"></div>
				<div id="content">
					<h1><tooltip-element tooltip="A region's population determines its tax production, recruitment, maximum harvest size, and food consumption. Population grows slowly, but regions can also attract refugees from neighboring regions that have battles or starvation.">Population</tooltip-element>: <tooltip-element tooltip="${r.population}">${Math.round(r.population / 1000)}k</tooltip-element></h1>
					<div>
						<tooltip-element tooltip="A region's dominant religion and ideology is determined by which religion and ideology has the most temples in the region.">Religion:</tooltip-element><tooltip-element tooltip="${religion_tooltips[religion]}">${religion}</tooltip-element>
						<tooltip-element tooltip="A region's culture is fixed and does not change.">Culture:</tooltip-element><tooltip-element tooltip="${culture_tooltips[r.culture]}">${r.culture}</tooltip-element>
					</div>
					<h1><tooltip-element tooltip="Regional unrest affects a region's recruitment, taxation, and maximum harvest, as well as how difficult it is to capture. Regional unrest is equal to the highest of popular unrest, noble unrest (if a noble rules the region), and clerical unrest (for most Iruhan-following regions).">Unrest</tooltip-element>: ${num(r.calcUnrest(), 0, 100)}%</h1>
					<div>
						<tooltip-element tooltip="Popular unrest is increased by high taxes, local battles, starvation, and forcible relocation or refugees. It can be lowered by lowering taxes or more generous rationing.">Popular Unrest:</tooltip-element><div>${Math.round(r.unrest_popular * 100)}%</div>
						${getNobleUnrestBlock(r)}
						${getClericalUnrestBlock(r)}
					</div>
					${getNobleBlock(r)}
					<h1><tooltip-element tooltip="Fortification measures how strong a region's defenses are. It depends on the number of fortification constructions constructed in the region.">Fortification:</tooltip-element> ${num(r.calcFortification(), 0, 100)}%</h1>
					<div>
						<tooltip-element tooltip="Regions can only be captured by armies strong enough to overthrow the local militia and government. The minimum conquerer strength is equal to the (square root of Population) × Fortification × (100% - Unrest) × 3%).">Conquest Threshold</tooltip-element><div>${num(r.calcMinConquestSize(), 1)}</div>
						<tooltip-element tooltip="Regions can only be patrolled by armies strong enough to keep the population in check. The minimum patrol strength is equal to the (square root of Population) × 3%.">Patrol Threshold</tooltip-element><div>${num(r.calcMinPatrolSize(), 1)}</div>
					</div>
					<h1><tooltip-element tooltip="Each inhabitant of the region normally eats one measure of food each week. If there is insufficient food, 5% of the unfed population will die, 5% of the unfed population will emigrate (if possible), and popular unrest will dramatically increase.">Food:</tooltip-element> ${num(r.calcFoodWeeks(), 1)} Weeks</h1>
					<div>
						<tooltip-element tooltip="The total measures of food stockpiled in the region.">Total Measures:</tooltip-element><div><tooltip-element tooltip="${r.food}">${Math.round(r.food / 1000)}k</tooltip-element></div>
					</div>
					<h1><tooltip-element tooltip="Crops are harvested into food every 4 weeks, and new crops are planted. Fewer crops are planted during the fall and winter.">Crops:</tooltip-element> +${num(r.calcHarvestWeeks(), 1)} Weeks</h1>
					<div>
						<tooltip-element tooltip="The total crops in this region.">Crops:</tooltip-element><div>${num(r.calcCrops(), 0, 1 / 1000)}k</div>
						<tooltip-element tooltip="The maximum amount of crops the region currently has labor available to harvest.">Harvest Capacity:</tooltip-element><div>${num(r.calcHarvestCapacity(), 0, 1 / 1000)}k</div>
						<div>Time Until Harvest:</div><div>${3 - g_data.date % 4} Weeks</div>
					</div>
					<h1><tooltip-element tooltip="Taxation output of the region, when taxed at 100%. Tax is primarily determined by population and unrest, though several religious ideologies also affect taxation output.">Tax:</tooltip-element> +${num(r.calcTaxation())} gold</h1>
					<h1><tooltip-element tooltip="Recuit output of the region. Recuritment is primarily determined by population and unrest, though several religious ideologies also affect recruitment output.">Recruits:</tooltip-element> +${num(r.calcRecruitment())} recruits</h1>
					<h1>Contents</h1>
					<div id="objects"></div>
					<h1>Other</h1>
					<div>
						<tooltip-element tooltip="Pirate threat is the probability that pirates will appear in this region during the resolution of the current turn, assuming no further pirate bribes. It is decreased by lowered unrest, by paying off the pirates to avoid the nation (or paying them to prefer another nation), by followers of the Northern (Alyrja) ideology, and by the use of nobles.">Pirate Threat:</tooltip-element><div>${num(r.calcPirateThreat(), 1, 100)}%</div>
					</div>
				</div>
			`;
		} else {
			html = `
				<div id="name">${r.name}</div>
				<div id="cct"></div>
				<div id="content">
					<h1><tooltip-element tooltip="Sea regions produce 20 gold for the largest navy they contain, and 10 for the runner-up.">Sea Region</tooltip-element></h1>
					<h1>Contents</h1>
					<div id="objects"></div>
				</div>
			`;
		}
		// CSS
		let style = document.createElement("style");
		style.innerHTML = `
			:host {
				height: 100%;
				overflow-y: auto;
				box-shadow: 1em 0 1em rgba(0, 0, 50, .2);
				background-color: #fff;
				z-index: 1;
			}
			#name {
				background: url("images/${r.kingdom == undefined ? "sea" : r.kingdom.toLowerCase()}_strip.jpg") no-repeat center center;
				background-size: cover;
				padding-top: .2em;
				padding-bottom: .2em;
				font-size: 150%;
				color: #fff;
				text-shadow: 0 0 6px #000, 0 0 0 #000, 0 0 3px #000, 1px 1px 3px #000, -1px -1px 3px #000;
				text-align: center;
			}
			#content {
				margin-left: 0.3em;
				margin-right: 0.3em;
			}
			#cct tooltip-element::first-letter {
				text-transform: capitalize;
			}
			#cct {
				display: flex;
				justify-content: space-around;
			}
			h1 {
				margin-top: 0.7em;
				margin-bottom: 0;
				margin-left: 0;
				font-size: 100%;
				font-weight: bold;
				font-family: sans-serif;
			}
			#content > div {
				display: grid;
				grid-template-columns: auto auto;
				margin-left: 0.7em;
				font-size: 90%;
			}
			#content > div :nth-child(even) {
				text-align: right;
			}
			#content > div > tooltip-element::first-letter {
				text-transform: capitalize;
			}
			#content > #objects {
				display: block;
			}
			#content > #objects > div::first-letter {
				text-transform: capitalize;
			}
			#content > #objects :nth-child(even) {
				text-align: left;
			}
			#content > div.crisis {
				display: flex;
				justify-content: space-around;
				font-size: 110%;
			}
			`;
		shadow.appendChild(style);
		let content = document.createElement("div");
		content.innerHTML = html;
		shadow.appendChild(content);
		let ctn = shadow.getElementById("objects");
		let armies = [];
		for (let c of g_data.armies) {
			if (c.location ==	r.id) armies.push(c);
		}
		armies.sort((a, b) => (b.calcStrength().v - a.calcStrength().v));
		for (let c of armies) {
			let d = document.createElement("div");
			let n = document.createElement("report-link");
			n.innerHTML = (c.type == "army" ? "Army " : "Navy ") + c.id;
			n.setAttribute("href", "army/" + c.id);
			d.appendChild(n);
			d.appendChild(document.createTextNode(" ("));
			let kn = document.createElement("report-link");
			kn.innerHTML = c.kingdom;
			kn.setAttribute("href", "kingdom/" + c.kingdom);
			d.appendChild(kn);
			d.appendChild(document.createTextNode(", " + Math.round(c.calcStrength().v) + ")"));
			ctn.appendChild(d);
		}
		let chars = [];
		for (let c of g_data.characters) {
			if (c.location ==	r.id) chars.push(c);
		}
		chars.sort((a, b) => (a.kingdom > b.kingdom ? 1 : a.kingdom < b.kingdom ? -1 : a.name > b.name ? 1 : a.name < b.name ? -1 : 0));
		for (let c of chars) {
			let d = document.createElement("div");
			let n = document.createElement("report-link");
			n.innerHTML = c.name;
			n.setAttribute("href", "character/" + c.name);
			d.appendChild(n);
			d.appendChild(document.createTextNode(" ("));
			let kn = document.createElement("report-link");
			kn.innerHTML = c.kingdom;
			kn.setAttribute("href", "kingdom/" + c.kingdom);
			d.appendChild(kn);
			d.appendChild(document.createTextNode(")"));
			ctn.appendChild(d);
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
			if (constructions.hasOwnProperty(c)) {
				let d = document.createElement("div");
				d.innerHTML = constructions[c] > 1 ? c + " (x" + constructions[c] + ")" : c;
				ctn.appendChild(d);
			}
		}
		let localRings = [];
		for (let ring of g_data.spy_rings) if (ring.location == r.id) localRings.push(ring);
		localRings.sort((a, b) => b.strength - a.strength);
		for (let ring of localRings) {
			let d = document.createElement("div");
			d.innerHTML = "Spy Ring (" + ring.nation + ", Strength " + Math.round(ring.strength) + ", " + (ring.hidden ? "Hidden)" : "Exposed)");
			ctn.appendChild(d);
		}
		if (g_data.regions[g_geo.holycity] == r) {
			let holy = document.createElement("tooltip-element");
			holy.setAttribute("tooltip", "This region contains the Holy City of the Church of Iruhan. Plots to affect Church opinion take place here, and Cardinals may meet here to bolster all Iruhan-worshiping nations.");
			holy.appendChild(document.createTextNode("Holy City"));
			shadow.getElementById("cct").appendChild(holy);
		}
	}
}
customElements.define("region-report", RegionReport);

let capitalize = function(s) {
	return s.charAt(0).toUpperCase() + s.slice(1);
}

let capitalizeForce = function(s) {
	return s.charAt(0).toUpperCase() + s.slice(1).toLowerCase();
}

let getNobleUnrestBlock = function(r) {
	if (r.noble.name == undefined) return "";
	else return `<tooltip-element tooltip="Nobles in a region primarily care about the ruler's performance in a certain area.">Noble Unrest:</tooltip-element><div>${Math.round(r.noble.unrest * 100)}%</div>`;
}

let getClericalUnrestBlock = function(r) {
	if (r.religion.indexOf("Iruhan") < 0 || r.religion.indexOf("Vessel of Faith") >= 0) return "";
	let clericalUnrest = r.kingdom == "Unruled" ? 0 : Math.min(1, Math.max(0, -g_data.kingdoms[r.kingdom].goodwill / 100));
	return `<tooltip-element id="clerical_unrest" tooltip="Clerical unrest is caused by the Church of Iruhan's wrath toward a ruler. It can be decreased by fighting excommunicated nations, building temples to Iruhan (but not Vessel of Faith temples), or removed entirely from this region by converting the region to a different religion or the Vessel of Faith ideology.">Clerical Unrest:</tooltip-element><div>${Math.round(clericalUnrest * 100)}%</div>`;
}

let getNobleBlock = function(r) {
	if (r.noble.name == undefined) return "";
	let crisis = "";
	if (r.noble.crisis != undefined && r.noble.crisis.type != undefined && r.noble.crisis.type != "NONE") {
		let crisisDescription = "";
		if (r.noble.crisis.type == "CONQUEST") {
			crisisDescription = r.noble.name + " is concerned their nation is too small.";
			crisisDescription += "\nGoal: " + r.kingdom + " controls nine regions.";
		} else if (r.noble.crisis.type == "FAITH") {
			crisisDescription = r.noble.name + " is concerned about heresy.";
			crisisDescription += "\nGoal: " + r.name + "'s ideology matches " + r.kingdom + "'s state ideology.";
		} else if (r.noble.crisis.type == "FORTIFY") {
			crisisDescription = r.noble.name + " is concerned about defenses.";
			crisisDescription += "\nGoal: " + r.name + "'s fortification is at least 175%.";
		} else if (r.noble.crisis.type == "GROW") {
			crisisDescription = r.noble.name + " is concerned about population.";
			crisisDescription += "\nGoal: " + r.name + "'s population is at least 200k.";
		} else if (r.noble.crisis.type == "INSPIRE") {
			crisisDescription = r.noble.name + " is concerned about the church.";
			crisisDescription += "\nGoal: At least one cardinal inspires the faithful.";
		} else if (r.noble.crisis.type == "NAVY") {
			crisisDescription = r.noble.name + " is concerned about naval strength.";
			crisisDescription += "\nGoal: " + r.name + " has at least 2 shipyards.";
		} else if (r.noble.crisis.type == "NOBILITY") {
			crisisDescription = r.noble.name + " is concerned about the nobility.";
			crisisDescription += "\nGoal: " + r.kingdom + " has a noble in every region.";
		} else if (r.noble.crisis.type == "PARADE") {
			crisisDescription = r.noble.name + " is concerned about the lack of local heroes.";
			crisisDescription += "\nGoal: A character of " + r.kingdom + " ends a turn in " + r.name + ".";
		} else if (r.noble.crisis.type == "PATROL") {
			crisisDescription = r.noble.name + " is concerned about local military.";
			crisisDescription += "\nGoal: An army of " + r.kingdom + " at least " + Math.ceil(10 * r.calcMinPatrolSize().v) / 10 + " strength must end the turn in the region.";
		} else if (r.noble.crisis.type == "RECESSION") {
			crisisDescription = r.noble.name + " is concerned with a future economic condition.";
			crisisDescription += "\nGoal: " + r.kingdom + " has a national treasury of at least 200 gold.";
		} else if (r.noble.crisis.type == "SPY") {
			crisisDescription = r.noble.name + " is concerned with intrigue.";
			crisisDescription += "\nGoal: " + r.kingdom + " has a spy ring in " + r.name + ".";
		} else if (r.noble.crisis.type == "UPRISING") {
			crisisDescription = r.noble.name + " is facing a popular rebellion.";
			crisisDescription += "\nGoal: Popular unrest in " + r.name + " is 50% or less.";
		} else if (r.noble.crisis.type == "WEDDING") {
			crisisDescription = r.noble.name + "'s child is getting married.";
			crisisDescription += "\nGoal: The ruler of " + r.kingdom + " is in the region.";
		} else if (r.noble.crisis.type == "WORSHIP") {
			crisisDescription = r.noble.name + " is concerned about local temples.";
			crisisDescription += "\nGoal: " + r.name + " has at least 3 temples.";
		}
		crisisDescription += "\nDue at the end of week " + r.noble.crisis.deadline + ".";
		crisis = `<tooltip-element tooltip="All nobles experience crises every six weeks. If resolved positively within six weeks, a crisis decreases noble unrest by 25 percentage points. If unresolved within the deadline, the noble gains 25 percentage points of unrest.">Crisis:</tooltip-element><tooltip-element id="crisis" tooltip="${crisisDescription}">${capitalizeForce(r.noble.crisis.type)}</tooltip-element>`
	}
	let levelDetail = "Nobles gain an experience point each turn, and have a level equal to the square root of one plus their experience. This noble's level gives the region:";
	levelDetail += "\n +" + Math.round(r.calcNobleLevel() * 0.1 * 100) + "% regional tax";
	levelDetail += "\n +" + Math.round(r.calcNobleLevel() * 0.1 * 100) + "% regional recruitment";
	levelDetail += "\n +" + Math.round(r.calcNobleLevel() * 0.05 * 100) + "% crops planted after each harvest";
	return `
		<h1>Noble: ${r.noble.name}</h1>
		<div>Level: <tooltip-element tooltip="${levelDetail}">${Math.round(r.calcNobleLevel() * 100) / 100}</tooltip-element></div>
		<div class="crisis">${crisis}</div>
	`;
}
