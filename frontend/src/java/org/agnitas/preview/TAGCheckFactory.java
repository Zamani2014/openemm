/*

    Copyright (C) 2019 AGNITAS AG (https://www.agnitas.org)

    This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
    This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
    You should have received a copy of the GNU Affero General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.

*/

package org.agnitas.preview;

import java.util.Locale;

import org.agnitas.emm.core.velocity.VelocityCheck;

public interface TAGCheckFactory {
	TAGCheck createTAGCheck(int mailingId, Locale locale) throws Exception;

	TAGCheck createTAGCheck(@VelocityCheck int companyId, int mailinglistId, Locale locale) throws Exception;

	TAGCheck createTAGCheck(@VelocityCheck int companyId, int mailingId, int mailinglistId, Locale locale) throws Exception;
}
