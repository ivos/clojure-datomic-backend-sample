# Clojure REST + Datomic backend sample

Sample of a REST backend on Datomic written in Clojure.

## Usage

### Development

1. Start Datomic transactor

		$DATOMIC/bin/transactor $DATOMIC/config/samples/free-transactor-template.properties &

2. Optionally start Datomic console

		$DATOMIC/bin/console -p 9080 free datomic:free://localhost:4334/ &

	Open console at [http://localhost:9080/browse](http://localhost:9080/browse).

3. Start application via Leiningen

		lein ring server-headless

	Changes to source code are now automatically picked-up by the running application.

4. Invoke application:

		curl -i -X POST -H "Content-Type: application/json" -d '{
		    "name" : "Name 1",
		    "code" : "code-1",
		    "visibility" : "private"
		}' "http://localhost:3000/projects"

### Production

1. Build and package for production

		lein uberjar

2. Run in production

		java -jar target/uberjar/clojure-datomic-backend-sample-*-standalone.jar

## License

Copyright © 2016 Ivo Maixner

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
