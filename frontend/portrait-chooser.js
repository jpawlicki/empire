// <portrait-chooser> is a filterable portrait-choosing element.
// Users must also import taggingdata.js, and define a selectPortrait function.
//   selectPortrait(index), where index is the numeric portrait number or null (cancelled).

class PortraitChooser extends HTMLElement {
	constructor() { super(); }
	connectedCallback() {
		let shadow = this.attachShadow({mode: "open"});
		shadow.innerHTML = `
			<style>
				:host {
					border: 2px solid white;
					border-radius: 1em;
					padding: 0.5em;
					background: #000;
					display: flex;
					flex-direction: row;
				}
				#filters {
					display: flex;
					flex-direction: column;
				}
				#filters label {
					display: flex;
					justify-content: space-between;
				}
				select {
					margin-left: 1em;
				}
				#portraits {
					display: flex;
					flex-wrap: wrap;
					overflow: auto;
				}
				#portraits > div {
					margin: 0.5em;
					width: 10em;
					height: 16.18em;
					background-size: 10em;
					background-repeat: no-repeat;
					background-position: top center;
					cursor: pointer;
				}
			</style>
			<div id="filters">
				<label>
					Style:
					<select name="style">
						<option>Any</option>
						<option value="portrait">Portrait</option>
						<option value="historical / allegorical">Historical / Allegorical</option>
					</select>
				</label>
				<label>
					Quality:
					<select name="quality">
						<option>Any</option>
						<option selected>Best</option>
						<option>Middle</option>
						<option>Poor</option>
					</select>
				</label>
				<label>
					Facial Hair:
					<select name="facial hair">
						<option>Either</option>
						<option>Yes</option>
						<option>No</option>
					</select>
				</label>
				<label>
					Crown:
					<select name="crown">
						<option>Either</option>
						<option>Yes</option>
						<option>No</option>
					</select>
				</label>
				<label>
					Child:
					<select name="child">
						<option>Either</option>
						<option>Yes</option>
						<option selected>No</option>
					</select>
				</label>
				<label>
					Cardinal:
					<select name="cardinal">
						<option>Either</option>
						<option>Yes</option>
						<option>No</option>
					</select>
				</label>
				<label>
					Moko:
					<select name="moko tattoos">
						<option>Either</option>
						<option>Yes</option>
						<option>No</option>
					</select>
				</label>
				<label>
					Magic:
					<select name="magic">
						<option>Either</option>
						<option>Yes</option>
						<option>No</option>
					</select>
				</label>
				<button>Cancel</button>
			</div>
			<div id="portraits">
			</div>
		`;
		let portDiv = shadow.getElementById("portraits");
		function click(i) {
			selectPortrait(i);
		}
		function filter() {
			let filters = [];
			for (let feature of ["facial hair", "crown", "child", "cardinal", "moko tattoos", "magic"]) {
				let val = shadow.querySelector("[name='" + feature + "']").value;
				if (val == "Yes") filters.push((quality, style, features) => features.includes(feature));
				else if (val == "No") filters.push((quality, style, features) => !features.includes(feature));
			}
			let quality = shadow.querySelector("[name='quality']").value;
			if (quality == "Best") filters.push((quality, style, features) => quality == 1);
			else if (quality == "Middle") filters.push((quality, style, features) => quality == 2);
			else if (quality == "Poor") filters.push((quality, style, features) => quality == 3);
			let styleval = shadow.querySelector("[name='style']").value;
			if (styleval != "Any") filters.push((quality, style, features) => style == styleval);

			for (let img of shadow.querySelectorAll("#portraits > div")) {
				let style = img.getAttribute("data-style");
				let quality = img.getAttribute("data-quality");
				let features = JSON.parse(img.getAttribute("data-features"));
				let pass = true;
				for (let filter of filters) if (!filter(quality, style, features)) {
					pass = false;
					break;
				}
				img.style.display = pass ? "block" : "none";
				if (pass) img.style.backgroundImage = "url(images/portraits/" + img.getAttribute("data-index") + ".png)";
			}
		}
		for (let i = 0; i < taggingdata.length; i++) {
			let d = document.createElement("div");
			d.addEventListener("click", () => click(i));
			d.setAttribute("data-index", i);
			d.setAttribute("data-quality", taggingdata[i].quality);
			d.setAttribute("data-style", taggingdata[i].style);
			d.setAttribute("data-features", JSON.stringify(taggingdata[i].features));
			portDiv.appendChild(d);
		}
		for (let x of shadow.querySelectorAll("select")) {
			x.addEventListener("change", filter);
		}
		filter();
		shadow.querySelector("button").addEventListener("click", () => selectPortrait(null));
	}
}
customElements.define("portrait-chooser", PortraitChooser);
