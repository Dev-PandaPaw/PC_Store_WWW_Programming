-- Enabled only by the semantic profile; ordinary installations need no extension.
CREATE EXTENSION IF NOT EXISTS vector;
-- Supabase commonly installs vector in extensions rather than public. Keep the
-- current migration target schema and append the extension's actual namespace.
DO $$ DECLARE vector_schema TEXT; BEGIN
    SELECT n.nspname INTO vector_schema FROM pg_extension e
        JOIN pg_namespace n ON n.oid=e.extnamespace WHERE e.extname='vector';
    PERFORM set_config('search_path',current_setting('search_path') || ', ' || quote_ident(vector_schema),true);
END $$;
CREATE TABLE product_semantic_vectors (
    id UUID PRIMARY KEY,
    content TEXT NOT NULL,
    metadata JSON NOT NULL,
    embedding VECTOR(768) NOT NULL
);
CREATE INDEX product_semantic_vectors_cosine_idx
    ON product_semantic_vectors USING hnsw (embedding vector_cosine_ops);
ALTER TABLE product_semantic_vectors ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON product_semantic_vectors FROM PUBLIC;
