-- ALTER TABLE user_flashcard_progress
--     ADD COLUMN repetition INT NOT NULL DEFAULT 0;

-- ALTER TABLE user_flashcard_progress
--     ADD COLUMN interval_days INT NOT NULL DEFAULT 1;

-- ALTER TABLE user_flashcard_progress
--     ADD COLUMN easiness_factor DOUBLE NOT NULL DEFAULT 2.5;

-- ALTER TABLE user_flashcard_progress
--     ADD COLUMN next_review DATE NOT NULL DEFAULT (CURRENT_DATE);

-- ALTER TABLE user_flashcard_progress
--     ADD COLUMN last_reviewed_at DATETIME NULL;

-- ALTER TABLE user_flashcard_progress
--     ADD COLUMN last_quality INT NULL;

-- Migracja: podział pola note na front_note i back_note (dwie strony fiszki)
-- ALTER TABLE user_flashcard_progress ADD COLUMN front_note TEXT;
-- ALTER TABLE user_flashcard_progress ADD COLUMN back_note TEXT;
-- UPDATE user_flashcard_progress SET front_note = note WHERE note IS NOT NULL;
-- ALTER TABLE user_flashcard_progress DROP COLUMN note;

