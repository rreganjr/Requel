/*
 * $Id: VerbNetSelectionRestrictionType.java,v 1.2 2009/02/09 10:12:30 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.nlp.dictionary;

import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlTransient;

/**
 * A selection restriction on a VerbNet semantic role from the frame syntax
 * element. Each restriction is assigned a synset and link type indicating the
 * type of entities that fit the restriction.<br>
 * For example:<br>
 * The "location" restriction maps to the "location#n#1" synset with a link type
 * of "hyponym" meaning the 2000 or so hyponyms of this synset comply with the
 * restriction.<br>
 * The "refl" restriction (self reference) maps to the "reflexive" synset with a
 * link type of "hyponym instance" meaning a word that is a reflexive pronoun is
 * appropriate for the restriction, such as himself, herself, or itself.
 * 
 * @author ron
 */
@Entity
@Table(name = "vnselres")
public class VerbNetSelectionRestrictionType implements
		Comparable<VerbNetSelectionRestrictionType>, Serializable {
	static final long serialVersionUID = 0;

	private Long id;
	private String name;
	private Synset synset;
	private Linkdef linkType;

	protected VerbNetSelectionRestrictionType() {
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "vnselresid", unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	protected void setId(Long id) {
		this.id = id;
	}

	@Column()
	public String getName() {
		return this.name;
	}

	protected void setName(String name) {
		this.name = name;
	}

	@ManyToOne(targetEntity = Synset.class, cascade = CascadeType.ALL, optional = false)
	@JoinColumn(name = "synsetid", insertable = false, updatable = false)
	public Synset getSynset() {
		return synset;
	}

	protected void setSynset(Synset synset) {
		this.synset = synset;
	}

	@ManyToOne(targetEntity = Linkdef.class, cascade = CascadeType.ALL, optional = true)
	@JoinColumn(name = "linkid", insertable = false, updatable = false)
	@XmlTransient
	public Linkdef getLinkType() {
		return linkType;
	}

	protected void setLinkType(Linkdef linkType) {
		this.linkType = linkType;
	}

	@Override
	public int compareTo(VerbNetSelectionRestrictionType o) {
		return getName().compareToIgnoreCase(o.getName());
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
			result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
			tmpHashCode = new Integer(result);
		}
		return tmpHashCode.intValue();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!getClass().isAssignableFrom(obj.getClass())) {
			return false;
		}
		final VerbNetFrame other = (VerbNetFrame) obj;
		if ((getId() != null) && getId().equals(other.getId())) {
			return true;
		}
		if (getName() == null) {
			if (other.getName() != null) {
				return false;
			}
		} else if (!getName().equals(other.getName())) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " " + getName();
	}
}
