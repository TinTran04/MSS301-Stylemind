ALTER TABLE product_images
    ADD COLUMN IF NOT EXISTS image_public_id VARCHAR(255);
