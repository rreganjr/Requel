import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot, convertToParamMap } from '@angular/router';
import { artifactNameResolver } from './artifact-name.resolver';
import { GoalService } from '../goal.service';
import { StakeholderService } from '../stakeholder.service';

function routeSnapshot(
  data: Record<string, unknown>,
  params: Record<string, string>,
): ActivatedRouteSnapshot {
  return { data, paramMap: convertToParamMap(params) } as unknown as ActivatedRouteSnapshot;
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function run(route: ActivatedRouteSnapshot, providers: any[]) {
  TestBed.configureTestingModule({ providers });
  return TestBed.runInInjectionContext(() =>
    artifactNameResolver(route, {} as RouterStateSnapshot),
  );
}

describe('artifactNameResolver (#154)', () => {
  it('resolves a goal to its name and calls the service with (projectName, numeric id)', async () => {
    const getGoal = vi.fn().mockResolvedValue({ id: 12, name: 'Login flow' });
    const result = await run(
      routeSnapshot({ artifactType: 'goal' }, { name: 'Acme', goalId: '12' }),
      [{ provide: GoalService, useValue: { getGoal } }],
    );
    expect(result).toBe('Login flow');
    expect(getGoal).toHaveBeenCalledWith('Acme', 12);
  });

  it('dispatches on artifactType (stakeholder → StakeholderService)', async () => {
    const getStakeholder = vi.fn().mockResolvedValue({ id: 3, name: 'Program Office' });
    const result = await run(
      routeSnapshot({ artifactType: 'stakeholder' }, { name: 'Acme', stakeholderId: '3' }),
      [{ provide: StakeholderService, useValue: { getStakeholder } }],
    );
    expect(result).toBe('Program Office');
    expect(getStakeholder).toHaveBeenCalledWith('Acme', 3);
  });

  it('fails soft to null when the fetch rejects (never blocks navigation)', async () => {
    const getGoal = vi.fn().mockRejectedValue(new Error('boom'));
    const result = await run(
      routeSnapshot({ artifactType: 'goal' }, { name: 'Acme', goalId: '12' }),
      [{ provide: GoalService, useValue: { getGoal } }],
    );
    expect(result).toBeNull();
  });

  it('returns null for a non-numeric id without calling any service', async () => {
    const getGoal = vi.fn();
    const result = await run(
      routeSnapshot({ artifactType: 'goal' }, { name: 'Acme', goalId: 'not-a-number' }),
      [{ provide: GoalService, useValue: { getGoal } }],
    );
    expect(result).toBeNull();
    expect(getGoal).not.toHaveBeenCalled();
  });

  it('returns null when artifactType is missing', async () => {
    const result = await run(
      routeSnapshot({}, { name: 'Acme', goalId: '12' }),
      [{ provide: GoalService, useValue: { getGoal: vi.fn() } }],
    );
    expect(result).toBeNull();
  });

  it('returns null when the project name param is absent', async () => {
    const result = await run(
      routeSnapshot({ artifactType: 'goal' }, { goalId: '12' }),
      [{ provide: GoalService, useValue: { getGoal: vi.fn() } }],
    );
    expect(result).toBeNull();
  });
});
