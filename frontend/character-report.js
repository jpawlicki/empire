class CharacterReport extends HTMLElement {
	constructor() { super(); }
	connectedCallback() {
		let c = undefined;
		let cname = this.getAttribute("character");
		for (let co of g_data.characters) {
			if (co.name == cname) c = co;
		}

		let html = `
			<div id="name">${c.name}</div>
			<div id="kingdom">${c.tags.length == 0 ? "" : c.tags.join(" and ") + " of "}<report-link href="kingdom/${c.kingdom}">${c.kingdom}</report-link></div>
			<div id="location">In ${c.location == -1 ? "Hiding" : "<report-link href=\"region/" + g_data.regions[c.location].name + "\">" + g_data.regions[c.location].name + "</report-link>"}</div>
			<div id="status">${c.captor == "" ? "" : "Captive of <report-link href=\"kingdom/" + c.captor + "\">" + c.captor + "</report-link>"}</div>
			<div id="content">
				<h1>Skills</h1>
				<div>
					<tooltip-element tooltip="Admirals make navies they lead more effective. They improve their skill by leading navies.">Admiral:</tooltip-element><div id="skill_admiral"></div>
					<tooltip-element tooltip="Generals make armies they lead more effective. They improve their skill by leading armies.">General:</tooltip-element><div id="skill_general"></div>
					<tooltip-element tooltip="Governors can increase tax and recruitment in a region more effectively when governing there. They improve their skill by governing.">Governor:</tooltip-element><div id="skill_governor"></div>
					<tooltip-element tooltip="Spies can increase the power of your plots and plot defense. They improve their skill while hiding or captured.">Spy:</tooltip-element><div id="skill_spy"></div>
				</div>
				<h1 id="values_header">Values</h1>
				<ul id="values">
				</ul>
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
				background: linear-gradient(rgba(0, 0, 0, 0.3), rgba(0, 0, 0, 0.3)), url("images/characters.png") no-repeat center center;
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
			ul {
				list-style-type: none;
			}
			`;
		let shadow = this.attachShadow({mode: "open"});
		shadow.appendChild(style);
		let content = document.createElement("div");
		content.innerHTML = html;
		shadow.appendChild(content);
		// Add skills.
		for (let skill of ["admiral", "general", "governor", "spy"]) {
			let l = c.calcLevel(skill);
			let s = "";
			for (let i = 0; i < l; i++) {
				s += "★";
			}
			let x = c.experience[skill];
			let rem = x >= 24 ? -1 : x >= 15 ? 24 - x : x >= 8 ? 15 - x : x >= 3 ? 8 - x : 3 - x;
			if (rem == -1) rem = "Max level.";
			else rem = rem + " experience to next level.";
			let t = document.createElement("tooltip-element");
			t.innerHTML = s;
			t.setAttribute("tooltip", getEffect(l, skill) + " " + rem);
			shadow.getElementById("skill_" + skill).appendChild(t);
		}
		// If values, show them; else hide them.
		let valueTooltips = {
			"food": "Feeding their people.",
			"prosperity": "The prosperity of their people.",
			"happiness": "The happiness of their general population.",
			"supremacy": "The supremacy and independence of their nation.",
			"conquest": "Conquering other lands.",
			"glory": "Fighting in glorious combat.",
			"religion": "Spreading their overall religion (vs other religions).",
			"ideology": "Spreading their interpretation of their religion (among followers of their overall religion).",
			"security": "Building large armies and navies (used or not).",
			"riches": "Amassing wealth.",
			"friendship": "Forming numerous alliances.",
			"culture": "The happiness of all people of their culture.",
			"unity": "Preventing war betwen nations of their culture.",
			"worldpeace": "World peace.",
		};
		if (c.values.length > 0) {
			for (let v of c.values) {
				let li = document.createElement("li");
				let tv = document.createElement("tooltip-element");
				tv.innerHTML = capitalize(v);
				tv.setAttribute("tooltip", valueTooltips[v]);
				li.appendChild(tv);
				shadow.getElementById("values").appendChild(li);
			}
		} else {
			shadow.getElementById("values_header").style.display = "none";
		}
	}
}
customElements.define("character-report", CharacterReport);

let getEffect = function(level, skill) {
	if (skill == "general") {
		return "Armies led are +" + level * 20 + "% as effective.";
	} else if (skill == "admiral") {
		return "Navies led are +" + level * 20 + "% as effective.";
	} else if (skill == "governor") {
		return "The Govern action produces +" + level * 50 + "% more taxation and recruitment.";
	} else if (skill == "spy") {
		return "+" + level * 30 + "% plot strength.";
	}
}
