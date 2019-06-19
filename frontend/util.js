function titleCase(s) {
	return s.replace(/_/g, " ").replace(/\w\S*/g, t => t.charAt(0).toUpperCase() + t.substr(1).toLowerCase());
}
