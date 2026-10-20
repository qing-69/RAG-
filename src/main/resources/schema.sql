-- noinspection SqlNoDataSourceInspectionForFile
-- noinspection SqlResolveForFile

CREATE TABLE IF NOT EXISTS kb_document (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  file_name VARCHAR(255) NOT NULL,
  content_type VARCHAR(120),
  file_size BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL,
  chunk_count INT NOT NULL DEFAULT 0,
  error_message TEXT,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS kb_chunk (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  document_id BIGINT NOT NULL,
  vector_id VARCHAR(120) NOT NULL,
  chunk_index INT NOT NULL,
  title VARCHAR(500),
  section_path VARCHAR(1000),
  content MEDIUMTEXT NOT NULL,
  token_estimate INT NOT NULL,
  created_at DATETIME NOT NULL,
  INDEX idx_kb_chunk_document_id (document_id),
  UNIQUE KEY uk_kb_chunk_vector_id (vector_id)
);

CREATE TABLE IF NOT EXISTS long_memory (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  vector_id VARCHAR(120) NOT NULL,
  user_id VARCHAR(120) NOT NULL,
  title VARCHAR(255),
  content TEXT NOT NULL,
  tags VARCHAR(500),
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  INDEX idx_long_memory_user_id (user_id),
  UNIQUE KEY uk_long_memory_vector_id (vector_id)
);

CREATE TABLE IF NOT EXISTS chat_query_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id VARCHAR(120) NOT NULL,
  session_id VARCHAR(120) NOT NULL,
  question TEXT NOT NULL,
  rewritten_query TEXT,
  sub_queries TEXT,
  expanded_keywords TEXT,
  knowledge_count INT NOT NULL DEFAULT 0,
  top_vector_score DOUBLE,
  keyword_hit INT NOT NULL DEFAULT 0,
  confidence_level VARCHAR(16) NOT NULL,
  confidence_score DOUBLE NOT NULL DEFAULT 0,
  confidence_reason VARCHAR(500),
  created_at DATETIME NOT NULL,
  INDEX idx_chat_query_log_created_at (created_at),
  INDEX idx_chat_query_log_confidence_level (confidence_level)
);

CREATE TABLE IF NOT EXISTS chat_session (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id VARCHAR(120) NOT NULL,
  session_id VARCHAR(120) NOT NULL,
  title VARCHAR(255) NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  UNIQUE KEY uk_chat_session_user_session (user_id, session_id)
);

CREATE TABLE IF NOT EXISTS chat_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id VARCHAR(120) NOT NULL,
  session_id VARCHAR(120) NOT NULL,
  role VARCHAR(16) NOT NULL,
  content MEDIUMTEXT NOT NULL,
  confidence_level VARCHAR(16),
  sources_json TEXT,
  created_at DATETIME NOT NULL,
  INDEX idx_chat_message_session (user_id, session_id)
);

CREATE TABLE IF NOT EXISTS chat_feedback (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id VARCHAR(120) NOT NULL,
  session_id VARCHAR(120) NOT NULL,
  question TEXT NOT NULL,
  rating VARCHAR(8) NOT NULL,
  comment VARCHAR(500),
  created_at DATETIME NOT NULL,
  INDEX idx_chat_feedback_created_at (created_at),
  INDEX idx_chat_feedback_rating (rating)
);
