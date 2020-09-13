// <tutorial-panel>
class TutorialPanel extends HTMLElement {
	constructor() {
		super();
		let shadow = this.attachShadow({mode: "open"});
		shadow.innerHTML = `
			<style>
				:host > div {
					display: flex;
					flex-direction: column;
					justify-content: space-between;
					height: 100%;
				}
				#controls {
					display: flex;
					flex-direction: row;
					justify-content: space-between;
				}
				#controls span {
					cursor: pointer;
					color: #00f;
					font-weight: bold;
				}
			</style>
			<div>
				<div id="contents"></div>
				<div id="controls">
					<span id="prev">← Prev</span>
					<span id="next">Next →</span>
					<span id="stop">Stop</span>
				</div>
			</div>
		`;
		shadow.querySelector("#stop").addEventListener("click", () => document.querySelector("tutorial-panel").style.display = "none");
		this.contents = shadow.getElementById("contents");
		this.tutorials = [
			"Welcome! I hope you enjoy the game!<p>You can launch this tutorial at any time from the Settings panel, accessed by the gear icon in the top right of the page.</p><p>You can close the tutorial at any time by clicking \"Stop\" at the bottom of this panel.</p><p>Click \"Next\" to continue.</p>",
		];
		this.page = 0;
		this.shadow = shadow;
		let e = this;
		shadow.querySelector("#prev").addEventListener("click", () => e.changePage(e.page - 1));
		shadow.querySelector("#next").addEventListener("click", () => e.changePage(e.page + 1));
	}

	launch() {
		this.style.display = "block";
		if (this.tutorials.length == 1) {
			{ // Goals
				let goals = "";
				let tutorialGoalDescs = {
					"PROSPERITY": "Feeding your people.",
					"HAPPINESS": "Keeping your people happy.",
					"RELIGION": "Spreading the " + g_data.kingdoms[whoami].calcStateReligion() + " religion.",
					"RICHES": "Treasuring up gold.",
					"TERRITORY": "Expanding your borders.",
					"GLORY": "Participating in large battles.",
				};
				for (let s of g_data.kingdoms[whoami].profiles) {
					goals += "<li>" + tutorialGoalDescs[s] + "</li>";
				}
				if (goals == "") goals = "You aren't being scored this game, and can play whatever way you want to.";
				else goals = "You earn points by: <ul>" + goals + "</ul>";
				this.tutorials.push("It is a time of international crisis. You rule over the nation of <kingdom-label style=\"font-size: 50%; vertical-align: bottom; display: inline-block;\" kingdom=\"" + whoami + "\"></kingdom-label>. " + goals);
			}
			this.tutorials.push("To accomplish your goals, you command national policies, characters, nobles, armies, navies, and spies. You will issue orders to these agents by using the controls on the right side of the screen. That side of the screen is divided into ten tabs.");
			this.tutorials.push("By default, the right side of the screen shows the \"home\" tab (a house icon). You can use the sliders here to control national tax, shipbuilding, rationing, and soldier pay policies.<p>For now, leave everything as-is, but take this opportunity to locate your nation on the map (you may have to scroll to bring it into view). Your nation's color is shown on the home tab next to your insignia and nation name. You can double-check that a region on map is controlled by your nation by clicking on it and matching the national color, insignia, and name in the pop-up to the ones in the home tab.</p>");
			this.tutorials.push("Click the shield icon to go to the \"units\" tab. Each of your characters, armies, navies, and nobles can take one action each turn. Additionally, you can divide your armies or navies by clicking the division button.<p>Armies and navies are more powerful when concentrated into single forces. Try ordering your armies to travel to a common region. You should see arrows appear on the map indicating how the armies will move as you do. For now, avoid entering regions controlled by other players, as this can cause battles.</p>");
			// TODO: Characters: suggest an admiral/general/governor/cardinal.
			this.tutorials.push("Your characters are powerful assets as well, and they get better at the jobs you order them to do.<p>It's a good idea to make one character into an admiral, to make your navies more effective. Click on the names of your characters and find the one with the best \"Admiral\" skill. Order them to travel to wherever your closest ships are. Don't worry if they can't reach the region directly - next turn you can order them to complete the move.</p><p>Similarly, it's a good idea to have a strong general. Find your best general and order them to \"Lead\" the army in the same region. The map will indicate the leadership by joining the character and army icons with a dashed line.</p><p>It's also a good idea to have a good spy or two. Spies do their work by hiding in nations and / or establishing spy rings. Find your best spy and order them to \"establish spy ring\". If the option isn't available, you already have a spy ring in that character's territory: order them to \"hide\" in an adjacent territory.</p><p>If you have any nobles, order them to \"Train\". (This is the default.)</p>");
			// TODO: Rebellious nation tutorial tweaks.
			this.tutorials.push("Click the pen icon to go to the \"letters\" tab. Diplomacy is paramount to your success, and you should write each of your neighbor nations a nice letter suggesting cooperation. You don't start the game knowing what they are trying to accomplish - it's a good idea to try to find out.");
			this.tutorials.push("Click the crossed eye icon to go to the \"intrigue\" tab. Each turn you will gain or lose leverage over other nations depending on your spies and theirs. Leverage can be spent to hatch plots, which can accomplish a great variety of things and can't necessarily be traced back to you. Your spies are more efficient at gaining leverage over your allies than your enemies - so be aware of you who befriend, and who befriends you. For now, though, there is nothing to do here.");
			this.tutorials.push("Click the barley icon to go to the \"food\" tab. On the first turn, your regions all have balanced food, so there is no need to move food around your nation. On future turns, it may be wise to evacuate food stocks from a region that is about to be invaded: though moving food is expensive, starvation is almost always worse.");
			this.tutorials.push("Click the crowd icon to go to the \"relationships\" tab. You can use these controls to adjust your relationships with other nations, or to cede them territory.<p>Attack Them: Generally when you are invading another nation, you want to order your forces to always attack that nation. (This is required to conquer territory.) Otherwise, though, you'll want to leave it as \"In our land\" or \"Never\" (if you want to allow the nation to move forces through your territory undeterred).</p><p>Tribute is an effective means of deterring nations with nobles from attacking you. Consider paying 25% tribute to your enemies when you find yourself on the losing side of a fight: it's better to lose a little gold than lose everything.</p><p>The remaining options determine what kinds of gifts you accept from nations. Land is almost always good, refugees increase your population (and therefore your income) but also the burden on your food supplies, buildings are beneficial but can allow other nations to convert your people, and armies are always handy but cost gold to maintain.</p>");
			this.tutorials.push("Click the pirate icon to go to the \"pirates\" tab. This tab shows how many pirates appear in the game next turn and the probability of them appearing in each nation. You generally don't want pirates in your regions, and can bribe them to go somewhere else, but they are not too dangerous at the start of the game, so it is fine to leave this be for now.");
			this.tutorials.push("Click the hurricane icon to go to the \"gothi\" tab. Nations following the Northern religion have a number of votes to summon powerful but destructive global storms. If your nation appears in this list, you control some of those votes. For now, don't vote to summon anything.<p></p>You can also use this tab to swear loyalty to the Cult. The Cult will raise any soldiers you've lost from the dead, but it's a risky move: if the cult spreads too much, you might lose everything. At any rate, during the first turn, you haven't lost any soldiers yet, do don't do it now.</p>");
			this.tutorials.push("Click the church icon to go to the \"church\" tab. Unlike the Northern religion, the Iruhan religion has a centralized authority that keeps an account of the morality of each nation (according to their point of view, anyways). Being a favorite of the church has benefits: the Church produces an enormous amount of gold each turn and divides it between nations according to those opinion scores.");
			this.tutorials.push("Click the gear icon to go to the \"settings\" tab. Here, you can access this tutorial, view the complete game rules, view past turns with the \"timeline\" tool, opt out of e-mail notifications, and write little notes to share with other players after the end of the game.<p>If at any point you decide you'd prefer to <span style=\"text-decoration: strike-through\">break my heart</span> quit the game, simply opt out of further e-mails. If you don't take any actions for two turns, an AI will take over for you. (You can always come back, though!)</p>");
			this.tutorials.push("You've now seen the controls for everything you can do in the game! The other side of your screen (the map) is used for figuring out <i>what</i> to do. The map shows the various regions of the world, armies, and navies.");
			this.tutorials.push("Armies and navies are colored according to their nation. You can click on them for more information about them. The number of pips surrounding an army or navy indicate its relative strength. It's best to get what you want without fighting, but if you do have to fight, make sure your forces are superior.<p>Armies and characters can be hidden from your sight. Most nations in the northern half of the map have armies that always hide when in friendly territory: don't assume these nations have no armies just because you can't see them!</p>");
			this.tutorials.push("The map has other view modes. The paintbrush will show cultures (which determines what kinds of armies a region produces), the church shows religion and ideology, the people icon shows population, the barley icon shows food supplies per capita, the frown shows unrest, and the four-pointed star shows what regions the Cult has accessed.");
			this.tutorials.push("There's alternatives to the map as well. The tabular view shows data in tabular format about nations, while the connections icon will take you to a summary of international relations. (That last one is pretty boring on the first turn, isn't it?) The chart icon shows your score ranking relative to other players of your nation in other games, and reminds you what you get or lose points for. See how high you can get! And don't worry if your score is low - to be perfectly honest, most of the players of this game have been playing it for a while. It might take a while to get the hang of.<p>Once you are happy with your orders for the turn, you can close the page and go about your business. Other players will put in their orders and you'll get an e-mail when the turn advances.</p>");
			this.tutorials.push("This is the end of the tutorial! If you want to learn more, check out the rules link from the settings tab. Good luck, have fun, and hey, it's your first game, don't worry about it too much!");
			this.tutorials.push("A few other tips:<ul><li>You can scroll the map by click and dragging.</li><li>You can zoom the map by holding shift and using the scrolling input on your pointing device.</li><li>Click on blue text for pop-ups.</li><li>Hover over underlined text for explanations or tooltips.</li><li>Report bugs on <a href=\"https://groups.google.com/g/empire-playtesters\">the mailing list</a>, by e-mailing Josh (gilgarn@gmail.com), or on the <a href=\"https://github.com/jpawlicki/empire\">project github</a>!</li><li>Thanks for playing! ♥</li></ul>");
			this.changePage(0);
		}
	}

	changePage(pageNum) {
		this.page = pageNum;
		this.contents.innerHTML = this.tutorials[pageNum];
		this.shadow.querySelector("#next").style.visibility = pageNum == this.tutorials.length - 1 ? "hidden" : "visible";
		this.shadow.querySelector("#prev").style.visibility = pageNum == 0 ? "hidden" : "visible";
	}
}
customElements.define("tutorial-panel", TutorialPanel);

function showTutorialPopup() {
	document.querySelector("tutorial-panel").launch();
}
