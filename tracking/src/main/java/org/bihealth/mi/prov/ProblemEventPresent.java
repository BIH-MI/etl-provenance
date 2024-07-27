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

import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

/**
 * Describes the presence of a problem.
 * 
 * @author Helmut Spengler
 * @author Marco Johns
 * @author Fabian Prasser
 */
@SuperBuilder
public class ProblemEventPresent extends ProblemEvent {

	/** The key attribute */
	@Getter
	protected final String sourceEntityKeyAttr;

	/** The key of the entry */
	@Getter
	protected final String sourceEntityKey;

	/** The erroneous entry */
	@Getter
	protected final String sourceEntityErrorVal;

	/** Activity */
	@Getter
	protected final String activity;

	/** Additional information */
	@Getter
	protected final String info;

	/**
	 * Constructor
	 * 
	 * @param problem
	 * @param sourceEntity
	 * @param sourceEntityKeyAttr
	 * @param sourceEntityKey
	 * @param sourceEntityAttr
	 * @param sourceEntityErrorVal
	 * @param activity
	 * @param info
	 */
	public ProblemEventPresent(@NonNull Problem problem, @NonNull String sourceEntity,
			@NonNull String sourceEntityKeyAttr, @NonNull String sourceEntityKey, @NonNull String sourceEntityAttr,
			String sourceEntityErrorVal, String activity, String info) {

		super(problem, sourceEntity, sourceEntityAttr);
		if ("".equals(sourceEntityKeyAttr)) {
			throw new IllegalArgumentException("Parameter 'sourceEntityKeyAttr' may not be empty.");
		}
		if ("".equals(sourceEntityKey)) {
			throw new IllegalArgumentException("Parameter 'sourceEntityKey' may not be null or empty.");
		}
		this.sourceEntityKeyAttr = sourceEntityKeyAttr;
		this.sourceEntityKey = sourceEntityKey;
		this.sourceEntityErrorVal = sourceEntityErrorVal;
		this.activity = activity;
		this.info = info;
	}
}
