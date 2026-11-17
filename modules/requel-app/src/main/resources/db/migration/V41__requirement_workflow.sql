ALTER TABLE ideas ADD COLUMN version bigint NOT NULL DEFAULT 0;

CREATE TABLE workflow_requirements (
 id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
 version bigint NOT NULL DEFAULT 0,
 project_id bigint NOT NULL,
 idea_id bigint,
 requirement_code varchar(255),
 shall_text longtext NOT NULL,
 status varchar(30) NOT NULL,
 created_by varchar(255) NOT NULL,
 created_at datetime(6) NOT NULL,
 updated_at datetime(6) NOT NULL,
 UNIQUE KEY uq_workflow_reference (project_id, requirement_code),
 KEY idx_workflow_project (project_id),
 KEY idx_workflow_idea (idea_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE workflow_reviews (
 requirement_id bigint NOT NULL,
 department varchar(30) NOT NULL,
 result varchar(30) NOT NULL,
 comments longtext,
 reviewer varchar(255),
 reviewer_role varchar(255),
 reviewed_at datetime(6),
 PRIMARY KEY (requirement_id, department)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
