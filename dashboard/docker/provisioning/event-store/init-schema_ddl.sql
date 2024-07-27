-----------------------------------------------------
-----------------------------------------------------
-- DDL                                             --
-----------------------------------------------------
-----------------------------------------------------

--DROP SCHEMA prov CASCADE;
CREATE SCHEMA prov AUTHORIZATION postgres;

-----------------------------------------------------
-- TABLE prov.problem_category                         -
-----------------------------------------------------
-- DROP TABLE prov.problem_category;
CREATE TABLE prov.problem_category (
	problem_subcategory_pk int NOT NULL,
	problem_category_name varchar(64) NOT NULL,
	problem_subcategory_name varchar(64) NOT NULL,
	CONSTRAINT problem_subcategory_pk PRIMARY KEY (problem_subcategory_pk),
	CONSTRAINT problem_subcategory_un UNIQUE (problem_category_name, problem_subcategory_name)
);
-- Permissions
ALTER TABLE prov.problem_category OWNER TO postgres;
GRANT ALL ON TABLE prov.problem_category TO postgres;

-----------------------------------------------------
-- TABLE prov.problem_dimension                        -
-----------------------------------------------------
-- DROP TABLE prov.problem_dimension;
CREATE TABLE prov.problem_dimension (
	problem_pk int NOT NULL,
	problem_subcategory_fk int NOT NULL,
	problem_name varchar(64) NOT NULL,
	problem_short_name varchar(64) NOT NULL,
	default_severity_score float8 NULL,
	CONSTRAINT problem_dimension_pk PRIMARY KEY (problem_pk),
	CONSTRAINT problem_dimension_category_fk FOREIGN KEY (problem_subcategory_fk) REFERENCES prov.problem_category(problem_subcategory_pk),
	CONSTRAINT problem_dimension_un UNIQUE (problem_short_name)
);
-- Permissions
ALTER TABLE prov.problem_dimension OWNER TO postgres;
GRANT ALL ON TABLE prov.problem_dimension TO postgres;

-----------------------------------------------------
-- TABLE prov.job_dimension                           -
-----------------------------------------------------
-- DROP TABLE prov.job_dimension;
CREATE TABLE prov.job_dimension (
	job_pk serial NOT NULL,
	job_name varchar(512) NOT NULL,
	job_start timestamptz NOT NULL,
	job_finish timestamptz NULL,
	source_system varchar(512) NOT NULL,
	phase varchar(16) NOT NULL,
	status varchar(16) NOT NULL,
	CONSTRAINT job_dimension_pk PRIMARY KEY (job_pk)
);
-- Permissions
ALTER TABLE prov.job_dimension OWNER TO postgres;
GRANT ALL ON TABLE prov.job_dimension TO postgres;

-----------------------------------------------------
-- TABLE prov.baseline_dimension                      -
-----------------------------------------------------
-- DROP TABLE prov.baseline_dimension;
CREATE TABLE prov.baseline_dimension (
	baseline_pk serial NOT NULL,
	job_fk int NOT NULL,
	entity_name varchar(512) NOT NULL,
	row_count int NOT NULL,
	CONSTRAINT baseline_dimension_pk PRIMARY KEY (baseline_pk),
	CONSTRAINT baseline_dimension_job_fk FOREIGN KEY (job_fk) REFERENCES prov.job_dimension(job_pk),
	CONSTRAINT baseline_dimension_un UNIQUE (job_fk, entity_name)
);
-- Permissions
ALTER TABLE prov.baseline_dimension OWNER TO postgres;
GRANT ALL ON TABLE prov.baseline_dimension TO postgres;

-----------------------------------------------------
-- TABLE prov.error_event_fact                              -
-----------------------------------------------------
-- DROP TABLE prov.error_event_fact;
CREATE TABLE prov.error_event_fact (
	event_pk bigserial NOT NULL,
	job_fk int NOT NULL,
	problem_fk int NOT NULL,
	source_entity_name varchar(512) NOT NULL,
	source_entity_key_attr varchar(128) NOT NULL,
	source_entity_key varchar(128) NOT NULL,
	source_entity_error_attr varchar(128) NOT NULL,
	source_entity_error_val varchar(128) NOT NULL,
	final_severity_score float8 NOT NULL,
	info varchar(2048) NULL,
	activity varchar(2048) NULL,
	CONSTRAINT error_event_fact_pk PRIMARY KEY (event_pk),
	CONSTRAINT error_event_fact_job_fk FOREIGN KEY (job_fk) REFERENCES prov.job_dimension(job_pk),
	CONSTRAINT error_event_fact_problem_fk FOREIGN KEY (problem_fk) REFERENCES prov.problem_dimension(problem_pk)
);
-- Permissions
ALTER TABLE prov.error_event_fact OWNER TO postgres;
GRANT ALL ON TABLE prov.error_event_fact TO postgres;

-----------------------------------------------------
-- TABLE prov.event_count_fact                        -
-----------------------------------------------------
-- DROP TABLE prov.event_count_fact;
CREATE TABLE prov.event_count_fact (
	event_pk bigserial NOT NULL,
	job_fk int NOT NULL,
	problem_fk int NOT NULL,
	entity_name varchar(512) NOT NULL,
	attr_name varchar(128) NOT NULL,
	passed boolean NOT NULL,
	row_count int NOT NULL,
	CONSTRAINT event_count_fact_pk PRIMARY KEY (event_pk),
	CONSTRAINT event_count_fact_job_fk FOREIGN KEY (job_fk) REFERENCES prov.job_dimension(job_pk),
	CONSTRAINT event_count_fact_problem_fk FOREIGN KEY (problem_fk) REFERENCES prov.problem_dimension(problem_pk),
	CONSTRAINT event_count_fact_un UNIQUE (job_fk, problem_fk, entity_name, attr_name, passed)
);
-- Permissions
ALTER TABLE prov.event_count_fact OWNER TO postgres;
GRANT ALL ON TABLE prov.event_count_fact TO postgres;

-----------------------------------------------------
-- TABLE prov.schema_version                          -
-----------------------------------------------------

-- DROP TABLE prov.schema_version;
CREATE TABLE prov.schema_version (
	init_date timestamptz,
	version varchar(32),
	CONSTRAINT schema_version_un UNIQUE (version)
);
-- Permissions
ALTER TABLE prov.schema_version OWNER TO postgres;
GRANT ALL ON TABLE prov.schema_version TO postgres;
INSERT INTO prov.schema_version (version, init_date) VALUES ('1.1.4-SNAPSHOT', NULL);


-----------------------------------------------------
-- VIEW prov.v_problem_categories                      -
-----------------------------------------------------
CREATE OR REPLACE VIEW prov.v_problem_categories AS
SELECT 
    sc.problem_category_name,
    sc.problem_subcategory_name,
    sd.problem_pk,
    sd.problem_name,
    sc.problem_subcategory_pk
FROM prov.problem_category sc
  LEFT JOIN prov.problem_dimension sd ON sc.problem_subcategory_pk = sd.problem_subcategory_fk
ORDER BY sc.problem_category_name, sc.problem_subcategory_name;
-- Permissions
GRANT ALL ON TABLE prov.v_problem_categories TO postgres;

-----------------------------------------------------
-- VIEW prov.v_problem_count_breakdown                 -
-----------------------------------------------------
CREATE OR REPLACE VIEW prov.v_problem_count_breakdown AS
SELECT sc.problem_category_name,
  sc.problem_subcategory_name,
  event_breakdown.problem_name,
  event_breakdown.source_entity_name,
  event_breakdown.source_entity_error_attr,
  event_breakdown.count
FROM prov.problem_category sc
JOIN (
  SELECT
    job_problem_entity_attrname_combis.problem_name,
    job_problem_entity_attrname_combis.problem_pk,
    job_problem_entity_attrname_combis.problem_subcategory_fk,
    job_problem_entity_attrname_combis.job_pk,
    job_problem_entity_attrname_combis.source_entity_name,
    ef.source_entity_error_attr,
    COUNT(ef.source_entity_key)
  FROM (
	SELECT
       sd.problem_name,sd.problem_pk,
       sd.problem_subcategory_fk,
       jd.job_pk,
       entity_attr_names.source_entity_name
     FROM
       prov.problem_dimension AS sd,
       prov.job_dimension AS jd,
       (SELECT DISTINCT
          ef.source_entity_name
        FROM prov.error_event_fact ef) AS entity_attr_names
	) AS job_problem_entity_attrname_combis
  JOIN prov.error_event_fact ef ON
    job_problem_entity_attrname_combis.job_pk             = ef.job_fk AND
    job_problem_entity_attrname_combis.problem_pk          = ef.problem_fk AND
    job_problem_entity_attrname_combis.source_entity_name = ef.source_entity_name
  GROUP BY
    job_problem_entity_attrname_combis.problem_name,
    job_problem_entity_attrname_combis.problem_pk,
    job_problem_entity_attrname_combis.problem_subcategory_fk,
    job_problem_entity_attrname_combis.job_pk,
    job_problem_entity_attrname_combis.source_entity_name,
    ef.source_entity_error_attr
) AS event_breakdown ON
  sc.problem_subcategory_pk = event_breakdown.problem_subcategory_fk
WHERE event_breakdown.job_pk = (( SELECT max(jd_1.job_pk) AS max
           FROM prov.job_dimension jd_1
          WHERE jd_1.status::text = 'COMPLETED'::text)) OR event_breakdown.job_pk IS NULL
ORDER BY event_breakdown.source_entity_name, sc.problem_category_name, sc.problem_subcategory_name;
-- Permissions
ALTER VIEW prov.v_problem_count_breakdown OWNER TO postgres;
GRANT ALL ON TABLE prov.v_problem_count_breakdown TO postgres;

-----------------------------------------------------
-- VIEW prov.v_last_job_duration_seconds              -
-----------------------------------------------------
CREATE OR REPLACE VIEW prov.v_last_job_duration_seconds AS
SELECT jd.job_name, EXTRACT(EPOCH FROM (jd.job_finish - jd.job_start)) AS duration
FROM prov.job_dimension jd
WHERE 
  jd.job_pk = (SELECT MAX(job_pk) FROM prov.job_dimension jd2 WHERE jd2.status = 'COMPLETED');
-- Permissions
ALTER VIEW prov.v_last_job_duration_seconds OWNER TO postgres;
GRANT ALL ON TABLE prov.v_last_job_duration_seconds TO postgres;

-----------------------------------------------------
-- VIEW prov.v_pass_fail                              -
-----------------------------------------------------
CREATE OR REPLACE VIEW prov.v_pass_fail AS
SELECT base_coordinates.problem_fk, base_coordinates.entity_name, base_coordinates.attr_name, COALESCE(passed_results.row_count, 0) AS passed, COALESCE (failed_results.row_count, 0) AS failed,
COALESCE(passed_results.row_count, 0) + COALESCE (failed_results.row_count, 0) AS total,
(COALESCE(passed_results.row_count, 0)*1.0) / ((COALESCE(passed_results.row_count, 0) + COALESCE (failed_results.row_count, 0))*1.0) AS qrate,
(COALESCE(failed_results.row_count, 0)*1.0) / ((COALESCE(passed_results.row_count, 0) + COALESCE (failed_results.row_count, 0))*1.0) AS erate
FROM 
 (SELECT DISTINCT ecf.problem_fk, ecf.entity_name, ecf.attr_name FROM prov.event_count_fact ecf
  JOIN prov.job_dimension jd ON ecf.job_fk = jd.job_pk 
  WHERE jd.status ='COMPLETED' AND
    jd.job_pk = (SELECT MAX(jd2.job_pk) FROM prov.job_dimension jd2))
  AS base_coordinates
LEFT OUTER JOIN (SELECT ecf.problem_fk, ecf.entity_name, ecf.attr_name, SUM(ecf.row_count) as row_count
  FROM prov.event_count_fact ecf
  JOIN prov.job_dimension jd ON ecf.job_fk = jd.job_pk 
  WHERE jd.status ='COMPLETED' AND
    jd.job_pk = (SELECT MAX(jd2.job_pk) FROM prov.job_dimension jd2) AND
    ecf.passed = false
  GROUP BY ecf.problem_fk, ecf.entity_name, ecf.attr_name
  ) as failed_results
ON base_coordinates.problem_fk = failed_results.problem_fk and 
   base_coordinates.entity_name= failed_results.entity_name and
   base_coordinates.attr_name = failed_results.attr_name
LEFT OUTER JOIN (SELECT ecf.problem_fk, ecf.entity_name, ecf.attr_name, SUM(ecf.row_count) as row_count
  FROM prov.event_count_fact ecf
  JOIN prov.job_dimension jd ON ecf.job_fk = jd.job_pk 
  WHERE jd.status ='COMPLETED' AND
    jd.job_pk = (SELECT MAX(jd2.job_pk) FROM prov.job_dimension jd2) AND
    ecf.passed = true
  GROUP BY ecf.problem_fk, ecf.entity_name, ecf.attr_name
  ) as passed_results
ON base_coordinates.problem_fk = passed_results.problem_fk and 
   base_coordinates.entity_name= passed_results.entity_name and
   base_coordinates.attr_name = passed_results.attr_name;
-- Permissions
ALTER VIEW prov.v_pass_fail OWNER TO postgres;
GRANT ALL ON TABLE prov.v_pass_fail TO postgres;