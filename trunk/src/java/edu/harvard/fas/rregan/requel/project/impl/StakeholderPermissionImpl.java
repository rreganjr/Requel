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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.xml.sax.SAXException;

import com.sun.istack.SAXException2;
import com.sun.xml.bind.v2.runtime.unmarshaller.Patcher;
import com.sun.xml.bind.v2.runtime.unmarshaller.UnmarshallingContext;

import edu.harvard.fas.rregan.requel.project.ProjectRepository;
import edu.harvard.fas.rregan.requel.project.StakeholderPermission;
import edu.harvard.fas.rregan.requel.project.StakeholderPermissionType;
import edu.harvard.fas.rregan.requel.utils.jaxb.UnmarshallerListener;

/**
 * @author ron
 */
@Entity
@Table(name = "stakeholder_permissions", uniqueConstraints = { @UniqueConstraint(columnNames = {
		"entity_type", "permission_type" }) })
@XmlRootElement(name = "projectPermission", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
@XmlType(name = "projectPermission", namespace = "http://www.people.fas.harvard.edu/~rregan/requel")
public class StakeholderPermissionImpl implements StakeholderPermission {

	private Long id;
	private Class<?> entityType;
	private StakeholderPermissionType permissionType;

	/**
	 * @param entityType
	 * @param permissionType
	 */
	public StakeholderPermissionImpl(Class<?> entityType, StakeholderPermissionType permissionType) {
		super();
		setEntityType(entityType);
		setPermissionType(permissionType);
	}

	protected StakeholderPermissionImpl() {
		// for hibernate
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	protected Long getId() {
		return id;
	}

	protected void setId(Long id) {
		this.id = id;
	}

	@Transient
	public String getPermissionKey() {
		return generatePermissionKey(getEntityType(), getPermissionType());
	}

	@Column(name = "entity_type", nullable = false, updatable = false)
	@XmlAttribute(name = "entityType")
	@XmlJavaTypeAdapter(StakeholderEntityTypeAdapter.class)
	public Class<?> getEntityType() {
		return entityType;
	}

	protected void setEntityType(Class<?> entityType) {
		this.entityType = entityType;
	}

	@Enumerated(EnumType.STRING)
	@Column(name = "permission_type", nullable = false, updatable = false)
	@XmlAttribute(name = "permissionType")
	@XmlJavaTypeAdapter(StakeholderPermissionTypeAdapter.class)
	public StakeholderPermissionType getPermissionType() {
		return permissionType;
	}

	protected void setPermissionType(StakeholderPermissionType permissionType) {
		this.permissionType = permissionType;
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
			result = prime * result + ((getEntityType() == null) ? 0 : getEntityType().hashCode());
			result = prime * result
					+ ((getPermissionType() == null) ? 0 : getPermissionType().hashCode());
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
		final StakeholderPermissionImpl other = (StakeholderPermissionImpl) obj;
		if ((getId() != null) && getId().equals(other.getId())) {
			return true;
		}
		if (getEntityType() == null) {
			if (other.getEntityType() != null) {
				return false;
			}
		} else if (!getEntityType().equals(other.getEntityType())) {
			return false;
		}
		if (getPermissionType() == null) {
			if (other.getPermissionType() != null) {
				return false;
			}
		} else if (!getPermissionType().equals(other.getPermissionType())) {
			return false;
		}
		return true;
	}

	@Override
	public int compareTo(StakeholderPermission o) {
		int entityTypeCompare = (getEntityType() == null ? -1 : getEntityType().getName()
				.compareTo(o.getEntityType().getName()));
		int permissionTypeCompare = (getPermissionType() == null ? -1 : getPermissionType()
				.compareTo(o.getPermissionType()));
		return (entityTypeCompare == 0 ? permissionTypeCompare : entityTypeCompare);
	}

	@Override
	public String toString() {
		return getPermissionKey();
	}

	public static final String generatePermissionKey(Class<?> entityType,
			StakeholderPermissionType permissionType) {
		return entityType.getName() + "[" + permissionType.toString() + "]";
	}

	/**
	 * This class is used by JAXB to convert the StakeholderPermissionType of a
	 * StakeholderPermission into a string for an attribute in the xml file and
	 * the reverse when unmartialing.
	 * 
	 * @author ron
	 */
	@XmlTransient
	public static class StakeholderPermissionTypeAdapter extends
			XmlAdapter<String, StakeholderPermissionType> {

		@Override
		public StakeholderPermissionType unmarshal(String typeString) throws Exception {
			return StakeholderPermissionType.valueOf(typeString);
		}

		@Override
		public String marshal(StakeholderPermissionType type) throws Exception {
			return type.toString();
		}
	}

	/**
	 * This class is used by JAXB to convert the StakeholderPermissionType of a
	 * StakeholderPermission into a string for an attribute in the xml file and
	 * the reverse when unmartialing.
	 * 
	 * @author ron
	 */
	@XmlTransient
	public static class StakeholderEntityTypeAdapter extends XmlAdapter<String, Class<?>> {

		@Override
		public Class<?> unmarshal(String className) throws Exception {
			return Class.forName(className);
		}

		@Override
		public String marshal(Class<?> type) throws Exception {
			return type.getName();
		}
	}

	/**
	 * This is for JAXB to patchup the parent/child relationship and to patchup
	 * existing persistent objects for the objects that are attached directly to
	 * this object.
	 * 
	 * @param projectRepository
	 * @param parent -
	 *            the stakeholder that should be granted the permission.
	 * @see UnmarshallerListener
	 */
	public void afterUnmarshal(final ProjectRepository projectRepository, final Object parent) {
		UnmarshallingContext.getInstance().addPatcher(new Patcher() {
			@Override
			public void run() throws SAXException {
				try {
					StakeholderPermission permission = StakeholderPermissionImpl.this;
					((StakeholderImpl) parent).getStakeholderPermissions().remove(permission);
					StakeholderPermission existingPermission = projectRepository
							.findStakeholderPermission(permission.getEntityType(), permission
									.getPermissionType());
					((StakeholderImpl) parent).getStakeholderPermissions().add(existingPermission);
				} catch (RuntimeException e) {
					throw e;
				} catch (Exception e) {
					throw new SAXException2(e);
				}
			}
		});
	}
}
