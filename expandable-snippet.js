// <expandable-snippet>
// Attributes:
//   text          - the textual contents of the snippet.
//   max-length    - optional: how many characters to cut at.
//   show-newlines - optional: whether to show or suppress newlines (while cut).

class ExpandableSnippet extends HTMLElement {
	constructor() {
		super();
		this.expanded = false;
	}
	connectedCallback() {
		var shadow = this.attachShadow({mode: "open"});
		var maxlength = this.hasAttribute("max-length") ? this.getAttribute("max-length") : -1;
		var text = this.getAttribute("text");
		var style = "<style>:host { display: block; margin-left: 0.7em; " + (maxlength >= 0 && text.length > maxlength ? "cursor: pointer;" : "") + "color: #777; font-size: 75%; }</style>";
		var showNewlines = this.getAttribute("show-newlines");
		if (maxlength >= 0 && text.length > maxlength) text = text.substring(0, maxlength) + "...";
		if (showNewlines) text = text.replace(/\n/g, "<br/>");
		shadow.innerHTML = style + text;
		this.addEventListener("click", function() {
			this.expanded = !this.expanded;
			var text = this.getAttribute("text");
			if (!this.expanded && maxlength >= 0 && text.length > maxlength) text = text.substring(0, maxlength) + "...";
			if (this.expanded || showNewlines) text = text.replace(/\n/g, "<br/>");
			shadow.innerHTML = style + text;
		});
	}
}
customElements.define("expandable-snippet", ExpandableSnippet);
