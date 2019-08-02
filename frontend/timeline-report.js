class TimelineReport extends HTMLElement {
	constructor() { super(); }
	connectedCallback() {
		let html = `
			<div id="abspos">
				<div>Timeline</div>
				<input id="timeline" type="range" min="1" max="${g_turndata.length - 1}" step="1" value="${g_data.date}"/>
				<div id="weekdisplay">Week ${g_data.date}</div>
				<div id="timeleft"></div>
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
				background: url(${"images/timeline_strip.jpg"}) no-repeat center center;
				background-size: cover;
				padding-top: .2em;
				padding-bottom: .2em;
				font-size: 200%;
				color: #fff;
				text-shadow: 0 0 6px #000, 0 0 0 #000, 0 0 3px #000, 1px 1px 3px #000, -1px -1px 3px #000;
				text-align: center;
			}
			input {
				margin-top: 1em;
				width: 95%;
			}
			`;
		let shadow = this.attachShadow({mode: "open"});
		shadow.appendChild(style);
		let div = document.createElement("div");
		div.innerHTML = html;
		shadow.appendChild(div);
		shadow.getElementById("timeline").addEventListener("change", function() {
			let newDate = parseInt(shadow.getElementById("timeline").value);
			if (g_turndata[newDate] == undefined) {
				shadow.getElementById("weekdisplay").innerHTML = "Week " + newDate + " (Loading)";
				startRequest(newDate, function() {
					shadow.getElementById("weekdisplay").innerHTML = "Week " + newDate;
				});
			} else {
				start(newDate);
				shadow.getElementById("weekdisplay").innerHTML = "Week " + newDate;
			}
		});
		function recalcTime() {
			let view = "";
			if (g_turndata[g_turndata.length - 1].gameover) {
				view = "This game is over.";
			} else {
				let msRemaining = g_turndata[g_turndata.length - 1].next_turn - Date.now();
				if (msRemaining < 0) {
					view = "Turn expired - waiting for new turn data from server...";
				} else if (msRemaining < 1000 * 60) { // a minute
					view = "Orders due in " + Math.round(msRemaining / 1000) + " seconds";
				} else if (msRemaining < 1000 * 60 * 10) { // an hour
					view = "Orders due in " + Math.floor(msRemaining / 1000 / 60) + " minutes, " + Math.round(msRemaining / 1000) % 60 + " seconds";
				} else {
					view = "Orders due in " + Math.floor(msRemaining / 1000 / 60 / 60) + " hours, " + Math.floor(msRemaining / 1000 / 60) % 60 + " minutes";
				}
				if (shadow.isConnected) window.setTimeout(recalcTime, 500 - msRemaining % 1000);
			}
			shadow.getElementById("timeleft").innerHTML = view;
		}
		recalcTime();
	}
}
customElements.define("timeline-report", TimelineReport);
