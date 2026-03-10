-- V2__add_name_index.sql
-- Add index on name column for faster searches

CREATE INDEX IF NOT EXISTS idx_historical_characters_name 
ON historical_characters (LOWER(name));
