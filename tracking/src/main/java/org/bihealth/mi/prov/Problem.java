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

import java.util.HashSet;
import java.util.Set;

import lombok.Getter;

/**
 * Reference for the available problems and their associated primary keys
 * 
 * @author Helmut Spengler
 * @author Renata Falguera
 * @author Marco Johns
 * @author Fabian Prasser
 */
public enum Problem {

	/** Invalid format, e.g. wrong decimal separator */
	INVALID_FORMAT(1, ProblemCategorySub.VALUE_CONFORMANCE, "Invalid format", "cv_invalid_format", 1.0),

	/** Wrong data type, e.g. expected a date but received a number */
	INVALID_DATATYPE(2, ProblemCategorySub.VALUE_CONFORMANCE, "Invalid datatype", "cv_invalid_datatype", 1.0),

	/**
	 * Data values are not conform to allowable values/range (categorical values)
	 */
	INVALID_CATEGORY(3, ProblemCategorySub.VALUE_CONFORMANCE, "Data values are not conform to allowable values/range",
			"cv_invalid_category", 1.0),

	/**
	 * Unique (key) data values are duplicated, e.g, violation of primary/foreign
	 * key constraints, a medical record number is assigned to more than a single
	 * patient
	 */
	REFERENTIAL_INTEGRITY_VIOLATION(4, ProblemCategorySub.RELATIONAL_CONFORMANCE, "Referential integrity violation",
			"cr_ref_int_viol", 1.0),

	/** Wrong score calculation */
	WRONG_SCORE_CALCULATION(5, ProblemCategorySub.COMPUTATIONAL_CONFORMANCE, "Wrong score calculation",
			"cc_wrong_score_calc", 1.0),

	/** Missing value */
	MISSING_VALUE(6, ProblemCategorySub.COMPLETENESS, "Missing value", "c_missing_val", 1.0),

	/** Insufficient Precision */
	INSUFFICIENT_PRECISION(7, ProblemCategorySub.COMPLETENESS, "Insufficient precision", "c_insuff_prec", 1.0),

	/**
	 * Null Violation - variables that not allowed to have null values have null
	 * values
	 */
	NULL_VIOLATION(8, ProblemCategorySub.COMPLETENESS, "Null Violation", "c_null_violation", 1.0),

	/** Same entity with different key */
	DUPLICATE_ENTITY(9, ProblemCategorySub.UNIQUENESS_PLAUSIBILITY, "Duplicate entity with different key",
			"pu_dupl_entity_diff_key", 1.0),

	/**
	 * Observed or derived values are nt conform temporal properties, e.g, Date of
	 * first manifestation of disease is bigger that Date fo disease diagnosis
	 */
	TEMPORAL_PROPERTIES_VIOLATION(10, ProblemCategorySub.TEMPORAL_PLAUSIBILITY,
			"Observed or derived values are nt conform temporal properties", "pt_temp_prop_viol", 1.0),

	/**
	 * Dates can not be put in a meaningful order, e.g, if two diagnosis were given
	 * at the same year, but only the year is given, etc.
	 */
	DATE_MEANINGFUL_ORDER(11, ProblemCategorySub.TEMPORAL_PLAUSIBILITY, "Dates can not be put in a meaningful order",
			"pt_no_meaningful_order", 1.0),

	/**
	 * Invalid range, e.g. negative age, or count of unique patients by diagnosis
	 * are not as expected
	 */
	VALUE_RANGE_VIOLATION(12, ProblemCategorySub.ATEMPORAL_PLAUSIBILITY, "Value range violation", "pa_val_range_viol",
			1.0),

	/**
	 * Logical constrainst between values do not agree with local or common
	 * knowlegde, e.g, sex values do not agree with sex specific contexts
	 * (pregnancy, prostate cancer)
	 */
	LOGICAL_CONSTRAINTS_VIOLATION(13, ProblemCategorySub.ATEMPORAL_PLAUSIBILITY, "Logical constraints violation",
			"pa_logical_cons_viol", 1.0);

	// Check for duplicate primary keys
	static {
		Set<Integer> problemPks = new HashSet<>();
		for (Problem problem : Problem.values()) {
			if (!problemPks.add(problem.getPk())) {
				throw new RuntimeException(
						"Key " + problem.getPk() + "for problem " + problem.toString() + " already exists.");
			}
		}
	}

	/** The sub-category this problem belongs to */
	@Getter
	private final ProblemCategorySub problemSubCategory;

	/** Unique identifier of the problem as used in the database */
	@Getter
	private final int pk;

	/** Description of the problem */
	@Getter
	private final String description;

	/** Short description of the problem */
	@Getter
	private final String shortDescription;

	/** Default severity score */
	@Getter
	private final double defaultSeverityScore;

	/**
	 * Constructor
	 * 
	 * @param pk                   unique identifier of the problem as used in the
	 *                             database
	 * @param problemSubCategory   the problem sub-category
	 * @param desc                 description of the problem
	 * @param shortDesc            short description of the problem
	 * @param defaultSeverityScore the default severity score
	 */
	private Problem(int pk, ProblemCategorySub problemSubCategory, String desc, String shortDesc,
			double defaultSeverityScore) {
		this.pk = pk;
		this.problemSubCategory = problemSubCategory;
		this.description = desc;
		this.shortDescription = shortDesc;
		this.defaultSeverityScore = defaultSeverityScore;
	}
}
