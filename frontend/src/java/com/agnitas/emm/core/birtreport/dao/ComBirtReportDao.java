/*

    Copyright (C) 2019 AGNITAS AG (https://www.agnitas.org)

    This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
    This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
    You should have received a copy of the GNU Affero General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.

*/

package com.agnitas.emm.core.birtreport.dao;

import java.util.List;
import java.util.Map;

import org.agnitas.emm.core.velocity.VelocityCheck;

import com.agnitas.emm.core.birtreport.bean.ComBirtReport;
import com.agnitas.emm.core.birtreport.bean.ComLightweightBirtReport;

public interface ComBirtReportDao {
    boolean insert(ComBirtReport report) throws Exception;

    boolean update(ComBirtReport report) throws Exception;
    
    boolean update(ComBirtReport report, List<Integer> justDeactivateSettingTypes) throws Exception;

    List<ComLightweightBirtReport> getLightweightBirtReportList(@VelocityCheck int companyID);

    void updateReportMailinglists(int reportId, int reportType, List<Integer> mailinglistIds);

    void insertSentMailings(Integer reportId, Integer companyID, List<Integer> sentMailings);

    List<Map<String, Object>> getReportParamValues(@VelocityCheck int companyID, String paramName);

    List<ComLightweightBirtReport> getLightweightBirtReportsBySelectedTarget(@VelocityCheck int companyID, int targetGroupID);

	int resetBirtReportsForCurrentHost();

	boolean announceStart(ComBirtReport birtReport);

	void announceEnd(ComBirtReport birtReport);

	int getRunningReportsByHost(String hostName);

	List<ComBirtReport> getReportsToSend(List<Integer> includedCompanyIds, List<Integer> excludedCompanyIds);

	void deactivateBirtReport(int reportID);

	List<ComBirtReport> selectErrorneousReports();
}
