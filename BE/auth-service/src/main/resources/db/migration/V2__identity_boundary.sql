ALTER TABLE users ADD COLUMN IF NOT EXISTS account_status VARCHAR(20);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'users'
          AND column_name = 'enabled'
    ) THEN
        EXECUTE 'UPDATE users
                 SET account_status = CASE
                     WHEN enabled THEN ''ACTIVE''
                     ELSE ''DISABLED''
                 END
                 WHERE account_status IS NULL';
    END IF;
END $$;

UPDATE users SET account_status = 'ACTIVE' WHERE account_status IS NULL;

DO $$
DECLARE
    has_profile_data BOOLEAN;
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'users'
          AND column_name = 'full_name'
    ) THEN
        EXECUTE 'SELECT EXISTS (
                     SELECT 1 FROM users WHERE full_name IS NOT NULL
                 )'
            INTO has_profile_data;

        IF has_profile_data THEN
            RAISE EXCEPTION
                'auth_db.users.full_name still contains data; run BE/scripts/migrations/migrate-auth-full-name-to-user-profile.sh first';
        END IF;
    END IF;
END $$;

ALTER TABLE users
    ALTER COLUMN account_status SET DEFAULT 'ACTIVE',
    ALTER COLUMN account_status SET NOT NULL;

ALTER TABLE users DROP COLUMN IF EXISTS enabled;
ALTER TABLE users DROP COLUMN IF EXISTS full_name;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_users_role'
          AND conrelid = 'users'::regclass
    ) THEN
        ALTER TABLE users
            ADD CONSTRAINT ck_users_role CHECK (role IN ('CUSTOMER', 'ADMIN'));
    END IF;
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_users_account_status'
          AND conrelid = 'users'::regclass
    ) THEN
        ALTER TABLE users
            ADD CONSTRAINT ck_users_account_status
            CHECK (account_status IN ('ACTIVE', 'DISABLED'));
    END IF;
END $$;
