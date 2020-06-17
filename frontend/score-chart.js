// <score-chart>
// setData([[a,b,c,d],[e,f,g,h]], vertical_axis_mins, vertical_axis_maxes)

class ScoreChart extends HTMLElement {
	constructor() {
		super();
	}
	connectedCallback() {
		this.shadow = this.attachShadow({mode: "open"});
		this.shadow.innerHTML = `
			<style>
				path {
					transition: d 1s;
				}
				#axis_v > path {
					stroke: #aaa;
					stroke-width: 0.25;
				}
			</style>
			<svg viewBox="0 0 260 100">
				<g id="axis_v"></g>
				<g>
					<path id="p0_100" d="M0,0L400,0" fill="#ccccff" stroke="#aaa" stroke-width="0.25"/>
					<path id="p10_90" d="M0,0L400,0" fill="#ccaacc"/>
					<path id="p20_80" d="M0,0L400,0" fill="#cc88aa"/>
					<path id="p30_70" d="M0,0L400,0" fill="#cc6688"/>
					<path id="p40_60" d="M0,0L400,0" fill="#cc4466"/>
				</g>
			</svg>
		`;
	}

	setData(data, mins, maxes) {
		let height = 100;
		let width = 260;
		let max = 0;
		let min = 0;
		for (let i of mins) if (i < min) min = i;
		for (let i of maxes) if (i > max) max = i;
		let range = max - min;
		let zero = height + min / range * height;
		let dataSeries = [];
		for (let i = 0; i < 21; i++) dataSeries.push([]);
		for (let column of data) {
			let columnSorted = [];
			for (let n of column) columnSorted.push(n);
			columnSorted.sort((a, b) => a - b);
			for (let pct = 0; pct <= 100; pct += 5) {
				let point = (columnSorted.length - 1) * pct / 100;
				let val = columnSorted[Math.floor(point)] * (1 - point % 1) + columnSorted[Math.ceil(point)] * (point % 1);
				dataSeries[pct / 5].push(-val * height / range + zero);
			}
		}
		this.shadow.getElementById("axis_v").innerHTML = "";
		for (let i = 0; i < mins.length; i++) {
			let p = document.createElementNS("http://www.w3.org/2000/svg", "path");
			p.setAttribute("d", "M" + (width * i / (mins.length - 1)) + "," + (zero - mins[i] * height / range) + "V" + (zero - maxes[i] * height / range));
			this.shadow.getElementById("axis_v").appendChild(p);
		}
		function toPath(seriesA, seriesB) {
			let d = "M0," + seriesA[0];
			for (let i = 1; i < seriesA.length; i++) d += "L" + (width / (seriesA.length - 1) * i) + "," + seriesA[i];
			for (let i = seriesB.length - 1; i >= 0; i--) d += "L" + (width / (seriesB.length - 1) * i) + "," + seriesB[i];
			d += "Z";
			return d;
		}
		this.shadow.getElementById("p0_100").setAttribute("d", toPath(dataSeries[0], dataSeries[20]));
		this.shadow.getElementById("p10_90").setAttribute("d", toPath(dataSeries[2], dataSeries[18]));
		this.shadow.getElementById("p20_80").setAttribute("d", toPath(dataSeries[4], dataSeries[16]));
		this.shadow.getElementById("p30_70").setAttribute("d", toPath(dataSeries[6], dataSeries[14]));
		this.shadow.getElementById("p40_60").setAttribute("d", toPath(dataSeries[8], dataSeries[12]));
	}
}
customElements.define("score-chart", ScoreChart);
