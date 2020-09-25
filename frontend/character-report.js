class CharacterReport extends HTMLElement {
	constructor() { super(); }
	connectedCallback() {
		let c = undefined;
		let cname = this.getAttribute("character");
		for (let co of g_data.characters) {
			if (co.name == cname) c = co;
		}

		let html = `
			<div id="portrait"></div>
			<div>
				<div id="name">${c.honorific} ${c.name}</div>
				<kingdom-label kingdom="${c.kingdom}"></kingdom-label>
				<div>${c.tags.length == 0 ? "" : c.tags.join(" and ")}</div>
				<div id="location">In ${c.location == -1 ? "Hiding" : "<report-link href=\"region/" + g_data.regions[c.location].name + "\">" + g_data.regions[c.location].name + "</report-link>"}</div>
				<div id="content">
					<h1>Skills</h1>
					<div>
						<tooltip-element tooltip="Admirals make navies they lead more effective and cause more local shipbuilding. They improve their skill by leading navies.">Admiral:</tooltip-element><div id="skill_admiral"></div>
						<tooltip-element tooltip="Generals make armies they lead more effective and cause more local recruitment. They improve their skill by leading armies.">General:</tooltip-element><div id="skill_general"></div>
						<tooltip-element tooltip="Governors can construct for less gold and cause more local tax income. They improve their skill by constructing and installing nobles.">Governor:</tooltip-element><div id="skill_governor"></div>
						<tooltip-element tooltip="Spies provide leverage over the nations they occupy, and reduce enemy leverage over our nation when in friendly territory. They improve their skill by hiding or establishing spy rings.">Spy:</tooltip-element><div id="skill_spy"></div>
					</div>
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
				min-width: 20em;
			}
			#container {
				display: grid;
				grid-template-columns: 50% 50%;
			}
			#portrait {
				background-size: 100%;
				background-repeat: no-repeat;
				min-height: 25em;
			}
			#name {
				padding-top: .2em;
				padding-bottom: .2em;
				font-size: 150%;
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
		content.setAttribute("id", "container");
		content.innerHTML = html;
		shadow.appendChild(content);
		// Add skills.
		for (let skill of ["admiral", "general", "governor", "spy"]) {
			let t = document.createElement("tooltip-element");
			t.innerHTML = Math.floor(c.calcLevel(skill) * 10) / 10;
			t.setAttribute("tooltip", getEffect(c.calcLevel(skill), skill));
			shadow.getElementById("skill_" + skill).appendChild(t);
		}
		if (c.portrait != undefined && c.portrait >= 0) {
			shadow.getElementById("portrait").style.backgroundImage = "url(images/portraits/" + c.portrait + ".png";
		}
	}
}
customElements.define("character-report", CharacterReport);

let getEffect = function(level, skill) {
	if (skill == "general") {
		return "Armies led are +" + Math.round(level * 20) + "% as effective. +" + Math.round(level * 25) + "% more local recruitment.";
	} else if (skill == "admiral") {
		return "Navies led are +" + Math.round(level * 20) + "% as effective. +" + Math.round(level * 15) + "% more local shipbuilding.";
	} else if (skill == "governor") {
		return "-" + Math.round(level * 15) + "% construction cost. +" + Math.round(level * 20) + "% more local tax income.";
	} else if (skill == "spy") {
		return "-" + Math.round(level * 15) + "% spy ring establishment cost. +" + Math.round(level * 2) + " leverage over occupied nations.";
	}
}
