/*
 * $Id: DateUtils.java,v 1.5 2009/01/07 09:50:37 rregan Exp $
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirments
 * Elicitation System.
 *
 * Requel is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Requel is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Requel. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package edu.harvard.fas.rregan.requel.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import edu.harvard.fas.rregan.ApplicationException;

/**
 * helper functions for working with dates.
 * 
 * @author ron
 */
public class DateUtils {

	private DateUtils() {
	}

	public static final DateFormat standardDateAndTime = new SimpleDateFormat(
			"yyyy-MM-dd'T'HH:mm:ss");
	public static final DateFormat standardDate = new SimpleDateFormat("yyyy-MM-dd");

	/**
	 * Date parts
	 */
	public static final DateFormat yearAs_yy = new SimpleDateFormat("yy");
	public static final DateFormat yearAs_yyyy = new SimpleDateFormat("yyyy");
	public static final DateFormat monthAs_M = new SimpleDateFormat("M");
	public static final DateFormat monthAs_MM = new SimpleDateFormat("MM");
	public static final DateFormat monthAs_MMM = new SimpleDateFormat("MMM");
	public static final DateFormat dayAs_d = new SimpleDateFormat("d");
	public static final DateFormat dayAs_dd = new SimpleDateFormat("dd");
	public static final DateFormat hoursAs_H = new SimpleDateFormat("H");
	public static final DateFormat hoursAs_HH = new SimpleDateFormat("HH");
	public static final DateFormat hoursAs_h = new SimpleDateFormat("h");
	public static final DateFormat hoursAs_hh = new SimpleDateFormat("hh");
	public static final DateFormat minutesAs_m = new SimpleDateFormat("m");
	public static final DateFormat minutesAs_mm = new SimpleDateFormat("mm");

	/**
	 * Returns a Map<String,String> of the date parts of the supplied date.<br>
	 * NOTE: only yy, yyyy, M, MM, d, dd, H, HH, h, hh, m and mm are implemented<br>
	 * <blockquote> <table border=0 cellspacing=3 cellpadding=0 summary="Chart
	 * shows pattern letters, date/time component, presentation, and examples.">
	 * <tr bgcolor="#ccccff">
	 * <th align=left>Letter
	 * <th align=left>Date or Time Component
	 * <th align=left>Presentation
	 * <th align=left>Examples
	 * <tr>
	 * <td><code>G</code>
	 * <td>Era designator
	 * <td><a href="#text">Text</a>
	 * <td><code>AD</code>
	 * <tr bgcolor="#eeeeff">
	 * <td><code>y</code>
	 * <td>Year
	 * <td><a href="#year">Year</a>
	 * <td><code>1996</code>; <code>96</code>
	 * <tr>
	 * <td><code>M</code>
	 * <td>Month in year
	 * <td><a href="#month">Month</a>
	 * <td><code>July</code>; <code>Jul</code>; <code>07</code>
	 * <tr bgcolor="#eeeeff">
	 * <td><code>w</code>
	 * <td>Week in year
	 * <td><a href="#number">Number</a>
	 * <td><code>27</code>
	 * <tr>
	 * <td><code>W</code>
	 * <td>Week in month
	 * <td><a href="#number">Number</a>
	 * <td><code>2</code>
	 * <tr bgcolor="#eeeeff">
	 * <td><code>D</code>
	 * <td>Day in year
	 * <td><a href="#number">Number</a>
	 * <td><code>189</code>
	 * <tr>
	 * <td><code>d</code>
	 * <td>Day in month
	 * <td><a href="#number">Number</a>
	 * <td><code>10</code>
	 * <tr bgcolor="#eeeeff">
	 * <td><code>F</code>
	 * <td>Day of week in month
	 * <td><a href="#number">Number</a>
	 * <td><code>2</code>
	 * <tr>
	 * <td><code>E</code>
	 * <td>Day in week
	 * <td><a href="#text">Text</a>
	 * <td><code>Tuesday</code>; <code>Tue</code>
	 * <tr bgcolor="#eeeeff">
	 * <td><code>a</code>
	 * <td>Am/pm marker
	 * <td><a href="#text">Text</a>
	 * <td><code>PM</code>
	 * <tr>
	 * <td><code>H</code>
	 * <td>Hour in day (0-23)
	 * <td><a href="#number">Number</a>
	 * <td><code>0</code>
	 * <tr bgcolor="#eeeeff">
	 * <td><code>k</code>
	 * <td>Hour in day (1-24)
	 * <td><a href="#number">Number</a>
	 * <td><code>24</code>
	 * <tr>
	 * <td><code>K</code>
	 * <td>Hour in am/pm (0-11)
	 * <td><a href="#number">Number</a>
	 * <td><code>0</code>
	 * <tr bgcolor="#eeeeff">
	 * <td><code>h</code>
	 * <td>Hour in am/pm (1-12)
	 * <td><a href="#number">Number</a>
	 * <td><code>12</code>
	 * <tr>
	 * <td><code>m</code>
	 * <td>Minute in hour
	 * <td><a href="#number">Number</a>
	 * <td><code>30</code>
	 * <tr bgcolor="#eeeeff">
	 * <td><code>s</code>
	 * <td>Second in minute
	 * <td><a href="#number">Number</a>
	 * <td><code>55</code>
	 * <tr>
	 * <td><code>S</code>
	 * <td>Millisecond
	 * <td><a href="#number">Number</a>
	 * <td><code>978</code>
	 * <tr bgcolor="#eeeeff">
	 * <td><code>z</code>
	 * <td>Time zone
	 * <td><a href="#timezone">General time zone</a>
	 * <td><code>Pacific Standard Time</code>; <code>PST</code>;
	 * <code>GMT-08:00</code>
	 * <tr>
	 * <td><code>Z</code>
	 * <td>Time zone
	 * <td><a href="#rfc822timezone">RFC 822 time zone</a>
	 * <td><code>-0800</code> </table> </blockquote>
	 * 
	 * @param date
	 * @return
	 */
	public static Map<String, String> getDateParts(Date date) {
		Map<String, String> dateParts = new HashMap<String, String>();
		dateParts.put("yy", yearAs_yy.format(date));
		dateParts.put("yyyy", yearAs_yyyy.format(date));
		dateParts.put("M", monthAs_M.format(date));
		dateParts.put("MM", monthAs_MM.format(date));
		dateParts.put("d", dayAs_d.format(date));
		dateParts.put("dd", dayAs_dd.format(date));

		dateParts.put("H", hoursAs_H.format(date));
		dateParts.put("HH", hoursAs_HH.format(date));
		dateParts.put("h", hoursAs_h.format(date));
		dateParts.put("hh", hoursAs_hh.format(date));
		dateParts.put("m", minutesAs_m.format(date));
		dateParts.put("mm", minutesAs_mm.format(date));

		return dateParts;
	}

	/**
	 * Try to parse the supplied dateString as a date or datetime and if it
	 * can't be parsed throw an exception.
	 * 
	 * @param dateString
	 * @return
	 * @throws ApplicationException -
	 *             if the supplied dateString can't be parsed.
	 */
	public static Date parseDate(String dateString) throws ApplicationException {
		return parseDateOrDefault(dateString, null);
	}

	/**
	 * Try to parse the supplied dateString as a date or datetime and if it
	 * can't be parsed return the defaultValue date.
	 * 
	 * @param dateString
	 * @param defaultValue
	 * @return
	 */
	public static Date parseDateOrDefault(String dateString, Date defaultValue) {
		for (DateFormat dateFormat : new DateFormat[] { standardDate, standardDateAndTime }) {
			try {
				return dateFormat.parse(dateString);
			} catch (Exception e) {
			}
		}
		if (defaultValue != null) {
			return defaultValue;
		}
		throw ApplicationException.unsupportedDateString(dateString);
	}

	/**
	 * Format a date using java.text.SimpleDate format
	 * 
	 * @param date
	 *            date to format
	 * @param formatString
	 *            date and time pattern suitable for java.text.SimpleDateFormat
	 * @return
	 */
	public static String simpleDateFormat(Date date, String formatString) {
		DateFormat df = new SimpleDateFormat(formatString);
		String result = df.format(date);
		return result;
	}

	/**
	 * Returns the given date with milliseconds set to 000
	 * 
	 * @param date
	 * @return
	 */
	public static Date startOfSecond(Date date) {
		if (date != null) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			cal.set(Calendar.MILLISECOND, 0);
			return cal.getTime();
		} else {
			return null;
		}
	}

	/**
	 * Returns the given date with the time set to 00:00:00.000 (midnight)
	 * 
	 * @param date
	 * @return
	 */
	public static Date startOfDay(Date date) {
		if (date != null) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			return cal.getTime();
		} else {
			return null;
		}
	}

	/**
	 * Returns the given date with the time set to 11:59:59.999
	 * 
	 * @param date
	 * @return
	 */
	public static Date endOfDay(Date date) {
		if (date != null) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			cal.set(Calendar.HOUR_OF_DAY, 23);
			cal.set(Calendar.MINUTE, 59);
			cal.set(Calendar.SECOND, 59);
			cal.set(Calendar.MILLISECOND, 999);
			return cal.getTime();
		} else {
			return null;
		}
	}

	public static Date addDays(Date date, int days) {
		if (date != null) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			cal.add(Calendar.DAY_OF_YEAR, days);
			return cal.getTime();
		} else {
			return null;
		}
	}

	public static Date addHours(Date date, int hours) {
		if (date != null) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			cal.add(Calendar.HOUR_OF_DAY, hours);
			return cal.getTime();
		} else {
			return null;
		}
	}

	public static Date nextDay(Date date) {
		return addDays(date, 1);
	}

	public static Date nextWeek(Date date) {
		return addDays(date, 7);
	}

	public static Date nextMonth(Date date) {
		if (date != null) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			cal.add(Calendar.MONTH, 1);
			return cal.getTime();
		} else {
			return null;
		}
	}

	public static Date nextYear(Date date) {
		if (date != null) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			cal.add(Calendar.YEAR, 1);
			return cal.getTime();
		} else {
			return null;
		}
	}

	public static Date endOfTheWeek(Date date) {
		if (date != null) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			cal.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
			return cal.getTime();
		} else {
			return null;
		}
	}
}
