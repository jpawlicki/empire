// <notifications-panel>
class NotificationsPanel extends HTMLElement {
	constructor() {
		super();
		let shadow = this.attachShadow({mode: "open"});
		this.shadow = shadow;

		let style = document.createElement("style");
		style.textContent = `
			:host {
				display: flex;
				flex-direction: row;
				background-color: #eee;
				border-top: 1px solid #aaa;
			}
			#index {
				color: #00f;
			}
			#index div {
				float: right;
			}
			#index svg {
				cursor: pointer;
			}
			svg:hover path {
				fill: #800;
			}
			#index, #view {
				overflow: auto;
				flex-basis: 50%;
			}
			#view {
				flex-basis: 50%;
				white-space: pre-line;
			}
			#view svg {
				float: right;
				cursor: pointer;
				width: 24px;
				height: 24px;
				margin: 0.2em;
			}
			ul {
				margin: 0;
				padding: 0;
				list-style: none;
			}
			li {
				font-family: sans-serif;
				cursor: pointer;
				margin-top: 0.4em;
			}
			li:hover {
				text-decoration: underline;
			}
		`;
		let minimize = document.createElement("div");
		minimize.setAttribute("id", "minimize");
		let index = document.createElement("div");
		index.setAttribute("id", "index");
		let view = document.createElement("div");
		view.setAttribute("id", "view");
		let refresh = document.createElement("div");
		refresh.innerHTML = `
			<svg style="width:24px;height:24px" viewBox="0 0 24 24" title="Show hidden notifications">
				<path fill="#000000" d="M17.65,6.35C16.2,4.9 14.21,4 12,4A8,8 0 0,0 4,12A8,8 0 0,0 12,20C15.73,20 18.84,17.45 19.73,14H17.65C16.83,16.33 14.61,18 12,18A6,6 0 0,1 6,12A6,6 0 0,1 12,6C13.66,6 15.14,6.69 16.22,7.78L13,11H20V4L17.65,6.35Z" />
			</svg></div>`;
		index.appendChild(refresh);

		shadow.appendChild(style);
		shadow.appendChild(index);
		shadow.appendChild(view);
		shadow.appendChild(minimize);

		this.indexList = document.createElement("ul");
		index.appendChild(this.indexList);
		this.view = view;

		this.refreshButton = refresh;
		refresh.addEventListener("click", () => {
			for (let e of shadow.querySelectorAll("li")) e.style.display = "block";
			refresh.style.display = "none";
		});
	}

	setNotifications(notifications) {
		let refresh = this.refreshButton;
		refresh.style.display = "none";
		while (this.indexList.firstChild) this.indexList.removeChild(this.indexList.firstChild); // TODO: may leak DOM nodes since they still have listeners.
		let viewPane = this.view;
		for (let n of notifications) {
			let li = document.createElement("li");
			li.textContent = n.title;
			li.addEventListener("click", () => {
				for (let item of this.shadow.querySelectorAll("li")) item.style.fontWeight = "normal";
				li.style.fontWeight = "bold";
				viewPane.textContent = "";
				let closer = document.createElementNS("http://www.w3.org/2000/svg", "svg");
				closer.setAttribute("viewBox", "0 0 24 24");
				closer.setAttribute("title", "Hide");
				let p = document.createElementNS("http://www.w3.org/2000/svg", "path");
				p.setAttribute("d", "M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z");
				closer.appendChild(p);
				viewPane.appendChild(closer);
				closer.addEventListener("click", () => {
					li.style.display = "none";
					li.style.fontWeight = "normal";
					viewPane.textContent = "";
					refresh.style.display = "block";
					let count = 0;
				});
				viewPane.appendChild(document.createTextNode(n.text));
			});
			this.indexList.appendChild(li);
		}
	}
}
customElements.define("notifications-panel", NotificationsPanel);
