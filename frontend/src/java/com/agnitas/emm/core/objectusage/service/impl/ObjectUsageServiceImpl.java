/*

    Copyright (C) 2019 AGNITAS AG (https://www.agnitas.org)

    This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
    This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
    You should have received a copy of the GNU Affero General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.

*/

package com.agnitas.emm.core.objectusage.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;

import com.agnitas.emm.core.objectusage.common.ObjectUsage;
import com.agnitas.emm.core.objectusage.common.ObjectUsages;
import com.agnitas.emm.core.objectusage.service.ObjectUsageService;
import com.agnitas.emm.core.profilefields.ProfileFieldException;
import com.agnitas.emm.core.profilefields.service.ProfileFieldService;
import com.agnitas.emm.core.target.service.ReferencedItemsService;

/**
 * Implementation of {@link ObjectUsageService} interface.
 */
public class ObjectUsageServiceImpl implements ObjectUsageService {
	
	/** The logger. */
	private static final transient Logger LOGGER = Logger.getLogger(ObjectUsageServiceImpl.class);

	/** Service to detect objects referencing target groups. */
	private ReferencedItemsService referencedItemsService;
	
	/** Service service handling profile fields. */
	private ProfileFieldService profileFieldService;
	
	@Override
	public final ObjectUsages listUsageOfAutoImport(final int companyID, final int autoImportID) {
		final List<ObjectUsage> list = new ArrayList<>();
		
		list.addAll(ObjectUsageServiceHelper.targetGroupsToObjectUsage(this.referencedItemsService.listTargetGroupsReferencingAutoImport(companyID, autoImportID)));
		// TODO List other objects referencing to auto-imports here

		return new ObjectUsages(list);
	}
	
	@Override
	public final ObjectUsages listUsageOfMailing(final int companyID, final int mailingID) {
		final List<ObjectUsage> list = new ArrayList<>();
		
		list.addAll(ObjectUsageServiceHelper.targetGroupsToObjectUsage(this.referencedItemsService.listTargetGroupsReferencingMailing(companyID, mailingID)));
		// TODO List other objects referencing to mailings here

		return new ObjectUsages(list);
	}

	@Override
	public final ObjectUsages listUsageOfProfileFieldByVisibleName(final int companyID, final String visibleName) {
		final List<ObjectUsage> list = new ArrayList<>();
		
		list.addAll(ObjectUsageServiceHelper.targetGroupsToObjectUsage(this.referencedItemsService.listTargetGroupsReferencingProfileFieldByVisibleName(companyID, visibleName)));
		// TODO List other objects referencing to mailings here

		return new ObjectUsages(list);
	}

	@Override
	public final ObjectUsages listUsageOfProfileFieldByDatabaseName(final int companyID, final String databaseName) {
		try {
			final String visibleName = this.profileFieldService.translateDatabaseNameToVisibleName(companyID, databaseName);
			
			return listUsageOfProfileFieldByVisibleName(companyID, visibleName);
		} catch(final ProfileFieldException e) {
			if(LOGGER.isInfoEnabled()) {
				LOGGER.info(String.format("No profile field with database name '%s'", databaseName));
			}
			
			return ObjectUsages.empty();
		}
	}

	@Override
	public final ObjectUsages listUsageOfLink(final int companyID, final int linkID) {
		final List<ObjectUsage> list = new ArrayList<>();
		
		list.addAll(ObjectUsageServiceHelper.targetGroupsToObjectUsage(this.referencedItemsService.listTargetGroupsReferencingLink(companyID, linkID)));
		// TODO List other objects referencing to mailings here

		return new ObjectUsages(list);
	}
	
	@Override
	public ObjectUsages listUsageOfReferenceTable(int companyID, int tableID) {
		// Implemented in extended class
		return ObjectUsages.empty();
	}

	@Override
	public ObjectUsages listUsageOfReferenceTableColumn(int companyID, int tableID, String columnName) {
		// Implemented in extended class
		return ObjectUsages.empty();
	}

	protected final ReferencedItemsService getReferencedItemsService() {
		return this.referencedItemsService;
	}
	
	/**
	 * Set service to detect objects referencing target groups.
	 * 
	 * @param service service to detect objects referencing target groups
	 */
	@Required
	public final void setTargetGroupReferencedItemsService(final ReferencedItemsService service) {
		this.referencedItemsService = Objects.requireNonNull(service,"Target group referenced items service is null");
	}
	
	/**
	 * Set service handling profile fields.
	 * 
	 * @param service service handling profile fields
	 */
	@Required
	public final void setProfileFieldService(final ProfileFieldService service) {
		this.profileFieldService = Objects.requireNonNull(service, "ProfileFieldService is null");
	}
}
