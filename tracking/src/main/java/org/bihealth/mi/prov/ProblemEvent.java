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
 * Encapsulates all data related to a problem
 * 
 * @author Helmut Spengler
 * @author Marco Johns
 * @author Fabian Prasser
 */
@SuperBuilder
public abstract class ProblemEvent {

	/** The problem performed */
	@NonNull
	@Getter
	protected final Problem problem;

	/** The affected source entity */
	@NonNull
	@Getter
	protected final String sourceEntity;

	/** The affected attribute */
	@NonNull
	@Getter
	protected final String sourceEntityAttr;

	/**
	 * Constructor
	 * 
	 * @param problem          the problem performed
	 * @param sourceEntity     the affected source entity
	 * @param sourceEntityAttr the affected attribute
	 */
	protected ProblemEvent(@NonNull Problem problem, @NonNull String sourceEntity, @NonNull String sourceEntityAttr) {

		if ("".equals(sourceEntity)) {
			throw new IllegalArgumentException("Parameter 'sourceEntity' may not be or empty.");
		} else if ("".equals(sourceEntityAttr)) {
			throw new IllegalArgumentException("Parameter 'sourceEntityAttr' may not be or empty.");
		}

		this.problem = problem;
		this.sourceEntity = sourceEntity;
		this.sourceEntityAttr = sourceEntityAttr;
	}
}
