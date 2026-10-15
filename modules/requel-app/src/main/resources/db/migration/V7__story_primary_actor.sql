-- Add primary actor support to stories
ALTER TABLE stories
    ADD COLUMN primary_actor_id BIGINT NULL,
    ADD CONSTRAINT FK_story_primary_actor
        FOREIGN KEY (primary_actor_id) REFERENCES actors (id);
