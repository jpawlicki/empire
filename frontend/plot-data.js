class PlotData extends HTMLElement {
	constructor() { super(); }
	connectedCallback() {
		let plot = g_data.plots.find(p => p.plotId == parseInt(this.getAttribute("plot")));

		let html = `
			<h2>Operation ${c.plotId}</h2>
			<div id="objective">${c.type} ${c.targetId}</div>
			<div id="conspirators"></div>
			<div id="power">???</div>
			<label id="trigger"><input type="checkbox" name="plot_execute_${c.plotId}"></input>Trigger</label>
		`;
		// CSS
		let style = document.createElement("style");
		style.innerHTML = `
			h2 {
				
			}
			#objective {
				font-weight: bold;
				text-align: center;
			}
			label {
				display: block;
				font-size: 80%;
			}
			`;
		let shadow = this.attachShadow({mode: "open"});
		shadow.appendChild(style);
		let content = document.createElement("div");
		content.innerHTML = html;
		shadow.appendChild(content);
		// Add conspirators.
		{
			let cdiv = shadow.getElementById("conspirators");
			cdiv.innerHTML = plot.conspirators.sort().map(k => "<report-link href=\"kingdom/" + k + "\">" + k + "</report-link>").join(", ");
			let invitees = [];
			for (let k in g_data.kingdoms) if (!plot.conspirators.includes(k)) invitees.push(k);
			invitees.sort();
			for (let k of invitees) {
				let l = document.createElement("label");
				let i = document.createElement("input");
				i.setAttribute("type", "checkbox");
				i.setAttribute("name", "plot_invite_" + plot.plotId + "_" + k);
				l.appendChild(i);
				l.appendChild(document.createTextNode("Invite "));
				let r = document.createElement("report-link");
				r.setAttribute("href", "kingdom/" + k);
				r.appendChild(document.createTextNode(k));
				l.appendChild(r);
				cdiv.appendChild(l);
			}
		}
	}
}
customElements.define("plot-data", PlotData);
