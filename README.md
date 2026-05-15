# Carpooling System Database

## Overview

Carpooling system database and web app built around a MySQL schema, with a Tomcat deployment path for local testing. The system supports:

* Create and manage users (passengers and drivers)
* Create, schedule and manage rides (assign drivers and vehicles)
* Book rides as passengers
* Manage vehicles
* Store and retrieve reviews and notifications
* Track activity and audit logs
* Save frequently used routes for quick booking

## Database Schema

The database includes 10 tables:

1. Users — Application user accounts and basic profile information.

2. Passengers — Passenger profile data (extends `Users`).

3. Drivers — Driver profile and verification details (extends `Users`).

4. Vehicles — Driver-owned vehicle records.

5. Rides — Scheduled ride offerings created by drivers.

6. Bookings — Passenger reservations for rides.

7. Reviews — Ratings and comments for rides/drivers.

8. Notifications — Messages delivered to users.

9. Saved_Routes — User-saved origin/destination pairs for quick reuse.

10. Logs — Audit and activity records for user and system actions.

Key design notes:

* ISA relationship
  * `Passengers` and `Drivers` are subclasses of `Users`
  * They share the same primary key (`User_ID`)
* Data integrity
  * Foreign keys enforce relationships between tables
  * `Bookings`, `Rides`, `Reviews`, `Notifications`, `Logs`, `Saved_Routes`, `Passengers`, `Drivers`, and `Vehicles` are linked through primary key and foreign key constraints

## Tech Stack

* Java 21
* Jakarta/Servlet-based web app with JSP views
* Maven 3.8+
* Apache Tomcat 9.x
* MySQL 8.4 with JDBC via `mysql-connector-j`
* MySQL Workbench for schema design and inspection
* BCrypt for password hashing

## Run Locally

Follow these steps to prepare the database and run the web app locally.

### Database (MySQL)

1. Open MySQL Workbench.
2. Create or select a schema (example: `team7`).
3. Run the SQL schema file to create tables and insert sample data.

Or open and run `sql/schema.sql` in MySQL Workbench.

Refresh the schema in your client if the tables do not appear.

### Environment variables

The web app reads DB connection info from environment variables. Set these before starting Tomcat:

- For Tomcat, create `$CATALINA_HOME/bin/setenv.sh` with the exports

```bash
export DB_URL="jdbc:mysql://localhost:3306/team7"
export DB_USER="root"
export DB_PASSWORD="yourpassword"
```


### Web app (Tomcat)

Prerequisites: Java 21, Maven 3.8+, Apache Tomcat 9.x

Set your Tomcat home directory once in your shell:

```bash
export CATALINA_HOME=/path/to/apache-tomcat-9.0.xx
```

You can add this to your shell profile, for example `~/.zshrc`, so it is always available.

Build and deploy the WAR to the local Tomcat root with the provided Maven profile:

```bash
mvn -Pdeploy-local-tomcat package
```

This will build `target/ROOT.war` and copy it to `$CATALINA_HOME/webapps/ROOT.war`.

Start Tomcat:

```bash
$CATALINA_HOME/bin/startup.sh
```

Open the app at:

* http://localhost:8080/

After code changes, run the same Maven command to publish updates:

```bash
mvn -Pdeploy-local-tomcat package
```

Stop Tomcat:

```bash
$CATALINA_HOME/bin/shutdown.sh
```
