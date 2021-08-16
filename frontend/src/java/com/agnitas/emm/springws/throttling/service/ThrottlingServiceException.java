/*

    Copyright (C) 2019 AGNITAS AG (https://www.agnitas.org)

    This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
    This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
    You should have received a copy of the GNU Affero General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.

*/

package com.agnitas.emm.springws.throttling.service;

/**
 * Exception indicating an error in {@link ThrottlingService}.
 */
public class ThrottlingServiceException extends Exception {

	/** Serial version UID. */
	private static final long serialVersionUID = 5760815470801098292L;

	/**
	 * Creates a new instance.
	 */
	public ThrottlingServiceException() {
		// Empty
	}

	/**
	 * Creates a new instance with error message.
	 * 
	 * @param message error message
	 */
	public ThrottlingServiceException(final String message) {
		super(message);
	}

	/**
	 * Creates a new instance with cause.
	 * 
	 * @param cause cause
	 */
	public ThrottlingServiceException(final Throwable cause) {
		super(cause);
	}

	/**
	 * Creates a new instance with error message and cause.
	 * 
	 * @param message error message 
	 * @param cause cause
	 */
	public ThrottlingServiceException(final String message, final Throwable cause) {
		super(message, cause);
	}

}
