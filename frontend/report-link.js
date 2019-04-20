// <report-link>
// Attributes:
//   text      - the textual contents of the link.
//   href      - the report to pull up, in type/target form, e.g. kingdom/Zemsim

class ReportLink extends HTMLElement {
	constructor() { super(); }
	connectedCallback() {
		let href = this.getAttribute("href");
		var shadow = this.attachShadow({mode: "open"});
		shadow.innerHTML = "<style>:host { cursor: pointer; color: #00f; } slot { text-decoration: inherit; }</style><slot></slot>";
		this.addEventListener("click", function() { changeReport(href); });
		this.addEventListener("mouseover", function() { crosshair(href); });
		this.addEventListener("mouseout", function() { crosshair(undefined); });
	}
}
customElements.define("report-link", ReportLink);
