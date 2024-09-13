# ETL Provenance Framework and Dashboard

This project implements a framework for tracking provenance about errors and problems in ETL processes. The goal is to track the origins, transformations, and movements of data through various stages of the ETL pipeline. Moreover, a dashboard is provided for visualizing the results and enabling the tracing back of errors and problems to their root causes. 

## Project Structure

 * `/tracking/` contains the provenance tracking framework written in Java.
 * `/dashboard/` contains a docker environment including:
   * Grafana and the provided dashboards
   * PostgreSQL and the initial database schema
* `/example/` contains a usage example project written in Java.

## Requirements & Development

* Docker and docker-compose are required.
* This project uses Lombok, which needs to be installed in your IDE.
* It is easiest to retrieve dependencies using Maven.
## Usage

### Docker environment

1. Install docker and docker-compose on the host system
2. Navigate to the folder `dashboard/docker`
3. Edit the file `.env`:
	* `PWD` should point to the absolute path of the docker folder.
	* `ES_PORT` will be the port of the event store / postgres database container
	* `ES_POSTGRES_PW` will be the password for user `postgres` in the event store
	* `GRAFANA_PORT` will be the port of grafana / the dashboards
4. Run `docker-compose up -d` to compose the docker images and start the containers

To close the containers run `docker-compose down`
To close the containers and remove all persisted data run `docker-compose down --volume`

The Grafana dashboards can be accessed at the host system and the configured port, e.g.: http://localhost:8443/
### Example project

Note that the event store is unpopulated when initialized. All problem categories will be filled once the framework first connects to the event store.

Additionally an example project is provided that connects to the event store (and populates it with the respective problem categories) and adds some problem events as an example to be viewed in the provided Grafana dashboards.

For this project to be run, a working set-up of Java, Maven and Lombok is required. Furthermore, the framework should be installed into the local Maven repository using `mvn install`.

The connection configuration is read from the file provided in `resources/db.properties`, which needs to contain `host`, `port`, `schema`, `user`, and `password` for the event store, respectively. A default file is provided.
## License

This project is licensed under the GPL V3 License. See the LICENSE files in the respective directories for details.
