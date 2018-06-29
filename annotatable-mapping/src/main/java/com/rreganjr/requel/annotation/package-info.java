/**
 * Define the mapping for AbstractAnnotation annotatables in a way so that the annotation package doesn't depend
 * on project objects
 */
@AnyMetaDef(name = "annotatables", idType = "long", metaType = "string", metaValues = {
    @MetaValue(value = "com.rreganjr.requel.annotation.Annotation", targetEntity = AbstractAnnotation.class),
    @MetaValue(value = "com.rreganjr.requel.project.Project", targetEntity = ProjectImpl.class ),
    @MetaValue(value = "com.rreganjr.requel.project.ProjectTeam", targetEntity = ProjectTeamImpl.class),
    @MetaValue(value = "com.rreganjr.requel.project.Goal", targetEntity = GoalImpl.class),
    @MetaValue(value = "com.rreganjr.requel.project.GoalRelation", targetEntity = GoalRelationImpl.class),
    @MetaValue(value = "com.rreganjr.requel.project.UseCase", targetEntity = UseCaseImpl.class),
    @MetaValue(value = "com.rreganjr.requel.project.Scenario", targetEntity = ScenarioImpl.class),
    @MetaValue(value = "com.rreganjr.requel.project.Step", targetEntity = StepImpl.class),
    @MetaValue(value = "com.rreganjr.requel.project.Story", targetEntity = StoryImpl.class),
    @MetaValue(value = "com.rreganjr.requel.project.Actor", targetEntity = ActorImpl.class),
    @MetaValue(value = "com.rreganjr.requel.project.GlossaryTerm", targetEntity = GlossaryTermImpl.class),
    @MetaValue(value = "com.rreganjr.requel.project.NonUserStakeholder", targetEntity = NonUserStakeholderImpl.class),
    @MetaValue(value = "com.rreganjr.requel.project.UserStakeholder", targetEntity = UserStakeholderImpl.class)
})
package com.rreganjr.requel.annotation;

    import com.rreganjr.requel.annotation.impl.AbstractAnnotation;
    import com.rreganjr.requel.project.impl.*;
    import org.hibernate.annotations.AnyMetaDef;
    import org.hibernate.annotations.MetaValue;