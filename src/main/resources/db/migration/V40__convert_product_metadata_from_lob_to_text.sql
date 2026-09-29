-- ============================================================================
-- Migration V40: Convert products.metadata from OID (Large Object) to TEXT
--
-- Product.metadata was mapped as @Lob in JPA, causing Hibernate to create the
-- column as `oid` in PostgreSQL. In PostgreSQL, OID columns reference entries
-- in `pg_largeobject`, requiring continuous LOB streaming within an open
-- transaction and breaking with "Unable to access lob stream: large object does
-- not exist" whenever entries are detached or dumped/restored.
--
-- Since product metadata stores JSON string data (default '{}'), TEXT is the
-- standard and safe column type.
-- ============================================================================

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'products'
          AND column_name = 'metadata'
          AND udt_name = 'oid'
    ) THEN
        ALTER TABLE products ALTER COLUMN metadata TYPE TEXT USING '{}';
    END IF;
END $$;

ALTER TABLE products ALTER COLUMN metadata SET DEFAULT '{}';
UPDATE products SET metadata = '{}' WHERE metadata IS NULL OR metadata = '' OR metadata ~ '^\d+$';
