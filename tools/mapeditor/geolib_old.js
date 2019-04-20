// TODO: rewrite this in terms of cross.
function isLeft(v1, v2, t) {
	return (v2[0] - v1[0]) * (t[1] - v1[1]) - (t[0] - v1[0]) * (v2[1] - v1[1]);
}
// This deliberately includes all points on the polygon boundary.
function winding(point, polygon) {
	let wn = 0;
	for (let poly of polygon) {
		for (let i = 0; i < poly.length; i++) {
			let p = poly[i];
			let pn = poly[(i + 1) % poly.length];
			let pToPoint = minus(point, p);
			let pToPn = minus(pn, p);
			let dotp = dot(pToPoint, pToPn);
			let maxL = length(pToPn);
			if (maxL == 0) continue;
			maxL *= maxL;
			let crs = cross(pToPoint, pToPn);
			if (crs < 0.001 && crs > -0.001 && dotp >= 0 && dotp <= maxL) {
				return 0;
			}
			// Otherwise, this is not on the boundary, use the normal winding rule.
			if (p[1] <= point[1]) {
				if (pn[1] > point[1] && isLeft(p, pn, point) > 0) wn++;
			} else {
				if (pn[1] <= point[1] && isLeft(p, pn, point) < 0) wn--;
			}
		}
	}
	if (wn < 0) wn = -wn;
	return (wn % 2) == 1 ? 1 : -1;
}
function cross(v1, v2) {
	return v1[0] * v2[1] - v1[1] * v2[0];
}
function dot(v1, v2) {
	return v1[0] * v2[0] + v1[1] * v2[1];
}
function minus(v1, v2) {
	return [v1[0] - v2[0], v1[1] - v2[1]];
}
function length(v) {
	return Math.sqrt(v[0] * v[0] + v[1] * v[1]);
}
const add = function(a, b) { return [a[0] + b[0], a[1] + b[1]]; };
const scale = function(a, b) { return [a[0] * b, a[1] * b]; };
// Deliberately excludes the end points of each segment.
function intersection(p1, p2, q1, q2) {
	const eq = function(a, b) { return a[0] == b[0] && a[1] == b[1]; };
	if (eq(p1, q1) || eq(p1, q2) || eq(p2, q1) || eq(p2, q2)) return null;
	let r = minus(p2, p1);
	let s = minus(q2, q1);
	let crs = cross(r, s);
	if (crs == 0) return null; // Parallel or colinear.
	let t = cross(minus(q1, p1), (scale(s, 1 / crs)));
	let u = cross(minus(q1, p1), (scale(r, 1 / crs)));
	if (0 < t && t < 1 && 0 < u && u < 1) {
		return add(q1, scale(s, u));
	}
	return null;
}
// input: an array of border IDs
// returns: a dict of regions to arrays of point indexes
function segmentBorders(borderData, points) {
	// rotate borderData to make the 0 point not on a border.
	let adj = {};
	for (let i of borderData) for (let j of i) {
		if (j != -1) adj[j] = [];
	}
	for (let oo in adj) {
		let o = parseInt(oo);
		if (!adj.hasOwnProperty(o)) continue;
		let rotation = 0;
		// Find the best start point.
		let foundOff = false;
		for (let i = 0; i < borderData.length; i++) {
			if (borderData[i].includes(o)) {
				if (foundOff) {
					rotation = i;
					break;
				}
			} else {
				foundOff = true;
			}
		}
		let l = [];
		for (let i = 0; i < borderData.length; i++) {
			let ni = (i + rotation) % borderData.length;
			if (!borderData[ni].includes(o)) {
				if (l.length > 0) {
					adj[o].push(l);
					l = [];
				}
				continue;
			}
			l.push(points[ni]);
		}
		if (l.length > 0) adj[o].push(l);
	}
	// TODO(waffles): some island borders need Z, to close.
	return adj;
}
function toPath(pp, closed) {
	let path = "";
	for (let p of pp) {
		path += "M" + p[0][0] + "," + p[0][1];
		for (let i = 1; i < p.length; i++) {
			path += "L" + p[i][0] + "," + p[i][1];
		}
		if (closed) path += "Z";
	}
	return path;
}
function geoize(doBorder=true) {
	let regionElement = document.getElementById("regions");
	let seaRegionElement = document.getElementById("sea_regions");
	let borderElement = document.getElementById("borders");
	for (let i = 0; i < g_regions.length; i++) {
		let region = g_regions[i];
		let p = document.createElementNS("http://www.w3.org/2000/svg", "path");
		p.setAttribute("id", "region_" + i);
		p.setAttribute("d", toPath(region.path, true));
		p.addEventListener("click", function () { regionClick(i); });
		if (region.type == "land") regionElement.appendChild(p);
		else seaRegionElement.appendChild(p);
		if (!doBorder) continue;
		// // // // //
		for (let p of g_regions[i].path) {
			// Add any intersection points.
			for (let j = 0; j < i; j++) {
				for (let op of g_regions[j].path) {
					for (let oseg = 0; oseg < op.length; oseg++) {
						for (let thisseg = 0; thisseg < p.length; thisseg++) {
							let s = intersection(p[thisseg], p[(thisseg + 1) % p.length], op[oseg], op[(oseg + 1) % op.length]);
							if (s != null) {
								p.splice(thisseg + 1, 0, s);
								thisseg++;
								op.splice(oseg + 1, 0, s);
							}
						}
					}
				}
			}
		}
	}
	if (!doBorder) return;
	let finalborder = [];
	for (let i = 0; i < g_regions.length; i++) {
		for (let p of g_regions[i].path) {
			let border = []; // the id of the region it borders, or -1 if not part of a border
			outer: for (let pp of p) {
				for (let j = i + 1; j < g_regions.length; j++) {
					if (winding(pp, g_regions[j].path) == 1) {
						border.push([-1]);
						continue outer;
					}
				}
				// pp is a border if it is contained in a lower polygon but isn't contained in upper polygons.
				let found = false;
				let x = [];
				for (let j = i - 1; j >= 0; j--) {
					let w = winding(pp, g_regions[j].path);
					if (w == 0 || (!found && w > 0)) {
						x.push(j);
						found = w == 1;
					}
				}
				if (x.length != 0) {
					border.push(x);
				} else {
					border.push([-1]);
				}
			}
			let borders = segmentBorders(border, p);
			for (let b in borders) {
				if (!borders.hasOwnProperty(b)) continue;
				let pe = document.createElementNS("http://www.w3.org/2000/svg", "path");
				pe.setAttribute("d", toPath(borders[b]));
				borderElement.appendChild(pe);
				finalborder.push({"a": i, "b": b, "weight": 1, "border": borders[b]});
			}
		}
	}
	console.log("g_borders = " + JSON.stringify(finalborder) + ";");
}
function parallel(pathSet, offset) {
	let ret = [];
	for (let path of pathSet) {
		let opath = [];
		let prevOrientation = [0, 0];
		let prevOrientationWeight = 0;
		for (let i = 0; i < path.length - 1; i++) {
			let orientation = minus(path[i + 1], path[i]);
			let len = length(orientation);
			if (len == 0) continue;
			orientation = scale(orientation, offset / len);
			let temp = orientation[0];
			orientation[0] = orientation[1];
			orientation[1] = -temp;
			let weightedOrientation = [(orientation[0] + prevOrientation[0] * prevOrientationWeight) / (1 + prevOrientationWeight), (orientation[1] + prevOrientation[1] * prevOrientationWeight) / (1 + prevOrientationWeight)];
			opath.push(add(path[i], weightedOrientation));
			prevOrientation = orientation;
			prevOrientationWeight = 1;
		}
		opath.push(add(path[path.length - 1], prevOrientation));
		ret.push(opath);
	}
	return ret;
}
