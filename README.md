# Running

You need to run ```mvn install -DskipTests``` before running any tests. DynamoDBLocal needs the SQLite dependency jars in ```target/```, which is managed by a the by ```maven-dependency-plugin``` in the ```process-test-resources``` phase. Running the ```install``` goal ensures that all necessary precursors to running the tests are met.

Generally, running the tests though maven will not be a problem, but many IDEs don't come configured to run the maven build steps, instead just building the classes themselves. This means, you don't get the SQLite jar in the ```target/``` directory, and the tests appear to be broken.
