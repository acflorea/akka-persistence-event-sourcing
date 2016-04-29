Akka Persistence Event Sourcing backed up by <a href="http://cassandra.apache.org/">Cassandra</a> and <a href="http://thinkaurelius.github.io/titan/">Titan</a>
==============================================================================================

Example project with a simple CRUD REST API to a domain model persisted 
using akka-persistence with event sourcing.

* Uses Cassandra for journal persistence
* Uses Titan for snapshots persistence - WIP!

To start the spray-can server from sbt:
> re-start

To stop:
> re-stop


### Project summary

- simple CRUD REST API
- Spray "actor per request" model inspired by spray-actor-per-request Activator template 
and Cameo Pattern from Jamie Allen's "Effective Akka"
- simple domain model representing a Project and User
(A project is basically a collection of Bags of Items, items can be added to the project indirectly 
 by adding the Bag that contains them. The user can update the items's quantity and 
 a multiplicationFactor. She can also place a "fictional" order based on the configured project)
- akka-persistence event sourcing used to track changes to the domain model



