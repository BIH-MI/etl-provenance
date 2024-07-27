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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Interface to track problems encountered during ETL processes.
 * 
 * @author Helmut Spengler
 * @author Marco Johns
 * @author Fabian Prasser
 */
public class ProblemTracker {

	/**
	 * Represents a problem collection for a specific job, problem, entity and
	 * attribute
	 */
	@Getter
	@AllArgsConstructor
	@EqualsAndHashCode
	private class ProblemCollection {

		/** The JobId */
		private final Integer jobPk;

		/** The problem */
		private final Problem problem;

		/** The entity */
		private final String entity;

		/** The attribute */
		private final String attribute;
	}

	/**
	 * Encapsulates all information relevant for an ETL job
	 */
	public class Job {

		/** The jobPk */
		private final int pk;

		/** The counts of absent problems */
		@Getter
		private final Map<ProblemCollection, Integer> problemAbsentCounts;

		/** The counts of present problems */
		@Getter
		private final Map<ProblemCollection, Integer> problemPresentCounts;

		/**
		 * Constructor
		 * 
		 * @param pk the primary key of the job
		 */
		public Job(int pk) {
			this.pk = pk;
			this.problemAbsentCounts = new HashMap<>();
			this.problemPresentCounts = new HashMap<>();
		}
	}

	/** For self-logging */
	final static Logger logger = LoggerFactory.getLogger(ProblemTracker.class);

	/** Singleton instance of this class */
	private static ProblemTracker instance;

	/** Database connection */
	private final Connection connection;

	/**
	 * Prohibit call of constructor from outside this class.
	 */
	private ProblemTracker() {
		this.connection = null;
	};

	/**
	 * Constructor
	 * 
	 * @param connection the database connection
	 */
	private ProblemTracker(Connection connection) {
		this.connection = connection;
	};

	/**
	 * Return the singleton instance of this class
	 * 
	 * @return the logger
	 */
	public static synchronized ProblemTracker getInstance() {
		if (instance.connection == null) {
			throw new RuntimeException("The instance has not been initialized, yet. Call init().");
		}
		return ProblemTracker.instance;
	}

	/**
	 * Return the singleton instance of this class, setting or updating the database
	 * connection
	 * 
	 * @param connection the database connection
	 * @return the singleton instance
	 * 
	 */
	public static synchronized ProblemTracker init(Connection connection) {
		if (instance != null) {
			throw new AssertionError("Initialization may only be done once.");
		}
		instance = new ProblemTracker(connection);
		return instance;
	}

	/**
	 * Open database connection, validate schema and persist the start parameters of
	 * the ETL process
	 * 
	 * @param jobName      name of the job
	 * @param sourceSystem name of the source system
	 * @return a job object. Needed for logging single events and for holding the
	 *         counts of absent problems
	 * @throws SQLException if the job information cannot be persisted to the
	 *                      database or if no job identifier cannot be obtained
	 */
	public Job jobStarted(String jobName, String sourceSystem) throws SQLException {

		// Validate schema and populate with defined problem categories, if necessary
		Persistence.getInstance().validateEventStoreSchema(connection);

		// Prepare SQL statement
		String SQL_INSERT = "INSERT INTO prov.job_dimension\n" + "(job_name, job_start, source_system, phase, status)\n"
				+ "VALUES\n" + "(?, ?, ?, ?, ?);";
		PreparedStatement insertStmt = connection.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);

		// Set parameters
		insertStmt.setString(1, jobName);
		insertStmt.setTimestamp(2, new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()));
		insertStmt.setString(3, sourceSystem);
		insertStmt.setString(4, "ETL");
		insertStmt.setString(5, ETLStatus.RUNNING.toString());

		// Execute statement
		int affectedRows = insertStmt.executeUpdate();
		if (affectedRows == 0) {
			throw new SQLException("Creating entry in job_dimension failed, no rows affected.");
		}

		// Get the unique id of the job, which has been autogenerated from the database
		int jobPk = -1;
		try (ResultSet generatedKeys = insertStmt.getGeneratedKeys()) {
			if (generatedKeys.next()) {
				jobPk = generatedKeys.getInt(1);
			} else {
				throw new SQLException("Creating entry in job_dimension failed, no ID obtained.");
			}
		}
		return new Job(jobPk);
	}

	/**
	 * Persist the finish parameters (including the numbers of pass() events) of the
	 * ETL process and close database connection.
	 * 
	 * @param job    the job object
	 * @param status the status
	 * @throws SQLException if the job information cannot be persisted to the
	 *                      database
	 */
	public void jobFinished(Job job, ETLStatus status) throws SQLException {

		// Prepare statement
		String SQL_UPDATE = "UPDATE prov.job_dimension\n" + "SET job_finish=?, status=?\n" + "WHERE job_pk=?;";
		PreparedStatement updateStmt = connection.prepareStatement(SQL_UPDATE);

		// Set parameters
		updateStmt.setTimestamp(1, new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()));
		updateStmt.setString(2, status.toString());
		updateStmt.setInt(3, job.pk);

		// Execute statement
		int affectedRows = updateStmt.executeUpdate();
		if (affectedRows == 0) {
			throw new SQLException("Creating entry in job_dimension failed, no rows affected.");
		}

		// Write the counts of absent problems
		writeCounts(job, true);

		// Write the counts of present problems
		writeCounts(job, false);
	}

	/**
	 * Write the number of problem absent events to the database.
	 * 
	 * @param job    the job object
	 * @param passed whether the passed counts should be written or the failed
	 *               counts
	 * @throws SQLException if the numbers cannot be written to the database
	 */
	private void writeCounts(Job job, boolean passed) throws SQLException {

		for (Map.Entry<ProblemCollection, Integer> entry : (passed ? job.getProblemAbsentCounts()
				: job.getProblemPresentCounts()).entrySet()) {

			// Prepare SQL statement
			String SQL_INSERT = "INSERT INTO prov.event_count_fact\n"
					+ "(job_fk, problem_fk, entity_name, attr_name, row_count, passed)\n" + "VALUES\n"
					+ "(?, ?, ?, ?, ?, ?);";
			PreparedStatement insertStmt = connection.prepareStatement(SQL_INSERT);

			// Set parameters
			insertStmt.setInt(1, entry.getKey().jobPk);
			insertStmt.setInt(2, entry.getKey().problem.getPk());
			insertStmt.setString(3, entry.getKey().entity);
			insertStmt.setString(4, entry.getKey().attribute);
			insertStmt.setInt(5, entry.getValue());
			insertStmt.setBoolean(6, passed);

			// Execute statement
			int affectedRows = insertStmt.executeUpdate();
			if (affectedRows == 0) {
				throw new SQLException("Creating entry in job_dimension failed, no rows affected.");
			}
		}
	}

	/**
	 * Persist an event to the event store. Currently the severity score persisted
	 * equals the default severity score as of the problem definition
	 * 
	 * @param job                 the job
	 * @param problemPresentEvent event containing error information
	 * @throws SQLException if the event cannot be persisted to the database
	 */
	public void fail(Job job, ProblemEventPresent problemPresentEvent) throws SQLException {

		// Get default severity score for problem
		String SQL_GET_SEVERITY_SCORE = "SELECT default_severity_score FROM prov.problem_dimension WHERE problem_pk='"
				+ problemPresentEvent.problem.getPk() + "';";
		Statement getSeverityScore = connection.createStatement();
		ResultSet rs = getSeverityScore.executeQuery(SQL_GET_SEVERITY_SCORE);
		double defaultSeverityScore = -1d;
		while (rs.next()) {
			defaultSeverityScore = rs.getDouble(1);
		}

		// Prepare statements
		String SQL_INSERT = "INSERT INTO prov.error_event_fact\n"
				+ "(job_fk, problem_fk, source_entity_name, source_entity_key_attr, source_entity_key, source_entity_error_attr, source_entity_error_val, final_severity_score, activity, info)\n"
				+ "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		PreparedStatement insertStmt = connection.prepareStatement(SQL_INSERT);

		// Set parameters
		insertStmt.setInt(1, job.pk);
		insertStmt.setInt(2, problemPresentEvent.problem.getPk());
		insertStmt.setString(3, problemPresentEvent.sourceEntity);
		insertStmt.setString(4, problemPresentEvent.sourceEntityKeyAttr);
		insertStmt.setString(5, problemPresentEvent.sourceEntityKey);
		insertStmt.setString(6,
				problemPresentEvent.sourceEntityAttr != null ? problemPresentEvent.sourceEntityAttr : "");
		insertStmt.setString(7,
				problemPresentEvent.sourceEntityErrorVal != null ? problemPresentEvent.sourceEntityErrorVal : "");
		insertStmt.setDouble(8, defaultSeverityScore);
		insertStmt.setString(9, problemPresentEvent.activity != null ? problemPresentEvent.activity : "");
		insertStmt.setString(10, problemPresentEvent.info != null ? problemPresentEvent.info : "");

		// Execute query
		int affectedRows = insertStmt.executeUpdate();
		if (affectedRows == 0) {
			throw new SQLException("Creating entry in job_dimension failed, no rows affected.");
		}

		// Problem statistics
		job.getProblemPresentCounts().merge(new ProblemCollection(job.pk, problemPresentEvent.problem,
				problemPresentEvent.sourceEntity, problemPresentEvent.sourceEntityAttr), 1, Integer::sum);
	}

	/**
	 * Register a positive problem result
	 * 
	 * @param job                the job
	 * @param problemAbsentEvent the pass event containing detailed information
	 */
	public void pass(Job job, ProblemEventAbsent problemAbsentEvent) {

		// Problem statistics
		job.getProblemAbsentCounts().merge(new ProblemCollection(job.pk, problemAbsentEvent.problem,
				problemAbsentEvent.sourceEntity, problemAbsentEvent.sourceEntityAttr), 1, Integer::sum);
	}
}
