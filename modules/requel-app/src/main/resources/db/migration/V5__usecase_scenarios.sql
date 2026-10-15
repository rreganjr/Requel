-- Additional (non-primary) scenarios on a use case: Alternative and Exception flows.
-- The primary scenario is already tracked via usecases.scenario_id.
CREATE TABLE IF NOT EXISTS `usecase_scenarios` (
  `usecase_id` bigint NOT NULL,
  `scenario_id` bigint NOT NULL,
  PRIMARY KEY (`usecase_id`, `scenario_id`),
  CONSTRAINT `FK_uc_scenarios_usecase` FOREIGN KEY (`usecase_id`) REFERENCES `usecases` (`id`),
  CONSTRAINT `FK_uc_scenarios_scenario` FOREIGN KEY (`scenario_id`) REFERENCES `scenarios` (`id`)
);
