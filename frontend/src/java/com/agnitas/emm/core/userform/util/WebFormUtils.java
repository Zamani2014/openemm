/*

    Copyright (C) 2019 AGNITAS AG (https://www.agnitas.org)

    This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
    This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
    You should have received a copy of the GNU Affero General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.

*/

package com.agnitas.emm.core.userform.util;

import static org.agnitas.util.AgnUtils.getNormalizedRdirDomain;

import org.agnitas.emm.core.commons.util.ConfigService;
import org.agnitas.emm.core.velocity.VelocityCheck;

public class WebFormUtils {

	private static final String IMAGE_SRC_PATTERN = "{rdir-domain}formImage/{license-id}/{company-id}/{form-id}/{name}";
    private static final String IMAGE_SRC_PATTERN_NO_CACHE = "{rdir-domain}formImage/nc/{license-id}/{company-id}/{form-id}/{name}";
    private static final String IMAGE_THUMBNAIL_PATTERN = "{rdir-domain}formImage/thb/{company-id}/{form-id}/{name}";

	/**
	 * Gets the form image link.
	 *
	 * @param rdirDomain the rdir domain
	 * @param licenceId the license id
	 * @param companyId the company id
	 * @param formId the form id
	 * @param formName the name
	 * @param noCache the no caching
	 * @return the form image link
	 */
    @Deprecated // TODO Unused?
	public static String getFormImageLink(String rdirDomain, int licenceId, @VelocityCheck int companyId, int formId, String formName, boolean noCache) {
		return getImageSrcPattern(rdirDomain, licenceId, companyId, formId, noCache)
            .replace("{name}", formName);
	}

    /**
	 * Gets the form image link without licenceId
	 *
	 * @param rdirDomain the rdir domain
	 * @param companyId the company id
	 * @param formId the form id
	 * @param formName the name
	 * @param noCache the no caching
	 * @return the form image link
	 * 
	 * @see #getFormImageLink(String, int, int, String, boolean, ConfigService)
	 */
    @Deprecated // TODO Unused?
	public static String getFormImageLink(String rdirDomain, int companyId, int formId, String formName, boolean noCache) {
		return getFormImageLink(rdirDomain, companyId, formId, formName, noCache, ConfigService.getInstance());
	}

    /**
	 * Gets the form image link without licenceId
	 *
	 * @param rdirDomain the rdir domain
	 * @param companyId the company id
	 * @param formId the form id
	 * @param formName the name
	 * @param noCache the no caching
	 * @param configService configuration service
	 * 
	 * @return the form image link
	 */
    @Deprecated // TODO Unused?
	public static String getFormImageLink(String rdirDomain, int companyId, int formId, String formName, boolean noCache, final ConfigService configService) {
		final int licenseId = configService.getLicenseID();
		
		return getFormImageLink(rdirDomain, licenseId, companyId, formId, formName, noCache);
	}

	/**
	 * Gets the form image thumbnail link.
	 *
	 * @param rdirDomain the rdir domain
	 * @param companyId the company id
	 * @param formId the form id
	 * @param name the name
	 * @return the form image link
	 * 
	 * @see #getFormImageThumbnailLink(String, int, int, String, ConfigService)
	 */
    @Deprecated // TODO Unused?
	public static String getFormImageThumbnailLink(final String rdirDomain, final int companyId, final int formId, final String name) {
		return getFormImageThumbnailLink(rdirDomain, companyId, formId, name, ConfigService.getInstance());
	}

	/**
	 * Gets the form image thumbnail link.
	 *
	 * @param rdirDomain the rdir domain
	 * @param companyId the company id
	 * @param formId the form id
	 * @param name the name
	 * @param configService configuration service
	 * @return the form image link
	 */
    @Deprecated // TODO Unused?
	public static String getFormImageThumbnailLink(String rdirDomain, int companyId, int formId, String name, final ConfigService configService) {
		final int licenceId = configService.getLicenseID();
		
		return getImageSrcPattern(IMAGE_THUMBNAIL_PATTERN, rdirDomain, licenceId, companyId, formId)
				.replace("{name}", name);
	}

    public static String getImageSrcPattern(String rdirDomain, int licenceId, @VelocityCheck int companyId, int formId, boolean noCache) {
        if (noCache) {
			return getImageSrcPattern(IMAGE_SRC_PATTERN_NO_CACHE, rdirDomain, licenceId, companyId, formId);
		} else {
			return getImageSrcPattern(IMAGE_SRC_PATTERN, rdirDomain, licenceId, companyId, formId);
		}
    }

    public static String getImageThumbnailPattern(String rdirDomain, int licenceId, @VelocityCheck int companyId, int formId) {
       return getImageSrcPattern(IMAGE_THUMBNAIL_PATTERN, rdirDomain, licenceId, companyId, formId);
    }

	private static String getImageSrcPattern(String pattern, String rdirDomain, int licenceId, @VelocityCheck int companyId, int formId) {
		return pattern
				.replace("{form-id}", Integer.toString(formId))
                .replace("{license-id}", Integer.toString(licenceId))
                .replace("{rdir-domain}", getNormalizedRdirDomain(rdirDomain))
                .replace("{company-id}", Integer.toString(companyId));
    }
}
