// <tooltip-element>
// Attributes:
//   tooltip       - the tooltip to show on click.
class TooltipElement extends HTMLElement {
	constructor() {
		super();
		this.expanded = false;
		this.mouseovertime = 0;
	}
	connectedCallback() {
		var shadow = this.attachShadow({mode: "open"});
		var style = `<style>
				:host { cursor: help; position: relative; }
				#tooltip {
					display: block;
					visibility: hidden;
					background: #ddd;
					color: #000;
					font-size: 90%;
					border-radius: 0.2em;
					border: 2px solid white;
					position: absolute;
					z-index: 10;
					max-width: 20em;
					padding: 0.2em;
					text-align: left;
					font-size: small;
					text-decoration: none;
					font-weight: normal;
					text-transform: none;
				}
				slot {
					text-decoration: solid underline;
					text-decoration-skip-ink: none;
				}
			</style>`;
		shadow.innerHTML = style + "<slot></slot>";
		var tooltipDiv = document.createElement("div");
		tooltipDiv.setAttribute("id", "tooltip");
		tooltipDiv.innerHTML = this.getAttribute("tooltip").replace(/\n/g, "<br/>");
		shadow.appendChild(tooltipDiv);
		this.addEventListener("click", function() {
			this.expanded = !this.expanded;
			shadow.querySelector("#tooltip").style.visibility = this.expanded ? "visible" : "hidden";
			this.mouseovertime = 0;
		});
		this.addEventListener("mouseout", function() {
			this.expanded = false;
			shadow.querySelector("#tooltip").style.visibility = "hidden";
			this.mouseovertime = 0;
		});
		this.addEventListener("mouseover", function() {
			this.mouseovertime = 1;
			setTimeout(function(obj) { if (obj.mouseovertime == 1) { obj.expanded = true; shadow.querySelector("#tooltip").style.visibility = "visible"; }}, 750, this);
		});
	}
}
customElements.define("tooltip-element", TooltipElement);

let num = function(c, places = 0, scale = 1) {
	return "<tooltip-element tooltip=\"" + c.v + " = " + c.explain() + "\">" + (Math.round(c.v * scale * Math.pow(10, places)) / Math.pow(10, places)) + "</tooltip-element>";
}
