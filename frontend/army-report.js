class ArmyReport extends HTMLElement {
	constructor() { super(); }
	connectedCallback() {
		let army = undefined;
		let aid = parseInt(this.getAttribute("army"));
		for (let a of g_data.armies) {
			if (a.id == aid) army = a;
		}

		let html = `
			<div id="name">${army.type == "army" ? "Army" : "Navy"} ${army.id}</div>
			<kingdom-label kingdom="${army.kingdom}"></kingdom-label>
			<div id="location">In <report-link href="region/${g_data.regions[army.location].name}">${g_data.regions[army.location].name}</report-link></div>
			<div id="content">
				<h1>Strength: ${num(army.calcStrength(), 2)}</h1>
				<div>Size: ${Math.floor(army.size)}</div>
				<div>Hoarding: ${Math.floor(army.gold)} gold</div>
				<div>Upkeep: ${num(army.calcCost(true), 1)} gold</div>
				<h1 id="tags_h1">Tags</h1>
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
		if (army.tags.length == 0) {
			shadow.getElementById("tags_h1").style.display="none";
		}
		for (let tag of army.tags) {
			let r = document.createElement("tooltip-element");
			r.innerHTML = tag;
			r.setAttribute("tooltip", tagTooltips[tag]);
			shadow.getElementById("tags").appendChild(r);
		}
	}
}
customElements.define("army-report", ArmyReport);
