class ArmyReport extends HTMLElement {
	constructor() { super(); }
	connectedCallback() {
		let tagTooltips = {
			"Steel": "+15% as strong.",
			"Formations": "If the army would suffer between 45% and 90% casualties, it instead suffers 45% casualties.",
			"Pillagers": "While this army is the largest army in an enemy or neutral region, it suppresses recruitment and diverts that region's taxes to this army's government.",
			"Raiders": "This army is hidden in friendly regions.",
			"Seafaring": "+150% as strong in sea regions.",
			"Impressment": "15% of casualties this army inflicts on enemies join this army instead of perishing.",
			"Riders": "This army (and any leader) move 2 edges per week.",
			"Crafts-soldiers": "This army costs 50% less gold on any turn it does not move.",
			"Weathered": "Treacherous and Seasonal regions have no effect on this army.",
			"Pathfinders": "This army (and any leader) move 3 edges per week when moving into a friendly region.",
			"Pillagers (Pirate)": "While this army is the largest army in a region, it suppresses recruitment and diverts that region's taxes to pirate threat.",
			"Unpredictable": "This army moves randomly into a neighboring land region.",
			"Unruly": "If larger than 2000 soldiers, this army divides into two equal forces. This army (and any divisions) move randomly into a neighboring land region.",
			"Higher Power": "This army does not require payment and does not inflict casualties on other armies with Higher Power.",
			"Undead": "Any non-Undead soldiers who fall in battle with at least one Undead army rise from the dead and are distributed as soldiers evenly among participating Undead armies.",
		};

		let army = undefined;
		let aid = parseInt(this.getAttribute("army"));
		for (let a of g_data.armies) {
			if (a.id == aid) army = a;
		}

		let html = `
			<div id="name">${army.type == "army" ? "Army" : "Navy"} ${army.id}</div>
			<div id="kingdom"><report-link href="kingdom/${army.kingdom}">${army.kingdom}</report-link></div>
			<div id="location">In <report-link href="region/${g_data.regions[army.location].name}">${g_data.regions[army.location].name}</report-link></div>
			<div id="content">
				<h1>Strength: ${num(army.calcStrength(), 2)}</h1>
				<div>Size: ${Math.floor(army.size)}</div>
				<h1>Tags</h1>
				<div id="tags">
				</div>
			</div>
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
			}
			#name {
				background: url("images/${army.type == "navy" ? "navy" : "army"}_strip.jpg") no-repeat center center;
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
			#kingdom {
				text-align: center;
				margin-top: 0.2em;
				margin-bottom: 0.2em;
				font-variant: small-caps;
				font-size: 125%;
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
				margin-left: 0.7em;
				font-size: 90%;
			}
			#content > div > tooltip-element::first-letter {
				text-transform: capitalize;
			}
			#tags tooltip-element {
				display: block;
			}
			`;
		let shadow = this.attachShadow({mode: "open"});
		shadow.appendChild(style);
		let content = document.createElement("div");
		content.innerHTML = html;
		shadow.appendChild(content);
		for (let tag of army.tags) {
			let r = document.createElement("tooltip-element");
			r.innerHTML = tag;
			r.setAttribute("tooltip", tagTooltips[tag]);
			shadow.getElementById("tags").appendChild(r);
		}
	}
}
customElements.define("army-report", ArmyReport);
