function getNation(who) {
	if (who == "Unruled") return new Kingdom("Unruled", {
		"name": "Unruled",
		"color_bg": "#dddddd",
		"color_fg": "#888888",
		"core_regions": [],
		"culture": "",
		"gold": 0,
		"gothi": {},
		"relationships": {},
		"tags": [],
		"goodwill": 0,
		"loyal_to_cult": false,
		"court": [],
		"score": {},
	});
	return g_data.kingdoms[who];
}

// ============ CALC ============
class Calc {
	constructor(op, args) {
		this.op = op;
		this.args = args;
		if (op == "+") {
			this.v = args.reduce((a, b) => (a + b.v), 0);
		} else if (op == "-") {
			this.v = args[0].v - args[1].v;
		} else if (op == "*") {
			this.v = args.reduce((a, b) => (a * b.v), 1);
		} else if (op == "/") {
			this.v = args[0].v / args[1].v;
		} else if (op == "max") {
			this.v = args.reduce((a, b) => (a > b.v ? a : b.v), 0);
		} else if (op == "min") {
			this.v = args.reduce((a, b) => (a < b.v ? a : b.v), Number.POSITIVE_INFINITY);
		} else if (op == "sqrt") {
			this.v = Math.sqrt(args[0].v);
		}
	}

	explain() {
		if (this.op == "sqrt") return "√(" + this.explainArg(this.args[0]) + ")";
		else if (this.op == "max") return "max(" + this.args.reduce((a, b) => (a + ", " + this.explainArg(b)), "").slice(2) + ")";
		else if (this.op == "min") return "min(" + this.args.reduce((a, b) => (a + ", " + this.explainArg(b)), "").slice(2) + ")";
		else return "(" + this.args.reduce((a, b) => (a + " " + this.op.replace("*", "×") + " " + this.explainArg(b)), "").slice(3) + ")";
	}

	explainArg(arg) {
		if (arg.explain != undefined) return arg.explain();
		return Math.round(10000 * (arg.unit.startsWith("%") ? arg.v * 100 : arg.v)) / 10000 + arg.unit + " {" + arg.why + "}";
	}

	static moddedNum(base, mods, flats = []) {
		let m2 = mods.slice();
		m2.unshift({"v": 1, "unit": "%", "why": "Normal Rate"});
		let inner = new Calc("*", [base, new Calc("+", m2)]);
		if (flats.length != 0) {
			let f2 = flats.slice();
			f2.unshift(inner);
			inner = new Calc("+", f2);
		}
		return new Calc("max", [{"v": 0, "unit": "", "why": "Minimum"}, inner]);
	}
}

// ============ PRIORITYQUEUE ============
// From https://stackoverflow.com/questions/42919469/efficient-way-to-implement-priority-queue-in-javascript
const parent = i => ((i + 1) >>> 1) - 1;
const left = i => (i << 1) + 1;
const right = i => (i + 1) << 1;
class PriorityQueue {
  constructor(comparator = (a, b) => a > b) {
    this._heap = [];
    this._comparator = comparator;
  }
  size() {
    return this._heap.length;
  }
  isEmpty() {
    return this.size() == 0;
  }
  peek() {
    return this._heap[0];
  }
  push(...values) {
    values.forEach(value => {
      this._heap.push(value);
      this._siftUp();
    });
    return this.size();
  }
  pop() {
    const poppedValue = this.peek();
    const bottom = this.size() - 1;
    if (bottom > 0) {
      this._swap(0, bottom);
    }
    this._heap.pop();
    this._siftDown();
    return poppedValue;
  }
  replace(value) {
    const replacedValue = this.peek();
    this._heap[0] = value;
    this._siftDown();
    return replacedValue;
  }
  _greater(i, j) {
    return this._comparator(this._heap[i], this._heap[j]);
  }
  _swap(i, j) {
    [this._heap[i], this._heap[j]] = [this._heap[j], this._heap[i]];
  }
  _siftUp() {
    let node = this.size() - 1;
    while (node > 0 && this._greater(node, parent(node))) {
      this._swap(node, parent(node));
      node = parent(node);
    }
  }
  _siftDown() {
    let node = 0;
    while (
      (left(node) < this.size() && this._greater(left(node), node)) ||
      (right(node) < this.size() && this._greater(right(node), node))
    ) {
      let maxChild = (right(node) < this.size() && this._greater(right(node), left(node))) ? right(node) : left(node);
      this._swap(node, maxChild);
      node = maxChild;
    }
  }
}
// ============ WORLD ============
class World {
	static calcGlobalIdeology() {
    let ideoW = {};
    for (let r of g_data.regions) {
      if (r.religion == undefined) continue;
      if (!r.religion.startsWith("Iruhan")) continue;
      if (!ideoW.hasOwnProperty(r.religion)) ideoW[r.religion] = 0;
      ideoW[r.religion] += r.population;
    }
    let max = 0;
    let dominantIdeology = undefined;
    for (let i in ideoW) {
      if (!ideoW.hasOwnProperty(i)) continue;
      if (ideoW[i] > max) {
        max = ideoW[i];
        dominantIdeology = i;
      }
    }
		return dominantIdeology;
	}
}

// ============ REGION ============
class Region {
	constructor(id, constEntry, dataEntry, date) {
		this.id =  id;
		this.name = dataEntry.name;
		this.type = dataEntry.type;
		this.path = constEntry.path;
		this.culture = dataEntry.culture;
		this.population = dataEntry.population;
		this.kingdom = dataEntry.kingdom;
		this.unrest_popular = dataEntry.unrest_popular;
		this.noble = dataEntry.noble;
		this.constructions = dataEntry.constructions;
		this.food = dataEntry.food;
		this.crops = dataEntry.crops;
		this.religion = dataEntry.religion;
		this.date = date;
		this.cult_accessed = dataEntry.cult_accessed;
		if (this.noble == undefined) this.noble = {};
	}

	calcNobleLevel() {
		if (this.noble.experience == undefined) return 0;
		return Math.sqrt(this.noble.experience + 1);
	}

	calcRecruitment(extraMod=0, governor=null, rationing=1, nobleMod=0) {
		let baseAmount = [
			{"v": this.population, "unit": " citizens", "why": "Regional Population"},
			{"v": 1 / 2000.0, "unit": " recruits / citizen", "why": "Recruitment Rate"}];
		let unrest = this.calcUnrest().v;
		if (unrest > .25) baseAmount.push({"v": 1.25 - unrest, "unit": "%", "why": "Unrest"});
		let base = new Calc("*", baseAmount);

		let mods = [];
		if (governor != null) {
			mods.push({"v": governor.calcLevel("governor") * 0.5 + 1, "unit": "%", "why": "Governor"});
		}
		if (this.calcNobleLevel() > 0) mods.push({"v": this.calcNobleLevel() * .1, "unit": "%", "why": "Noble"});
		if (this.nobleMod != 0) mods.push({"v": nobleMod, "unit": "%", "why": "Noble Action"});
		if (unrest > .25) mods.push({"v": .25 - unrest, "unit": "%", "why": "Unrest"});
		if (this.religion == "Northern (Rjinku)") mods.push({"v": 1, "unit": "%", "why": "Worships Rjinku"});
		if (this.religion == "Iruhan (Sword of Truth)") mods.push({"v": 1, "unit": "%", "why": "Sword of Truth ideology"});
		let effectiveRationing = Math.min(rationing, this.food / this.population);
		if (this.religion == "Tavian (River of Kuun)" && effectiveRationing > 1) {
			mods.push({"v": (effectiveRationing - 1) * 3, "unit": "%", "why": "Plentiful rations w/ River of Kuun"});
		}
		if ("Unruled" != this.kingdom && contains(getNation(this.kingdom).tags, "Patriotic")) mods.push({"v": .15, "unit": "%", "why": "Patriotic rulers"});
		if ("Unruled" != this.kingdom && contains(getNation(this.kingdom).tags, "War-like") && contains(getNation(this.kingdom).core_regions, this.id)) {
			let conquests = 0;
			for (let i = 0; i < g_data.regions.length; i++) if (this.kingdom == g_data.regions[i].kingdom && !contains(getNation(this.kingdom).core_regions, i)) conquests++;
			mods.push({"v": conquests * .07, "unit": "%", "why": "War-like rulers with " + conquests + " conquered regions"});
		}
		if (extraMod != 0) mods.push({"v": extraMod, "unit": "%", "why": "Hypothetical"});

		let flats = [];
		for (let c of this.constructions) {
			if (c.type == "temple") {
				if (c.religion == "Northern (Rjinku)") flats.push({"v": 40, "unit": " soldiers", "why": "Rjinku Temple"});
				if (c.religion == "Iruhan (Sword of Truth)") flats.push({"v": 40, "unit": " soldiers", "why": "Sword of Truth Temple"});
			}
		}

		return Calc.moddedNum(base, mods, flats);
	}

	calcUnrest() {
		let unrests = [];
		unrests.push({"v": this.unrest_popular, "unit": "% disapproval", "why": "Popular Unrest"});
		if (this.noble.unrest != undefined) {
			unrests.push({"v": this.noble.unrest, "unit": "% disapproval", "why": "Noble Unrest"});
		}
		let wrath = this.kingdom == "Unruled" ? 0 : Math.min(1, Math.max(0, -getNation(this.kingdom).goodwill / 100.0));
		if (this.religion == "Iruhan (Sword of Truth)" || this.religion == "Iruhan (Chalice of Compassion)" || this.religion == "Iruhan (Tapestry of People)") {
			unrests.push({"v": wrath, "unit": "% disapproval", "why": "Church Wrath"});
		}
		return new Calc("max", unrests);
	}

	calcTaxation(taxRate=1, governor=null, rationing=1, nobleMod=0) {
		let baseAmount = [
			{"v": this.population, "unit": " citizens", "why": "Regional Population"},
			{"v": 1 / 10000.0, "unit": " gold / citizen", "why": "Base Taxation Rate"},
			{"v": taxRate, "unit": "%", "why": "Hypothetical Tax Rate"}];
		let unrest = this.calcUnrest().v;
		if (unrest > .25) baseAmount.push({"v": 1.25 - unrest, "unit": "%", "why": "Unrest"});
		let base = new Calc("*", baseAmount);

		let mods = [];
		if (governor != null) {
			mods.push({"v": governor.calcLevel("governor") * 0.5 + 1, "unit": "%", "why": "Governor"});
		}
		if ("Unruled" != this.kingdom && contains(getNation(this.kingdom).tags, "Mercantile")) mods.push({"v": .15, "unit": "%", "why": "Mercantile rulers"});
		if (this.calcNobleLevel() > 0) mods.push({"v": this.calcNobleLevel() * .1, "unit": "%", "why": "Noble"});
		if (this.nobleMod != 0) mods.push({"v": nobleMod, "unit": "%", "why": "Noble Action"});
		let effectiveRationing = Math.min(rationing, this.food / this.population);
		if (this.religion == "Tavian (River of Kuun)" && effectiveRationing > 1) {
			mods.push({"v": (effectiveRationing - 1) * 3, "unit": "%", "why": "Plentiful rations w/ River of Kuun"});
		}
		if (this.religion == "Northern (Syrjen)") mods.push({"v": 0.75, "unit": "%", "why": "Worships Syrjen"});
		if (this.religion == "Iruhan (Chalice of Compassion)") mods.push({"v": -.3, "unit": "%", "why": "Chalice of Compassion ideology"});
		if ("Unruled" != this.kingdom && contains(getNation(this.kingdom).tags, "War-like") && contains(getNation(this.kingdom).core_regions, this.id)) {
			let conquests = 0;
			for (let i = 0; i < g_data.regions.length; i++) if (this.kingdom == g_data.regions[i].kingdom && !contains(getNation(this.kingdom).core_regions, i)) conquests++;
			mods.push({"v": conquests * .07, "unit": "%", "why": "War-like rulers with " + conquests + " conquered regions"});
		}
		let flats = [];
		let numForts = 0;
		for (let c of this.constructions) if (c.type == "fortifications") numForts++;
		for (let c of this.constructions) {
			if (c.type == "temple") {
				if (c.religion == "Northern (Syrjen)") flats.push({"v": 3, "unit": " gold", "why": "Syrjen Temple"});
				if (c.religion == "Iruhan (Tapestry of People)") flats.push({"v": 3, "unit": " gold", "why": "Tapestry of People Temple"});
				if (c.religion == "Northern (Alyrja)") flats.push(new Calc("*", [{"v": 1, "unit": " gold per fort", "why": "Tapestry of People Temple"}, {"v": numForts, "unit": " forts", "why": "Forts"}]));
				if (c.religion == "Iruhan (Chalice of Compassion)") flats.push({"v": -1, "unit": " gold", "why": "Chalice of Compassion Temple"});
			}
		}

		return Calc.moddedNum(base, mods, flats);
	}

	calcCrops() {
		return Calc.moddedNum({"v": this.crops, "unit": " crops", "why": "Already Planted"}, []);
	}

	calcHarvestCapacity() {
		let base = new Calc("*", [
			{"v": this.population, "unit": " citizens", "why": "Regional Population"},
			{"v": 25, "unit": " reaps / citizen", "why": "Global Havest Rate"}]);
		let mods = [];
		let unrest = this.calcUnrest().v;
		if (unrest > .25 && !contains(getNation(this.kingdom).tags, "Stoic")) mods.push({"v": .25 - unrest, "unit": "%", "why": "Unrest"});
		return Calc.moddedNum(base, mods);
	}

	calcHarvest() {
		return new Calc("min", [this.calcCrops(), this.calcHarvestCapacity()]);
	}

	calcNextHarvest() {
		let mods = [];
		if (this.calcNobleLevel() > 0) mods.push({"v": this.calcNobleLevel() * .05, "unit": "%", "why": "Noble"});
		let base = Calc.moddedNum(
			new Calc("*", [
				{"v": this.population, "unit": " citizens", "why": "Regional Population"},
				{"v": 13, "unit": " plants / citizen", "why": "Global Planting Rate"}]),
			mods);
		return new Calc("max", base, this.calcHarvestCapacity());
	}

	calcHarvestWeeks() {
		return new Calc("/", [
			this.calcHarvest(),
			this.calcConsumption()]);
	}

	calcNextHarvestWeeks() {
		return new Calc("/", [
			this.calcNextHarvest(),
			this.calcConsumption()]);
	}

	calcConsumption() {
		return Calc.moddedNum(
			new Calc("*", [{"v": this.population, "unit": " citizens", "why": "Regional Population"},
			{"v": 1, "unit": " measures / citizen", "why": "Base Consumption Rate"}]),
			[]);
	}

	calcFoodWeeks() {
		return new Calc("/", [
			{"v": this.food, "unit": " measures of food", "why": "Regional Stockpile"},
			this.calcConsumption()]);
	}

	calcFortification() {
		let forts = 0;
		for (let c of this.constructions) if (c.type == "fortifications") forts++;
		if (forts == 0) {
			return new Calc("+", [{"v": 1, "unit": "%", "why": "Base Fortification"}]);
		}
		return new Calc("min", [new Calc("+", [{"v": 1, "unit": "%", "why": "Base Fortification"}, {"v": forts * .15, "unit": "%", "why": "Fortifications (x" + forts + ")"}]), {"v": 3.5, "unit": "%", "why": "Maximum Fortification"}]);
	}

	calcMinConquestSize() {
		let mods = [];
		if ("Unruled" != this.kingdom && contains(getNation(this.kingdom).tags, "Stoic")) mods.push({"v": 1.5, "unit": "%", "why": "Stoic Nation"});
		mods.push({"v": this.calcFortification().v - 1, "unit": "%", "why": "Fortification"});
		// √(the population of the region) × the region’s fortification multiplier × (100% - the region’s unrest percentage) × 3
		return Calc.moddedNum(
			new Calc("*", [
				new Calc("sqrt", [{"v": this.population, "unit": " citizens", "why": "Regional Population"}]),
				new Calc("-", [{"v": 1, "unit": "%", "why": "Base Opposition"}, new Calc("*", [this.calcUnrest(), {"v": 0.5, "unit": "%", "why": "Unrest Importance"}])]),
				{"v": 6 / 100, "unit": "%", "why": "Base Conquest Factor"}
			]),
			mods);
	}

	calcMinPatrolSize() {
		let mods = [];
		if ("Unruled" != this.kingdom && contains(getNation(this.kingdom).tags, "Disciplined")) mods.push({"v": -.5, "unit": "%", "why": "Disciplined Nation"});
		mods.push({"v": this.calcUnrest().v * 2 - 0.7, "unit": "%", "why": "Unrest"});
		// √(the population of the region) × 3%
		return Calc.moddedNum(
			new Calc("*", [
				new Calc("sqrt", [{"v": this.population, "unit": " citizens", "why": "Regional Population"}]),
				{"v": 3 / 100, "unit": "%", "why": "Base Patrol Factor"}
			]),
			mods);
	}

	getNeighbors() {
		let n = new Set();
		for (var b of g_borders) {
			if (b.a == this.id) n.add(g_data.regions[b.b]);
			else if (b.b == this.id) n.add(g_data.regions[b.a]);
		}
		let nn = [];
		for (let r of n) nn.push(r);
		nn.sort((a, b) => a.name.localeCompare(b.name));
		return nn;
	}
	
	isCoastal() {
		for (let r of this.getNeighbors()) if (r.type == "water") return true;
		return false;
	}

	getFoodTransferDestinations() {
		let visited = {};
		let n = [];
		let q = this.getNeighbors();
		for (var i of q) {
			visited[i.name] = true;
			if (i.type == "land") n.push(i);
		}
		while (q.length > 0) {
			let i = q[0];
			q = q.slice(1);
			if (i.type == "water") {
				for (var j of i.getNeighbors()) {
					if (!visited.hasOwnProperty(j.name)) {
						visited[j.name] = true;
						q.push(j);
						if (j.type == "land") n.push(j);
					}
				}
			}
		}
		return n;
	}

	calcPirateWeight() {
		if (this.type == "water") return {"v": 0, "unit": " shares", "why": "Sea Region"};
		if (this.religion == "Northern (Alyrja)") return {"v": 0, "unit": " shares", "why": "Follows Alyrja"};
		let base = {"v": this.calcUnrest().v, "unit": " shares", "why": "Unrest"};
		let mods = [];
		if (this.noble.name != undefined) mods.push({"v": -0.5, "unit": "%", "why": "Noble"});
		let bribe = g_data.pirates.bribes[this.kingdom];
		if (bribe != undefined) {
			mods.push({"v": Math.pow(2, bribe / 30) - 1, "unit": "%", "why": "Bribe (" + bribe + " Gold)"});
		}
		return Calc.moddedNum(base, mods);
	}

	calcPirateThreat() {
		let globalWeight = 0;
		for (let r of g_data.regions) globalWeight += r.calcPirateWeight().v;
		return new Calc("/", [this.calcPirateWeight(), {"v": globalWeight, "unit": " shares", "why": "Total Global Shares"}]);
	}

	getRandomPointInRegion(centrality = 0, centerIfPossible=false) {
		function winding(point, polygon) {
			function isLeft(v1, v2, t) {
				return (v2.x - v1.x) * (t.y - v1.y) - (t.x - v1.x) * (v2.y - v1.y);
			}
			function minus(v1, v2) {
				return [v1.x - v2.x, v1.y - v2.y];
			}
			function cross(v1, v2) {
				return v1.x * v2.y - v1.y * v2.x;
			}
			function dot(v1, v2) {
				return v1.x * v2.x + v1.y * v2.y;
			}
			function length(v) {
				return Math.sqrt(v.x * v.x + v.y * v.y);
			}
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
					if (p.y <= point.y) {
						if (pn.y > point.y && isLeft(p, pn, point) > 0) wn++;
					} else {
						if (pn.y <= point.y && isLeft(p, pn, point) < 0) wn--;
					}
				}
			}
			if (wn < 0) wn = -wn;
			return (wn % 2) == 1 ? 1 : -1;
		}
		function getBoundingRect(path) {
			let minx = 9999999;
			let miny = 9999999;
			let maxx = -9999999;
			let maxy = -9999999;
			for (let p of path) {
				for (let i of p) {
					if (i.x > maxx) maxx = i.x;
					if (i.y > maxy) maxy = i.y;
					if (i.x < minx) minx = i.x;
					if (i.y < miny) miny = i.y;
				}
			}
			return [minx, miny, maxx - minx, maxy - miny];
		}
		let rect = getBoundingRect(this.path);
		rect[0] = rect[0] + centrality * rect[2] / 2;
		rect[1] = rect[1] + centrality * rect[3] / 2;
		rect[2] *= (1 - centrality);
		rect[3] *= (1 - centrality);
		for (let i = 0; i < 100; i++) { // 100 trials to find a suitable point.
			let rp;
			if (centerIfPossible && i == 0) {
				rp = {x: rect[0] + rect[2] / 2, y: rect[1] + rect[3] / 2};
			} else {
				rp = {x: rect[0] + Math.random() * rect[2], y: rect[1] + Math.random() * rect[3]};
			}
			if (winding(rp, this.path) == 1) {
				// Check quincrux inclusion, too.
				if (winding(minus(rp, {x: -5, y: -5}), this.path) != 1) continue;
				if (winding(minus(rp, {x:  5, y: -5}), this.path) != 1) continue;
				if (winding(minus(rp, {x: -5, y:  5}), this.path) != 1) continue;
				if (winding(minus(rp, {x:  5, y:  5}), this.path) != 1) continue;
				return rp;
			}
		}
		console.log("Failed to find a point in region " + this.id);
		return {x: 0,  y: 0};
	}

	calcCostToBuildTemple(builderNation, ideology) {
		let base = {"v": 30, "unit": "gold", "why": "Base cost"};
		let mods = [];
		let nation = getNation(builderNation);
		if (nation.tags.includes("Industrial")) mods.push({"v": -.25, "unit": "%", "why": "Industrial Nation"});
		if (nation.tags.includes("Mystical")) mods.push({"v": -.5, "unit": "%", "why": "Mystical Nation"});
		if (nation.tags.includes("Evangelical") && nation.calcStateReligion() != this.religion) mods.push({"v": -1, "unit": "%", "why": "Evangelical Nation"});
		if (ideology.startsWith("Iruhan ") && !this.religion.startsWith("Iruhan ") && World.calcGlobalIdeology() == "Iruhan (Vessel of Faith)") mods.push({"v": -1, "unit": "%", "why": "Vessel of Faith global ideology"});
		if (this.religion == "Iruhan (Tapestry of People)") {
			let templeBonus = true;
			for (let r of this.getNeighbors()) if (r.isLand() && (r.religion != this.religion || r.culture != this.culture)) templeBonus = false;
			if (templeBonus) mods.push({"v": -1, "unit": "%", "why": "Tapestry of People temple bonus"});
		}
		return Calc.moddedNum(base, mods);
	}

	calcCostToEstablishSpyRing(builderNation) {
		let base = {"v": 50, "unit": "gold", "why": "Base cost"};
		let mods = [];
		if (getNation(builderNation).calcStateReligion() == "Northern (Lyskr)") mods.push({"v": -1, "unit": "%", "why": "Lyskr state ideology"});
		return Calc.moddedNum(base, mods);
	}


	calcCostToBuildShipyard(builderNation) {
		let base = {"v": 80, "unit": "gold", "why": "Base cost"};
		let mods = [];
		let nation = getNation(builderNation);
		if (nation.tags.includes("Industrial")) mods.push({"v": -.25, "unit": "%", "why": "Industrial Nation"});
		return Calc.moddedNum(base, mods);
	}

	calcCostToBuildFortifications(builderNation) {
		let base = {"v": 20, "unit": "gold", "why": "Base cost"};
		let mods = [];
		let nation = getNation(builderNation);
		if (nation.tags.includes("Industrial")) mods.push({"v": -.25, "unit": "%", "why": "Industrial Nation"});
		if (this.religion == "Tavian (Flame of Kith)") mods.push({"v": -1, "unit": "%", "why": "Flame of Kith"});
		return Calc.moddedNum(base, mods);
	}
}


// ============ KINGDOM ============
class Kingdom {
	constructor(name, dataEntry) {
		this.name = name;
		this.color_bg = dataEntry.color_bg;
		this.color_fg = dataEntry.color_fg;
		this.core_regions = dataEntry.core_regions;
		this.culture = dataEntry.culture;
		this.gold = dataEntry.gold;
		this.gothi = dataEntry.gothi;
		this.relationships = dataEntry.relationships;
		this.tags = dataEntry.tags;
		this.goodwill = dataEntry.goodwill;
		this.loyal_to_cult = dataEntry.loyal_to_cult;
		this.taxratehint = dataEntry.taxratehint;
		this.shipratehint = dataEntry.shipratehint;
		this.signingbonushint = dataEntry.signingbonushint;
		this.rationhint = dataEntry.rationhint;
		this.score = dataEntry.score;
		this.profiles = dataEntry.profiles;
		this.send_email = dataEntry.send_email;
	}

	calcRecruitment() {
		let parts = [];
		for (let r of g_data.regions) {
			if (r.kingdom == this.name) parts.push({"v": r.calcRecruitment().v, "unit": " soldiers", "why": r.name});
		}
		return new Calc("+", parts);
	}

	calcTaxation() {
		let parts = [];
		for (let r of g_data.regions) {
			if (r.kingdom == this.name) parts.push({"v": r.calcTaxation().v, "unit": " gold", "why": r.name});
		}
		return new Calc("+", parts);
	}

	calcRelationship(kingdom) {
		if (kingdom == this) return "friendly";
		if (kingdom == "Unruled") return "enemy";
		let out = this.relationships[kingdom.name].battle;
		let inc = kingdom.relationships[this.name].battle;
		if (out == "ATTACK" || inc == "ATTACK") return "enemy";
		if (inc == "DEFEND" && out == "DEFEND") return "friendly";
		return "neutral";
	}

	calcFoodWeeks() {
		let partsFood = [];
		let partsConsumption = [];
		for (let r of g_data.regions) {
			if (r.kingdom != this.name) continue;
			partsFood.push({"v": r.food, "unit": " measures", "why": r.name});
			partsConsumption.push({"v": r.calcConsumption().v, "unit": " measures / week", "why": r.name});
		}
		return new Calc("/", [new Calc("+", partsFood), new Calc("+", partsConsumption)]);
	}

	calcPopulation() {
		let parts = [];
		for (let r of g_data.regions) {
			if (r.kingdom == this.name) parts.push({"v": r.population, "unit": " citizens", "why": r.name});
		}
		return new Calc("+", parts);
	}

	calcStateReligionWeights() {
		let weights = {};
		for (let r of g_data.regions) {
			if (r.kingdom != this.name) continue;
			if (!weights.hasOwnProperty(r.religion)) weights[r.religion] = 0;
			weights[r.religion] += r.population;
		}
		return weights;
	}

	calcStateReligion() {
		let weights = this.calcStateReligionWeights();
		let max = "Company";
		for (let w in weights) {
			if (!weights.hasOwnProperty(w)) continue;
			if (max == "Company" || weights[w] > weights[max]) max = w;
		}
		return max;
	}

	calcNumUniqueIdeologies() {
		let m = {};
		for (let r of g_data.regions) {
			if (r.kingdom != this.name) continue;
			for (let c of r.constructions) {
				if (c.type != "temple") continue;
				m[c.religion] = true;
			}
		}
		let c = 0;
		for (let k in m) if (m.hasOwnProperty(k)) c++;
		return c;
	}

	calcPirateThreat() {
		let totalweight = 0;
		let mods = [];
		for (let r of g_data.regions) {
			let w = r.calcPirateWeight().v;
			totalweight += w;
			if (r.kingdom == this.name) mods.push({"v": w, "unit": " shares", "why": r.name});
		}
		return new Calc("/", [new Calc("+", mods), {"v": totalweight, "unit": " shares", "why": "Global Total Weight"}]);
	}

	getRuler() {
		for (let c of g_data.characters) {
			if (c.kingdom == this.name && contains(c.tags, "Ruler")) return c;
		}
		return undefined;
	}

	calcGothiVotes() {
		let votes = {
			"Alyrja": {"v": 0, "total": 0},
			"Rjinku": {"v": 0, "total": 0},
			"Lyskr": {"v": 0, "total": 0},
			"Syrjen": {"v": 0, "total": 0},
		};
		for (let c of g_data.regions) {
			if (c.religion != undefined && c.religion.startsWith("Northern")) {
				let ideology = c.religion.substring(c.religion.indexOf("(") + 1, c.religion.indexOf(")"));
				if (c.kingdom == this.name) votes[ideology].v++;
				votes[ideology].total++;
			}
		}
		return votes;
	}
}


// ============ CHARACTER ============
class Character {
	constructor(dataEntry) {
		this.name = dataEntry.name;
		this.honorific = dataEntry.honorific;
		this.kingdom = dataEntry.kingdom;
		this.location = dataEntry.location;
		this.preparation = dataEntry.preparation;
		this.tags = dataEntry.tags;
		this.experience = dataEntry.experience;
		this.orderhint = dataEntry.orderhint;
		this.values = dataEntry.values;
		this.leadingArmy = dataEntry.leading_army;
		this.hidden = dataEntry.hidden;
		this.portrait = dataEntry.portrait;
	}

	calcLevel(dimension) {
		return Math.sqrt(this.experience[dimension] + 1);
	}

	calcPlotStrengthMod() {
		return this.calcLevel("spy") * 0.3;
	}
}


// ============ ARMY ============
class Army {
	constructor(dataEntry) {
		this.id = dataEntry.id;
		this.type = dataEntry.type;
		this.size = dataEntry.size;
		this.kingdom = dataEntry.kingdom;
		this.location = dataEntry.location;
		this.preparation = dataEntry.preparation;
		this.tags = dataEntry.tags;
		this.orderhint = dataEntry.orderhint;
		this.gold = dataEntry.gold;
	}

	calcStrength() {
		let base = this.type == "army"
				? new Calc("*", [{"v": this.size, "unit": " soldiers", "why": "Army Size"}, {"v": 1/100.0, "unit": " strength per soldier", "why": "Base Strength Rate"}])
				: new Calc("*", [{"v": this.size, "unit": " warships", "why": "Navy Size"}, {"v": 1, "unit": " strength per warship", "why": "Base Strength Rate"}]);
		let mods = [];
		let r = g_data.regions[this.location];
		let k = getNation(this.kingdom);
		let religion = k != undefined ? k.calcStateReligion() : "";
		if (contains(this.tags, "Steel")) mods.push({"v": .15, "unit": "%", "why": "Steel"});
		if (contains(this.tags, "Seafaring") && r.type == "water") mods.push({"v": 1.5, "unit": "%", "why": "Seafaring in Sea Region"});
		if (this.type == "army" && k != undefined && contains(k.tags, "Disciplined")) mods.push({"v": .1, "unit": "%", "why": "Disciplined Nation"});
		if (this.type == "army" && r.kingdom != "Unruled" && r.kingdom != undefined && k != undefined && k.calcRelationship(getNation(r.kingdom)) == "friendly") {
			let fort = r.calcFortification().v;
			if (fort > 1 && this.type == "army") {
				mods.push({"v": fort - 1, "unit": "%", "why": "Fortifications"});
			}
		}
		if (k != undefined) {
			let state = k.calcStateReligion();
			if (state == "Iruhan (Sword of Truth)") mods.push({"v": .15, "unit": "%", "why": "Iruhan (Sword of Truth) state ideology"});
			if (World.calcGlobalIdeology() == "Iruhan (Sword of Truth)" && state.startsWith("Iruhan")) mods.push({"v": .15, "unit": "%", "why": "Iruhan (Sword of Truth) global ideology and Iruhan state religion"});
			let numUniqueIdeologies = k.calcNumUniqueIdeologies();
			if (state == "Iruhan (Tapestry of People)") mods.push(new Calc("*", [{"v": 0.025, "unit": "%", "why": "Tapestry of People bonus per unique ideology"}, {"v": numUniqueIdeologies, "unit": " regions", "why": "unique ideologies"}]));
		}
		if (k != undefined && k.calcStateReligion().startsWith("Iruhan") && g_data.inspires_hint > 0) {
			mods.push(new Calc("*", [{"v": .05, "unit": "%", "why": "bonus per inspiration"}, {"v": g_data.inspires_hint, "unit": " Inspirations", "why": g_data.inspires_hint + " Iruhan cardinals inspiring in Sancta Civitate"}]));
		}
		for (let c of g_data.characters) {
			if (c.leadingArmy == this.id) mods.push({"v": c.calcLevel(this.type == "army" ? "general" : "admiral") * 0.2, "unit": "%", "why": "Led by " + c.name});
		}
		return Calc.moddedNum(base, mods);
	}

	calcCost(travelling) {
		if (this.tags.includes("Higher Power")) return Calc.moddedNum({"v": 0, "unit": " gold", "why": "High Power armies are free."}, []);
		let base = this.type == "army"
				? new Calc("*", [{"v": this.size, "unit": " soldiers", "why": "Army Size"}, {"v": 1 / 100.0, "unit": " gold per soldier", "why": "Base Cost Rate"}])
				: new Calc("*", [{"v": this.size, "unit": " warships", "why": "Navy Size"}, {"v": 1 / 3.0, "unit": " gold per warship", "why": "Base Cost Rate"}]);
		let mods = [];
		if (this.tags.includes("Crafts-soldiers") && !travelling) mods.push({"v": -0.5, "unit": "%", "why": "Crafts-soldiers not travelling."});
		let k = getNation(this.kingdom);
		if (k != undefined) {
			if (k.calcStateReligion().startsWith("Company")) mods.push({"v": -0.5, "unit": "%", "why": "Company state ideology"});
			if (k.tags.includes("Rebellious") && k.core_regions.includes(this.location)) mods.push({"v": -0.5, "unit": "%", "why": "Rebellious unit in core territory"});
		}
		return Calc.moddedNum(base, mods);
	}
}

// ============ SPY RING ============
class SpyRing {
	constructor(dataEntry) {
		this.nation = dataEntry.nation;
		this.location = dataEntry.location;
		this.strength = dataEntry.strength;
		this.hidden = dataEntry.hidden;
		this.involved_in_plot_id = dataEntry.involved_in_plot_id;
		this.involvement_type = dataEntry.involvement_type;
	}

	calcStrengthIn(region) {
		let ring = this;
		let mod = 1;
		if (g_data.tivar.veil) mod += 1;
		if (g_data.regions[this.location].kingdom == this.nation) for (let c of g_data.regions[this.location].constructions) if (c.type == "temple" && c.religion == "Northern (Lyskr)") mod += 0.2;
		for (let c of g_data.characters) if (c.kingdom == this.nation && c.location == region) mod += c.calcPlotStrengthMod().v;
		let gp = function(power, loc) {
			let r = g_data.regions[loc];
			if (r.kingdom == undefined || "Unruled" == r.kingdom) return [power * .8, loc];
			if (getNation(ring.nation).calcRelationship(getNation(r.kingdom)) == "friendly") {
				if (r.religion == "Northern (Lyskr)") return [power, loc];
				return [power * (.9 - r.calcUnrest().v / 5), loc];
			}
			return [power * (.7 + r.calcUnrest().v / 5), loc];
		}
		let pq = new PriorityQueue((a, b) => (a[0] > b[0]));
		let visited = {};
		pq.push(gp(this.strength * mod, this.location));
		while (!pq.isEmpty()) {
			let i = pq.pop();
			if (visited[i[1]]) continue;
			visited[i[1]] = true;
			if (i[1] == region) {
				return i[0];
			}
			for (let b of g_data.regions[i[1]].getNeighbors()) {
				if (!visited[b.id]) pq.push(gp(i[0], b.id));
			}
		}
		return 0;
	}
}

// ============ PLOT ============
class Plot {
	constructor(dataEntry) {
		this.plot_id = dataEntry.plot_id;
		this.power_hint = dataEntry.power_hint;
		this.power_hint_total = dataEntry.power_hint_total;
		this.target_id = dataEntry.target_id;
		this.type = dataEntry.type;
		this.conspirators = dataEntry.conspirators;
	}

	getTargetRegion() {
		let getTargetRegionCharacter = () => {
			let character = g_data.characters.find(c => c.name == this.target_id);
			return (character == undefined || character.location == -1) ? undefined : g_data.regions[character.location];
		}
		let getTargetRegionRegion = () => g_data.regions.find(r => r.name == this.target_id);
		let getTargetRegionChurch = () => g_data.regions[g_geo.holycity];
		let getTargetRegionNation = () => {
			if (g_data.kingdoms[this.target_id] == undefined) return undefined;
			let character = g_data.kingdoms[this.target_id].getRuler();
			return (character == undefined || character.location == -1) ? undefined : g_data.regions[character.location];
		}
		if (this.type == "ASSASSINATE") return getTargetRegionCharacter();
		if (this.type == "BURN_SHIPYARD" || this.type == "SABOTAGE_FORTIFICATIONS" || this.type == "SPOIL_FOOD" || this.type == "SPOIL_CROPS" || this.type == "INCITE_UNREST" || this.type == "PIN_FOOD" || this.type == "MURDER_NOBLE" || this.type == "POISON_RELATIONS") return getTargetRegionRegion();
		if (this.type == "PRAISE" || this.type == "DENOUNCE") return getTargetRegionChurch();
		if (this.type == "INTERCEPT_COMMUNICATIONS" || this.type == "SURVEY_NATION" || this.type == "STEAL_GOLD") return getTargetRegionNation();
	}

	getDefender() {
		let characterPlots = ["ASSASSINATE"];
		let regionPlots = ["BURN_SHIPYARD", "SABOTAGE_FORTIFICATIONS", "SPOIL_FOOD", "SPOIL_CROPS", "INCITE_UNREST", "PIN_FOOD", "MURDER_NOBLE", "POISON_RELATIONS"];
		let nationPlots = ["DENOUNCE", "INTERCEPT_COMMUNICATIONS", "SURVEY_NATION", "STEAL_GOLD"];
		let goodwillPlots = ["PRAISE"];
		if (characterPlots.includes(this.type)) {
			let c = g_data.characters.find(c => c.name == this.target_id);
			if (c == undefined) return undefined;
			return c.kingdom;
		}
		if (regionPlots.includes(this.type)) return g_data.regions.find(c => c.name == this.target_id).kingdom;
		if (nationPlots.includes(this.type)) return this.target_id;
		if (goodwillPlots.includes(this.type)) {
			let nations = [];
			for (let k in g_data.kingdoms) nations.push(k);
			nations.sort((a, b) => g_data.kingdoms[b].goodwill - g_data.kingdoms[a].goodwill);
			let index = nations.findIndex(a => a == this.target_id);
			return nations[index == nations.size - 1 ? index - 1 : index + 1];
		}
		return undefined;
	}

	getObjective() {
		let desc = "";
		if (this.type == "ASSASSINATE") desc = "Assassinate";
		else if (this.type == "BURN_SHIPYARD") desc = "Burn a shipyard in";
		else if (this.type == "SABOTAGE_FORTIFICATIONS") desc = "Sabotage a fortifications in";
		else if (this.type == "SPOIL_FOOD") desc = "Spoil food in";
		else if (this.type == "SPOIL_CROPS") desc = "Spoil crops in";
		else if (this.type == "INCITE_UNREST") desc = "Incite unrest in";
		else if (this.type == "PIN_FOOD") desc = "Pin food in";
		else if (this.type == "MURDER_NOBLE") desc = "Murder the noble of";
		else if (this.type == "POISON_RELATIONS") desc = "Poison noble/ruler relations in";
		else if (this.type == "PRAISE") desc = "Praise the deeds of";
		else if (this.type == "DENOUNCE") desc = "Denounce the deeds of";
		else if (this.type == "INTERCEPT_COMMUNICATIONS") desc = "Intercept communications of";
		else if (this.type == "SURVEY_NATION") desc = "Survey";
		else if (this.type == "STEAL_GOLD") desc = "Steal gold from";
		return desc + " " + this.target_id + " (vs " + this.getDefender() + ")";
		return undefined;
	}
}

function religionFromIdeology(ideo) {
	ideo = ideo.replace(/[()]/g, "");
	if (ideo == "Vessel of Faith" || ideo == "Chalice of Compassion" || ideo == "Sword of Truth" || ideo == "Tapestry of People") return "Iruhan (" + ideo + ")";
	if (ideo == "Alyrja" || ideo == "Rjinku" || ideo == "Lyskr" || ideo == "Syrjen") return "Northern (" + ideo + ")";
	if (ideo == "Flame of Kith" || ideo == "River of Kuun") return "Tavian (" + ideo + ")";
	return ideo;
}

// ============ CONSTANTS ============
let doctrineDescriptions = {
	"ANTIAPOSTASY": ["-10 opinion each turn for nations loyal to the Cult."],
	"ANTIECUMENISM": ["-20 opinion for constructing a non-Iruhan temple.", "-5 opinion each turn for having a non-Iruhan, non-Company state ideology."],
	"ANTISCHISMATICISM": ["-15 opinion for constructing a Vessel of Faith temple."],
	"ANTITERRORISM": ["-30 opinion for voting to summon any Gothi spell."],
	"CRUSADE": ["-35 opinion each turn for any Iruhan nation not attacking every non-Iruhan nation (except Companies)."],
	"DEFENDERS_OF_FAITH": ["+2 opinion per 100 casualties inflicted on nations with negative opinion.", "+15 opinion for conquering a region belonging to a ruler with negative opinion."],
	"FRATERNITY": ["-35 opinion each turn for any Iruhan nation attacking another Iruhan nation."],
	"INQUISITION": ["-35 opinion each turn for any Iruhan nation not attacking every Vessel of Faith nation."],
	"MANDATORY_MINISTRY": ["-35 opinion each turn for any nation controlling an unimprisoned Cardinal not in the Holy City."],
	"WORKS_OF_IRUHAN": ["+10 opinion for constructing an Iruhan temple."]
};

let g_scoreProfiles = {
	"PROSPERITY": {"selectable": true,  "description": ["+1 point per million rations of food your population consumes.", "+1 point per million measures consumed due to rations above 100%.", "-1 point per 6000 civilians that starve to death in your historical regions."]},
	"HAPPINESS":  {"selectable": true,  "description": ["+1 point per turn all your population is below 25% unrest.", "+1 point per turn 90% of your population is below 35% unrest.", "-1 point per turn 25% of your population is above 25% unrest.", "-2 points per turn 33% of your population is above 50% unrest."]},
	"RICHES":     {"selectable": true,  "description": ["+2 points per turn you have more than 2000 gold.", "+1 point per turn you have more than 500 gold.", "-1 point per turn you have less than 300 gold."]},
	"TERRITORY":  {"selectable": true,  "description": ["+3 points whenever you gain rulership of a region.", "-3 points whenever you lose rulership of a region."]},
	"GLORY":      {"selectable": true,  "description": ["+1 point per 1000 casualties you suffer or inflict.", "-1 point per turn."]},
	"RELIGION":   {"selectable": true,  "description": ["+1 points whenever a region is converted to your state religion.", "-1 points whenever a region is converted away from your state religion.", "+2 points whenever a region is converted to your state ideology.", "-2 points whenever a region is converted away from your state ideology."]},
	"CULTIST":    {"selectable": false, "description": ["+3 points whenever the Cult gains access to one of its objective regions.", "+1 point whenever the Cult gains access to a region."]}
};
		
let culture_tooltips = {
	"eolsung": "Eolsung are hearty northerners. Soldiers recruited in this region make skilled raiders, able to conceal their presence and attack enemy supply lines.",
	"anpilayn": "Anpilayn are cultured southerners. Soldiers recruited in this region are well-equipped and extremely formidable in battle.",
	"hansa": "Hansa are seafaring southerners. Soldiers recruited in this region make skilled sailors who can impress their enemies into service.",
	"tavian": "Tavian are desert-dwelling mystics. Soldiers recruited in this region make excellent riders and offset their own cost while garrisoning a region.",
	"tyrgaetan": "Tyrgaetan are nomadic hunters. Soldiers recruited in this region are immune to attrition due to climate and skilled at moving quickly through friendly territory."
};

let religion_tooltips = {
	"Iruhan (Sword of Truth)": "This region's inhabitants primarily follow the teachings of Iruhan, a philanthropist and miracle-worker. They pay particular attention to his cautions against the lure of idols and false gods. Consequently, this region experiences increased recruitment.",
	"Iruhan (Vessel of Faith)": "This region's inhabitants primarily follow the teachings of Iruhan, a philanthropist and miracle-worker. They pay particular attention to his message that a divine conscience is a part of each person. They reject the authority of the hierarchical Church of Iruhan, in favor of local ministers. Consequently, they do not suffer clerical unrest and are pleased whenever a temple following the Vessel of Faith ideology is constructed.",
	"Iruhan (Tapestry of People)": "This region's inhabitants primarily follow the teachings of Iruhan, a philanthropist and miracle-worker. They pay particular attention to his sermon that all people have an intrinsic value, regardless of their culture or creed, and that no single culture or creed has uncovered the divine truth. Consequently, they seek out new ideas differing viewpoints, and are more effective if neighboring a culturally, religiously, or ideologically diverse region.",
	"Iruhan (Chalice of Compassion)": "This region's inhabitants primarily follow the teachings of Iruhan, a philanthropist and miracle-worker. They pay particular attention to his conviction that all living beings must be treated with understanding and compassion. Consequently, they are more likely to sustainably treat their environments but generate less tax income.",
	"Northern (Alyrja)": "This region's inhabitants primarily follow the eldritch deity known as Alyrja. Alyrja demands that her worshippers be constantly vigilant, guarding those that trust them against all danger, and rewards her faithful with magical powers to achieve this. Consequently, the inhabitants of this region destroy pirates and don't mind limited rations.",
	"Northern (Rjinku)": "This region's inhabitants primarily follow the eldritch deity known as Rjinku. Rjinku demands that his worshippers show courage in all things, embrace conflict, and battle to prove their worth. Consequently, inhabitants of this region are more likely to become soldiers.",
	"Northern (Syrjen)": "This region's inhabitants primarily follow the eldritch deity known as Syrjen. Syrjen demands that her worshippers work cooperatively with each other to build and create. Consequently, this region benefits from increased economic activity.",
	"Northern (Lyskr)": "This region's inhabitants primarily follow the eldritch deity known as Lyskr. Lyskr demands that his worshippers guard their secrets closely. Consequently, foreigners find it more difficult to conduct intrigue here.",
	"Tavian (Flame of Kith)": "This region's inhabitants primarily follow the antitheist Tavian religion, which teaches that the deities of the world are enemies of humanity. They react to this unpalatable truth by forging a social compact known as the Flame of Kith, under which individuals are welcomed into the community simply because they are human. Consequently, this region is more attractive to immigrants.",
	"Tavian (River of Kuun)": "This region's inhabitants primarily follow the antitheist Tavian religion, which teaches that the deities of the world are enemies of humanity. They react to this unpalatable truth by forging a social compact known as the River of Kuun, under which the prosperity of the elite is forcibly shared among the community. Consequently, the feast is an important element of the region's culture and when plentifully rationed the region will have increased economic activity and patriotism."
};

let tagTooltips = {
	"Steel": "+15% as strong.",
	"Formations": "If the army would suffer between 45% and 90% casualties, it instead suffers 45% casualties.",
	"Pillagers": "While this army is the largest army in an enemy or neutral region, it suppresses recruitment and hoards the region's taxes. When it enters a region controlled by the same ruler, that ruler will gain those taxes.",
	"Raiders": "This army is hidden in friendly regions.",
	"Seafaring": "+150% as strong in sea regions.",
	"Impressment": "15% of casualties this army inflicts on enemies join this army instead of perishing.",
	"Scheming": "The leader of this army gains 1 spy experience each week.",
	"Crafts-soldiers": "This army costs 50% less gold on any turn it does not move.",
	"Weathered": "This army ignores the effects of gothi spells.",
	"Pathfinders": "This army never needs to spend extra weeks to cross a double or triple border.",
	"Pillagers (Pirate)": "While this army is the largest army in a region, it suppresses recruitment and diverts that region's taxes to pirate threat.",
	"Unpredictable": "This army moves randomly into a neighboring land region.",
	"Unruly": "If larger than 2000 soldiers, this army divides into two equal forces. This army (and any divisions) move randomly into a neighboring land region.",
	"Higher Power": "This army does not require payment and does not inflict casualties on other armies with Higher Power.",
	"Undead": "50% of non-Undead soldiers who fall in battle with at least one Undead army rise from the dead and are distributed as soldiers evenly among participating Undead armies.",
};
