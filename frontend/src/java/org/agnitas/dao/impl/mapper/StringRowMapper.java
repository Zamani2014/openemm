/*

    Copyright (C) 2019 AGNITAS AG (https://www.agnitas.org)

    This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
    This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
    You should have received a copy of the GNU Affero General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.

*/

package org.agnitas.dao.impl.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

/**
 * Simple {@link RowMapper} for retrieving String values from ResultSet.
 * 
 * The source column must be at index 1.
 */
public class StringRowMapper implements RowMapper<String> {
	
	/** Singleton of this {@link RowMapper}. */
	public static final StringRowMapper INSTANCE = new StringRowMapper();

	/**
	 * Use {@link #INSTANCE} instead.
	 */
	@Deprecated
	public StringRowMapper() {	// TODO Make private if all references have been replaced
		super();
	}
	
	@Override
	public String mapRow(ResultSet resultSet, int row) throws SQLException {
		final String str = resultSet.getString(1);
		
		
		return "".equals(str) ? null : str;  // Required for MariaDB / MySQL to behave like Oracle
	}
	
}
