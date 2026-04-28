-- Performance indexes
CREATE INDEX idx_documents_user_id
    ON documents(user_id);

CREATE INDEX idx_documents_status
    ON documents(status);

CREATE INDEX idx_conversations_user_id
    ON conversations(user_id);

CREATE INDEX idx_conversations_status
    ON conversations(status);

CREATE INDEX idx_messages_conversation_id
    ON messages(conversation_id);

CREATE INDEX idx_messages_created_at
    ON messages(created_at);

CREATE INDEX idx_document_chunks_document_id
    ON document_chunks(document_id);

CREATE INDEX idx_feedback_message_id
    ON feedback(message_id);

-- Update trigger for updated_at columns
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_documents_updated_at
    BEFORE UPDATE ON documents
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_conversations_updated_at
    BEFORE UPDATE ON conversations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();