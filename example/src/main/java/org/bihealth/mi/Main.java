package org.bihealth.mi;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.bihealth.mi.prov.*;

public class Main {

    final static Logger logger = LoggerFactory.getLogger(Main.class);

    public static void runExample(Properties dbParams) {
        logger.info(" - Connecting to event store.");
        try {
            ProblemTracker probTrack = ProblemTracker.init(Persistence.getInstance().getDatabaseConnection(
                    dbParams.getProperty("host"),
                    dbParams.getProperty("port"),
                    dbParams.getProperty("schema"),
                    dbParams.getProperty("user"),
                    dbParams.getProperty("password")));

            ProblemTracker.Job exampleJob = probTrack.jobStarted("Test Job", "etl-provenance-example");
            logger.info(" - ETL Job started");

            logger.info(" - Adding problem events:");
            // Adding 5 problem absent events for subject age / missing data
            for (int i = 0; i <= 5; i++) {
                ProblemEventAbsent problemEvent = ProblemEventAbsent.builder().
                        problem(Problem.MISSING_VALUE).
                        sourceEntity("Subject").
                        sourceEntityAttr("Age").
                        build();
                probTrack.pass(exampleJob, problemEvent);
                logger.info(" - - Recorded problem event: MISSING VALUE [ABSENT]");
            }

            // Adding 4 problem absent events for subject age / invalid datatype
            for (int i = 0; i <= 4; i++) {
                ProblemEventAbsent problemEvent = ProblemEventAbsent.builder().
                        problem(Problem.INVALID_DATATYPE).
                        sourceEntity("Subject").
                        sourceEntityAttr("Age").
                        build();
                probTrack.pass(exampleJob, problemEvent);
                logger.info(" - - Recorded problem event: INVALID DATATYPE [ABSENT]");
            }

            // Adding 1 problem present event for subject age / invalid datatype
            ProblemEventPresent problemEvent = ProblemEventPresent.builder().
                    problem(Problem.INVALID_DATATYPE).
                    sourceEntity("Subject").
                    sourceEntityAttr("Age").
                    sourceEntityKeyAttr("SubjectID").
                    sourceEntityKey("123").
                    sourceEntityErrorVal("unknown").
                    activity("Subject age verification").
                    info("Expected numeric value, found text.").
                    build();
            probTrack.fail(exampleJob, problemEvent);
            logger.info(" - - Recorded problem event: INVALID DATATYPE [PRESENT]");

            probTrack.jobFinished(exampleJob, ETLStatus.COMPLETED);
            logger.info(" - ETL Job finished");

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        logger.info("Starting etl-provenance-example.");
        Properties dbConfig = new Properties();
        try {
            dbConfig.load(Main.class.getClassLoader().getResourceAsStream("db.properties"));
            runExample(dbConfig);
        }
        catch (IOException ex) {
            logger.error("Error loading db.properties");
        }
        logger.info("Finished etl-provenance-example.");
    }
}