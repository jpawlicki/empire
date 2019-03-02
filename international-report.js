class InternationalReport extends HTMLElement {
	constructor() { super(); }
	connectedCallback() {
		let html = `
			<div id="abspos">
				<div>International</div>
				<svg id="graph" viewbox="0,0,1,1">
				</svg>
				<select id="chart_type">
					<option value="overall">Friendships / Enmities / Tributaries</option>
					<option value="refugees">Refugees</option>
					<option value="construction">Construction</option>
					<option value="cede">Region Cedes</option>
					<option value="fealty">Army Transfers</option>
				</select>
				<div id="legend">
				</div>
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
				background: url(${"images/international.jpg"}) no-repeat center center;
				background-size: cover;
				padding-top: .2em;
				padding-bottom: .2em;
				font-size: 200%;
				color: #fff;
				text-shadow: 0 0 6px #000, 0 0 0 #000, 0 0 3px #000, 1px 1px 3px #000, -1px -1px 3px #000;
				text-align: center;
			}
			#graph {
				width: 100%;
			}
			svg text {
				font: 0.055px sans-serif;
				text-anchor: middle;
				alignment-baseline: middle;
				pointer-events: none;
			}
			svg path {
				stroke-width: 0.002;
				fill: transparent;
				opacity: 1;
				transition: opacity .2s;
			}
			#legend svg {
				width: 25%;
				margin-right: 1em;
			}
			.dotted {
				stroke-dasharray: 0.02, 0.04;
				animation: dash 1.6s linear infinite;
			}
			@keyframes dash {
				to {
					stroke-dashoffset: -0.06;
				}
			}
			`;
		let shadow = this.attachShadow({mode: "open"});
		shadow.appendChild(style);
		let div = document.createElement("div");
		div.innerHTML = html;
		shadow.appendChild(div);
		let values = {
			"overall": {
				"legend": {
					"TRIBUTE": {"color": "#008800", "text": "Tribute"},
					"ATTACK": {"color": "#ff0000", "text": "Attacking"},
					"DEFEND": {"color": "#0000ff", "text": "Offering Friendship"}},
				"rel": function(a, b) {
					if (g_data.kingdoms[a].relationships[b].tribute > 0) return "TRIBUTE";
					return g_data.kingdoms[a].relationships[b].battle;
				},
				"weights": {"ATTACK": -1, "DEFEND": 2},
			},
			"refugees": {
				"legend": {
					"ACCEPT": {"color": "#0000ff", "text": "Accepting Refugees"}},
				"rel": function(a, b) {
					return g_data.kingdoms[a].relationships[b].refugees;
				},
				"weights": {"ACCEPT": 1},
			},
			"construction": {
				"legend": {
					"PERMIT": {"color": "#0000ff", "text": "Permits Construction"}},
				"rel": function(a, b) {
					return g_data.kingdoms[a].relationships[b].construct;
				},
				"weights": {"PERMIT": 1},
			},
			"cede": {
				"legend": {
					"ACCEPT": {"color": "#0000ff", "text": "Accepts Region Cedes"}},
				"rel": function(a, b) {
					return g_data.kingdoms[a].relationships[b].cede;
				},
				"weights": {"ACCEPT": 1},
			},
			"fealty": {
				"legend": {
					"ACCEPT": {"color": "#0000ff", "text": "Accepts Army Transfers"}},
				"rel": function(a, b) {
					return g_data.kingdoms[a].relationships[b].fealty;
				},
				"weights": {"PERMIT": 1},
			},
		};
		let gsel = shadow.getElementById("chart_type");
		let ele = this;
		gsel.addEventListener("change", function() { ele.makeSvg(values[gsel.value], shadow.getElementById("graph"), shadow.getElementById("legend")); });
		gsel.dispatchEvent(new CustomEvent("change"));
	}

	makeSvg(t, svg, legendEle) {
		svg.innerHTML = "";
		let rel = t.rel;
		let weights = t.weights;
		let legend = t.legend;
		let kingdoms = [];
		for (let k in g_data.kingdoms) kingdoms.push(k);
		function p(i) {
			return [
				.5 + Math.sin(i * 2.0 / kingdoms.length * Math.PI) * .46,
				.5 + Math.cos(i * 2.0 / kingdoms.length * Math.PI) * .46];
		}
		{
			function dist(a, b) {
				return Math.sqrt(Math.pow(a[0]-b[0], 2) + Math.pow(a[1]-b[1], 2));
			}
			function scoreG() {
				let s = 0;
				for (let i = 0; i < kingdoms.length; i++) {
					for (let j = 0; j < kingdoms.length; j++) {
						if (i == j) continue;
						let r = rel(kingdoms[i], kingdoms[j]);
						if (!weights.hasOwnProperty(r)) continue;
						// Reciporicated relationships get extra weight.
						s += weights[r] * dist(p(i), p(j)) * (r == rel(kingdoms[j], kingdoms[i]) ? 2 : 1);
					}
				}
				return s;
			}
			let changed = true;
			while (changed) {
				changed = false;
				let score = scoreG();
				for (let i = 0; i < kingdoms.length; i++) {
					for (let j = i + 1; j < kingdoms.length; j++) {
						let t = kingdoms[i];
						kingdoms[i] = kingdoms[j];
						kingdoms[j] = t;
						let s = scoreG();
						if (score > s) {
							score = s;
							changed = true;
						} else {
							// Switch back.
							kingdoms[j] = kingdoms[i];
							kingdoms[i] = t;
						}
					}
				}
			}
		}
		let lines = [];
		let allLines = [];
		for (let i = 0; i < kingdoms.length; i++) lines.push([]);
		for (let i = 0; i < kingdoms.length; i++) {
			let pA = p(i);
			for (let j = i + 1; j < kingdoms.length; j++) {
				let pB = p(j);
				let rAB = rel(kingdoms[i], kingdoms[j]);
				let rBA = rel(kingdoms[j], kingdoms[i]);
				if (rAB == rBA) {
					if (!legend.hasOwnProperty(rAB)) continue;
					// Draw a solid line
					let l = document.createElementNS("http://www.w3.org/2000/svg", "path");
					l.setAttribute("d", "M" + pA[0] + "," + pA[1] + "L" + pB[0] + "," +pB[1]);
					l.setAttribute("stroke", legend[rAB].color);
					svg.appendChild(l);
					lines[i].push(l);
					lines[j].push(l);
					allLines.push(l);
				} else {
					// Draw two dotted lines with arrow heads, curved out.
					let perp = [pB[1] - pA[1], pA[0] - pB[0]];
					let perpFac = 0.03;
					let perpA = [(pA[0] + pB[0]) / 2 + perp[0] * perpFac, (pA[1] + pB[1]) / 2 + perp[1] * perpFac];
					let perpB = [(pA[0] + pB[0]) / 2 + perp[0] * -perpFac, (pA[1] + pB[1]) / 2 + perp[1] * -perpFac];
					if (legend.hasOwnProperty(rAB)) {
						let l = document.createElementNS("http://www.w3.org/2000/svg", "path");
						l.setAttribute("d", "M" + pA[0] + "," + pA[1] + "Q" + perpA[0] + "," + perpA[1] + "," + pB[0] + "," +pB[1]);
						l.setAttribute("stroke", legend[rAB].color);
						l.setAttribute("class", "dotted");
						svg.appendChild(l);
						lines[i].push(l);
						lines[j].push(l);
						allLines.push(l);
					}
					// Draw two dotted lines with arrow heads, curved out.
					if (legend.hasOwnProperty(rBA)) {
						let l = document.createElementNS("http://www.w3.org/2000/svg", "path");
						l.setAttribute("d", "M" + pB[0] + "," + pB[1] + "Q" + perpB[0] + "," + perpB[1] + "," + pA[0] + "," +pA[1]);
						l.setAttribute("stroke", legend[rBA].color);
						l.setAttribute("class", "dotted");
						svg.appendChild(l);
						lines[i].push(l);
						lines[j].push(l);
						allLines.push(l);
					}
				}
			}
		}
		for (let i = 0; i < kingdoms.length; i++) {
			let pp = p(i);
			let c = document.createElementNS("http://www.w3.org/2000/svg", "circle");
			c.setAttribute("cx", pp[0]);
			c.setAttribute("cy", pp[1]);
			c.setAttribute("r", .03);
			c.setAttribute("fill", g_data.kingdoms[kingdoms[i]].color_bg);
			svg.appendChild(c);
			c.addEventListener("mouseover", function () {
				for (let l of allLines) {
					if (!contains(lines[i], l)) l.style.opacity = .2;
				}
			});
			c.addEventListener("mouseout", function () {
				for (let l of allLines) l.style.opacity = 1;
			});
			c.innerHTML = "<title>" + kingdoms[i] + "</title>";
			let t = document.createElementNS("http://www.w3.org/2000/svg", "text");
			t.setAttribute("x", pp[0]);
			t.setAttribute("y", pp[1] - .00475);
			t.innerHTML = "&nbsp;" + kingdoms[i].substr(0, 1) + "&nbsp;";
			t.setAttribute("fill", g_data.kingdoms[kingdoms[i]].color_fg);
			svg.appendChild(t);
		}
		legendEle.innerHTML = "";
		for (let l in legend) {
			legendEle.innerHTML += "<div><svg viewbox=\"0,-.0125,.25,.025\"><path class=\"dotted\" stroke=\"" + legend[l].color + "\" d=\"M0,0L.25,0\"></path></svg>" + legend[l].text;
			legendEle.innerHTML += "<div><svg viewbox=\"0,-.0125,.25,.025\"><path stroke=\"" + legend[l].color + "\" d=\"M0,0L.25,0\"></path></svg>" + legend[l].text + " (Reciprocated)";
		}
	}
}
customElements.define("international-report", InternationalReport);
