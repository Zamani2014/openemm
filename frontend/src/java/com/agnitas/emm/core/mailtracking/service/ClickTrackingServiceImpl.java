/*

    Copyright (C) 2019 AGNITAS AG (https://www.agnitas.org)

    This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
    This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
    You should have received a copy of the GNU Affero General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.

*/

package com.agnitas.emm.core.mailtracking.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.agnitas.beans.TrackableLink;
import org.agnitas.emm.core.commons.util.ConfigService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;

import com.agnitas.dao.ComTrackableLinkDao;
import com.agnitas.emm.core.commons.uid.ComExtensibleUID;
import com.agnitas.emm.core.mailing.cache.MailingContentTypeCache;
import com.agnitas.emm.core.mailtracking.service.TrackingVetoHelper.TrackingLevel;
import com.agnitas.emm.core.mailtracking.service.event.OnLinkClickedHandler;
import com.agnitas.emm.core.mobile.bean.DeviceClass;

/**
 * Implementation of {@link ClickTrackingService} interface.
 */
public final class ClickTrackingServiceImpl implements ClickTrackingService {

	/** The logger. */
	private static final transient Logger logger = Logger.getLogger(ClickTrackingServiceImpl.class);

	/** DAO handling trackable links. */
	private ComTrackableLinkDao trackableLinkDao;
	
	/** Configuration service. */
	private ConfigService configService;
	
	/** Cache for content types of mailings. */
	private MailingContentTypeCache mailingContentTypeCache;
	
	private final List<OnLinkClickedHandler> linkClickedHandlerList;
	
	public ClickTrackingServiceImpl() {
		this.linkClickedHandlerList = new ArrayList<>();
	}
	
	@Override
	public final void trackLinkClick(final ComExtensibleUID uid, final String remoteAddress, final DeviceClass deviceClass, final int deviceID, final int clientID) {
		if(uid == null) {
			logger.warn("No UID!", new Exception("No UID"));	// Create Exception to log stack trace
		} else {
			final TrackableLink link = trackableLinkDao.getTrackableLink(uid.getUrlID(), uid.getCompanyID());

			if(link != null) {
				final TrackingLevel trackingLevel = TrackingVetoHelper.computeTrackingLevel(uid, configService, mailingContentTypeCache);
				
				if(trackingLevel == TrackingLevel.ANONYMOUS) {
					if(logger.isInfoEnabled()) {
						logger.info(String.format("Recipient %d disagreed tracking", uid.getCustomerID()));
					}
				}
				
				final int trackedRecipientID = (trackingLevel == TrackingLevel.ANONYMOUS) ? 0 : uid.getCustomerID();
				
				final boolean result = trackableLinkDao.logClickInDB(link, trackedRecipientID, ((trackingLevel == TrackingLevel.ANONYMOUS) ? null : remoteAddress), deviceClass, deviceID, clientID);
					
				if(!result) {
					logger.warn(String.format("Could not track click on link %d for customer %d (company ID %d)", uid.getUrlID(), uid.getCustomerID(), uid.getCompanyID()));
				}
				
				notifyOnLinkClickedHandlers(uid.getCompanyID(), uid.getMailingID(), trackedRecipientID, uid.getUrlID());
			} else {
				logger.warn(String.format("Trackable link %d for company %d not found", uid.getUrlID(), uid.getCompanyID()), new Exception("Trackable link not found"));
			}
		}
	}
	
	private final void notifyOnLinkClickedHandlers(final int companyID, final int mailingID, final int customerID, final int linkID) {
		for(final OnLinkClickedHandler handler : this.linkClickedHandlerList) {
			try {
				handler.handleLinkClicked(companyID, mailingID, customerID, linkID);
			} catch(final Exception e) {
				logger.warn(
						String.format(
								"OnLinkClickedHandler of type '%s' threw an exception (company ID %d, mailing ID %d, customer ID %d, link ID %d)", 
								handler.getClass().getCanonicalName(),
								companyID,
								mailingID,
								customerID,
								linkID), 
						e);
			}
		}
	}
	
	/**
	 * Sets the DAO handling trackable links.
	 * 
	 * @param dao DAO handling trackable links
	 */
	@Required
	public final void setTrackableLinkDao(final ComTrackableLinkDao dao) {
		this.trackableLinkDao = Objects.requireNonNull(dao, "Trackable link DAO cannot be null");
	}

	/**
	 * Set configuration service.
	 * 
	 * @param service configuration service
	 */
	@Required
	public final void setConfigService(final ConfigService service) {
		this.configService = Objects.requireNonNull(service, "Config service cannot be null");
	}
	
	/**
	 * Set cache for mailing content types.
	 * 
	 * @param cache cache for mailing content types
	 */
	@Required
	public final void setMailingContentTypeCache(final MailingContentTypeCache cache) {
		this.mailingContentTypeCache = Objects.requireNonNull(cache, "Content type cache cannot be null");
	}
	
	/**
	 * Set list of handlers for "linkClicked" event.
	 * 
	 * @param handlers list of event handlers
	 */
	// Not @Required. List is initialized as empty list by default and this list can be left empty.
	public final void setOnLinkClickedHandlers(final List<OnLinkClickedHandler> handlers) {
		this.linkClickedHandlerList.clear();
		
		if(handlers != null) {
			this.linkClickedHandlerList.addAll(handlers);
		}
	}
}
