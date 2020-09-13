let g_palette = [
	{"color": "#fefe00", "color_fg": "#220044"}, 
	{"color": "#fe7299", "color_fg": "#000000"}, 
	{"color": "#20afe5", "color_fg": "#ffffff"}, 
	{"color": "#007e00", "color_fg": "#ffffff"}, 
	{"color": "#0300ef", "color_fg": "#ffffff"}, 
	{"color": "#53f2cc", "color_fg": "#ffffff"}, 
	{"color": "#fe5a00", "color_fg": "#000000"}, 
	{"color": "#a20066", "color_fg": "#000000"}, 
	{"color": "#5a3f00", "color_fg": "#ffffff"}, 
	{"color": "#ffdcff", "color_fg": "#000000"}, 
	{"color": "#978c00", "color_fg": "#ffffff"}, 
	{"color": "#54cc02", "color_fg": "#ffffff"}, 
	{"color": "#4f0000", "color_fg": "#ffffff"}, 
	{"color": "#005866", "color_fg": "#ffffff"}, 
	{"color": "#feffec", "color_fg": "#000000"}, 
	{"color": "#00998c", "color_fg": "#ffffff"}, 
	{"color": "#4f4c66", "color_fg": "#ffffff"}, 
	{"color": "#95594c", "color_fg": "#000000"}, 
	{"color": "#003300", "color_fg": "#ffffff"}, 
	{"color": "#998cbf", "color_fg": "#000000"}, 
	{"color": "#fea600", "color_fg": "#000000"}, 
	{"color": "#000044", "color_fg": "#ffffff"}, 
	{"color": "#bfa599", "color_fg": "#000000"}, 
	{"color": "#086dd8", "color_fg": "#ffffff"}, 
	{"color": "#ed00ff", "color_fg": "#000000"}, 
	{"color": "#727266", "color_fg": "#ffffff"}
];

function getColor(kingdom) {
	for (let i = 0; i < g_geo.kingdoms.length; i++) if (g_geo.kingdoms[i].name == kingdom) return g_palette[i].color;
	return "#000000";
}

function getForegroundColor(kingdom) {
	for (let i = 0; i < g_geo.kingdoms.length; i++) if (g_geo.kingdoms[i].name == kingdom) return g_palette[i].color_fg;
	return "#ffffff";
}

/* Mixes colors a and b. If amount = 0, returns a. If amount = 1, returns b. Otherwise, returns a linear mix of a and b. */
function mixColor(a, b, amount) {
	function hexToRgb(hex) {
		var result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
		return result ? {
			r: parseInt(result[1], 16),
			g: parseInt(result[2], 16),
			b: parseInt(result[3], 16)
		} : null;
	}
	function rgbToHex(c) {
		function componentToHex(c) {
			var hex = parseInt(c).toString(16);
			return hex.length == 1 ? "0" + hex : hex;
		}
		return "#" + componentToHex(c.r) + componentToHex(c.g) + componentToHex(c.b);
	}
	let av = hexToRgb(a);
	let bv = hexToRgb(b);
	return rgbToHex({
		r: av.r * (1 - amount) + bv.r * amount,
		g: av.g * (1 - amount) + bv.g * amount,
		b: av.b * (1 - amount) + bv.b * amount,
	});
}
