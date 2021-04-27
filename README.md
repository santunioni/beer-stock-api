## Beerstock API tests

This is my implementation of the test suite for a beerstock Rest API. The API was developed by [**Rodrigo Peleias**](https://github.com/rpeleias/beer_api_digital_innovation_one/tree/3269aa71b73318e0374dac79b6ebed65662b3e14), who also developed his test suite. My contributions were:

- Develop my own version of the test suite, using AssertJ instead of Hamscrest. In my opinion AssertJ is better because it is more readable and ***readability counts***.

- Write the **docker** configurations for running the MySQL databases in docker and writing the spring boot config-files, leaving the H2 database only for tests.

- Write **kubernetes** configuration files in case you want to run the API there.

- The tests were inspired by the awesome text [practical-test-pyramid](https://martinfowler.com/articles/practical-test-pyramid.html#TheImportanceOftestAutomation), which emphasizes importance of tests and guides to what a good test suite should be.

- Create the [requests.http](requests.http) file to document the API. In my opinion, making requests in the IDE is better than using Postman, because you can have files documenting the API in the same repository as the API it-self.

## Running

You can run the API in one of three ways. The first assumes you have **docker** installed, the second assumes you have **docker compose** also installed and the third assumes you have a running kubernetes cluster (on local machine with minikube, for example):

- **Docker:** Use the script `./scriptUp` as another option for running the infrastructure. The script forces the spring-boot app give it some time for the MySQL database to start gracefully. Bring down the infrastructure later with `./scriptDown`.

- **Docker compose:** Use the command `docker-compose up` to create the database and application containers. If you see error-messages, it is because the spring boot app didn't wait for MySQL to start gracefully. The app will crash once or twice, but will initialize after each crash, until the database is ready to accept connections. Bring down the infrastructure later with `docker-compose down`.

- **Kubernetes:** not working yet. For some reason the spring-boot api don't find the database service. 
