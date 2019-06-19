class ChurchReport extends HTMLElement {
	constructor() { super(); }
	connectedCallback() {
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

		let ideologyTooltips = {
			"Chalice of Compassion": "Followers of the Chalice of Compassion ideology most strongly believe in charitable living and merciful conduct in war. While Chalice of Compassion is the dominant Iruhan ideology, refugees do not cause unrest in regions worshipping Iruhan.",
			"Sword of Truth": "Followers of the Sword of Truth ideology most strongly believe that the other religions of the world pose an existential threat to humanity. While Sword of Truth is the dominant Iruhan ideology, all armies and navies of nations with an Iruhan state religion are +15% as strong. Armies and navies of nations with the Sword of Truth state ideology are a further +10% as strong.",
			"Tapestry of People": "Followers of the Tapestry of People ideology believe most strongly in the value of a diverse multitude of points of view. While Tapestry of People is the dominant Iruhan ideology, all nations with an Iruhan state religion produce three percentage points more tax and recruits per unique ideology (including those of other religions) within that nation.",
			"Vessel of Faith": "Followers of the Vessel of Faith ideology do not recognize the Church of Iruhan as a legitimate authority, instead believing that Iruhan speaks to each individual personally. They strongly believe in spreading the word of Iruhan to all people. While Vessel of Faith is the dominant Iruhan ideology, the Tiecel is powerless and construction of Iruhan temples is free.",
		};

		let html = `
			<div id="name">Church of Iruhan</div>
			<div id="content">
				<h1>Opinions</h1>
				<div id="opinions" class="striped">
				</div>
				<h1>Dominant Ideology: <tooltip-element tooltip="${ideologyTooltips[dominantIdeology]}">${dominantIdeology}</tooltip-element></h1>
				<div id="ideologies">
				</div>
				<h1>Doctrines</h1>
				<div id="doctrines" class="summary">
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
				background: url("images/church.jpg") no-repeat center center;
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
			.striped > :nth-child(4n + 3), .striped > :nth-child(4n + 4) {
				background: #eee;
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
			#doctrines ul {
				margin-top: 0;
				font-size: 90%;
			}
			.summary {
				margin-left: 0.7em;
				font-size: 90%;
			}
			.summary p {
				margin-top: 0;
				text-indent: 1em;
			}
			`;
		let shadow = this.attachShadow({mode: "open"});
		shadow.appendChild(style);
		let content = document.createElement("div");
		content.innerHTML = html;
		shadow.appendChild(content);
		// Add opinions.
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
			d.innerHTML = Math.round(i.o * 10) / 10;
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
		{
			let doctrines = shadow.getElementById("doctrines");
			for (let doctrine of g_data.church.doctrines.slice().sort()) {
				doctrines.appendChild(document.createTextNode(titleCase(doctrine)));
				let d = document.createElement("ul");
				for (let desc of doctrineDescriptions[doctrine]) {
					let li = document.createElement("li");
					li.appendChild(document.createTextNode(desc));
					d.appendChild(li);
				}
				doctrines.appendChild(d);
			}
		}
	}
}
customElements.define("church-report", ChurchReport);
