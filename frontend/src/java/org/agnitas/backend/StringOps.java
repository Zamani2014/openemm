/*

    Copyright (C) 2019 AGNITAS AG (https://www.agnitas.org)

    This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
    This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
    You should have received a copy of the GNU Affero General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.

*/

package org.agnitas.backend;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.IDN;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

/**
 * Some useful string operations as static methods
 */
public class StringOps {
	/**
	 * translation table for transforming between HTML and text
	 */
	static private Map<String, String> transtab = null;
	static private Map<String, String> rtranstab = null;
	static private Pattern entityFinder = null;
	static private Map <String, String> homeDirectories = null;

	static {
		transtab = new HashMap<>();

		transtab.put("lt", "<");
		transtab.put("gt", ">");
		transtab.put("amp", "&");
		transtab.put("quot", "\"");
		transtab.put("apos", "'");
		transtab.put("nbsp", " ");
		transtab.put("bdquo", "„");
		transtab.put("sbquo", "‚");
		transtab.put("lsquo", "‘");
		transtab.put("ldquo", "“");
		transtab.put("rdquo", "”");
		transtab.put("lsquo", "‘");
		transtab.put("rsquo", "’");
		transtab.put("laquo", "«");
		transtab.put("raquo", "»");
		transtab.put("lsaquo", "‹");
		transtab.put("rsaquo", "›");

		rtranstab = new HashMap<>();
		StringBuffer pm = new StringBuffer();
		String pfix = "&(";
		for (Entry<String, String> entry : transtab.entrySet()) {
			rtranstab.put(entry.getValue(), entry.getKey());
			pm.append(pfix);
			pm.append(entry.getKey());
			pfix = "|";
		}
		pm.append(");");
		entityFinder = Pattern.compile(pm.toString());
	}

	/**
	 * Decode a single HTML entity to its representation. If not found
	 * use the passed default value
	 *
	 * @param ent  the entity to decode
	 * @param dflt the default value to return, if entity is not known
	 * @return the decoded value, if available, dflt otherwise
	 */
	public static String decodeEntity(String ent, String dflt) {
		String rc = transtab.get(ent);
		return rc == null ? dflt : rc;
	}

	/**
	 * Decode a single HTML entity to its representation. If not found
	 * return null
	 *
	 * @param ent the entity to decode
	 * @return the decoded value, if available, null otherwise
	 */
	public static String decodeEntity(String ent) {
		return transtab.get(ent);
	}

	/**
	 * Encode a single character to its HTML entity. If not found
	 * use the passed default value
	 *
	 * @param plain the string with a single character to encode
	 * @param dflt  the default value
	 * @return the HTML entity, if available, dflt otherwise
	 */
	public static String encodeEntity(String plain, String dflt) {
		String rc = rtranstab.get(plain);
		return rc == null ? dflt : rc;
	}

	/**
	 * Encode a single character to its HTML entity. If not found
	 * return null
	 *
	 * @param plain the string with a single character to encode
	 * @return the HTML entity, if available, null otherwise
	 */
	public static String encodeEntity(String plain) {
		return rtranstab.get(plain);
	}

	/**
	 * Remove HTML entities from input string and replace them,
	 * if known, with a plain variant
	 *
	 * @param s the string to process
	 * @return the input string with removed HTML entities
	 */
	public static String removeEntities(String s) {
		Matcher m = entityFinder.matcher(s);
		int slen = s.length();
		StringBuffer d = new StringBuffer(slen);
		int pos = 0;

		while ((pos < slen) && m.find(pos)) {
			int start = m.start();

			if (pos < start) {
				d.append(s, pos, start);
			}
			d.append(transtab.get(m.group(1)));
			pos = m.end();
		}
		if (pos < slen) {
			d.append(s.substring(pos));
		}
		return d.toString();
	}

	/**
	 * replaces every occurance of `pattern' in `str' with `replace'
	 *
	 * @param str     the source
	 * @param pattern the pattern to replace
	 * @param replace the substition
	 * @return the new string with replacements
	 */
	public static String replace(String str, String pattern, String replace) {
		int s = 0;
		int e = 0;
		StringBuffer result = new StringBuffer();
		while ((e = str.indexOf(pattern, s)) >= 0) {
			result.append(str, s, e);
			result.append(replace);
			s = e + pattern.length();
		}
		result.append(str.substring(s));
		return result.toString();
	}

	/**
	 * fill the left side of the string with `0's until `length' is reached
	 *
	 * @param text   the source
	 * @param length the length to extend the string to
	 * @return the filled string
	 */
	public static String format_number(String text, int length) {
		int text_length = text.length();

		if (text_length >= length) {
			return text;
		}
		String result = text;
		for (int i = text_length; i != length; i++) {
			result = "0" + result;
		}
		return result;
	}

	public static String format_number(long number, int length) {
		return format_number(Long.toString(number), length);
	}

	/**
	 * fill the left side of a string representation of a number with `0's until
	 * length is reached
	 *
	 * @param nr     the source
	 * @param length the length to extend the string to
	 * @return the filled string
	 */
	public static String format_number(int nr, int length) {
		return format_number(Integer.toString(nr), length);
	}

	/**
	 * Split a comma separated string into its elements
	 *
	 * @param str the input string
	 * @return the filled vector
	 */
	public static Vector<String> splitString(String str) {
		return splitString(null, str);
	}

	/**
	 * Split a comma separated string into its elements
	 *
	 * @param v   optional existing vector to append to
	 * @param str the input string
	 * @return the filled vector
	 */
	public static Vector<String> splitString(Vector<String> v, String str) {
		if (v == null) {
			v = new Vector<>();
		}
		if (StringUtils.isNotEmpty(str)) {
			for (String part : str.split(",")) {
				v.add(part.trim());
			}
		}

		return v;
	}

	/**
	 * Converts a DB blob into a string
	 *
	 * @param blob     the source
	 * @param encoding the encoding of the blob
	 * @return the extracted string
	 */
	public static String blob2string(Blob blob, String encoding) throws SQLException, UnsupportedEncodingException {
		return blob == null ? "" : new String(blob.getBytes(1, (int) blob.length()), encoding);
	}

	/**
	 * Converts a DB clob into a string
	 *
	 * @param clob the source
	 * @return the extracted string
	 */
	public static String clob2string(Clob clob) throws SQLException {
		return clob == null ? "" : clob.getSubString(1, (int) clob.length());
	}

	/**
	 * Simple formating for a date
	 *
	 * @param date the source
	 * @return the formated date as string
	 */
	public static String formatDate(Date date) {
		SimpleDateFormat fmt = new SimpleDateFormat("dd-MM-yyyy:HH:mm:ss");

		return fmt.format(date);
	}

	/**
	 * Format a string to its SQL representation
	 *
	 * @param str the source
	 * @return the SQL conform string
	 */
	public static String sqlString(String str) {
		StringBuffer r = new StringBuffer(str.length() + 8);

		r.append('\'');
		for (int n = 0; n < str.length(); ++n) {
			char ch = str.charAt(n);

			r.append(ch);
			if (ch == '\'') {
				r.append(ch);
			}
		}
		r.append('\'');
		return r.toString();
	}

	/**
	 * Transform an SQL representation to a string
	 *
	 * @param str the source
	 * @return the stripped off version
	 */
	public static String unSqlString(String str) {
		int start, end;

		start = 0;
		end = str.length();
		if ((end > 0) && (str.charAt(0) == '\'') && (str.charAt(end - 1) == '\'')) {
			++start;
			--end;

			StringBuffer r = new StringBuffer(end - start + 1);
			for (int n = start; n < end; ++n) {
				char ch = str.charAt(n);

				r.append(ch);
				if ((ch == '\'') && (n + 1 < end) && (str.charAt(n + 1) == '\'')) {
					++n;
				}
			}
			str = r.toString();
		}
		return str;
	}

	public static String punycodeDomain(String domain) {
		try {
			return IDN.toASCII(domain);
		} catch (Exception e) {
			return domain;
		}
	}

	public static String punycodeEMail(String email) {
		int at = email.lastIndexOf('@');

		if (at != -1) {
			return email.substring(0, at + 1) + punycodeDomain(email.substring(at + 1));
		}
		return email;
	}

	/**
	 * convert old style <agn ...> and </agn ...> to new style [agn ...] and
	 * [/agn ...]
	 *
	 * @param in the source
	 * @return the converted string
	 */
	public static String convertOld2New(String in) {
		int ilen = in.length();
		StringBuffer out = new StringBuffer(ilen);
		int n, cur, pos;

		cur = 0;
		while ((cur < ilen) && ((n = in.indexOf('<', cur)) != -1) && (n + 5 < ilen)) {
			out.append(in, cur, n);
			pos = n++;
			if (in.substring(n, n + 3).equals("agn") || in.substring(n, n + 4).equals("/agn")) {
				char quote = '\0';
				char ch;

				out.append('[');
				while (n < ilen) {
					ch = in.charAt(n);
					if (quote == '\0') {
						if (ch == '>') {
							break;
						}
						if ((ch == '"') || (ch == '\'')) {
							quote = ch;
						}
					} else if (quote == ch) {
						quote = '\0';
					}
					out.append(ch);
					++n;
				}
				if (n < ilen) {
					out.append(']');
				}
				cur = n + 1;
			} else {
				out.append(in.charAt(pos));
				cur = pos + 1;
			}
		}
		if (cur < ilen) {
			out.append(in.substring(cur));
		}
		return out.toString();
	}

	/**
	 * Removed HTML entities and tags from the input
	 *
	 * @param inputString input string
	 * @return the HTML-cleared string
	 */
	private static Pattern FINDHTML = Pattern.compile("<[ \t]*[!/?%a-z][^>]*>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

	public static String removeHTMLTagsAndEntities(String inputString) {
		if (StringUtils.isEmpty(inputString)) {
			return inputString;
		} else {
			Matcher regexMatcher = FINDHTML.matcher(inputString);
			boolean hasNextMatch = regexMatcher.find();
			if (!hasNextMatch) {
				return StringEscapeUtils.unescapeHtml4(inputString);
			} else {
				StringBuffer returnValue = new StringBuffer(inputString.length());

				int endOfPreviousMatchingString = 0;

				while (hasNextMatch) {
					int matchingStringStart = regexMatcher.start();

					if (endOfPreviousMatchingString < matchingStringStart) {
						returnValue.append(inputString, endOfPreviousMatchingString, matchingStringStart);
					}
					endOfPreviousMatchingString = regexMatcher.end();
					hasNextMatch = regexMatcher.find();
				}

				if (endOfPreviousMatchingString < inputString.length()) {
					returnValue.append(inputString.substring(endOfPreviousMatchingString));
				}

				return StringEscapeUtils.unescapeHtml4(returnValue.toString());
			}
		}
	}

	/**
	 * Find possible columns in a hash-tag that may be used for
	 * further processing
	 *
	 * @param s the string to examine
	 * @return a list of found columns
	 */
	public static List<String> findColumnsInHashtags(String s) {
		List<String> rc = new ArrayList<>();

		if (s == null) {
			return rc;
		}

		int slen = s.length();

		for (int pos = 0; pos < slen; ) {
			int start = s.indexOf("##", pos);

			if (start != -1) {
				int end = s.indexOf("##", start + 2);

				if (end != -1) {
					String hashtag = s.substring(start + 2, end);
					String[] elements = hashtag.split(":");

					if (elements.length > 0) {
						switch (elements[0]) {
							case "MAILING_ID":
							case "URL_ID":
							case "SENDDATE-UNENCODED":
							case "DATE":
							case "MAILING":
							case "MAILING-UNENCODED":
							case "AGNUID":
							case "PUBID":
								break;
							default:
								rc.add(elements[elements.length - 1].trim().toLowerCase());
								break;
						}
					}
					pos = end + 2;
				} else {
					pos = slen;
				}
			} else {
				pos = slen;
			}
		}
		return rc;
	}

	/**
	 * Converts a string to a boolean according to its content
	 *
	 * @param s    the string to parse
	 * @param dflt the default value
	 * @return the boolean value, if the string could be parsed, dflt otherwise
	 */
	public static boolean atob(String s, boolean dflt) {
		if (s != null) {
			if (s.equalsIgnoreCase("true") ||
			    s.equalsIgnoreCase("yes") ||
			    s.equalsIgnoreCase("on") ||
			    s.equalsIgnoreCase("enabled") ||
			    s.equals("+") ||
			    s.equals("1")) {
				return true;
			} else if (s.equalsIgnoreCase("false") ||
				   s.equalsIgnoreCase("no") ||
				   s.equalsIgnoreCase("off") ||
				   s.equalsIgnoreCase("disabled") ||
				   s.equals("-") ||
				   s.equals("0")) {
				return false;
			}
		}
		return dflt;
	}
	public static boolean atob (String s) {
		return atob (s, false);
	}
	public static String btoa (boolean b) {
		return b ? "true" : "false";
	}

	/**
	 * Converts a string to an Integer
	 *
	 * @param s    the string to parse
	 * @param dflt the default value
	 * @return the parsed value, if parsable, dflt otherwise
	 */
	public static int atoi(String s, int dflt) {
		int rc;

		try {
			rc = Integer.parseInt(s);
		} catch (Exception e) {
			rc = dflt;
		}
		return rc;
	}
	public static int atoi (String s) {
		return atoi (s, 0);
	}

	/**
	 * Converts a string to an Long
	 *
	 * @param s    the string to parse
	 * @param dflt the default value
	 * @return the parsed value, if parsable, dflt otherwise
	 */
	public static long atol(String s, long dflt) {
		long rc;

		try {
			rc = Long.parseLong(s);
		} catch (Exception e) {
			rc = dflt;
		}
		return rc;
	}

	/**
	 * Converts a string to an double (float)
	 *
	 * @param s    the string to parse
	 * @param dflt the default value
	 * @return the parsed value, if parsable, dflt otherwise
	 */
	public static double atof(String s, double dflt) {
		double rc;

		try {
			rc = Double.parseDouble(s);
		} catch (Exception e) {
			rc = dflt;
		}
		return rc;
	}

	/**
	 * simple string template processor where a missing referenced token
	 * will result in a RuntimeException. The operation system enviromen
	 * will be automatically be accessable
	 *
	 * @param s    the string to process
	 * @param parm a list of pairs variable/value to be used for replacement
	 * @return the processed string
	 * @throws RuntimeException
	 */
	private static Pattern FIND_PLACEHOLDER = Pattern.compile("\\$\\$|\\$(\\{[^}]+\\}|[a-z_][a-z0-9_]*)", Pattern.MULTILINE | Pattern.DOTALL);

	public static String fill(String s, Map<String, String> variables) {
		StringBuffer result = new StringBuffer();
		Matcher m = FIND_PLACEHOLDER.matcher(s);
		Map<String, String> env = System.getenv();
		int pos = 0;
		int start;
		String placeholder, replace;

		while (m.find(pos)) {
			start = m.start();
			if (start > pos) {
				result.append(s, pos, start);
			}
			placeholder = m.group(1);
			if (placeholder == null) {
				replace = "$";
			} else {
				if (placeholder.startsWith("{") && placeholder.endsWith("}")) {
					placeholder = placeholder.substring(1, placeholder.length() - 1);
				}
				replace = variables.get(placeholder);
				if (replace == null) {
					replace = env.get(placeholder);
				}
			}
			result.append(replace != null ? replace : m.group());
			pos = m.end();
		}
		if (pos < s.length()) {
			result.append(s.substring(pos));
		}
		return result.toString();
	}

	public static String fill(String s, Object... param) {
		Map<String, String> p = new HashMap<>();

		for (int n = 0; n + 1 < param.length; n += 2) {
			p.put(param[n].toString(), param[n + 1].toString());
		}
		return fill(s, p);
	}

	public static final String pathSeparator = System.getProperty("file.separator", "/");

	/**
	 * Create a valid path for this operating system where each element
	 * is treated as a template string. Beside the enviroment, the two
	 * token ${home} is replaced by the system property "user.home"
	 *
	 * @param elements the elements for the path
	 * @return the generated path
	 * @throws RuntimeException
	 */
	public static String makePath(Object... elements) {
		return Arrays.stream(elements)
				.map(element -> (element instanceof String) ?
						fill((String) element, "home", Data.home, "user", Data.user) :
						element.toString())
				.map(element -> expand (element))
				.reduce((s, e) -> s + (e.startsWith (pathSeparator) ? "" : pathSeparator) + e)
				.orElse(".");
	}
	public static String makePath (String path) {
		return makePath ((Object[]) path.split (pathSeparator));
	}
	private static synchronized String expand (String element) {
		if ("~".equals (element)) {
			return Data.home;
		}
		if ((element != null) && (element.startsWith ("~"))) {
			if (homeDirectories == null) {
				try {
					homeDirectories = new HashMap <> ();
					
					File	file = new File ("/etc/passwd");
					if (file.exists () && file.canRead ()) {
						try (FileInputStream fd = new FileInputStream (file)) {
							byte[]	content = new byte[(int) file.length ()];
							
							fd.read (content);
							for (String line : (new String (content, StandardCharsets.UTF_8)).split ("\n")) {
								String[]	pwd = line.split (":");

								if (pwd.length == 7) {
									homeDirectories.put (pwd[0], pwd[5]);
								}
							}
						} catch (IOException e) {
							// do nothing
						}
					}
				} catch (SecurityException e) {
					// do nothing
				}
			}
			return homeDirectories.getOrDefault (element.substring (1), element);
		}
		return element;
	}
}
