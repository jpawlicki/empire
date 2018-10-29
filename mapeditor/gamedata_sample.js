g_turndata = {
	"regions": [],
	"kingdoms": {},
	"communications": [],
	"characters": [],
	"armies": [],
	"pirate": {},
	"gothi": {},
	"notifications": []
};

function generateSampleGamedata() {
	let dimensions = ["population", "food", "army", "navy", "gold"];
	let shares = {};
	let totalshares = {};
	for (let dimension of dimensions) totalshares[dimension] = 0;
	for (let k of g_gamestate.kingdoms) {
		shares[k.name] = {};
		for (let dimension of dimensions) {
			shares[k.name][dimension] = Math.random() + 0.5;
			totalshares[dimension] += shares[k.name][dimension];
		}
	}
	g_turndata.communications.push(
		{
			"signed": "Fiskrbaer", // Anonymous letters will say "Anonymous".
			"to": ["Aefoss"],
			"text": "Hallbjorn, you are so cool.",
			"post_date": 3,
		});
	g_turndata["pirate"] = {
		"threat": Math.random() * 100,
		"bribes": {
			"Fiskrbaer": 0,
			"Aefoss": 50,
			"Hosshofn": -50,
			"Holmslatr": -23,
		},
	}
	for (let k of g_gamestate.kingdoms) {
		g_turndata.kingdoms[k.name] = {
			"gold": 2423 / totalshares.gold * shares[k.name].gold,
			"gothi": {
				"Alyrja": Math.random() < 0.5,
				"Rjinku": Math.random() < 0.5,
				"Lyskr": Math.random() < 0.5,
				"Syrjen": Math.random() < 0.5,
			},
			"goodwill": (Math.random() - 0.5) * 300,
			"loyal_to_cult": Math.random() < 0.2,
			"court": [],
			"relationships": {},
		};
		for (let kk of g_gamestate.kingdoms) {
			if (k == kk) continue;
			g_turndata.kingdoms[k.name].relationships[kk.name] = {
				"battle": Math.random() < 0.33 ? "only_in_territory" : Math.random() < 0.5 ? "yes" : "no",
				"refugees": Math.random() < 0.5 ? "no" : "yes",
				"defend": Math.random() < 0.33 ? "yes" : Math.random() < 0.5 ? "no" : "defensive_wars_only",
				"tribute": Math.random() < 0.5 ? 0 : .25,
				"construct": Math.random() < 0.5 ? "yes" : "no",
			};
		}
		for (let i = 1; i < 5; i++) {
			let captor = (Math.random() < 0.1 && k.name != "Aefoss") ? "Aefoss" : "";
			g_turndata.characters.push({
				"name": "Character " + Math.floor(Math.random() * 100000),
				"kingdom": k.name,
				"captor": captor,
				"location": (captor == "" && k.name != "Aefoss" && Math.random() < 0.2) ? -1 : k.core_regions[Math.floor(Math.random() * k.core_regions.length)],
				"preparation": [],
				"tags": i == 1 ? ["Ruler"] : [],
				"experience": {
					"general": Math.floor(Math.random() * 30),
					"admiral": Math.floor(Math.random() * 30),
					"governor": Math.floor(Math.random() * 30),
					"spy": Math.floor(Math.random() * 30)
				},
				"orderhint": ""
			});
		}
	}
	let crises = ["recession", "wedding", "banditry", "unsafe border", "ennui", "cultism", "overwhelmed", "uprising", "starvation", "guild takeover"];
	let armyid = 1;
	let religions = [
		"Iruhan (Sword of Truth)",
		"Iruhan (Vessel of Faith)",
		"Iruhan (Chalice of Compassion)",
		"Iruhan (Tapestry of People)",
		"Northern (Alyrja)",
		"Northern (Lyskr)",
		"Northern (Rjinku)",
		"Northern (Syrjen)",
		"Tavian (Flame of Kith)",
		"Tavian (River of Kuun)",
	];
	for (let i = 0; i < g_regions.length; i++) {
		if (g_regions[i].type == "land") {
			let kingdom = "";
			for (let k of g_gamestate.kingdoms) {
				if (k.core_regions.includes(i)) kingdom = k;
			}
			g_regions[i].culture = kingdom.culture;
			g_turndata.regions.push({
				"population": 21000000 / totalshares.population * shares[kingdom.name].population / kingdom.core_regions.length,
				"kingdom": kingdom.name,
				"religion": religions[Math.floor(Math.random() * religions.length)],
				"unrest_popular": Math.random() / 2 + 0.1,
				"noble": Math.random() < 0.5 ? {} : {
					"name": "Sir Iassac Newton",
					"tags": ["Inspiring"],
					"crisis": Math.random() < 0.25 ? {} : { 
						"type": crises[Math.floor(Math.random() * crises.length)],
						"deadline": 4,
					},
					"unrest": Math.random() / 2 + 0.1,
				},
				"constructions": [
					{"type": "shipyard", "original_cost": 33},
					{"type": "temple", "religion": "Northern (Alyrja)", "original_cost": 33},
					{"type": "fortifications", "original_cost": 22}
				],
				"food": 21000000 * 9 / totalshares.food * shares[kingdom.name].food / kingdom.core_regions.length,
				"harvest": 7.5
			});
			g_turndata.armies.push({
				"id": armyid++,
				"type": "army",
				"size": 80769 / totalshares.army * shares[kingdom.name].army / kingdom.core_regions.length,
				"kingdom": kingdom.name,
				"location": i,
				"preparation": [],
				"tags": kingdom.culture == "anpilayn" ? ["Steel", "Formations"] :
						kingdom.culture == "eolsung" ? ["Pillagers", "Raiders"] :
						kingdom.culture == "hansa" ? ["Seafaring", "Impressment"] :
						kingdom.culture == "tavian" ? ["Riders", "Crafts-soldiers"] :
						["Weathered", "Pathfinders"],
				"orderhint": ""
			});
		} else {
			g_turndata.regions.push({});
		}
	}
	let coastalsByKingdom = {};
	for (let i = 0; i < g_regions.length; i++) {
		if (g_regions[i].type != "water") continue;
		let kingdoms = {};
		for (let j = 0; j < g_borders.length; j++) {
			if (g_borders[j].a == i && g_turndata.regions[g_borders[j].b].kingdom != undefined) kingdoms[g_turndata.regions[g_borders[j].b].kingdom] = true;
			if (g_borders[j].b == i && g_turndata.regions[g_borders[j].a].kingdom != undefined) kingdoms[g_turndata.regions[g_borders[j].a].kingdom] = true;
		}
		for (let k in kingdoms) {
			if (!coastalsByKingdom.hasOwnProperty(k)) coastalsByKingdom[k] = [];
			coastalsByKingdom[k].push(i);
		}
	}
	for (let k in coastalsByKingdom) {
		if (!coastalsByKingdom.hasOwnProperty(k)) continue;
		for (let i of coastalsByKingdom[k]) {
			g_turndata.armies.push({
				"id": armyid++,
				"type": "navy",
				"size": 2423 / totalshares.navy * shares[k].navy / coastalsByKingdom[k].length,
				"kingdom": k,
				"location": i,
				"preparation": [],
				"tags": [],
				"orderhint": ""
			});
		}
	}
	g_turndata.notifications.push({"title": "Battle in Silfroddr", "text": "Our armies were routed in Silfroddr - our force of 8421 soldiers suffered 6544 casualties. We have compiled a list of <a href=\"battlereport.html\" target=\"_blank\">names of the fallen</a>."});
	g_turndata.notifications.push({"title": "Treasury Report", "text": "We collected 321 gold in tax and 233 gold from sea trade, but spent 123 gold on the upkeep of our armies and navies."});
	g_turndata.kingdoms["Aefoss"].court.push({"name": "Lady Whelmsby", "tags": ["Inspiring"]});
	g_turndata.kingdoms["Aefoss"].court.push({"name": "Lord Ashford", "tags": ["Soothing"]});
}
