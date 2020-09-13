function minus(v1, v2) {
	return {x: v1.x - v2.x, y: v1.y - v2.y};
}
function length(v) {
	return Math.sqrt(v.x * v.x + v.y * v.y);
}
function parallel(path, offset) {
	function add(a, b) { return {x: a.x + b.x, y: a.y + b.y }; }
	function scale(a, b) { return {x: a.x * b, y: a.y * b}; }
	let opath = [];
	let prevOrientation = {x: 0,  y: 0};
	let prevOrientationWeight = 0;
	for (let i = 0; i < path.length - 1; i++) {
		let orientation = minus(path[i + 1], path[i]);
		let len = length(orientation);
		if (len == 0) continue;
		orientation = scale(orientation, offset / len);
		let temp = orientation.x;
		orientation.x = orientation.y;
		orientation.y = -temp;
		let weightedOrientation = {x: (orientation.x + prevOrientation.x * prevOrientationWeight) / (1 + prevOrientationWeight), y: (orientation.y + prevOrientation.y * prevOrientationWeight) / (1 + prevOrientationWeight)};
		opath.push(add(path[i], weightedOrientation));
		prevOrientation = orientation;
		prevOrientationWeight = 1;
	}
	opath.push(add(path[path.length - 1], prevOrientation));
	return opath;
}
function getRegionShape(r) {
	let points = [];
	function eq(p, r) {
		return r.x == p.x && r.y == p.y;
	}
	function add(p) {
		for (let r of points) if (eq(p, r)) return;
		points.push(p);
	}
	function toNextPoint(point, points, path) {
		for (let b of g_geo.borders) {
			if (b.a != r.id && b.b != r.id) continue;
			let candidate = null;
			let reverseDirection = false;
			if (eq(point, b.path[0])) {
				candidate = b.path[b.path.length - 1];
			} else if (eq(point, b.path[b.path.length - 1])) {
				candidate = b.path[0];
				reverseDirection = true;
			}
			if (candidate == null) continue; // No point of this border is on this shape.
			let index = -1;
			for (let i = 0; i < points.length; i++) if (eq(candidate, points[i])) index = i;
			if (index == -1) continue; // Other point of this border is not on this shape.
			points.splice(index, 1);
			if (reverseDirection) for (let i = b.path.length - 2; i >= 1; i--) path.push(b.path[i]);
			else for (let i = 1; i < b.path.length - 1; i++) path.push(b.path[i]);
			return candidate;
		}
		return null;
	}
	for (let b of g_geo.borders) if (b.a == r.id || b.b == r.id) {
		add(b.path[0]);
		add(b.path[b.path.length - 1]);
	}
	let paths = [];
	while (points.length > 0) {
		let path = [];
		let point = points.pop();
		let zpoint = point;
		while (point != null) {
			path.push(point);
			// Pick a point, look for borders.
			let nextpoint = toNextPoint(point, points, path);
			if (nextpoint == null) { // Close to the zpoint.
				toNextPoint(point, [zpoint], path);
			}
			point = nextpoint;
		}
		paths.push(path);
	}
	return paths;
}
function shapeToPath(shapes, close = true) {
	return shapes.map(p => "M" + p.map(pp => pp.x + "," + pp.y).join("L") + (close ? "Z" : "")).join("");
}
