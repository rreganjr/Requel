/*
 * $Id: Linkdef.java,v 1.3 2009/02/07 13:36:49 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.nlp.dictionary;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

/**
 * @author ron
 */
@Entity
@Table(name = "linkdef")
public class Linkdef implements Serializable {
	static final long serialVersionUID = 0;

	/**
	 * The id of hypernym link definition type.
	 */
	public static final int HYPERNYM = 1;

	/**
	 * The id of hyponym link definition type.
	 */
	public static final int HYPONYM = 2;

	/**
	 * The id of hypernym link definition type.
	 */
	public static final int HYPERNYM_INSTANCE = 3;

	/**
	 * The id of hyponym link definition type.
	 */
	public static final int HYPONYM_INSTANCE = 4;

	private Long linkid;
	private String name;
	private boolean recurses;

	protected Linkdef() {
	}

	protected Linkdef(Long linkid, boolean recurses) {
		this.linkid = linkid;
		this.recurses = recurses;
	}

	protected Linkdef(Long linkid, String name, boolean recurses) {
		this.linkid = linkid;
		this.name = name;
		this.recurses = recurses;
	}

	@Id
	@Column(name = "linkid", unique = true, nullable = false)
	public Long getId() {
		return this.linkid;
	}

	public void setId(Long linkid) {
		this.linkid = linkid;
	}

	@Column(name = "name")
	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Column(name = "recurses", nullable = false)
	@Type(type = "yes_no")
	public boolean getRecurses() {
		return this.recurses;
	}

	public void setRecurses(boolean recurses) {
		this.recurses = recurses;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Linkdef other = (Linkdef) obj;
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		return true;
	}
}
