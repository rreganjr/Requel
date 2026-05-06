/**
 * API helpers for test setup/teardown.
 * These call the backend REST API directly (no UI) so tests can create
 * and destroy fixtures without going through the browser.
 */
import { APIRequestContext } from '@playwright/test';

const BASE_URL = 'http://localhost:8080';

export interface ProjectFixture {
  id: number;
  version: number;
  name: string;
}

export interface GoalFixture {
  id: number;
  version: number;
  name: string;
  projectName: string;
}

export interface StoryFixture {
  id: number;
  version: number;
  name: string;
  projectName: string;
}

export interface ActorFixture {
  id: number;
  version: number;
  name: string;
  projectName: string;
}

export interface UseCaseFixture {
  id: number;
  version: number;
  name: string;
  projectName: string;
}

export interface ScenarioFixture {
  id: number;
  version: number;
  name: string;
  projectName: string;
}

export interface UserFixture {
  id: number;
  version: number;
  username: string;
}

export interface ReportFixture {
  id: number;
  version: number;
  name: string;
  projectName: string;
}

export interface TermFixture {
  id: number;
  version: number;
  name: string;
  projectName: string;
}

export interface PreferencesFixture {
  sidebarProjectLimit: number;
  sidebarProjectStaleness: string;
}

export interface IssueFixture {
  id: number;
  projectName: string;
}

export interface PositionFixture {
  id: number;
  issueId: number;
  projectName: string;
}

async function getAdminToken(api: APIRequestContext): Promise<string> {
  const username = process.env['E2E_ADMIN_USERNAME'] ?? 'admin';
  const password = process.env['E2E_ADMIN_PASSWORD'] ?? 'admin';
  const res = await api.post(`${BASE_URL}/api/auth/login`, {
    data: { username, password },
  });
  if (!res.ok()) {
    throw new Error(`API helper login failed: ${res.status()} — set E2E_ADMIN_PASSWORD if the default was changed`);
  }
  const { token } = await res.json();
  return token as string;
}

async function command(
  api: APIRequestContext,
  token: string,
  type: string,
  body: Record<string, unknown>
): Promise<Record<string, unknown>> {
  const res = await api.post(`${BASE_URL}/api/commands/${type}`, {
    data: body,
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok()) {
    throw new Error(`Command ${type} failed: ${res.status()} ${await res.text()}`);
  }
  return res.json() as Promise<Record<string, unknown>>;
}

export async function createProject(
  api: APIRequestContext,
  name: string,
  description = ''
): Promise<ProjectFixture> {
  const token = await getAdminToken(api);
  const result = await command(api, token, 'EditProject', {
    projectName: null,
    name,
    description: description || null,
    organizationName: 'E2E Test Org',
  });
  const entity = result['entity'] as { id: number; version: number; name: string };
  return { id: entity.id, version: entity.version, name: entity.name };
}

/**
 * Note: there is no DeleteProject command in the backend.
 * Test projects named e2e-* will accumulate — they can be removed manually via the admin UI.
 * This function is a no-op kept here as a placeholder.
 */
export async function deleteProject(
  _api: APIRequestContext,
  _projectName: string
): Promise<void> {
  // DeleteProject is not implemented in the backend; nothing to do.
}

/**
 * List all project names visible to the admin user via GET /api/projects.
 * Useful for round-trip tests that need to identify a newly-imported project
 * whose name the import flow auto-generated (e.g. "Imported Project" with
 * collision suffix).
 */
export async function listProjectNames(api: APIRequestContext): Promise<string[]> {
  const token = await getAdminToken(api);
  const res = await api.get(`${BASE_URL}/api/projects`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok()) {
    throw new Error(`Listing projects failed: ${res.status()} ${await res.text()}`);
  }
  const body = (await res.json()) as Array<{ name: string }>;
  return body.map(p => p.name);
}

/**
 * Fetch the XML export of a project as a string. Hits the same authenticated
 * GET /api/projects/{name}/export that ProjectEditorComponent.onExport() calls
 * via HttpClient — but bypasses the browser's blob-download path, which gives
 * tests deterministic bytes. (Playwright's download.saveAs() can intermittently
 * write 0-byte files when the underlying download is a blob: URL anchor; this
 * helper avoids that capture path entirely while still exercising the same
 * server endpoint that ships in the response.)
 */
export async function exportProjectXml(
  api: APIRequestContext,
  projectName: string
): Promise<string> {
  const token = await getAdminToken(api);
  const res = await api.get(
    `${BASE_URL}/api/projects/${encodeURIComponent(projectName)}/export`,
    { headers: { Authorization: `Bearer ${token}` } }
  );
  if (!res.ok()) {
    throw new Error(`Exporting project '${projectName}' failed: ${res.status()} ${await res.text()}`);
  }
  return res.text();
}

export async function createGoal(
  api: APIRequestContext,
  projectName: string,
  name: string,
  text = ''
): Promise<GoalFixture> {
  const token = await getAdminToken(api);
  const result = await command(api, token, 'EditGoal', {
    projectName,
    name,
    text: text || name,
  });
  const entity = result['entity'] as { id: number; version: number; name: string };
  return { id: entity.id, version: entity.version, name: entity.name, projectName };
}

export async function deleteGoal(
  api: APIRequestContext,
  goal: GoalFixture
): Promise<void> {
  const token = await getAdminToken(api);
  await command(api, token, 'DeleteGoal', {
    projectName: goal.projectName,
    goalId: goal.id,
    version: goal.version,
  });
}

export async function createGoalRelation(
  api: APIRequestContext,
  projectName: string,
  fromGoalName: string,
  toGoalName: string,
  relationType = 'Supports'
): Promise<void> {
  const token = await getAdminToken(api);
  await command(api, token, 'EditGoalRelation', {
    projectName,
    fromGoalName,
    toGoalName,
    relationType,
  });
}

export async function createStory(
  api: APIRequestContext,
  projectName: string,
  name: string,
  storyTypeName = 'Success',
  text = '',
  primaryActorName?: string
): Promise<StoryFixture> {
  const token = await getAdminToken(api);
  const result = await command(api, token, 'EditStory', {
    projectName,
    name,
    text: text || name,
    storyTypeName,
    ...(primaryActorName ? { primaryActorName } : {}),
  });
  const entity = result['entity'] as { id: number; version: number; name: string };
  return { id: entity.id, version: entity.version, name: entity.name, projectName };
}

export async function deleteStory(
  api: APIRequestContext,
  story: StoryFixture
): Promise<void> {
  const token = await getAdminToken(api);
  await command(api, token, 'DeleteStory', {
    projectName: story.projectName,
    storyId: story.id,
    version: story.version,
  });
}

export async function addActorToStory(
  api: APIRequestContext,
  projectName: string,
  storyId: number,
  actorId: number
): Promise<void> {
  const token = await getAdminToken(api);
  await command(api, token, 'AddActorToActorContainer', {
    projectName,
    actorContainerId: storyId,
    actorId,
  });
}

export async function addActorToUseCase(
  api: APIRequestContext,
  projectName: string,
  useCaseId: number,
  actorId: number
): Promise<void> {
  const token = await getAdminToken(api);
  await command(api, token, 'AddActorToActorContainer', {
    projectName,
    actorContainerId: useCaseId,
    actorId,
  });
}

export async function addGoalToUseCase(
  api: APIRequestContext,
  projectName: string,
  useCaseId: number,
  goalId: number
): Promise<void> {
  const token = await getAdminToken(api);
  await command(api, token, 'AddGoalToGoalContainer', {
    projectName,
    goalContainerId: useCaseId,
    goalId,
  });
}

export async function addStoryToUseCase(
  api: APIRequestContext,
  projectName: string,
  useCaseId: number,
  storyId: number
): Promise<void> {
  const token = await getAdminToken(api);
  await command(api, token, 'AddStoryToStoryContainer', {
    projectName,
    storyContainerId: useCaseId,
    storyId,
  });
}

export async function addScenarioToUseCase(
  api: APIRequestContext,
  projectName: string,
  useCaseId: number,
  scenarioId: number
): Promise<void> {
  const token = await getAdminToken(api);
  await command(api, token, 'AddScenarioToUseCase', {
    projectName,
    useCaseId,
    scenarioId,
  });
}

export async function setPrimaryScenarioOnUseCase(
  api: APIRequestContext,
  projectName: string,
  useCaseId: number,
  scenarioId: number
): Promise<void> {
  const token = await getAdminToken(api);
  await command(api, token, 'SetPrimaryScenarioOnUseCase', {
    projectName,
    useCaseId,
    scenarioId,
  });
}

export async function createActor(
  api: APIRequestContext,
  projectName: string,
  name: string,
  text = ''
): Promise<ActorFixture> {
  const token = await getAdminToken(api);
  const result = await command(api, token, 'EditActor', {
    projectName,
    name,
    text: text || name,
  });
  const entity = result['entity'] as { id: number; version: number; name: string };
  return { id: entity.id, version: entity.version, name: entity.name, projectName };
}

/**
 * Create a use case. primaryActorName is required by the backend (primary_actor_id NOT NULL).
 * If the actor doesn't exist in the project, the backend creates it automatically.
 */
export async function createUseCase(
  api: APIRequestContext,
  projectName: string,
  name: string,
  text = '',
  primaryActorName = 'E2E Actor'
): Promise<UseCaseFixture> {
  const token = await getAdminToken(api);
  const result = await command(api, token, 'EditUseCase', {
    projectName,
    name,
    text: text || name,
    primaryActorName,
  });
  const entity = result['entity'] as { id: number; version: number; name: string };
  return { id: entity.id, version: entity.version, name: entity.name, projectName };
}

export async function deleteUseCase(
  api: APIRequestContext,
  uc: UseCaseFixture
): Promise<void> {
  const token = await getAdminToken(api);
  await command(api, token, 'DeleteUseCase', {
    projectName: uc.projectName,
    useCaseId: uc.id,
    version: uc.version,
  });
}

export async function getUseCaseVersion(
  api: APIRequestContext,
  uc: UseCaseFixture
): Promise<number> {
  const token = await getAdminToken(api);
  const res = await api.get(
    `${BASE_URL}/api/projects/${encodeURIComponent(uc.projectName)}/use-cases/${uc.id}`,
    { headers: { Authorization: `Bearer ${token}` } }
  );
  if (!res.ok()) return uc.version;
  const data = await res.json() as { version: number };
  return data.version;
}

export async function createScenario(
  api: APIRequestContext,
  projectName: string,
  name: string,
  scenarioTypeName = 'Primary',
  text = ''
): Promise<ScenarioFixture> {
  const token = await getAdminToken(api);
  const result = await command(api, token, 'EditScenario', {
    projectName,
    name,
    text: text || name,
    scenarioTypeName,
    steps: [],
  });
  const entity = result['entity'] as { id: number; version: number; name: string };
  return { id: entity.id, version: entity.version, name: entity.name, projectName };
}

export async function deleteScenario(
  api: APIRequestContext,
  scenario: ScenarioFixture
): Promise<void> {
  const token = await getAdminToken(api);
  await command(api, token, 'DeleteScenario', {
    projectName: scenario.projectName,
    scenarioId: scenario.id,
    version: scenario.version,
  });
}

export async function getScenarioVersion(
  api: APIRequestContext,
  scenario: ScenarioFixture
): Promise<number> {
  const token = await getAdminToken(api);
  const res = await api.get(
    `${BASE_URL}/api/projects/${encodeURIComponent(scenario.projectName)}/scenarios/${scenario.id}`,
    { headers: { Authorization: `Bearer ${token}` } }
  );
  if (!res.ok()) return scenario.version;
  const data = await res.json() as { version: number };
  return data.version;
}

/** Fetches the current version of a story (needed after saves that increment the version). */
export async function getStoryVersion(
  api: APIRequestContext,
  story: StoryFixture
): Promise<number> {
  const token = await getAdminToken(api);
  const res = await api.get(
    `${BASE_URL}/api/projects/${encodeURIComponent(story.projectName)}/stories/${story.id}`,
    { headers: { Authorization: `Bearer ${token}` } }
  );
  if (!res.ok()) return story.version;
  const data = await res.json() as { version: number };
  return data.version;
}

/** Fetches the current version of an actor (needed after saves that increment the version). */
export async function getActorVersion(
  api: APIRequestContext,
  actor: ActorFixture
): Promise<number> {
  const token = await getAdminToken(api);
  const res = await api.get(
    `${BASE_URL}/api/projects/${encodeURIComponent(actor.projectName)}/actors/${actor.id}`,
    { headers: { Authorization: `Bearer ${token}` } }
  );
  if (!res.ok()) return actor.version; // fall back to stored version on error
  const data = await res.json() as { version: number };
  return data.version;
}

export async function deleteActor(
  api: APIRequestContext,
  actor: ActorFixture
): Promise<void> {
  const token = await getAdminToken(api);
  await command(api, token, 'DeleteActor', {
    projectName: actor.projectName,
    actorId: actor.id,
    version: actor.version,
  });
}

/**
 * Create a user via the EditUser command. The user will persist across test runs
 * since there is no DeleteUser command. Use e2e-user-* username prefixes with timestamps.
 */
export async function createUser(
  api: APIRequestContext,
  username: string,
  name: string,
  password: string,
  roleName = 'ProjectUserRole'
): Promise<UserFixture> {
  const token = await getAdminToken(api);
  const result = await command(api, token, 'EditUser', {
    id: null,
    version: 0,
    username,
    name,
    emailAddress: `${username}@example.com`,
    phoneNumber: '',
    organizationName: 'E2E Test Org',
    editable: true,
    password,
    repassword: password,
    userRoleNames: [roleName],
    userRolePermissionNames: { [roleName]: [] },
  });
  const entity = result['entity'] as { id: number; version: number; username: string };
  return { id: entity.id, version: entity.version, username: entity.username };
}

/**
 * Set a user's display name, re-fetching the current version to avoid optimistic-lock conflicts.
 */
export async function setUserName(
  api: APIRequestContext,
  username: string,
  name: string
): Promise<void> {
  const token = await getAdminToken(api);
  const res = await api.get(
    `${BASE_URL}/api/users/${encodeURIComponent(username)}`,
    { headers: { Authorization: `Bearer ${token}` } }
  );
  if (!res.ok()) throw new Error(`getUser ${username} failed: ${res.status()}`);
  const user = await res.json() as {
    id: number; version: number; username: string;
    emailAddress?: string | null; phoneNumber?: string | null;
    organizationName?: string | null; roles: string[];
    permissionsByRole?: Record<string, string[]> | null;
  };
  await command(api, token, 'EditUser', {
    id: user.id,
    version: user.version,
    username: user.username,
    name,
    emailAddress: user.emailAddress ?? null,
    phoneNumber: user.phoneNumber ?? null,
    organizationName: user.organizationName ?? null,
    editable: true,
    userRoleNames: user.roles,
    userRolePermissionNames: user.permissionsByRole ?? {},
  });
}

/**
 * Fetch the display name of a user by username.
 */
export async function getUserName(
  api: APIRequestContext,
  username: string
): Promise<string> {
  const token = await getAdminToken(api);
  const res = await api.get(
    `${BASE_URL}/api/users/${encodeURIComponent(username)}`,
    { headers: { Authorization: `Bearer ${token}` } }
  );
  if (!res.ok()) throw new Error(`getUser ${username} failed: ${res.status()}`);
  const user = await res.json() as { name?: string };
  return user.name ?? '';
}

/**
 * Add an issue to an entity (goal, story, etc.) via the EditIssue command.
 * Returns the created issue's id so tests can add positions to it.
 */
export async function addIssue(
  api: APIRequestContext,
  projectName: string,
  entityType: string,
  entityId: number,
  text: string,
  mustBeResolved = false
): Promise<IssueFixture> {
  const token = await getAdminToken(api);
  const result = await command(api, token, 'EditIssue', {
    projectName,
    entityType,
    entityId,
    text,
    mustBeResolved,
  });
  const entity = result['entity'] as { id: number };
  return { id: entity.id, projectName };
}

/**
 * Add a position to an issue via the EditPosition command.
 * Returns the created position's id so tests can add arguments to it.
 */
export async function addPosition(
  api: APIRequestContext,
  projectName: string,
  issueId: number,
  text: string
): Promise<PositionFixture> {
  const token = await getAdminToken(api);
  const result = await command(api, token, 'EditPosition', {
    projectName,
    issueId,
    text,
  });
  const entity = result['entity'] as { id: number };
  return { id: entity.id, issueId, projectName };
}

export async function getPreferences(
  api: APIRequestContext
): Promise<PreferencesFixture> {
  const token = await getAdminToken(api);
  const res = await api.get(`${BASE_URL}/api/user-preferences`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok()) throw new Error(`getPreferences failed: ${res.status()}`);
  return res.json() as Promise<PreferencesFixture>;
}

export async function savePreferences(
  api: APIRequestContext,
  prefs: PreferencesFixture
): Promise<void> {
  const token = await getAdminToken(api);
  const res = await api.put(`${BASE_URL}/api/user-preferences`, {
    data: prefs,
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok()) throw new Error(`savePreferences failed: ${res.status()}`);
}

export async function createTerm(
  api: APIRequestContext,
  projectName: string,
  name: string,
  text = ''
): Promise<TermFixture> {
  const token = await getAdminToken(api);
  const result = await command(api, token, 'EditGlossaryTerm', {
    projectName,
    termId: null,
    name,
    text: text || null,
    canonicalTermId: null,
  });
  const entity = result['entity'] as { id: number; version: number; name: string };
  return { id: entity.id, version: entity.version, name: entity.name, projectName };
}

export async function deleteTerm(
  api: APIRequestContext,
  term: TermFixture
): Promise<void> {
  const token = await getAdminToken(api);
  await command(api, token, 'DeleteGlossaryTerm', {
    projectName: term.projectName,
    termId: term.id,
  });
}

export async function createReport(
  api: APIRequestContext,
  projectName: string,
  name: string,
  text = ''
): Promise<ReportFixture> {
  const token = await getAdminToken(api);
  const result = await command(api, token, 'EditReportGenerator', {
    projectName,
    reportId: null,
    name,
    text: text || null,
  });
  const entity = result['entity'] as { id: number; version: number; name: string };
  return { id: entity.id, version: entity.version, name: entity.name, projectName };
}

export async function deleteReport(
  api: APIRequestContext,
  report: ReportFixture
): Promise<void> {
  const token = await getAdminToken(api);
  await command(api, token, 'DeleteReportGenerator', {
    projectName: report.projectName,
    reportId: report.id,
  });
}
