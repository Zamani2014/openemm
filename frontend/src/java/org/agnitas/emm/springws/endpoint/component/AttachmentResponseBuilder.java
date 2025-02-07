/*

    Copyright (C) 2019 AGNITAS AG (https://www.agnitas.org)

    This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
    This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
    You should have received a copy of the GNU Affero General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.

*/

package org.agnitas.emm.springws.endpoint.component;

import org.agnitas.beans.MailingComponent;
import org.agnitas.emm.springws.jaxb.Attachment;
import org.agnitas.emm.springws.jaxb.AttachmentDateTimeDefault;
import org.agnitas.emm.springws.jaxb.AttachmentDateTimeISO;
import org.apache.log4j.Logger;

public class AttachmentResponseBuilder {
	
	@SuppressWarnings("unused")
	private static Logger logger = Logger.getLogger(AttachmentResponseBuilder.class);
	
	public org.agnitas.emm.springws.jaxb.Attachment createResponse(MailingComponent component, boolean copyData, boolean useISODateFormat) {
		Attachment attachment;
		if (useISODateFormat) {
			AttachmentDateTimeISO attachmentDateTimeISO = new AttachmentDateTimeISO();
			attachmentDateTimeISO.setTimestamp(component.getTimestamp());
			attachment = attachmentDateTimeISO;
		} else {
			AttachmentDateTimeDefault attachmentDateTimeDefault = new AttachmentDateTimeDefault();
			attachmentDateTimeDefault.setTimestamp(component.getTimestamp());
			attachment = attachmentDateTimeDefault;
		}
		attachment.setComponentID(component.getId());
		attachment.setMimeType(component.getMimeType());
		attachment.setComponentType(component.getType().getCode());
		attachment.setComponentName(component.getComponentName());

		byte[] data = component.getBinaryBlock();
		attachment.setSize(data != null ? data.length : 0);
		if (copyData) {
			attachment.setData(data);
		}
		
		return attachment;
	}

    public org.agnitas.emm.springws.jaxb.Attachment createResponse(MailingComponent component, boolean copyData) {
        return createResponse(component, copyData, false);
    }
}
