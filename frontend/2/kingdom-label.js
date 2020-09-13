// <kingdom-label kingdom="Aefoss">
class KingdomLabel extends HTMLElement {
	constructor() {
		super();
		let shadow = this.attachShadow({mode: "open"});
		shadow.innerHTML = `
			<style>
				#container {
					display: flex;
					flex-direction: row;
					border-: 1px solid #aaa
					cursor: pointer;
					align-items: center;
				}
				#icon {
					height: 2.5em;
					filter: grayscale(100%) drop-shadow(0 0 5px #fff);
					margin-right: 0.2em;
				}
				#name {
					font-size: 200%;
				}
			</style>
			<div id="container">
				<img id="icon" />
				<span id="name"></span>
			</div>
		`;
		let e = this;
		shadow.addEventListener("click", () => changeReport("kingdom/" + e.getAttribute("kingdom")));

		this.iconEle = shadow.getElementById("icon");
		this.nameEle = shadow.getElementById("name");
		this.containerEle = shadow.getElementById("container");
	}

	static get observedAttributes() { return ['kingdom']; }

	attributeChangedCallback(name, old, value) {
		this.iconEle.setAttribute("src", "images/heraldry/" + value.toLowerCase() + ".png");
		this.nameEle.textContent = value;
		this.containerEle.style.borderLeft = "1em solid " + getColor(value);
	}
}
customElements.define("kingdom-label", KingdomLabel);
