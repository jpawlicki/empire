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

The frontend lives in the / (root) folder.

### Tools
The repository also contains some tools - some current and some vestigial. This
includes the map editor and similar.
