class ScoreReport extends HTMLElement {
	constructor() { super(); }
	connectedCallback() {
		let html = `
			<div id="name">Score</div>
			<div id="content">
				<div id="scoretable">
					<div>
						<div id="score_kingdom"></div>
						<div>NATION</div>
					</div>
					<div>
						<div id="score_culture"></div>
						<div>CULTURE</div>
					</div>
					<div>
						<div id="score_global"></div>
						<div>ALL-TIME</div>
					</div>
				</div>
				<expandable-snippet text="Your score is compared to players of your nation in other games (alternate histories), not to the other players in this game. It is finalized on week 27 and every 6 weeks thereafter - whether you are still in the game or not! You earn and lose points on active score profiles only. You can change which score profiles are active by ordering your ruler to spend the week reflecting. You keep points you have accumulated so far."></expandable-snippet>
				<h1>Total: <span id="scorepoints"></span></h1>
				<div id="profiles">
					<expandable-snippet text="All of the below conditions can stack."></expandable-snippet>
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
				background: url("images/score.jpg") no-repeat center center;
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
			#content > div {
				margin-left: 0.7em;
				font-size: 90%;
			}
			h1 {
				margin-top: 0.7em;
				margin-bottom: 0;
				margin-left: 0;
				font-size: 100%;
				font-weight: bold;
				font-family: sans-serif;
			}
			#scoretable {
				display: grid;
				grid-template-columns: 1fr 1fr;
				grid-template-rows: 6em 4em;
				text-align: center;
				font-family: sans-serif;
			}
			#scoretable > div > div:first-child {
				font-size: 200%;
			}
			#scoretable > div {
				font-size: 60%;
			}
			#scoretable > div:first-child {
				grid-column: 1 / 3;
				font-size: 150%;
			}
			.inactive {
				color: #777;
			}
			ul {
				font-size: 90%;
				margin-top: 0;
			}
			`;
		let shadow = this.attachShadow({mode: "open"});
		shadow.appendChild(style);
		let div = document.createElement("div");
		div.innerHTML = html;
		shadow.appendChild(div);
		let totalScore = 0;
		let profiles = g_data.kingdoms[whoami].profiles;
		for (let profile of Object.keys(g_scoreProfiles).sort((a, b) => {
			if (profiles.includes(a) && !profiles.includes(b)) return -1;
			if (profiles.includes(b) && !profiles.includes(a)) return 1;
			if (a < b) return -1;
			if (a > b) return 1;
			return 0;
		})) {
			if (!g_scoreProfiles[profile].selectable && !g_data.kingdoms[whoami].score.hasOwnProperty(profile) && !profiles.includes(profile)) continue;
			let p = document.createElement("div");
			p.appendChild(document.createTextNode(titleCase(profile)));
			if (g_data.kingdoms[whoami].score.hasOwnProperty(profile)) {
				p.appendChild(document.createTextNode(": " + Math.round(g_data.kingdoms[whoami].score[profile] * 10) / 10));
				totalScore += g_data.kingdoms[whoami].score[profile];
			}
			if (!profiles.includes(profile)) {
				p.setAttribute("class", "inactive");
				p.appendChild(document.createTextNode(" (inactive)"));
			}
			let d = document.createElement("ul");
			for (let desc of g_scoreProfiles[profile].description) {
				let li = document.createElement("li");
				li.appendChild(document.createTextNode(desc));
				d.appendChild(li);
			}
			p.appendChild(d);
			shadow.getElementById("profiles").appendChild(p);
		}
		shadow.getElementById("scorepoints").appendChild(document.createTextNode(Math.round(totalScore * 10) / 10));
		let percentiles = getHistoricalScore(g_data.date, whoami, totalScore);
		shadow.getElementById("score_kingdom").appendChild(document.createTextNode(Math.round(percentiles.nation * 100) + "%"));
		shadow.getElementById("score_culture").appendChild(document.createTextNode(Math.round(percentiles.culture * 100) + "%"));
		shadow.getElementById("score_global").appendChild(document.createTextNode(Math.round(percentiles.global * 100) + "%"));
	}
}
customElements.define("score-report", ScoreReport);
