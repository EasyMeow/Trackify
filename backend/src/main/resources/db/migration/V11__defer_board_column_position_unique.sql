-- Make the board_columns position unique constraint deferrable so that
-- reorder updates (which temporarily assign duplicate positions within a
-- transaction before all rows are written) pass without a constraint error.
-- The constraint is still enforced -- just at COMMIT time instead of per-row.
ALTER TABLE board_columns
    DROP CONSTRAINT board_columns_project_position_unique;

ALTER TABLE board_columns
    ADD CONSTRAINT board_columns_project_position_unique
        UNIQUE (project_id, position) DEFERRABLE INITIALLY DEFERRED;
