package com.rreganjr.requel.service.api.dto;

/**
 * A relationship between two goals (Supports or Conflicts).
 * Used in the goal detail view — relationsFromThisGoal shows outgoing,
 * relationsToThisGoal shows incoming.
 *
 * @param id            relation id
 * @param version       optimistic lock version
 * @param goalId        the other goal's id (toGoal for outgoing, fromGoal for incoming)
 * @param goalName      the other goal's display name
 * @param relationType  "Supports" or "Conflicts"
 */
public record GoalRelationDto(
        Long id,
        int version,
        Long goalId,
        String goalName,
        String relationType
) {
}
