function makeButton(title, name, path, listener, checked = false) {
	let l = document.createElement("label");
	l.setAttribute("title", title);
	l.addEventListener("click", listener);
	let i = document.createElement("input");
	i.setAttribute("type", "radio");
	i.setAttribute("name", name);
	i.checked = checked;
	l.appendChild(i);
	let s = document.createElementNS("http://www.w3.org/2000/svg", "svg");
	s.setAttribute("style", "width:24px;height:24px");
	s.setAttribute("viewBox", "0 0 24 24");
	l.appendChild(s);
	let p = document.createElementNS("http://www.w3.org/2000/svg", "path");
	p.setAttribute("d", path);
	s.appendChild(p);
	return l;
}
