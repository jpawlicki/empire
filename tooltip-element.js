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
				:host { cursor: help; }
				#tooltip {
					display: none;
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
				}
				slot {
					text-decoration: solid underline;
					text-decoration-skip-ink: none;
				}
			</style>`;
		shadow.innerHTML = style + "<slot></slot>";
		var tooltipDiv = document.createElement("div");
		tooltipDiv.setAttribute("id", "tooltip");
		tooltipDiv.appendChild(document.createTextNode(this.getAttribute("tooltip")));
		shadow.appendChild(tooltipDiv);
		this.addEventListener("click", function() {
			this.expanded = !this.expanded;
			shadow.querySelector("#tooltip").style.display = this.expanded ? "block" : "none";
			this.mouseovertime = 0;
		});
		this.addEventListener("mouseout", function() {
			this.expanded = false;
			shadow.querySelector("#tooltip").style.display = "none";
			this.mouseovertime = 0;
		});
		this.addEventListener("mouseover", function() {
			this.mouseovertime = 1;
			setTimeout(function(obj) { if (obj.mouseovertime == 1) { obj.expanded = true; shadow.querySelector("#tooltip").style.display = "block"; }}, 750, this);
		});
	}
}
customElements.define("tooltip-element", TooltipElement);
