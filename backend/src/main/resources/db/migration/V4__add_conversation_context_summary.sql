-- V4__add_conversation_context_summary.sql
-- Adds persistent long-term memory summary for chat conversations.

ALTER TABLE conversations
    ADD COLUMN IF NOT EXISTS context_summary TEXT;
