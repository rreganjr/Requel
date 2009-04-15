package edu.harvard.fas.rregan.requel.project;

import java.util.Set;

public interface ProjectTeam extends ProjectOrDomainEntity, Comparable<ProjectTeam> {
	public Set<Stakeholder> getMembers();
}
