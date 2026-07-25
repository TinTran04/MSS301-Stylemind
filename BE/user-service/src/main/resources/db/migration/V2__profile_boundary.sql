ALTER TABLE customer_style_profiles
    ADD COLUMN IF NOT EXISTS display_name VARCHAR(150);
