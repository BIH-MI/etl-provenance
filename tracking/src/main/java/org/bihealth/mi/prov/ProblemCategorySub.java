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
 * Reference for the available subcategories.
 * 
 * @author Helmut Spengler
 * @author Marco Johns
 * @author Fabian Prasser
 */
public enum ProblemCategorySub {

	/** Value conformance */
	VALUE_CONFORMANCE(1, ProblemCategory.CONFORMANCE, "Value conformance"),

	/** Relational conformance */
	RELATIONAL_CONFORMANCE(2, ProblemCategory.CONFORMANCE, "Relational conformance"),

	/** Computational conformance */
	COMPUTATIONAL_CONFORMANCE(3, ProblemCategory.CONFORMANCE, "Computational conformance"),

	/** Completeness */
	COMPLETENESS(4, ProblemCategory.COMPLETENESS, "Completeness"),

	/** Uniqueness plausibility */
	UNIQUENESS_PLAUSIBILITY(5, ProblemCategory.PLAUSIBILITY, "Uniqueness plausibility"),

	/** Atemporal plausibility */
	ATEMPORAL_PLAUSIBILITY(6, ProblemCategory.PLAUSIBILITY, "Atemporal plausibility"),

	/** Temporal plausibility */
	TEMPORAL_PLAUSIBILITY(7, ProblemCategory.PLAUSIBILITY, "Temporal plausibility");

	// Check for duplicate primary keys
	static {
		Set<Long> problemSubCatPks = new HashSet<>();
		for (ProblemCategorySub problemSubCat : ProblemCategorySub.values()) {
			if (!problemSubCatPks.add(problemSubCat.getPk())) {
				throw new RuntimeException("Key " + problemSubCat.getPk() + " for ProblemSubCategory "
						+ problemSubCat.toString() + " already exists.");
			}
		}
	}

	/** The category this sub-category belongs to */
	@Getter
	private final ProblemCategory problemCategory;

	/** Unique identifier of the problem as used in the database */
	@Getter
	private final long pk;

	/** Description */
	@Getter
	private final String description;

	/**
	 * Constructor
	 * 
	 * @param pk              the primary key for the database
	 * @param problemCategory the category this sub-category belongs to
	 * @param desc            description of the ProblemSubCategory
	 */
	private ProblemCategorySub(long pk, ProblemCategory problemCategory, String desc) {
		this.pk = pk;
		this.problemCategory = problemCategory;
		this.description = desc;
	}
}
