/*

    Copyright (C) 2019 AGNITAS AG (https://www.agnitas.org)

    This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
    This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
    You should have received a copy of the GNU Affero General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.

*/

package com.agnitas.web;

import static org.agnitas.web.StrutsActionBase.ACTION_NEW;
import static org.agnitas.web.StrutsActionBase.ACTION_SAVE;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;

import org.agnitas.beans.BindingEntry;
import org.agnitas.util.AgnUtils;
import org.agnitas.web.RecipientForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessages;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.agnitas.emm.core.Permission;
import com.agnitas.emm.core.mailinglist.service.MailinglistApprovalService;

public class ComRecipientForm extends RecipientForm {
    private static final long serialVersionUID = -175166723099243720L;

    private Set<Integer> bulkIDs = new HashSet<>();

    private Map<String, String> bulkChange = new HashMap<>();
    private boolean trackingVeto;

    public final boolean isTrackingVeto() {
		return trackingVeto;
	}

	public final void setTrackingVeto(final boolean trackingVeto) {
		this.trackingVeto = trackingVeto;
	}

	/**
     * Getter for property bindingEntry.
     *
     * @return Value of property bindingEntry.
     */
	@Override
    public BindingEntry getBindingEntry(int type, int id) {
        Map<Integer, BindingEntry> sub =
                mailing.computeIfAbsent(id, unused -> new HashMap<>());

        return sub.computeIfAbsent(type, t -> {
            BindingEntry entry = getWebApplicationContext().getBean("BindingEntry", BindingEntry.class);
            entry.setMailinglistID(id);
            entry.setMediaType(t);
            return entry;
        });
    }

    public BindingEntry getEmailEntry(int id) {
        return getBindingEntry(0, id);
    }

    public BindingEntry getFaxEntry(int id) {
        return getBindingEntry(1, id);
    }

    public BindingEntry getPostEntry(int id) {
        return getBindingEntry(2, id);
    }

    public BindingEntry getMmsEntry(int id) {
        return getBindingEntry(3, id);
    }

    public BindingEntry getSmsEntry(int id) {
        return getBindingEntry(4, id);
    }

	@Override
	protected boolean isParameterExcludedForUnsafeHtmlTagCheck(String parameterName, HttpServletRequest request) {
		if (action == ACTION_SAVE || action == ACTION_NEW) {
		    return true;
        }
        
        return AgnUtils.allowed(request, Permission.RECIPIENT_PROFILEFIELD_HTML_ALLOWED) ||
                super.isParameterExcludedForUnsafeHtmlTagCheck(parameterName, request);
	}

    @Override
    protected ActionMessages checkForHtmlTags(HttpServletRequest request) {
        if (action == ACTION_SAVE || action == ACTION_NEW) {
            return null;
        }

        return super.checkForHtmlTags(request);
    }

    @Override
	public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        clearBulkIds();
        bulkChange = new HashMap<>();
        trackingVeto = false;
    }

    public void setBulkID(int id, String value) {
        if (value != null && (value.equals("on") || value.equals("yes") || value.equals("true"))) {
			this.bulkIDs.add(id);
		}
    }

    public String getBulkID(int id) {
        return this.bulkIDs.contains(id) ? "on" : "";
    }

    public Set<Integer> getBulkIds() {
        return this.bulkIDs;
    }

    public void clearBulkIds() {
        this.bulkIDs.clear();
    }

	/**
	 * Load additional Data which should be stored in request attributes etc.
	 *
	 * @param mapping
	 * @param request
	 */
    @Override
	protected void loadNonFormDataForErrorView(ActionMapping mapping, HttpServletRequest request) {
        ApplicationContext springContext = WebApplicationContextUtils.getWebApplicationContext(request.getServletContext());
        request.setAttribute("mailinglists",
                springContext.getBean("MailinglistApprovalService", MailinglistApprovalService.class)
                        .getEnabledMailinglistsForAdmin(AgnUtils.getAdmin(request)));
	}
	
	public Map<String, String> getBulkChange() {
		bulkChange = AgnUtils.repairFormMap(bulkChange);
		return bulkChange;
	}

	public void setBulkChange(String key, String value) {
		bulkChange.put(key, value);
	}
}
