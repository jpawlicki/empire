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
		var style = "<style>:host { display: block; margin-left: 0.7em; " + (maxlength >= 0 && this.getAttribute("text").length > maxlength - 3 ? "cursor: pointer;" : "") + "color: #777; font-size: 75%; }</style>";
		var showNewlines = this.getAttribute("show-newlines");
		shadow.innerHTML = style + this.getText(this.getAttribute("text"), showNewlines, maxlength, false);
		this.addEventListener("click", function() {
			this.expanded = !this.expanded;
			var text = this.getText(this.getAttribute("text"), showNewlines, maxlength, this.expanded);
			shadow.innerHTML = style + text;
		});
	}
	getText(text, showNewlines, maxlength, expanded) {
		if (!expanded && maxlength >= 0 && text.length > maxlength - 3) text = text.substring(0, maxlength) + "...";
		if (expanded || showNewlines) text = text.replace(/\n/g, "<br/>");
		let preCount = 0;
		text = text.replace(/%ascii%/g, t => preCount++ % 2 == 0 ? "<pre>" : "</pre>");
		if (preCount % 2 == 1) text += "</pre>";
		return text;
	}
}
customElements.define("expandable-snippet", ExpandableSnippet);
