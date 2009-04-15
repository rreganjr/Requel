/*
 * $Id: VerbNetRoleRef.java,v 1.3 2009/02/11 00:47:45 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.nlp.dictionary;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * A reference from a VerbNet class to a VerbNet role including selectional
 * restrictions for the role in that class.
 * 
 * @author ron
 */
@Entity
@Table(name = "vnroleref")
public class VerbNetRoleRef implements Serializable {
	static final long serialVersionUID = 0;

	private Long id;
	private VerbNetClass vnClass;
	private VerbNetRole vnRole;
	private String restrictionXML;
	private Set<VerbNetSelectionRestriction> restrictions = new HashSet<VerbNetSelectionRestriction>();

	protected VerbNetRoleRef() {
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "rolerefid", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	protected void setId(Long id) {
		this.id = id;
	}

	@ManyToOne(targetEntity = VerbNetClass.class, cascade = CascadeType.ALL, optional = false)
	@JoinColumn(name = "classid", nullable = false, unique = false)
	public VerbNetClass getVerbNetClass() {
		return vnClass;
	}

	protected void setVerbNetClass(VerbNetClass vnClass) {
		this.vnClass = vnClass;
	}

	@ManyToOne(targetEntity = VerbNetRole.class, cascade = CascadeType.ALL, optional = false)
	@JoinColumn(name = "roletypeid", nullable = false, unique = false)
	public VerbNetRole getVerbNetRole() {
		return vnRole;
	}

	protected void setVerbNetRole(VerbNetRole role) {
		this.vnRole = role;
	}

	@Column(name = "selrestrs", nullable = false, unique = false)
	public String getRestrictionXML() {
		return restrictionXML;
	}

	public void setRestrictionXML(String restrictionXML) {
		this.restrictionXML = restrictionXML;
	}

	@OneToMany(targetEntity = VerbNetSelectionRestriction.class, cascade = { CascadeType.ALL }, fetch = FetchType.EAGER, mappedBy = "parent")
	public Set<VerbNetSelectionRestriction> getSelectionalRestrictions() {
		return restrictions;
	}

	protected void setSelectionalRestrictions(Set<VerbNetSelectionRestriction> restrictions) {
		this.restrictions = restrictions;
	}

	private Integer tmpHashCode = null;

	@Override
	public int hashCode() {
		if (tmpHashCode == null) {
			if (getId() != null) {
				tmpHashCode = new Integer(getId().hashCode());
			}
			final int prime = 31;
			int result = 1;
			result = prime * result + ((getRestrictionXML() == null) ? 0 : getRestrictionXML().hashCode());
			result = prime * result + ((getVerbNetClass() == null) ? 0 : getVerbNetClass().hashCode());
			result = prime * result + ((getVerbNetRole() == null) ? 0 : getVerbNetRole().hashCode());
			tmpHashCode = new Integer(result);
		}
		return tmpHashCode.intValue();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final VerbNetRoleRef other = (VerbNetRoleRef) obj;
		if (getRestrictionXML() == null) {
			if (other.getRestrictionXML() != null)
				return false;
		} else if (!getRestrictionXML().equals(other.getRestrictionXML()))
			return false;
		if (getVerbNetClass() == null) {
			if (other.getVerbNetClass() != null)
				return false;
		} else if (!getVerbNetClass().equals(other.getVerbNetClass()))
			return false;
		if (getVerbNetRole() == null) {
			if (other.getVerbNetRole() != null)
				return false;
		} else if (!getVerbNetRole().equals(other.getVerbNetRole()))
			return false;
		return true;
	}

}
