package edu.harvard.fas.rregan.requel.project.impl.assistant;

import edu.harvard.fas.rregan.requel.project.ProjectOrDomain;
import edu.harvard.fas.rregan.requel.project.ProjectOrDomainEntity;

public class TestUpdatedEntityNotifier implements UpdatedEntityNotifier {
	@Override
	public void entityUpdated(ProjectOrDomain pod) {
	}

	@Override
	public void entityUpdated(ProjectOrDomainEntity entity) {
	}
}