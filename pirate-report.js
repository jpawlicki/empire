class PirateReport extends HTMLElement {
	constructor() { super(); }
	connectedCallback() {
		let c = undefined;
		let cname = this.getAttribute("character");
		for (let co of g_data.characters) {
			if (co.name == cname) c = co;
		}

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

		let html = `
			<div id="name">Pirates</div>
			<div id="content">
				<h1>Threat: +${Math.round(g_data.pirates.threat * .25 * 100)} pirates</h1>
				<div id="threatshares" class="striped">
				</div>
				<h1>Current Bribes:</h1>
				<div id="bribes" class="striped">
				</div>
				<h1>Rules Summary</h1>
				<div class="summary">
					<p>Pirates are groups of malcontented soldiers and sailors who no longer serve any ruler. Soldiers or warships that desert due to lack of pay, soldiers that desert during a cross-cultural army merge, and soldiers and warships that are disbanded all turn to piracy, entering the pool of pirates. 25% of the current pool of pirates will appear each turn in a random region, weighted by unrest, the presence of nobles or Alyrja followers, and bribes.</p>
					<p>Rulers can anonymously bribe pirates to be more or less likely to appear in nations of their choice. Each 30 gold doubles (or halves) the probability that pirates will appear in regions belonging to that country. (Unlike most modifiers, bribes stack multiplicitively: a bribe of 90 gold makes pirates eight times as likely to appear.) The value of each bribe for or against a nation diminishes by 25% each turn. When pirate threat is large, sponsoring piracy in a nation is an effective means for that nation's enemies to force it to divide its forces.</p>
					<p>Pirate armies will wander randomly once they appear, fighting all other armies they encounter and converting the income of the region they occupy and any fleets they capture into additional pirate threat.</p>
				</div>
			</div>
		`;
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
				background: url("images/pirate.jpg") no-repeat center center;
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
			#content > div:not(.summary) {
				display: grid;
				grid-template-columns: auto auto;
				margin-left: 0.7em;
				font-size: 90%;
			}
			#content > div:not(.summary) :nth-child(even) {
				text-align: right;
			}
			#content > div:not(.summary) > tooltip-element::first-letter {
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
			.summary {
				margin-left: 0.7em;
				font-size: 90%;
			}
			.summary p {
				margin-top: 0;
				text-indent: 1em;
			}
			.striped > :nth-child(4n + 3), .striped > :nth-child(4n + 4) {
				background: #eee;
			}
			`;
		let shadow = this.attachShadow({mode: "open"});
		shadow.appendChild(style);
		let content = document.createElement("div");
		content.innerHTML = html;
		shadow.appendChild(content);
		// Add opinions.
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
			d.innerHTML = num(i.o, 1, 100) + "%";
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
}
customElements.define("pirate-report", PirateReport);
