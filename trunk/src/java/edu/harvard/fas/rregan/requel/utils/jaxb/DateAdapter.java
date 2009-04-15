/*
 * $Id: DateAdapter.java,v 1.5 2008/06/20 08:23:07 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.utils.jaxb;

import java.util.Date;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import edu.harvard.fas.rregan.requel.utils.DateUtils;

/**
 * @author ron
 */
public class DateAdapter extends XmlAdapter<String, Date> {

	@Override
	public Date unmarshal(String dateString) throws Exception {
		return DateUtils.parseDateOrDefault(dateString, new Date());
	}

	@Override
	public String marshal(Date date) throws Exception {
		if (date != null) {
			return DateUtils.standardDateAndTime.format(date);
		}
		return "";
	}
}
