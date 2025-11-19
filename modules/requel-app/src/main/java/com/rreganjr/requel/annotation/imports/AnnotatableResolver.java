package com.rreganjr.requel.annotation.imports;

import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.spi.AnnotatableTypeRegistry;
import com.rreganjr.requel.imports.ImportUnitOfWork;
import java.util.Optional;

/**
 * Resolves annotatable references using the discriminators registered in the type registry.
 */
public class AnnotatableResolver {

    private final AnnotatableTypeRegistry registry;

    public AnnotatableResolver(AnnotatableTypeRegistry registry) {
        this.registry = registry;
    }

    public Optional<Annotatable> resolve(String discriminator, String externalId, ImportUnitOfWork uow) {
        if (discriminator == null || externalId == null) {
            return Optional.empty();
        }
        return registry.resolveEntityType(discriminator)
                .flatMap(type -> uow.resolve(type, externalId))
                .map(type -> (Annotatable) type);
    }
}
