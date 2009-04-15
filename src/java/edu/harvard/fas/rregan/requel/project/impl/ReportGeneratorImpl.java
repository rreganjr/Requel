package edu.harvard.fas.rregan.requel.project.impl;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.hibernate.validator.NotEmpty;

import edu.harvard.fas.rregan.requel.project.ProjectOrDomain;
import edu.harvard.fas.rregan.requel.project.ReportGenerator;
import edu.harvard.fas.rregan.requel.user.User;

/**
 * Generate a report for the project by transforming the project xml via an
 * xslt.
 * 
 * @author ron
 */
@Entity
@Table(name = "reports", uniqueConstraints = { @UniqueConstraint(columnNames = {
		"projectordomain_id", "name" }) })
@XmlRootElement(name = "report", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
@XmlType(name = "report", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
public class ReportGeneratorImpl extends AbstractTextEntity implements ReportGenerator {
	static final long serialVersionUID = 0L;

	/**
	 * @param projectOrDomain
	 * @param createdBy
	 * @param name
	 * @param text
	 */
	public ReportGeneratorImpl(ProjectOrDomain projectOrDomain, User createdBy, String name,
			String text) {
		super(projectOrDomain, createdBy, name, text);
	}

	protected ReportGeneratorImpl() {
		super();
		// for reflection (hibernate, jaxb, etc.)
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
		return "RPT_" + getId();
	}

	@Override
	@Transient
	public String getDescription() {
		return "Report Generator: " + getName();
	}

	@Override
	public int compareTo(ReportGenerator o) {
		return getName().compareToIgnoreCase(o.getName());
	}
}