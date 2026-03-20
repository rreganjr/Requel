export interface StepDto {
  id: number;
  version: number;
  name: string;
  text: string | null;
  scenarioType: string | null;
  isScenario: boolean;
  scenarioId: number | null;
}

export interface ScenarioDto {
  id: number;
  version: number;
  name: string;
  text: string | null;
  scenarioType: string | null;
  createdBy: string | null;
  steps: StepDto[] | null;
}

export interface EditStepInput {
  stepId: number | null;
  name: string;
  text: string | null;
  scenarioTypeName: string | null;
  isScenario: boolean;
}
