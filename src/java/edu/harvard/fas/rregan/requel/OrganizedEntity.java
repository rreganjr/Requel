package edu.harvard.fas.rregan.requel;

import edu.harvard.fas.rregan.requel.user.Organization;

/**
 * An entity that has an organization assigned.
 * @author ron
 */
public interface OrganizedEntity {

	/**
	 * @return the default organzation of this user.
	 */
	public Organization getOrganization();

	/**
	 * set the default organization of this user.
	 * 
	 * @param organization
	 */
	public void setOrganization(Organization organization);
}
