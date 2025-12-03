-- Migrate annotation discriminators from FQCNs to short tokens

UPDATE annotation_annotatable SET annotatable_type = 'Project' WHERE annotatable_type = 'com.rreganjr.requel.project.Project';
UPDATE annotation_annotatable SET annotatable_type = 'ProjectTeam' WHERE annotatable_type = 'com.rreganjr.requel.project.ProjectTeam';
UPDATE annotation_annotatable SET annotatable_type = 'Goal' WHERE annotatable_type = 'com.rreganjr.requel.project.Goal';
UPDATE annotation_annotatable SET annotatable_type = 'GoalRelation' WHERE annotatable_type = 'com.rreganjr.requel.project.GoalRelation';
UPDATE annotation_annotatable SET annotatable_type = 'UseCase' WHERE annotatable_type = 'com.rreganjr.requel.project.UseCase';
UPDATE annotation_annotatable SET annotatable_type = 'Scenario' WHERE annotatable_type = 'com.rreganjr.requel.project.Scenario';
UPDATE annotation_annotatable SET annotatable_type = 'Step' WHERE annotatable_type = 'com.rreganjr.requel.project.Step';
UPDATE annotation_annotatable SET annotatable_type = 'Story' WHERE annotatable_type = 'com.rreganjr.requel.project.Story';
UPDATE annotation_annotatable SET annotatable_type = 'Actor' WHERE annotatable_type = 'com.rreganjr.requel.project.Actor';
UPDATE annotation_annotatable SET annotatable_type = 'GlossaryTerm' WHERE annotatable_type = 'com.rreganjr.requel.project.GlossaryTerm';
UPDATE annotation_annotatable SET annotatable_type = 'NonUserStakeholder' WHERE annotatable_type = 'com.rreganjr.requel.project.NonUserStakeholder';
UPDATE annotation_annotatable SET annotatable_type = 'UserStakeholder' WHERE annotatable_type = 'com.rreganjr.requel.project.UserStakeholder';

UPDATE annotations SET grouping_object_type = 'Project' WHERE grouping_object_type = 'com.rreganjr.requel.project.Project';
