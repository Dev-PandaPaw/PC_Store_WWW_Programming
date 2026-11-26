ALTER TABLE carts ADD COLUMN pc_build_requirements JSONB NOT NULL DEFAULT '{}'::jsonb CHECK (jsonb_typeof(pc_build_requirements) = 'object');
