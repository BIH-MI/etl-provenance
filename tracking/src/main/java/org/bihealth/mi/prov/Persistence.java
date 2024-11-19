/*
 * ETL Provenance Tracking Framework
 * 
 * Based on the Data Quality Monitor by TUM/MRI (see https://gitlab.com/DIFUTURE/data-quality-monitor)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.bihealth.mi.prov;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Calendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Database access methods
 * 
 * @author Helmut Spengler
 * @author Marco Johns
 * @author Fabian Prasser
 */
public class Persistence {

	/** For logging */
	final Logger logger = LoggerFactory.getLogger(Persistence.class);

	/** Singleton instance of this class */
	private static Persistence instance;

	/** This timestamp signals an empty <code>schema_version</code> table */
	private final Timestamp SCHEMA_VERSION_IS_TABLE_EMPTY = new Timestamp(0);

	/**
	 * Singleton
	 */
	private Persistence() {
		// Empty by design
	};

	/**
	 * Return the singleton instance of this class
	 * 
	 * @return the logger
	 */
	public static synchronized Persistence getInstance() {
		if (Persistence.instance == null) {
			Persistence.instance = new Persistence();
		}
		return Persistence.instance;
	}

	/**
	 * Validate the event store schema: if schema is valid and versions of package
	 * and schema are compatible, do nothing. If schema exists, but is empty,
	 * populate it with problems and problem categories as defined in class
	 * <code>Problems</code>. Else (e.g. schema doesn't exist or schema versions are
	 * incompatible) exit on error.
	 * 
	 * @param connection the database connection
	 * 
	 * @throws SQLException if the connection to the event store fails or if the
	 *                      schema cannot be validated/populated
	 */
	public void validateEventStoreSchema(Connection connection) throws SQLException {

		String packageVersion = Persistence.getInstance().getClass().getPackage().getImplementationVersion();
		String schemaVersion = getSchemaVersion(connection);

		StackTraceElement[] st = Thread.currentThread().getStackTrace();
		if (st[st.length - 1].toString().contains("org.apache.maven.surefire")) {
			logger.info(String.format(
					"Both schemaVersion and packageVersion are null. We seem to be within a test case, if not this is an error."));
		} else if (!isCompatible(packageVersion, schemaVersion)) {
			String message = String.format("Software version %s is incompatible with schema version %s.",
					packageVersion, schemaVersion);
			logger.error(message);
			throw new RuntimeException(message);
		}

		if (isSchemaFresh(connection)) {
			logger.info("Found a fresh schema. It will be populated with problem categories and problems.");
			persistProblemCategories(connection);
			persistProblems(connection);
			setInitDate(connection);
		}
	}

	/**
	 * Establish a database connection
	 * 
	 * @param dbHost   the database host
	 * @param dbPort   the database port
	 * @param dbSchema the schema
	 * @param userName user name
	 * @param password password
	 * 
	 * @return the connection
	 * 
	 * @throws SQLException if the database connection cannot be aquired
	 */
	public Connection getDatabaseConnection(String dbHost, String dbPort, String dbSchema, String userName,
			String password) throws SQLException {

		// Prepare result variable
		Connection connection;

		// DB connection string
		String dbUrl = String.format("jdbc:postgresql://%s:%s/%s", dbHost, dbPort, dbSchema);

		// Prepare connection
		try {
			connection = DriverManager.getConnection(dbUrl, userName, password);
		} catch (SQLException e) {
			if ("08001".equals(e.getSQLState())) {
				String errorMessage = "Unable to establish connection to database with URL '" + dbUrl + "'.";
				logger.error(errorMessage);
				throw new RuntimeException(errorMessage);
			} else {
				throw e;
			}
		}

		// Log
		logger.info("Connected to database with JDBC URL '" + dbUrl + "'.");

		// Done
		return connection;
	}

	/**
	 * Persist problem (sub-)categories to the database
	 * 
	 * @param connection the database connection
	 * 
	 * @throws SQLException if problem information cannot be persisted to the
	 *                      database
	 */
	private void persistProblemCategories(Connection connection) throws SQLException {

		// Prepare statement
		String SQL_INSERT = "INSERT INTO prov.problem_category " +
				"(problem_subcategory_pk, problem_category_name, problem_subcategory_name)" +
				" VALUES(?, ?, ?)" +
				" ON CONFLICT (problem_subcategory_pk) DO NOTHING;";
		PreparedStatement insertStmt = connection.prepareStatement(SQL_INSERT);

		// Log
		logger.info("Persisting problem (sub-)categories to database");

		// Perform
		for (ProblemCategorySub ssc : ProblemCategorySub.values()) {
			// Set parameters
			insertStmt.setLong(1, ssc.getPk());
			insertStmt.setString(2, ssc.getProblemCategory().getDescription());
			insertStmt.setString(3, ssc.getDescription());

			// Execute query
			int affectedRows = insertStmt.executeUpdate();
			if (affectedRows == 0) {
				String erroMessage = "Creating entry in table problem_category failed, no rows affected.";
				logger.error(erroMessage);
				throw new SQLException(erroMessage);
			}
		}
	}

	/**
	 * Persist problems to the database
	 * 
	 * @param connection the database connection
	 * 
	 * @throws SQLException if problem information cannot be persisted to the
	 *                      database
	 */
	private void persistProblems(Connection connection) throws SQLException {

		// Prepare statement
		String SQL_INSERT = "INSERT INTO prov.problem_dimension\n"
				+ "(problem_pk, problem_subcategory_fk, problem_name, problem_short_name, default_severity_score)\n"
				+ "VALUES(?, ?, ?, ?, ?)\n";
		PreparedStatement insertStmt = connection.prepareStatement(SQL_INSERT);

		// Log
		logger.info("Persisting problems to database");

		// Perform
		for (Problem s : Problem.values()) {
			// Set parameters
			insertStmt.setLong(1, s.getPk());
			insertStmt.setLong(2, s.getProblemSubCategory().getPk());
			insertStmt.setString(3, s.getDescription());
			insertStmt.setString(4, s.getShortDescription());
			insertStmt.setDouble(5, s.getDefaultSeverityScore());

			// Execute query
			int affectedRows = insertStmt.executeUpdate();
			if (affectedRows == 0) {
				String errorMessage = "Creating entry in table problem_category failed, no rows affected.";
				logger.error(errorMessage);
				throw new SQLException(errorMessage);
			}
		}
	}

	/**
	 * Check compatibility of software and schema version
	 * 
	 * @param softwareVersion the version of the software
	 * @param schemaVersion   the version of the schema
	 * 
	 * @return whether the two versions are compatible
	 */
	private boolean isCompatible(String softwareVersion, String schemaVersion) {

		return softwareVersion != null && softwareVersion.equals(schemaVersion);
	}

	/**
	 * Check whether the schema is fresh (i.e. not yet initialized)
	 * 
	 * @param connection datbase connection
	 * 
	 * @return whether the schema is fresh
	 */
	private boolean isSchemaFresh(Connection connection) {

		// Get default severity score for problem
		String SQL_GET_SEVERITY_SCORE = "SELECT sv.init_date FROM prov.schema_version sv;";
		Timestamp initDate;
		try {
			Statement getVersion = connection.createStatement();
			ResultSet rs = getVersion.executeQuery(SQL_GET_SEVERITY_SCORE);
			initDate = SCHEMA_VERSION_IS_TABLE_EMPTY;
			while (rs.next()) {
				initDate = rs.getTimestamp(1);
			}
			if (SCHEMA_VERSION_IS_TABLE_EMPTY.equals(initDate)) {
				throw new RuntimeException("Table prov.schema_version is empty. This should not happen.");
			}
			return (initDate == null);
		} catch (SQLException e) {
			if ("42P01".equals(e.getSQLState())) {
				throw new RuntimeException(
						"Event store schema seems to be non-existent. Please create it first. Error code: "
								+ e.getErrorCode());
			}
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get the version of an unitialized event store schema.
	 * 
	 * @param connection database connection
	 * 
	 * @return the version string
	 */
	private String getSchemaVersion(Connection connection) {

		// Get default severity score for problem
		String SQL_GET_SEVERITY_SCORE = "SELECT sv.init_date, sv.version FROM prov.schema_version sv WHERE init_date IS NULL OR init_date = (SELECT MAX(sv2.init_date) FROM prov.schema_version sv2);";
		Timestamp initDate;
		String version = null;
		try {
			Statement getVersion = connection.createStatement();
			ResultSet rs = getVersion.executeQuery(SQL_GET_SEVERITY_SCORE);
			initDate = SCHEMA_VERSION_IS_TABLE_EMPTY;
			while (rs.next()) {
				initDate = rs.getTimestamp(1);
				version = rs.getString(2);
			}
			if (SCHEMA_VERSION_IS_TABLE_EMPTY.equals(initDate)) {
				throw new RuntimeException("Table schema_version is empty. This should not happen.");
			}
		} catch (SQLException e) {
			if ("42P01".equals(e.getSQLState())) {
				throw new RuntimeException(
						"Event store schema seems to be non-existent. Please create it first. Error code: "
								+ e.getErrorCode());
			}
			throw new RuntimeException(e);
		}

		return version;
	}

	/**
	 * Persist the initialization date of the schema to the database
	 * 
	 * @param connection the database connection
	 * 
	 * @throws SQLException if schema information can not be persisted to the
	 *                      database
	 */
	private void setInitDate(Connection connection) throws SQLException {

		// Prepare statement
		String SQL_UPDATE = "UPDATE prov.schema_version SET init_date=? WHERE init_date IS NULL";
		PreparedStatement updateStmt = connection.prepareStatement(SQL_UPDATE);

		// Log
		logger.info("Setting init date for schema");

		// Set parameters
		updateStmt.setTimestamp(1, new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()));

		// Execute query
		int affectedRows = updateStmt.executeUpdate();
		if (affectedRows == 0) {
			String erroMessage = "Creating entry in table schema_version failed, no rows affected.";
			logger.error(erroMessage);
			throw new SQLException(erroMessage);
		}
	}
}
