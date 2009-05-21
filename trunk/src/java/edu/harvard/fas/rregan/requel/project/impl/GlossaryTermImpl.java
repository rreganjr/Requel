/*
 * $Id$
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
package edu.harvard.fas.rregan.requel.project.impl;

import java.util.Set;
import java.util.TreeSet;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.hibernate.annotations.AnyMetaDef;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.annotations.MetaValue;
import org.hibernate.annotations.Sort;
import org.hibernate.annotations.SortType;
import org.hibernate.validator.NotEmpty;

import edu.harvard.fas.rregan.requel.project.GlossaryTerm;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomain;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomainEntity;
import edu.harvard.fas.rregan.requel.user.User;
import edu.harvard.fas.rregan.requel.utils.jaxb.UnmarshallerListener;

/**
 * Implementation of a Glossary Term.
 * 
 * @author ron
 */
@Entity
@Table(name = "terms", uniqueConstraints = { @UniqueConstraint(columnNames = {
		"projectordomain_id", "name" }) })
@XmlRootElement(name = "term", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
@XmlType(name = "term", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
public class GlossaryTermImpl extends AbstractTextEntity implements GlossaryTerm,
		Comparable<GlossaryTerm> {
	static final long serialVersionUID = 0L;

	private GlossaryTerm canonicalTerm;
	private Set<GlossaryTerm> alternateTerms = new TreeSet<GlossaryTerm>();
	private Set<ProjectOrDomainEntity> referers = new TreeSet<ProjectOrDomainEntity>(
			new ProjectOrDomainEntityComparator());

	/**
	 * @param projectOrDomain
	 * @param name
	 * @param createdBy
	 */
	public GlossaryTermImpl(ProjectOrDomain projectOrDomain, String name, User createdBy) {
		setProjectOrDomain(projectOrDomain);
		setName(name);
		setCreatedBy(createdBy);
	}

	protected GlossaryTermImpl() {
		// for hibernate
	}

	@Override
	@Column(nullable = false, unique = false)
	@NotEmpty(message = "a unique name is required.")
	@XmlElement(name = "name", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
	public String getName() {
		return super.getName();
	}

	// hack for JAXB to set the name, for some reason it won't use the inherited
	// method.
	@Override
	public void setName(String name) {
		super.setName(name);
	}

	@Transient
	@XmlID
	@XmlAttribute(name = "id")
	public String getXmlId() {
		return "TRM_" + getId();
	}

	@Transient
	@XmlTransient
	@Override
	public String getDescription() {
		return "Term: " + getName();
	}

	@XmlIDREF
	@XmlAttribute(name = "canonicalTerm")
	@XmlJavaTypeAdapter(GlossaryTerm2GlossaryTermImplAdapter.class)
	@ManyToOne(targetEntity = GlossaryTermImpl.class, cascade = { CascadeType.REFRESH }, fetch = FetchType.EAGER, optional = true)
	@Override
	public GlossaryTerm getCanonicalTerm() {
		return canonicalTerm;
	}

	/**
	 * @param canonicalTerm
	 */
	public void setCanonicalTerm(GlossaryTerm canonicalTerm) {
		this.canonicalTerm = canonicalTerm;
	}

	@XmlTransient
	@Override
	@OneToMany(targetEntity = GlossaryTermImpl.class, cascade = { CascadeType.PERSIST,
			CascadeType.MERGE, CascadeType.REFRESH }, fetch = FetchType.LAZY, mappedBy = "canonicalTerm")
	@Sort(type = SortType.NATURAL)
	public Set<GlossaryTerm> getAlternateTerms() {
		return alternateTerms;
	}

	protected void setAlternateTerms(Set<GlossaryTerm> alternateTerms) {
		this.alternateTerms = alternateTerms;
	}

	@XmlTransient
	@ManyToAny(fetch = FetchType.LAZY, metaColumn = @Column(name = "referer_type", length = 255, nullable = false))
	@AnyMetaDef(idType = "long", metaType = "string", metaValues = {
			@MetaValue(value = "edu.harvard.fas.rregan.requel.project.Project", targetEntity = ProjectImpl.class),
			@MetaValue(value = "edu.harvard.fas.rregan.requel.project.Actor", targetEntity = ActorImpl.class),
			@MetaValue(value = "edu.harvard.fas.rregan.requel.project.Stakeholder", targetEntity = StakeholderImpl.class),
			@MetaValue(value = "edu.harvard.fas.rregan.requel.project.Goal", targetEntity = GoalImpl.class),
			@MetaValue(value = "edu.harvard.fas.rregan.requel.project.Scenario", targetEntity = ScenarioImpl.class),
			@MetaValue(value = "edu.harvard.fas.rregan.requel.project.Step", targetEntity = StepImpl.class),
			@MetaValue(value = "edu.harvard.fas.rregan.requel.project.UseCase", targetEntity = UseCaseImpl.class),
			@MetaValue(value = "edu.harvard.fas.rregan.requel.project.Story", targetEntity = StoryImpl.class) })
	@JoinTable(name = "terms_referers", joinColumns = { @JoinColumn(name = "term_id") }, inverseJoinColumns = {
			@JoinColumn(name = "referer_type"), @JoinColumn(name = "referer_id") })
	@Sort(type = SortType.COMPARATOR, comparator = ProjectOrDomainEntityComparator.class)
	public Set<ProjectOrDomainEntity> getReferers() {
		return referers;
	}

	protected void setReferers(Set<ProjectOrDomainEntity> referersToThisTerm) {
		this.referers = referersToThisTerm;
	}

	/**
	 * This is for JAXB to fixup the parent child relationship with the
	 * canonical term.
	 * 
	 * @see UnmarshallerListener
	 */
	@Override
	public void afterUnmarshal() {
		if (getCanonicalTerm() != null) {
			getCanonicalTerm().getAlternateTerms().add(this);
		}
	}

	@Override
	public int compareTo(GlossaryTerm o) {
		int projectCompare = (getProjectOrDomain().getName().compareTo(o.getProjectOrDomain()
				.getName()));
		int nameCompare = (getName().compareTo(o.getName()));
		return (projectCompare != 0 ? projectCompare : nameCompare);
	}
}
