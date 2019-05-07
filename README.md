Empire
============================

Empire is a lightweight browser-based grand strategy game with role-playing
elements, limited communication, and concurrent turns.

## Code Layout
The implementation is split into two groups - the frontend and backend.

Currently, the rules logic is implemented separately for the backend (in Java)
and the frontend (JavaScript) separately. 

All data is moved as JSON.

### Backend
The backend runs on a typical Google App Engine stack and is responsible for
syncing player orders and authoritatively evaluating the result of orders as
the turns advance.

The backend lives in the /backend folder.

### Frontend
The frontend serves a UI for players to view the current game state and issue
orders, along with predictive tools to help understand the outcome of actions
and applicable modifiers and forecasting. It can be run on any basic HTTP
server or even from local disk (no PHP or similar - it is entirely static
assets).

The frontend lives in the /frontend folder.

### Tools
The repository also contains some tools, including the map editor and various
GM tools.

The tools live in the /tools folder.

## Running Locally
The backend can be run locally and persists a database to your local
filesystem. Start a local instance of the backend:
```bash
cd backend && gradle appengineRun
```

The frontend can be run against a local server or against the production
server. By default, if you open a local file (i.e. use the `file:` protocol),
the frontend will connect to localhost:8080. If you use `http:` or `https:`, it
will connect to the standard production backend. The default can be overridden
by adding the `server=prod` or `server=local` query parameter to the URL of the
page.

Open the frontend and the kickoff tool using
```bash
google-chrome frontend/setup.html frontend/map1.html tools/kickoff.html
```

Modify the URL of both frontend tabs, appending `?g=0` to access game ID 0.

Set up the game as you see fit in the setup.html tab, start it using
kickoff.html, and then manipulate it using map1.html and kickoff.html to the
effect you desire.

When you are done, stop the backend (CTRL-C in the terminal running it). The
backend will reuse any state it saved on later runs. If you need to clear out
the state and start fresh, use:
```bash
rm backend/build/exploded-empire/WEB-INF/appengine-generated/local_db.bin
```
