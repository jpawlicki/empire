class GothiReport extends HTMLElement {
	constructor() { super(); }
	connectedCallback() {
		let votes = {
			"Alyrja": {"v": 0, "total": 0},
			"Rjinku": {"v": 0, "total": 0},
			"Lyskr": {"v": 0, "total": 0},
			"Syrjen": {"v": 0, "total": 0},
		};
		let spell = {
			"Alyrja": "<tooltip-element tooltip=\"Once summoned, the Warwind makes every sea region treacherous and pushes all occupants of each sea region to a random adjacent region each week. Like all gothi spells, the Warwind destroys 3% of crops worldwide each week, requires either 2/3rds of all gothi or five gothi total (whichever is more) to activate, and has a 50% chance of continuing each week after the gothi cease summoning it.\">Warwind</tooltip-element>",
			"Rjinku": "<tooltip-element tooltip=\"Once summoned, the Quake has a one-third chance to destroy each construction each week. Like all gothi spells, it destroys 3% of crops worldwide each week, requires either 2/3rds of all gothi or five gothi total (whichever is more) to activate, and has a 50% chance of continuing each week after the gothi cease summoning it.\">Quake</tooltip-element>",
			"Lyskr": "<tooltip-element tooltip=\"Once summoned, the Veil hides all navies, armies, and characters from other rulers, but does not otherwise obstruct their activities or prevent battles. Like all gothi spells, it destroys 3% of crops worldwide each week, requires either 2/3rds of all gothi or five gothi total (whichever is more) to activate, and has a 50% chance of continuing each week after the gothi cease summoning it.\">Veil</tooltip-element>",
			"Syrjen": "<tooltip-element tooltip=\"Once summoned, the Deluge makes every land region treacherous and allows navies to move between land regions, participate in battles in land regions, and prevents them from being captured. Like all gothi spells, it destroys 3% of crops worldwide each week, requires either 2/3rds of all gothi or five gothi total (whichever is more) to activate, and has a 50% chance of continuing each week after the gothi cease summoning it.\">Deluge</tooltip-element>",
		};
		for (let c of g_data.regions) {
			if (c.religion != undefined && c.religion.startsWith("Northern")) {
				let ideology = c.religion.substring(c.religion.indexOf("(") + 1, c.religion.indexOf(")"));
				votes[ideology].total++;
				if (g_data.kingdoms[c.kingdom].gothi[ideology]) votes[ideology].v++;
			}
		}
		let votesText = [];
		for (let r in votes) if (votes.hasOwnProperty(r)) {
			votes[r].total = Math.max(5, Math.ceil(votes[r].total / 3 * 2));
		}
		for (let r in votes) if (votes.hasOwnProperty(r)) {
			votesText.push("<div>" + r + " (" + spell[r] + "):</div><div>" + votes[r].v + " of " + votes[r].total + " needed</div>");
		}

		let html = `
			<div id="name">Gothi</div>
			<div id="content">
				<h1>Spells</h1>
				<div>
					${votesText.join("\n")}
				</div>
				<h1>Votes</h1>
				<table id="votes">
					<tr><th></th><th>Alyrja</th><th>Lyskr</th><th>Rjinku</th><th>Syrjen</th></tr>
				</table>
				<h1>Rules Summary</h1>
				<div class="summary">
					<p>The Gothi are religious leaders in the northern religion. Although the northern deities (known collectively as the "Tivar") magically interact with their individual faithful on a daily basis, the gothi are responsible for servicing the spiritual needs of the community and granted special favor by their gods.</p>
					<p>The Gothi are capable of working together to magically control weather globally, but magic is inherently destructive and damaging to living things. They therefore reserve these powers for the utmost emergencies, and only engage in manipulating weather when asked to do so by their rulers.</p>
					<p>Because of the incredible power of the Tivar (and by extension the Gothi and their rulers), many Anpilayn view the northern religion as extremely dangerous and unethical, and the Church of Iruhan despises it.</p>
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
				background: url("images/gothi.jpg") no-repeat center center;
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
			table tr:nth-child(even) {
				background: #eee;
			}
			table {
				width: 95%;
				border-collapse: collapse;
			}
			.summary {
				margin-left: 0.7em;
				font-size: 90%;
			}
			.summary p {
				margin-top: 0;
				text-indent: 1em;
			}
			td:nth-child(n + 2) {
				text-align: center;
			}
			`;
		let shadow = this.attachShadow({mode: "open"});
		shadow.appendChild(style);
		let content = document.createElement("div");
		content.innerHTML = html;
		shadow.appendChild(content);
		// Add voting powers.
		let odiv = shadow.getElementById("votes");
		let kingdoms = [];
		for (let k in g_data.kingdoms) if (g_data.kingdoms.hasOwnProperty(k)) kingdoms.push(g_data.kingdoms[k]);
		kingdoms.sort((a,b)=>(b.name < a.name ? 1 : b.name > a.name ? -1 : 0));
		for (let k of kingdoms) {
			let votes = k.calcGothiVotes();
			if (votes["Alyrja"].v == 0 && votes["Lyskr"].v == 0 && votes["Rjinku"].v == 0 && votes["Syrjen"].v == 0) continue;
			let tr = document.createElement("tr");
			let td = document.createElement("td");
			td.innerHTML = "<report-link href=\"\">" + k.name + "</report-link>";
			tr.appendChild(td);
			for (let tivar of ["Alyrja", "Lyskr", "Rjinku", "Syrjen"]) {
				let td = document.createElement("td");
				td.innerHTML = votes[tivar].v > 0 ? votes[tivar].v : "";
				tr.appendChild(td);
			}
			odiv.appendChild(tr);
		}
	}
}
customElements.define("gothi-report", GothiReport);
