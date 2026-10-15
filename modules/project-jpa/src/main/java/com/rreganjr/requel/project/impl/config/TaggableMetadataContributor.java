/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2026 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.requel.project.impl.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.boot.ResourceStreamLocator;
import org.hibernate.boot.spi.AdditionalMappingContributions;
import org.hibernate.boot.spi.AdditionalMappingContributor;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.SecondPass;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Value;

import com.rreganjr.requel.tagging.Taggable;

/**
 * Applies the discriminator mappings for the {@code @ManyToAny} tag-assignment
 * association declared in {@code com.rreganjr.requel.tagging.impl.TagImpl} using the
 * types registered in {@link TaggableRegistryConfiguration}.
 *
 * <p>This is the tagging counterpart of {@link ProjectAnnotatableMetadataContributor}:
 * the discriminator&rarr;class map is injected at Hibernate bootstrap so that neither
 * {@code tagging-jpa} nor {@code tagging-domain} depends on {@code project-jpa} impl
 * classes. It lives in {@code project-jpa} (rather than {@code tagging-jpa}) alongside
 * the registry configuration that names the project impls, exactly mirroring the
 * annotation seam.</p>
 */
public class TaggableMetadataContributor implements AdditionalMappingContributor {

	private static final Map<String, Class<? extends Taggable>> REGISTERED_TYPES = new ConcurrentHashMap<>();

	static void registerTaggableTypes(Map<String, Class<? extends Taggable>> mappings) {
		REGISTERED_TYPES.clear();
		REGISTERED_TYPES.putAll(mappings);
	}

	@Override
	public void contribute(
			AdditionalMappingContributions contributions,
			InFlightMetadataCollector metadataCollector,
			ResourceStreamLocator resourceStreamLocator,
			MetadataBuildingContext buildingContext) {
		if (REGISTERED_TYPES.isEmpty()) {
			return;
		}

		final Map<Object, String> taggableMeta = toMetaValueMap(REGISTERED_TYPES);

		metadataCollector.addSecondPass(new SecondPass() {
			@Override
			public void doSecondPass(Map<String, PersistentClass> persistentClasses) {
				for (PersistentClass persistentClass : persistentClasses.values()) {
					if (!isTaggingClass(persistentClass)) {
						continue;
					}
					for (Property property : persistentClass.getPropertyClosure()) {
						applyMetaValues(property.getValue(), taggableMeta);
					}
				}
			}
		});
	}

	private static boolean isTaggingClass(PersistentClass persistentClass) {
		final String className = persistentClass.getClassName();
		return (className != null) && className.startsWith("com.rreganjr.requel.tagging");
	}

	private static Map<Object, String> toMetaValueMap(Map<String, ? extends Class<?>> mappings) {
		final Map<Object, String> copy = new LinkedHashMap<>(mappings.size());
		mappings.forEach((discriminator, entityType) -> copy.put(discriminator, entityType.getName()));
		return Collections.unmodifiableMap(copy);
	}

	private static void applyMetaValues(Value value, Map<Object, String> metaValueMap) {
		if (value instanceof Any) {
			applyMetaValues((Any) value, metaValueMap);
		}
		else if (value instanceof Collection) {
			applyMetaValues(((Collection) value).getElement(), metaValueMap);
		}
		else if (value instanceof Component) {
			for (Property property : ((Component) value).getProperties()) {
				applyMetaValues(property.getValue(), metaValueMap);
			}
		}
	}

	private static void applyMetaValues(Any any, Map<Object, String> metaValueMap) {
		final Map<Object, String> merged = new LinkedHashMap<>(metaValueMap);
		final Map<Object, String> existing = any.getMetaValues();
		if (existing != null) {
			merged.putAll(existing);
		}
		any.setMetaValues(merged);
	}
}
