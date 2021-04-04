# InstaCorda
A spike/PoC for Brazil's Central Bank Instant Payment implemented on R3 Corda blockchain

It allows for moving funds between two end users by consolidating asset transfers between the relevant Finance Institutions directly using the base FungibleAssets contract.

## How it works
The blockchain layer is composed of a base Network consisting of two Banks, the Central Bank and a Notary.
Each Node can store data about:
* An end user Account State, which records id info as well as its balance on a given Bank.

## How to build and deploy
### Local nodes network
Build Corda nodes with:
```bash
./gradlew DeployNodes
```
And run with: 
```bash
./build/nodes/runnodes.sh
```
This will open all nodes on differend terminal shells and allow you to interact with each individual node.

### Webserver
After building and running the network, run the webserver with
```bash
./gradlew RunTemplateServer
```
Which will expose the REST API endpoints on `localhost:10050`

The REST endpoints are:

/account
  /create
    Creates a new user account on a given bank node
  /client/:id
    Returns a given user from its personal ID
  /history/:id
    Returns the user's transaction history from its pertonal ID
    
/bank
  /create
    Creates a new bank account on the CentralBank Node
  /:id
    Returns a given bank from its legal ID
  /history/:id
    Returns the bank's transaction history from its legal ID
  
