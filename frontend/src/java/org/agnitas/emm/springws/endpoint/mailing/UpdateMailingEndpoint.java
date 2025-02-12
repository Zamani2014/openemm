/*

    Copyright (C) 2019 AGNITAS AG (https://www.agnitas.org)

    This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
    This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
    You should have received a copy of the GNU Affero General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.

*/

package org.agnitas.emm.springws.endpoint.mailing;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.agnitas.emm.core.mailing.service.MailingModel;
import org.agnitas.emm.core.useractivitylog.UserAction;
import org.agnitas.emm.springws.endpoint.BaseEndpoint;
import org.agnitas.emm.springws.endpoint.MailingEditableCheck;
import org.agnitas.emm.springws.endpoint.Utils;
import org.agnitas.emm.springws.jaxb.UpdateMailingRequest;
import org.agnitas.emm.springws.jaxb.UpdateMailingRequest.TargetIDList;
import org.agnitas.emm.springws.jaxb.UpdateMailingResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import com.agnitas.emm.core.mailing.service.MailingService;

@Endpoint
public class UpdateMailingEndpoint extends BaseEndpoint {

	private final MailingService mailingService;
	private final MailingEditableCheck mailingEditableCheck;

	public UpdateMailingEndpoint(@Qualifier("MailingService") MailingService mailingService, final MailingEditableCheck mailingEditableCheck) {
		this.mailingService = Objects.requireNonNull(mailingService);
		this.mailingEditableCheck = Objects.requireNonNull(mailingEditableCheck);
	}

	@PayloadRoot(namespace = Utils.NAMESPACE_ORG, localPart = "UpdateMailingRequest")
	public @ResponsePayload UpdateMailingResponse updateMailing(@RequestPayload UpdateMailingRequest request) throws Exception {
		final int companyId = Utils.getUserCompany();
		
		this.mailingEditableCheck.requireMailingEditable(request.getMailingID(), companyId);
		
		final MailingModel model = new MailingModel();
		model.setMailingId(request.getMailingID());
		model.setCompanyId(companyId);
		model.setShortname(request.getShortname());
		model.setDescription(request.getDescription());
		model.setMailinglistId(request.getMailinglistID());
		final TargetIDList targetIDList = request.getTargetIDList();
		if (targetIDList != null) {
			model.setTargetIDList(targetIDList.getTargetID());
		}
		model.setTargetMode(request.getMatchTargetGroups());
		model.setMailingType(request.getMailingType());
		model.setSubject(request.getSubject());
		model.setSenderName(request.getSenderName());
		model.setSenderAddress(request.getSenderAddress());
		model.setReplyToName(request.getReplyToName());
		model.setReplyToAddress(request.getReplyToAddress());
		model.setCharset(request.getCharset());
		model.setLinefeed(request.getLinefeed());
		model.setFormatString(request.getFormat());
		model.setOnePixelString(request.getOnePixel());
//		model.setAutoUpdate(request.isAutoUpdate());

		final List<UserAction> userActions = new ArrayList<>();
		mailingService.updateMailing(model, userActions);
		Utils.writeLog(userActivityLogService, userActions);

		return new UpdateMailingResponse();
	}
}
