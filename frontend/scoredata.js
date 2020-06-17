/**
 * Returns an object of player percentiles.
 */
function getHistoricalScore(turn, kingdom, points) {
	let turnScores = {};
	if (scoreData.length > turn) turnScores = scoreData[turn];

	function filterScores(predicate) {
		let collection = [];
		for (let k in turnScores) {
			if (predicate(k)) for (let v of turnScores[k]) collection.push(v);
		}
		return collection;
	}

	function findPercentile(collection, value) {
		let lessThan = 0;
		let greaterThan = 0;
		for (let v of collection) {
			if (v <= value) lessThan++;
			else greaterThan++;
		}
		if (lessThan + greaterThan == 0) return 1;
		return lessThan / (lessThan + greaterThan);
	}

	return {
			"nation": findPercentile(filterScores((k) => k == kingdom), points),
			"culture": findPercentile(filterScores((k) => g_data.kingdoms.hasOwnProperty(k) && getNation(k).culture == getNation(kingdom).culture), points),
			"global": findPercentile(filterScores((k) => true), points)};
}

let scoreData = [];
{
	let req = new XMLHttpRequest();
	req.open("get", g_server + "/entry/scores", true);
	req.onerror = function (e) {
		console.log(e);
	};
	req.onload = function (ev) {
		if (req.status != 200) {
			req.onerror(req.status);
		} else {
			scoreData = JSON.parse(req.response).scores;
		}
	};
	req.send();
}
