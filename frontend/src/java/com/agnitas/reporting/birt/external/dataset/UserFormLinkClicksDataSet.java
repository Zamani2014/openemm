/*

    Copyright (C) 2019 AGNITAS AG (https://www.agnitas.org)

    This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
    This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
    You should have received a copy of the GNU Affero General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.

*/

package com.agnitas.reporting.birt.external.dataset;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.agnitas.emm.core.velocity.VelocityCheck;
import org.apache.log4j.Logger;

import com.agnitas.reporting.birt.external.beans.UserFormLinkClicksStatisticRow;

/**
 * BIRT-DataSet for user form URL clicks statistics
 */
public class UserFormLinkClicksDataSet extends BIRTDataSet {
	/** The logger. */
	private static final transient Logger logger = Logger.getLogger(UserFormLinkClicksDataSet.class);
	
	public List<UserFormLinkClicksStatisticRow> getClicksPerUrl(@VelocityCheck int formID, int companyID) {
		List<UserFormLinkClicksStatisticRow> urlClickList = new ArrayList<>();
		
		String queryMeasured =
			"SELECT"
			+ " COALESCE(formurl.shortname, formurl.full_url) AS url,"
			+ " formurl.url_id,"
			+ " COUNT(*) AS clicks_gross,"
			+ " COUNT(DISTINCT rlog.customer_id) AS clicks_net"
			+ " FROM rdir_url_userform_tbl formurl"
			+ " JOIN rdirlog_userform_" + companyID + "_tbl rlog ON rlog.url_id = formurl.url_id"
			+ " WHERE rlog.form_id = ?"
			+ " AND (rlog.customer_id IS NOT NULL AND rlog.customer_id != 0)"
			+ " GROUP BY formurl.url_id, COALESCE(formurl.shortname, formurl.full_url)";
		
		List<Map<String, Object>> resultMeasured = select(logger, queryMeasured, formID);
		for (Map<String, Object> row : resultMeasured) {
			UserFormLinkClicksStatisticRow statisticRow = new UserFormLinkClicksStatisticRow();
			
			statisticRow.setUrl((String) row.get("url"));
			statisticRow.setUrl_id(((Number) row.get("url_id")).intValue());
			statisticRow.setClicks_gros(((Number) row.get("clicks_gross")).intValue());
			statisticRow.setClicks_net(((Number) row.get("clicks_net")).intValue());
			statisticRow.setClicks_unique(0);
            
			urlClickList.add(statisticRow);
		}

		String queryAnonymous =
			"SELECT"
			+ " COALESCE(formurl.shortname, formurl.full_url) AS url,"
			+ " formurl.url_id,"
			+ " COUNT(*) AS clicks_anonym"
			+ " FROM rdir_url_userform_tbl formurl"
			+ " JOIN rdirlog_userform_" + companyID + "_tbl rlog ON rlog.url_id = formurl.url_id"
			+ " WHERE rlog.form_id = ?"
			+ " AND (rlog.customer_id IS NULL OR rlog.customer_id = 0)"
			+ " GROUP BY formurl.url_id, COALESCE(formurl.shortname, formurl.full_url)";
		
		List<Map<String, Object>> resultAnonymous = select(logger, queryAnonymous, formID);
		for (Map<String, Object> row : resultAnonymous) {
			int urlID = ((Number) row.get("url_id")).intValue();
			
			UserFormLinkClicksStatisticRow statisticRow = null;
			for (UserFormLinkClicksStatisticRow urlClickListItem : urlClickList) {
				if (urlClickListItem.getUrl_id() == urlID) {
					statisticRow = urlClickListItem;
					break;
				}
			}
			
			if (statisticRow != null) {
				// Add anonymous value to existing statistic row
	            statisticRow.setClicks_anonymous(((Number) row.get("clicks_anonym")).intValue());
			} else {
				// Add stand alone statistic row for anonymous value
				statisticRow = new UserFormLinkClicksStatisticRow();
				
				statisticRow.setUrl((String) row.get("url"));
				statisticRow.setUrl_id(urlID);
	            statisticRow.setClicks_anonymous(((Number) row.get("clicks_anonym")).intValue());
				statisticRow.setClicks_unique(0);
	            
				urlClickList.add(statisticRow);
			}
		}
		
		return urlClickList;
	}
}
