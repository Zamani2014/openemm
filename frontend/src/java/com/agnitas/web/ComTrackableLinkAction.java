/*

    Copyright (C) 2019 AGNITAS AG (https://www.agnitas.org)

    This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
    This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
    You should have received a copy of the GNU Affero General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.

*/

package com.agnitas.web;

import static org.agnitas.beans.BaseTrackableLink.KEEP_UNCHANGED;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.agnitas.actions.EmmAction;
import org.agnitas.beans.BaseTrackableLink;
import org.agnitas.beans.Mailing;
import org.agnitas.beans.Mediatype;
import org.agnitas.beans.TrackableLink;
import org.agnitas.beans.impl.PaginatedListImpl;
import org.agnitas.dao.EmmActionDao;
import org.agnitas.dao.MailingDao;
import org.agnitas.dao.TrackableLinkDao;
import org.agnitas.emm.core.commons.exceptions.InsufficientPermissionException;
import org.agnitas.emm.core.commons.util.ConfigService;
import org.agnitas.emm.core.commons.util.ConfigValue;
import org.agnitas.emm.core.useractivitylog.UserAction;
import org.agnitas.service.WebStorage;
import org.agnitas.util.AgnUtils;
import org.agnitas.web.BaseTrackableLinkForm;
import org.agnitas.web.StrutsActionBase;
import org.agnitas.web.TrackableLinkForm;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.agnitas.beans.ComAdmin;
import com.agnitas.beans.ComMailing;
import com.agnitas.beans.ComTrackableLink;
import com.agnitas.beans.LinkProperty;
import com.agnitas.beans.LinkProperty.PropertyType;
import com.agnitas.beans.MediatypeEmail;
import com.agnitas.emm.core.Permission;
import com.agnitas.emm.core.commons.intelliad.IntelliAdChecker;
import com.agnitas.emm.core.mailing.service.ComMailingBaseService;
import com.agnitas.emm.core.mediatypes.common.MediaTypes;
import com.agnitas.emm.core.trackablelinks.exceptions.TrackableLinkException;
import com.agnitas.emm.core.trackablelinks.service.ComTrackableLinkService;
import com.agnitas.emm.grid.grid.beans.ComGridTemplate;
import com.agnitas.service.ComWebStorage;
import com.agnitas.service.GridServiceWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Implementation of <strong>Action</strong> that validates a user logon.
 */
public class ComTrackableLinkAction extends StrutsActionBase {

	/** Checker for IntelliAd tracking strings. */
	private static final transient IntelliAdChecker intelliAdChecker = new IntelliAdChecker();

	/** The logger. */
	private static final transient Logger logger = Logger.getLogger(ComTrackableLinkAction.class);

	public static final int ACTION_SET_STANDARD_ACTION = ACTION_LAST + 1;
	public static final int ACTION_GLOBAL_USAGE = ACTION_LAST + 2;
	public static final int ACTION_SAVE_ALL = ACTION_LAST + 3;
	private static final int ACTION_ORG_LAST = ACTION_SAVE_ALL;

	public static final int ACTION_SET_EXTEND_LINKS = ACTION_ORG_LAST + 1;
	public static final int ACTION_SAVE_ADMIN_LINKS = ACTION_ORG_LAST + 3;
	
	public static final int ACTION_DELETE_GLOBAL_AND_INDIVIDUAL_EXTENSION = ACTION_ORG_LAST + 4;

	private static final String EDIT_LINKS_ACTION = "edit mailing links";
	private static final String LINK_TEMPLATE = "ID = %d. Trackable link %s. ";
	private static final String CHANGE_TEMPLATE = "%s changed from %s to %s. ";
	public static final String EMPTY = "\"\"";

	private MailingDao mailingDao;
	private EmmActionDao actionDao;
	private TrackableLinkDao linkDao;
	private ConfigService configService;

    private ComTrackableLinkService trackeableLinkService;

    private ComMailingBaseService mailingBaseService;
    
    private GridServiceWrapper gridService;
    private WebStorage webStorage;

	// --------------------------------------------------------- Public Methods

    @Override
    public String subActionMethodName(int subAction) {
        switch (subAction) {
            case ACTION_SAVE_ALL:
                return "save_all";
            case ACTION_DELETE_GLOBAL_AND_INDIVIDUAL_EXTENSION:
            	return "delete_global_and_individual_link_extensions";
            default:
                return super.subActionMethodName(subAction);
        }
    }
    
	/**
	 * Process the specified HTTP request, and create the corresponding HTTP
	 * response (or forward to another web component that will create it).
	 * Return an <code>ActionForward</code> instance describing where and how
	 * control should be forwarded, or <code>null</code> if the response has
	 * already been completed.
	 * 
	 * @param form
	 * @param req
	 * @param res
	 * @param mapping
	 *            The ActionMapping used to select this instance
	 * @exception IOException
	 *                if an input/output error occurs
	 * @exception ServletException
	 *                if a servlet exception occurs
	 * @return destination
	 */
    @Override
	public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
		// Validate the request parameters specified by the user
		ComTrackableLinkForm aForm = null;
		ActionMessages messages = new ActionMessages();
		ActionMessages errors = new ActionMessages();
		ActionForward destination = null;

		ComAdmin admin = AgnUtils.getAdmin(req);
		if (admin == null) {
			return mapping.findForward("logon");
		} else {
			aForm = (ComTrackableLinkForm) form;

            // set request params for grid mailing navigation
            req.setAttribute("isMailingGrid", req.getParameter("isMailingGrid"));
            req.setAttribute("templateId", req.getParameter("templateId"));
            req.setAttribute("limitedRecipientOverview", mailingBaseService.isLimitedRecipientOverview(admin, aForm.getMailingID()));

			if (logger.isInfoEnabled()) {
				logger.info("Action: " + aForm.getAction());
			}

			try {
				switch (aForm.getAction()) {
                    case ACTION_LIST:
                        destination = proceedWithList(mapping, aForm, req, admin); // mapping.findForward("list");
						writeUserActivityLog(AgnUtils.getAdmin(req), "trackable link list", "active tab - links");
						break;

                    case ACTION_VIEW:
                    	req.setAttribute("CAN_EDIT_URL", isUrlEditingAllowed(admin, aForm.getMailingID()));
                        aForm.setAction(ACTION_SAVE);
                        loadLink(aForm, req);
                        loadMailing(aForm, req, admin);
                        loadNotFormActions(req);
                        aForm.setCompanyHasDefaultLinkExtension(StringUtils.isNotBlank(configService.getValue(ConfigValue.DefaultLinkExtension, admin.getCompanyID())));
                        destination = mapping.findForward("view");
                        break;

                    case ACTION_SAVE:
						boolean intelliAdSettings = intelliAdSettingsValid(aForm, errors);
						boolean updateLinks = updateLinkUrlSuccessfully(aForm, req, errors);
						if (intelliAdSettings && updateLinks) {
							saveLink(aForm, req);
							aForm.setScrollToLinkId(aForm.getLinkID());
							aForm.setLinkAction(0);
							messages.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("default.changes_saved"));
							destination = proceedWithList(mapping, aForm, req, admin);
							break;
						}
						
						destination = mapping.findForward( "view");
                        break;

                    case ACTION_SAVE_ALL:
						if (intelliAdSettingsValid(aForm, errors)) {
							saveAll(aForm, req, admin);
	                        destination = proceedWithList(mapping, aForm, req, admin);
	                        aForm.setLinkAction(0);
	                        messages.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("default.changes_saved"));
	                        break;
						}
						
						destination = mapping.findForward( "view");
                        break;

                    case ACTION_DELETE_GLOBAL_AND_INDIVIDUAL_EXTENSION:
                    	try {
                    		trackeableLinkService.removeGlobalAndIndividualLinkExtensions(admin.getCompanyID(), aForm.getMailingID());
							writeUserActivityLog(admin, "edit mailing", "ID = " + aForm.getMailingID() + ". Removed global and individual link extensions");
	                        messages.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("default.changes_saved"));
                            destination = proceedWithList(mapping, aForm, req, admin);
                    	} catch (Exception e) {
                    		logger.error("Error removing global and individual link extensions (mailing ID: " + aForm.getMailingID() + ")", e);
                    		errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.trackablelinks.cannotRemoveExtensions"));
                            destination = mapping.findForward("view");
                        }

                    	break;

                    default:
                        loadMailing(aForm, req, admin);
						aForm.setAction(ACTION_LIST);
						this.loadLinks(aForm, req);
						destination = mapping.findForward("list");
				}
			} catch (Exception e) {
				logger.error("execute: " + e, e);
				errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.exception", configService.getValue(ConfigValue.SupportEmergencyUrl)));
			}
	
			if (!messages.isEmpty()) {
				saveMessages(req, messages);
			}
	
			if (destination != null && "list".equals(destination.getName())) {
				loadNotFormActions(req);
			}
	
			// Report any errors we have discovered back to the original form
			if (!errors.isEmpty()) {
				saveErrors(req, errors);
				logger.error("saving errors: " + destination);
			}
			return destination;
		}
	}

	private boolean updateLinkUrlSuccessfully(ComTrackableLinkForm aForm, HttpServletRequest req, ActionMessages errors) throws TrackableLinkException {
		try {
			updateLinkUrl(aForm, req);
		} catch (InsufficientPermissionException e) {
			errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.permissionDenied"));
			return false;
		}
		
		return true;
	}
	
	private boolean intelliAdSettingsValid(ComTrackableLinkForm aForm, ActionMessages errors) {
		if (aForm.isIntelliAdEnabled()) {
			if (StringUtils.isBlank(aForm.getIntelliAdIdString())) {
				errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage( "error.intelliad.no_tracking_string"));
				return false;
			}
			
			if (!intelliAdChecker.isValidTrackingString(aForm.getIntelliAdIdString())) {
				errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage( "error.intelliad.invalid_format"));
				return false;
			}
		}
		return true;
	}

	protected void loadLinks(TrackableLinkForm form, HttpServletRequest req) throws Exception {
		loadTrackableLinks(form, req);

		ComTrackableLinkForm aForm = (ComTrackableLinkForm) form;
		if (aForm.getMailingID() > 0) {
			aForm.setIsMailingUndoAvailable(mailingBaseService.checkUndoAvailable(aForm.getMailingID()));
			aForm.setWorkflowId(mailingBaseService.getWorkflowId(aForm.getMailingID(), AgnUtils.getCompanyID(req)));
		} else {
			aForm.setIsMailingUndoAvailable(false);
			aForm.setWorkflowId(0);
		}
	}

	protected void loadTrackableLinks(TrackableLinkForm aForm, HttpServletRequest req) throws Exception {
		Mailing aMailing = mailingDao.getMailing(aForm.getMailingID(), AgnUtils.getCompanyID(req));

		Map<String, ComTrackableLink> trackableLinks = aMailing.getTrackableLinks();

		final Comparator<ComTrackableLink> comparator = getComparator(aForm, webStorage);

		List<ComTrackableLink> sortedLinks = trackableLinks.values().stream()
				.sorted(comparator).collect(Collectors.toList());

		final PaginatedListImpl<ComTrackableLink> paginatedLinks = new PaginatedListImpl<>(
				sortedLinks, sortedLinks.size(), sortedLinks.size(), 1, aForm.getSort(), aForm.getOrder());
		req.setAttribute("trackableLinksList", paginatedLinks);

		aForm.setLinks(sortedLinks);
		aForm.setShortname(aMailing.getShortname());
		aForm.setDescription(aMailing.getDescription());
		aForm.setIsTemplate(aMailing.isIsTemplate());
		aForm.setOpenActionID(aMailing.getOpenActionID());
		aForm.setClickActionID(aMailing.getClickActionID());

		if (logger.isInfoEnabled()) {
			logger.info("loadMailing: mailing loaded");
		}
	}

	protected void loadNotFormActions(HttpServletRequest req) {
		List<EmmAction> emmNotFormActions = actionDao.getEmmNotFormActions(AgnUtils.getCompanyID(req), false);
		req.setAttribute("notFormActions", emmNotFormActions);
	}

	protected void loadLink(TrackableLinkForm form, HttpServletRequest req) {
		final int companyId = AgnUtils.getCompanyID(req);
		ComTrackableLinkForm aForm = (ComTrackableLinkForm) form;
		ComTrackableLink aLink = linkDao.getTrackableLink(aForm.getLinkID(), companyId);

		if (aLink != null) {
			final int mailingId = aLink.getMailingID();

			aForm.setLinkName(aLink.getShortname());
			aForm.setTrackable(aLink.getUsage());
			aForm.setLinkUrl(aLink.getFullUrl());
			aForm.setLinkAction(aLink.getActionID());
			aForm.setDeepTracking(aLink.getDeepTracking());
			aForm.setAltText(aLink.getAltText());
			aForm.setAdministrativeLink(aLink.isAdminLink());
			aForm.setLinkToView(aLink);
			aForm.setStaticLink(aLink.isStaticValue());

			aForm.setIsMailingUndoAvailable(mailingBaseService.checkUndoAvailable(mailingId));
			aForm.setWorkflowId(mailingBaseService.getWorkflowId(mailingId, companyId));
		} else {
			logger.error("could not load link: " + aForm.getLinkID());
		}
	}

    private boolean isUrlEditingAllowed(ComAdmin admin, int mailingID) {
    	return admin.permissionAllowed(Permission.MAILING_TRACKABLELINKS_URL_CHANGE) && this.trackeableLinkService.isUrlEditingAllowed(mailingID, admin.getCompanyID());
	}

    protected ActionForward proceedWithList(ActionMapping mapping, ComTrackableLinkForm aForm, HttpServletRequest req, ComAdmin admin) throws Exception {
        loadLinks(aForm, req);
        loadMailing(aForm, req, admin);
        setBulkID(aForm);
        setAdminLinks(aForm, req);
        setLinkItems(aForm);

        aForm.setCompanyHasDefaultLinkExtension(StringUtils.isNotBlank(configService.getValue(ConfigValue.DefaultLinkExtension, admin.getCompanyID())));

        Map<String, String> defaultExtensions = new HashMap<>();
        String defaultExtensionString = configService.getValue(ConfigValue.DefaultLinkExtension, admin.getCompanyID());
        if (StringUtils.isNotBlank(defaultExtensionString)) {
            if (defaultExtensionString.startsWith("?")) {
                defaultExtensionString = defaultExtensionString.substring(1);
            }
            String[] extensionProperties = defaultExtensionString.split("&");
            for (String extensionProperty : extensionProperties) {
                String[] extensionPropertyData = extensionProperty.split("=");
                String extensionPropertyName = URLDecoder.decode(extensionPropertyData[0], "UTF-8");
                String extensionPropertyValue = "";
                if (extensionPropertyData.length > 1) {
                    extensionPropertyValue = URLDecoder.decode(extensionPropertyData[1], "UTF-8");
                }
                defaultExtensions.put(extensionPropertyName, extensionPropertyValue);
            }
        }
        ObjectMapper mapper = new ObjectMapper();
        String JSON = mapper.writeValueAsString(defaultExtensions);
        req.setAttribute("defaultExtensions", JSON);
        return mapping.findForward("list");
    }

    protected void saveAll(ComTrackableLinkForm aForm, HttpServletRequest req, ComAdmin admin) throws Exception{
        ComMailing aMailing = mailingDao.getMailing(aForm.getMailingID(), admin.getCompanyID());
        if (aForm.isIntelliAdShown()) {
        	updateIntelliAdSettings(aForm, aMailing);
		}

        saveAllProceed(aMailing, aForm, req, admin);
        mailingDao.saveMailing(aMailing, false);
    }
    
    private void updateIntelliAdSettings( ComTrackableLinkForm form, ComMailing mailing) {
    	Map<Integer, Mediatype> mediaTypes = mailing.getMediatypes();
    	
    	try {
    		MediatypeEmail mediatype = (MediatypeEmail) mediaTypes.get(MediaTypes.EMAIL.getMediaCode());
    		
    		if (mediatype != null) {
				mediatype.setIntelliAdEnabled(form.isIntelliAdEnabled());
				if (form.isIntelliAdEnabled()) {
					mediatype.setIntelliAdString(form.getIntelliAdIdString());
				}
    		}
    	} catch (ClassCastException e) {
    		logger.warn( "No EMM email?", e);
    	}
    }

    protected void saveAllProceed(ComMailing aMailing, ComTrackableLinkForm aForm, HttpServletRequest req, ComAdmin admin) throws Exception {
		save(aMailing, aForm, admin);
		
		Set<Integer> bulkLinkIds = aForm.getBulkIDs();

        // ACTION_SET_EXTEND_LINKS
        // ACTION_SAVE_COMMON_EXTENSIONS
        if (admin.permissionAllowed(Permission.MAILING_EXTEND_TRACKABLE_LINKS)) {
            if (!aForm.isKeepExtensionsUnchanged()) {
				// search for link properties
				List<LinkProperty> linkProperties = new ArrayList<>();
				updateLinkPropertiesParameters(req, linkProperties);
				for (TrackableLink trackableLink : aMailing.getTrackableLinks().values()) {
					ComTrackableLink comTrackableLink = (ComTrackableLink)trackableLink;
					writeLinkExtensionsChangesLog(linkProperties, comTrackableLink, req);
				}
				
				trackeableLinkService.addExtensions(aMailing, bulkLinkIds, linkProperties);

                List<UserAction> userActions = new ArrayList<>();
                trackeableLinkService.replaceCommonExtensions(aMailing, linkProperties, bulkLinkIds, userActions);

                for (UserAction userAction : userActions) {
					writeUserActivityLog(admin, userAction);
				}

                trackeableLinkService.removeLegacyMailingLinkExtension(aMailing, bulkLinkIds);
            }
        }

        // ACTION_SAVE_ADMIN_LINKS
        if (aForm.isEveryPositionLink()){
            if (trackeableLinkService.saveEveryPositionLinks(aMailing, WebApplicationContextUtils.getRequiredWebApplicationContext(req.getSession().getServletContext()), bulkLinkIds)) {
                mailingBaseService.saveMailingWithUndo(aMailing, admin.getAdminID(), false);
            }
        }

        // saveAdminLinks(aForm, req);
        for (TrackableLink trackableLink : aMailing.getTrackableLinks().values()) {
            if (trackableLink != null) {
                trackableLink.setAdminLink(aForm.getAdminLink(trackableLink.getId()));
            }
        }

        // ACTION_SET_SHORTNAME
        trackeableLinkService.setShortname(aMailing, aForm.getLinkItemNames());
        //LOG description change

        // ACTION_SET_STANDARD_DEEPTRACKING
        trackeableLinkService.setStandardDeeptracking(aMailing, bulkLinkIds, aForm.getDeepTracking(), aForm.getLinkItemsDeepTracking());

        // ---
        // ACTION_ADD_DEFAULT_EXTENSION
        // replaced with JS

        // ACTION_CLEAR_EXTENSION
        // replaced with JS
    }

	private void save(Mailing aMailing, TrackableLinkForm aForm, ComAdmin admin) {

		// ACTION_SET_STANDARD_ACTION
		// setStandardActions
		TrackableLink aLink;
		ComTrackableLinkForm trackableLinkForm = (ComTrackableLinkForm) aForm;
		try {
			// set link actions
			int linkAction = aForm.getLinkAction();
			int globalUsage = aForm.getGlobalUsage();
			final int globalDeepTracking = trackableLinkForm.getDeepTracking();
			StringBuilder logMessage = new StringBuilder();
			Set<Integer> bulkIds = aForm.getBulkIDs();
			for (TrackableLink trackableLink : aMailing.getTrackableLinks().values()) {
				aLink = trackableLink;
				int id = aLink.getId();
				int linkItemAction = aForm.getLinkItemAction(id);
				int linkItemTrackable = aForm.getLinkItemTrackable(id);
				if (aLink.getActionID() != linkItemAction) {
					writeTrackableLinkActionChange(aLink, linkItemAction, logMessage);
					aLink.setActionID(linkItemAction);
				} else if ((linkAction != KEEP_UNCHANGED) && bulkIds.contains(id)) {
					writeTrackableLinkActionChange(aLink, linkAction, logMessage);
					aLink.setActionID(linkAction);
				}
				if (aLink.getUsage() != linkItemTrackable) {
					writeTrackableLinkTrackableChange(aLink, linkItemTrackable, logMessage);
					aLink.setUsage(linkItemTrackable);
				} else if ((globalUsage != KEEP_UNCHANGED) && bulkIds.contains(id)) {
					writeTrackableLinkTrackableChange(aLink, globalUsage, logMessage);
					aLink.setUsage(globalUsage);
				}
				String newName = trackableLinkForm.getLinkItemName(id);
				if (!newName.equals(aLink.getShortname())) {
					writeTrackableLinkDescriptionChange(aLink, newName, logMessage);
				}
				final boolean newIsAdministrativeLink = trackableLinkForm.getAdminLink(id);
				if(aLink.isAdminLink() != newIsAdministrativeLink){
					writeTrackableLinkAdministrativeChange(aLink, newIsAdministrativeLink, logMessage);
				}
				final int newDeepTracking = trackableLinkForm.getLinkItemDeepTracking(id);
				if(aLink.getDeepTracking() != newDeepTracking) {
					writeTrackableLinkDeepTrackableChange(aLink, newDeepTracking ,logMessage);
				} else if(globalDeepTracking != KEEP_UNCHANGED && bulkIds.contains(id)){
					writeTrackableLinkDeepTrackableChange(aLink, globalDeepTracking ,logMessage);
				}

				//Write UAL for single ling as a one entry
				if (logMessage.length() != 0) {
					logMessage.insert(0,
							String.format(LINK_TEMPLATE, aLink.getId(), aLink.getFullUrl()));
					writeUserActivityLog(admin, EDIT_LINKS_ACTION, logMessage.toString().trim());
					logMessage = new StringBuilder();
				}
			}
			writeCommonActionChanges(aMailing, aForm, admin);
			aMailing.setOpenActionID(aForm.getOpenActionID());
			aMailing.setClickActionID(aForm.getClickActionID());
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		// ACTION_GLOBAL_USAGE
		// saveGlobalUsage(aMailing, aForm, req);
	}
 
    private void updateLinkPropertiesParameters(HttpServletRequest request, List<LinkProperty> linkProperties) {
		Enumeration<String> parameterNamesEnum = request.getParameterNames();
		while (parameterNamesEnum.hasMoreElements()) {
			String parameterName = parameterNamesEnum.nextElement();
			if (parameterName.startsWith(BaseTrackableLinkForm.PROPERTY_NAME_PREFIX)) {
				int propertyID = Integer.parseInt(parameterName.substring(BaseTrackableLinkForm.PROPERTY_NAME_PREFIX.length()));
				String[] extensionNames = request.getParameterValues(parameterName);
				String[] extensionValues = request.getParameterValues(BaseTrackableLinkForm.PROPERTY_VALUE_PREFIX + propertyID);
				if (extensionNames != null && extensionNames.length > 0 && StringUtils.isNotBlank(extensionNames[0])) {
					LinkProperty newProperty;
					if (extensionValues != null && extensionValues.length > 0 && StringUtils.isNotBlank(extensionValues[0])) {
						newProperty = new LinkProperty(PropertyType.LinkExtension, extensionNames[0], extensionValues[0]);
					} else {
						newProperty = new LinkProperty(PropertyType.LinkExtension, extensionNames[0], "");
					}
					linkProperties.add(newProperty);
				}
			}
		}
	}

    private void setAdminLinks(ComTrackableLinkForm aForm, HttpServletRequest req) {
		aForm.clearAdminLinks();
		for (BaseTrackableLink trackableLink : aForm.getLinks()) {
			aForm.setAdminLink(trackableLink.getId(), ((TrackableLink)trackableLink).isAdminLink());
		}
	}

    private void setBulkID(ComTrackableLinkForm aForm) {
        aForm.clearBulkIDs();
        for (BaseTrackableLink trackableLink : aForm.getLinks()) {
            aForm.setBulkID(trackableLink.getId(), "on");
        }
    }

    private void setLinkItems(ComTrackableLinkForm aForm) {
        aForm.clearLinkItemActions();
        aForm.clearLinkItemDeepTracking();
        aForm.clearLinkItemTrackable();
        aForm.clearLinkItemName();
        for (BaseTrackableLink link : aForm.getLinks()) {
        	TrackableLink trackableLink = (TrackableLink) link;
            int id = trackableLink.getId();
            aForm.setLinkItemAction(id, trackableLink.getActionID());
            aForm.setLinkItemDeepTracking(id, trackableLink.getDeepTracking());
            aForm.setLinkItemTrackable(id, trackableLink.getUsage());
            aForm.setLinkItemName(id, trackableLink.getShortname());
        }
    }

	/**
	 * Saves link.
	 */
	protected void saveLink(ComTrackableLinkForm aForm, HttpServletRequest req) {
		ComTrackableLink aLink = linkDao.getTrackableLink(aForm.getLinkID(), AgnUtils.getCompanyID(req));
		if (aLink != null) {

			//User activity logging
			StringBuilder logMessage = new StringBuilder();
			writeTrackableLinkDescriptionChange(aLink, aForm.getLinkName(), logMessage);
			writeTrackableLinkTrackableChange(aLink, aForm.getTrackable(), logMessage);
			writeTrackableLinkActionChange(aLink, aForm.getLinkAction(), logMessage);
			writeTrackableLinkAdministrativeChange(aLink, aForm.isAdministrativeLink(), logMessage);
			writeTrackableLinkStaticChange(aLink, aForm.isStaticLink(), logMessage);

			aLink.setShortname(aForm.getLinkName());
			aLink.setUsage(aForm.getTrackable());
			aLink.setActionID(aForm.getLinkAction());
			aLink.setAdminLink(aForm.isAdministrativeLink());
			aLink.setStaticValue(aForm.isStaticLink());

			if (req.getParameter("deepTracking") != null) { // only if parameter is provided in form
				writeTrackableLinkDeepTrackableChange(aLink, aForm.getDeepTracking(), logMessage);
				aLink.setDeepTracking(aForm.getDeepTracking());
			}

			ComAdmin admin = AgnUtils.getAdmin(req);
			// Only change link properties if adminuser is allowed to
			if (admin != null && admin.permissionAllowed(Permission.MAILING_EXTEND_TRACKABLE_LINKS)) {
				// search for link properties
				List<LinkProperty> linkProperties = new ArrayList<>();
				updateLinkPropertiesParameters(req, linkProperties);
                writeLinkExtensionsChangesLog(linkProperties, aLink, req);

                for (Iterator<LinkProperty> iter = linkProperties.listIterator(); iter.hasNext(); ) {
                    LinkProperty linkProperty = iter.next();
                    if (StringUtils.isBlank(linkProperty.getPropertyName())) {
                        iter.remove();
                    }
                }

				aLink.setProperties(linkProperties);
				aLink.setExtendByMailingExtensions(aLink.isExtendByMailingExtensions());
			}
			linkDao.saveTrackableLink(aLink);

			if (logMessage.length() != 0) {
				logMessage.insert(0, String.format(LINK_TEMPLATE, aLink.getId(), aLink.getFullUrl()));
				writeUserActivityLog(admin, EDIT_LINKS_ACTION, logMessage.toString().trim());
			}
		}
	}

	/**
	 * Update link target including permission checks, ...
	 * 
	 * @param form {@link ComTrackableLinkForm}
	 * @param request {@link HttpServletRequest}
	 * 
	 * @throws InsufficientPermissionException if user does not have sufficient permissions to change link target
	 * @throws TrackableLinkException on errors updating link target
	 */
	private void updateLinkUrl(ComTrackableLinkForm form, HttpServletRequest request) throws InsufficientPermissionException, TrackableLinkException {
		if (StringUtils.isNotBlank(form.getLinkUrl())) {
			ComAdmin admin = AgnUtils.getAdmin(request);
			ComTrackableLink link = this.linkDao.getTrackableLink(form.getLinkID(), admin.getCompanyID());
			
			if (!form.getLinkUrl().equals(link.getFullUrl())) {
				if (admin.permissionAllowed(Permission.MAILING_TRACKABLELINKS_URL_CHANGE)) {
					this.trackeableLinkService.updateLinkTarget(link, form.getLinkUrl());

					this.writeUserActivityLog(admin, "edit link target", "Set target of link " + link.getId() + " to " + form.getLinkUrl());
				} else {
					logger.warn("Admin " + admin.getUsername() + " (ID " + admin.getAdminID() + ") has no permission to edit link URLs");
					throw new InsufficientPermissionException(admin.getAdminID(), "mailing.trackablelinks.url.change");
				}
			}
		}
	}
	
	protected void loadMailing(ComTrackableLinkForm aForm, HttpServletRequest req, ComAdmin admin) {
		aForm.setCompanyHasDefaultLinkExtension(StringUtils.isNotBlank(configService.getValue(ConfigValue.DefaultLinkExtension, admin.getCompanyID())));
		int mailingId = aForm.getMailingID();
		int companyId = AgnUtils.getCompanyID(req);

		ComMailing mailing = mailingDao.getMailing(mailingId, companyId);
		List<LinkProperty> commonLinkExtensions = mailing.getCommonLinkExtensions();
		aForm.setIsTemplate(mailing.isIsTemplate());
        aForm.setShortname(mailing.getShortname());
		aForm.setCommonLinkExtensions(commonLinkExtensions);
		
		Mediatype mediaType = mailing.getMediatypes().get(MediaTypes.EMAIL.getMediaCode());
		if (mediaType != null) {
			try {
				MediatypeEmail emailType = (MediatypeEmail) mediaType;
				
				aForm.setIntelliAdEnabled( emailType.isIntelliAdEnabled());
				aForm.setIntelliAdIdString( emailType.getIntelliAdString());
			} catch (ClassCastException e) {
				logger.warn( "No EMM Mail?", e);
			}
		}

        //restore mailingGrid attribute
        int gridTemplateId = gridService.getGridTemplateIdByMailingId(aForm.getMailingID());
        aForm.setMailingGrid(false);
        if (gridTemplateId != 0) {
            ComGridTemplate template = gridService.getGridTemplate(mailing.getCompanyID(), gridTemplateId);
            if (template != null) {
                aForm.setMailingGrid(true);
                req.setAttribute("templateId", gridTemplateId);
            }
        }
        // For backward compatibility
        req.setAttribute("isMailingGrid", aForm.isIsMailingGrid());
	}

    private void writeLinkExtensionsChangesLog(List <LinkProperty> newLinkProperties, ComTrackableLink aLink, HttpServletRequest request){
        try {
        	ComAdmin admin = AgnUtils.getAdmin(request);
            int mailingId = aLink.getMailingID();
            String linkName = aLink.getFullUrl();
            List <LinkProperty> oldLinkProperties = aLink.getProperties();

            if ((oldLinkProperties == null) || (newLinkProperties == null)){
                return;
            }

            String oldPropertyName;
            String oldPropertyValue;
            String newPropertyName;
            String newPropertyValue;

            //log edited or removed extensions
            int counter = 0;

            for (LinkProperty oldLinkProperty : oldLinkProperties) {
                oldPropertyName = oldLinkProperty.getPropertyName();
                oldPropertyValue = oldLinkProperty.getPropertyValue();
                newPropertyName = newLinkProperties.get(counter).getPropertyName();
                newPropertyValue = newLinkProperties.get(counter).getPropertyValue();

                if (StringUtils.isBlank(newPropertyName)){
                    writeUserActivityLog(admin, "edit mailing links",
							String.format("ID = %d. Trackable link %s extension %d removed",
									mailingId, linkName, counter+1));
                } else if ((!oldPropertyName.equals(newPropertyName)) || (!oldPropertyValue.equals(newPropertyValue)) ){
                    writeUserActivityLog(admin, "edit mailing links",
							String.format("ID = %d. Trackable link %s extension %d changed from %s : %s to %s : %s",
									mailingId, linkName, counter+1,
									oldPropertyName, oldPropertyValue,
									newPropertyName, newPropertyValue));
                }
                counter++;
            }

            //log added extensions
            int oldSize = oldLinkProperties.size();
            int newSize = newLinkProperties.size();

            if (newSize > oldSize){
                for (int i = oldSize; i <= newSize+1; i++){
                    writeUserActivityLog(admin, "edit mailing links",
							String.format("ID = %d. Trackable link %s extension %d added %s : %s",
									mailingId, linkName, counter+1,
									newLinkProperties.get(i).getPropertyName(), newLinkProperties.get(i).getPropertyValue()));
                    counter++;
                }
            }

            if (logger.isInfoEnabled()){
                logger.info("save Trackable links Extensions");
            }
        } catch (Exception e) {
            logger.error("Log EMM Trackable links extensions changes error: ComTrackableUserFormLinkAction" + e);
        }
    }

	private void writeTrackableLinkActionChange(TrackableLink link, int newAction, StringBuilder logMessage) {
		int companyId = link.getCompanyID(),
				oldAction = link.getActionID();
		if (newAction != oldAction) {
			logMessage.append(String.format(CHANGE_TEMPLATE, "Action",
					getActionName(oldAction, companyId),
					getActionName(newAction, companyId)));
		}
	}

	private void writeTrackableLinkDescriptionChange(TrackableLink aLink, String newShortname, StringBuilder logMessage) {
		String oldShortname = aLink.getShortname();
		newShortname = StringUtils.defaultString(newShortname);
		oldShortname = StringUtils.defaultString(oldShortname);
		if (!StringUtils.equals(oldShortname, newShortname)) {
			logMessage.append(String.format(CHANGE_TEMPLATE, "Description", oldShortname, newShortname));
		}
	}

	private void writeTrackableLinkTrackableChange(TrackableLink link, int newTrackable, StringBuilder logMessage){
		int oldTrackable = link.getUsage();
		if (oldTrackable != newTrackable) {
			logMessage.append(String.format(CHANGE_TEMPLATE, "Measurable ",
					getTrackableName(oldTrackable),
					getTrackableName(newTrackable)));
		}
	}

	private void writeTrackableLinkDeepTrackableChange(TrackableLink link, int newDeepTrackable, StringBuilder logMessage){
		int oldDeepTrackable = link.getDeepTracking();
		if (oldDeepTrackable != newDeepTrackable) {
			logMessage.append(String.format(CHANGE_TEMPLATE, "Tracking at shop/website ",
					getDeepTrackableName(oldDeepTrackable),
					getDeepTrackableName(newDeepTrackable)));
		}
	}

	private void writeTrackableLinkAdministrativeChange(final TrackableLink aLink, final boolean newIsAdminValue,
														final StringBuilder logMessage) {
		boolean adminLink = aLink.isAdminLink();
		if (adminLink != newIsAdminValue) {
			logMessage.append(String.format(CHANGE_TEMPLATE, "Is administrative link",
					AgnUtils.boolToString(adminLink),
					AgnUtils.boolToString(newIsAdminValue)));
		}
	}

	private void writeTrackableLinkStaticChange(final ComTrackableLink aLink, final boolean newIsStatic,
														final StringBuilder logMessage) {
		boolean staticValue = aLink.isStaticValue();
		if (staticValue != newIsStatic) {
			logMessage.append(String.format(CHANGE_TEMPLATE, "Is static",
					AgnUtils.boolToString(staticValue),
					AgnUtils.boolToString(newIsStatic)));
		}
	}

	protected void writeCommonActionChanges(Mailing mailing, TrackableLinkForm form, ComAdmin admin){
		try {
			int mailingId = form.getMailingID();
			int companyId = admin.getCompanyID();

			//log Open Action changes
			int newOpenAction = form.getOpenActionID();
			int oldOpenAction = mailing.getOpenActionID();
			if (oldOpenAction != newOpenAction){
				writeUserActivityLog(admin, EDIT_LINKS_ACTION,
						String.format("ID = %d. Trackable links Open Action changed from %s to %s", mailingId,
								getActionName(oldOpenAction, companyId), getActionName(newOpenAction, companyId)));
			}

			//log Open Action changes
			int newClickAction = form.getClickActionID();
			int oldClickAction = mailing.getClickActionID();
			if (oldClickAction != newClickAction){
				writeUserActivityLog(admin, EDIT_LINKS_ACTION,
						String.format("ID = %d. Trackable links Click Action changed from %s to %s", mailingId,
								getActionName(oldClickAction, companyId), getActionName(newClickAction, companyId)));
			}

			if (logger.isInfoEnabled()){
				logger.info("save Trackable links Open/Click Actions, mailing ID =  " + mailingId );
			}
		} catch (Exception e) {
			logger.error("Log Trackable links Open/Click Action changes error: " + e, e);
		}
	}

	/**
	 * Return mailing trackable link setting "Trackable" text representation by id
	 *
	 * @param type Trackable type id
	 * @return     "Trackable" setting text representation
	 */
	private String getTrackableName(int type){

		switch (type){
			case 0:
				return "not trackable";
			case 1:
				return "only text version";
			case 2:
				return "only HTML version";
			case 3:
				return "text and HTML version";
			default:
				return "unknown type";
		}
	}

	private String getDeepTrackableName(final int type){

		switch (type){
			case 0:
				return "no";
			case 1:
				return "with cookie";
			case 2:
				return "with URL-parameters";
			case 3:
				return "Cookie and URL";
			default:
				return "unknown type";
		}
	}

	private String getActionName(int actionId, int companyId ){
		EmmAction action = actionDao.getEmmAction(actionId, companyId);
		return action == null ? "" : action.getShortname();
	}

    private Comparator<ComTrackableLink> getComparator(final TrackableLinkForm form, final WebStorage webStorageParam) {
        syncForm(form, webStorageParam);
        Comparator<ComTrackableLink> comparator = Comparator.comparing(TrackableLink::getId);
        if (StringUtils.equalsIgnoreCase("fullUrlWithExtensions", form.getSort())) {
            comparator = Comparator.comparing(TrackableLink::getFullUrlWithExtensions);
        } else if (StringUtils.equalsIgnoreCase("description", form.getSort())) {
            comparator = Comparator.comparing((l) -> StringUtils.trimToEmpty(l.getShortname()));
        }

        if (!AgnUtils.sortingDirectionToBoolean(form.getOrder(), true)) {
            comparator = comparator.reversed();
        }

        return comparator;
    }

    private void syncForm(final TrackableLinkForm form, final WebStorage webStorageParam) {
        webStorageParam.access(ComWebStorage.TRACKABLE_LINKS, storage -> {
            if (StringUtils.isNoneBlank(form.getSort())) {
                storage.setColumnName(form.getSort());
                storage.setAscendingOrder(AgnUtils.sortingDirectionToBoolean(form.getOrder()));
            } else {
                form.setSort(storage.getColumnName());
                form.setOrder(storage.isAscendingOrder() ? "ascending" : "descending");
            }
        });
    }

	@Required
	public void setConfigService(ConfigService configService) {
		this.configService = configService;
	}

	public void setMailingDao(MailingDao mailingDao) {
		this.mailingDao = mailingDao;
	}

	public void setActionDao(EmmActionDao actionDao) {
		this.actionDao = actionDao;
	}

	public void setLinkDao(TrackableLinkDao linkDao) {
		this.linkDao = linkDao;
	}

    public void setTrackeableLinkService(ComTrackableLinkService trackeableLinkService) {
        this.trackeableLinkService = trackeableLinkService;
    }

    public void setMailingBaseService(ComMailingBaseService mailingBaseService) {
        this.mailingBaseService = mailingBaseService;
    }

    @Required
    public void setGridService(GridServiceWrapper gridServiceWrapper) {
        this.gridService = gridServiceWrapper;
    }

	@Required
	public void setWebStorage(WebStorage webStorage) {
		this.webStorage = webStorage;
	}
}
