/*
 * $Id: JAXBOrganizedEntityPatcher.java,v 1.4 2009/01/07 09:50:38 rregan Exp $
 * Copyright (c) 2008 Ron Regan Jr. All Rights Reserved.
 */
package edu.harvard.fas.rregan.requel.utils.jaxb;

import org.xml.sax.SAXException;

import com.sun.istack.SAXException2;
import com.sun.xml.bind.v2.runtime.unmarshaller.Patcher;

import edu.harvard.fas.rregan.requel.OrganizedEntity;
import edu.harvard.fas.rregan.requel.user.Organization;
import edu.harvard.fas.rregan.requel.user.UserRepository;
import edu.harvard.fas.rregan.requel.user.exception.NoSuchOrganizationException;

/**
 * This is used to swap the organization objects on CreatedEntity objects
 * assigned by the JAXB Unmarshaller with existing users from the database.
 * 
 * @author ron
 */
public class JAXBOrganizedEntityPatcher implements Patcher {

	private final OrganizedEntity organizedEntity;
	private final UserRepository userRepository;

	/**
	 * @param userRepository
	 * @param organizedEntity
	 */
	public JAXBOrganizedEntityPatcher(UserRepository userRepository, OrganizedEntity organizedEntity) {
		this.userRepository = userRepository;
		this.organizedEntity = organizedEntity;
	}

	@Override
	public void run() throws SAXException {
		try {
			if (organizedEntity.getOrganization() != null) {
				try {
					Organization existingOrganization = userRepository
							.findOrganizationByName(organizedEntity.getOrganization().getName());
					organizedEntity.setOrganization(existingOrganization);
				} catch (NoSuchOrganizationException e) {
					// new organization
					organizedEntity.setOrganization(userRepository.persist(organizedEntity
							.getOrganization()));
				}
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new SAXException2(e);
		}
	}

}
