-----------------------------------------------------
-- DDL                                             --
-----------------------------------------------------

CREATE SCHEMA prov AUTHORIZATION postgres;


-----------------------------------------------------
-- TABLE prov.problem_category                      -
-----------------------------------------------------

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
-- TABLE prov.problem_dimension                     -
-----------------------------------------------------
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
-- TABLE prov.job_dimension                         -
-----------------------------------------------------

CREATE TABLE prov.job_dimension (
	job_pk serial NOT NULL,
	job_name varchar(512) NOT NULL,
	job_start timestamptz NOT NULL,
	job_finish timestamptz NULL,
	source_system varchar(512) NOT NULL,
	status varchar(16) NOT NULL,
	CONSTRAINT job_dimension_pk PRIMARY KEY (job_pk)
);

-- Permissions
ALTER TABLE prov.job_dimension OWNER TO postgres;
GRANT ALL ON TABLE prov.job_dimension TO postgres;


-----------------------------------------------------
-- TABLE prov.error_event_fact                      -
-----------------------------------------------------

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
-- TABLE prov.event_count_fact                      -
-----------------------------------------------------

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
-- TABLE prov.schema_version                        -
-----------------------------------------------------

CREATE TABLE prov.schema_version (
	init_date timestamptz,
	version varchar(32),
	CONSTRAINT schema_version_un UNIQUE (version)
);

-- Permissions
ALTER TABLE prov.schema_version OWNER TO postgres;
GRANT ALL ON TABLE prov.schema_version TO postgres;

-- Inserts
INSERT INTO prov.schema_version (version, init_date) VALUES ('1.1.1-SNAPSHOT', NULL);
