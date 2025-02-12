/*

    Copyright (C) 2019 AGNITAS AG (https://www.agnitas.org)

    This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
    This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
    You should have received a copy of the GNU Affero General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.

*/

package com.agnitas.emm.core.objectusage.web;

import java.util.Locale;

import org.apache.commons.text.StringEscapeUtils;

import com.agnitas.emm.core.objectusage.common.ObjectUsage;
import com.agnitas.emm.core.objectusage.common.ObjectUserType;
import com.agnitas.emm.core.target.web.TargetGroupViewHelper;
import com.agnitas.messages.I18nString;

/**
 * <p>
 * Utility class for generating hyperlinks for error messages
 * based on object usage information.
 * </p>
 * 
 * <p>
 * This class requires a message key &quot;referencingObject.&lt;type&gt;&quot;
 * defined for each enum constant in {@link ObjectUserType}.
 * (i. e. <i>referencingObject.TARGET_GROUP</i>).
 * 
 * Placeholders:
 * <ul>
 *   <li>{0} takes link or plain text label with type and name of using object</i>
 * </ul>
 */
final class HyperlinkHelper {

	/**
	 * Creates hyperlink for given object usage.
	 * If user type is not implemented, a plain text representation is returned.

	 * @param usage object usage information
	 * @param locale locale

	 * @return hyperlink or plain text
	 */
	public static final String toHyperlink(final ObjectUsage usage, final Locale locale) {
		switch(usage.getObjectUserType()) {
		case TARGET_GROUP:
			return targetGroupHyperlink(usage, locale);
			
		default:
			return plainText(usage, locale);

		}
	}
	
	private static final String targetGroupHyperlink(final ObjectUsage usage, final Locale locale) {
		return String.format(
				"<a href=\"%s\" data-relative=\"\">%s</a>", 
				TargetGroupViewHelper.targetGroupViewUrl(usage.getObjectUserID()),
				plainText(usage, locale));
	}
	
	private static final String plainText(final ObjectUsage usage, final Locale locale) {
		return I18nString.getLocaleString("referencingObject." + usage.getObjectUserType().name(), locale, StringEscapeUtils.escapeHtml4(usage.getObjectUserName()));
	}
	
}
