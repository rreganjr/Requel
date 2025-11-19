package com.rreganjr.requel.imports;

/**
 * Domain-agnostic contract for completing an aggregate from an import draft.
 *
 * @param <D> draft type (format-agnostic input)
 * @param <A> aggregate type to assemble
 */
public interface AggregateAssembler<D, A> {

    Class<D> draftType();

    Class<A> aggregateType();

    /**
     * Assemble the target aggregate from the provided draft, using the supplied unit of work
     * for cross-aggregate reference resolution or caching.
     */
    A assemble(D draft, ImportUnitOfWork unitOfWork) throws ImportException;
}
