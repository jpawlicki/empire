// <zip-div>
// Attributes:
//   title         - the textual title.
//   color         - foreground color.
//   background    - background color.

class ZipDiv extends HTMLElement {
	constructor() {
		super();
		this.expanded = false;
	}
	connectedCallback() {
		let shadow = this.attachShadow({mode: "open"});
		let titleText = this.getAttribute("title");
		shadow.innerHTML = `
		  <style>
				:host {
					display: block;
					background: linear-gradient(270deg, ${this.getAttribute("background")} 25%, #fff);
					border: 1px solid black;
					margin: .2em;
				  padding: .1em;
				}
				#title {
					font-family: sans-serif;
				  cursor: pointer;
				}
				#content { display: none; }
			</style>
			<div id="title">▶ ${titleText}</div>
			<slot id="content"></slot>
		`;
		let title = shadow.getElementById("title");
		let o = this;
		title.addEventListener("click", function() {
			o.expanded = !o.expanded;
			shadow.getElementById("title").innerHTML = (o.expanded ? "▼ " : "▶ ") + titleText;
			shadow.getElementById("content").style.display = o.expanded ? "block": "none";
		});
	}
}
customElements.define("zip-div", ZipDiv);
