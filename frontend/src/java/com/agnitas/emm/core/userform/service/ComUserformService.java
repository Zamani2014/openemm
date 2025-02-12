/*

    Copyright (C) 2019 AGNITAS AG (https://www.agnitas.org)

    This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
    This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
    You should have received a copy of the GNU Affero General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.

*/

package com.agnitas.emm.core.userform.service;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.agnitas.emm.core.useractivitylog.UserAction;
import org.agnitas.emm.core.userforms.UserformService;
import org.agnitas.emm.core.velocity.VelocityCheck;
import org.agnitas.util.DbColumnType;

import com.agnitas.beans.ComAdmin;
import com.agnitas.emm.core.userform.dto.UserFormDto;
import com.agnitas.service.ServiceResult;
import com.agnitas.userform.bean.UserForm;

import net.sf.json.JSONArray;

public interface ComUserformService extends UserformService {

    String getUserFormName(int formId, @VelocityCheck int companyId);

    List<UserForm> getUserForms(@VelocityCheck int companyId);

    UserAction setActiveness(@VelocityCheck int companyId, Map<Integer, Boolean> activeness);

	UserFormDto getUserForm(@VelocityCheck int companyId, int formId);

	boolean isFormNameUnique(String formName, int formId, int companyId);

	ServiceResult<Integer> saveUserForm(ComAdmin admin, UserFormDto userFormDto) throws Exception;

	List<UserFormDto> bulkDeleteUserForm(List<Integer> bulkIds, @VelocityCheck int companyId);

	boolean deleteUserForm(int formId, @VelocityCheck int companyId);

	String getCloneUserFormName(String name, @VelocityCheck int companyId, Locale locale);

	ServiceResult<Integer> cloneUserForm(ComAdmin admin, int userFormId);

	File exportUserForm(ComAdmin admin, int userFormId, String userFormName);
	
	String getUserFormUrlPattern(final ComAdmin admin, final String formName, final boolean resolveUID, final Optional<String> companyToken);
	List<UserFormTestUrl> getUserFormUrlForAllAdminAndTestRecipients(final ComAdmin admin, final String formName, final Optional<String> companyToken);
	String getUserFormUrlWithoutUID(ComAdmin admin, String formName, Optional<String> companyToken);

    JSONArray getUserFormsJson(ComAdmin admin);

    List<String> getUserFormNames(@VelocityCheck int companyId);

	Map<String, String> getMediapoolImages(ComAdmin admin);

	Map<String, String> getProfileFields(ComAdmin admin, DbColumnType.SimpleDataType... allowedTypes);

}
