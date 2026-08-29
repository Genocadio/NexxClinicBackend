#!/usr/bin/env bash
# ============================================================================
# NexxClinic Insurance Price Populator
#
# Ensures EVERY supported-by-clinic insurance provider has a product coverage
# for EVERY product, using a price ladder anchored on the "base" provider
# (RSSB by convention):
#
#   BASE  provider (RSSB)          -> coverage cost stays as-is (the anchor)
#   SAME  providers (MMI)          -> coverage cost == base cost (exact)
#   OTHER insurance providers      -> coverage cost == base cost + INSURANCE_MARKUP (+100)
#   PRIVATE (private_rhic_price)   -> base cost + PRIVATE_MARKUP (+300), but only
#                                     filled when the product currently has no
#                                     private price (never overwrites existing).
#
# Base cost resolution for a product (first match wins):
#   1. existing RSSB coverage cost when > 0
#   2. highest coverage cost across ALL insurers for that product
#   3. existing private_rhic_price - 300
#   4. otherwise the product is skipped (no price reference at all)
#
# The BASE provider's coverage is written (and private filled) so re-running
# the script is idempotent: a second run produces the same ladder with no churn.
#
# Providers are matched by acronym / name (case-insensitive). Any other
# supported provider not identified as BASE or SAME is treated as OTHER (gets
# the +INSURANCE_MARKUP). Unsupported providers (supported_by_clinic=false)
# are never touched.
#
# Usage:
#   ./populate-insurance-prices.sh            # applies changes (COMMIT)
#   ./populate-insurance-prices.sh --dry-run  # preview only (ROLLBACK)
#
# Environment overrides:
#   PG_PSQL       connection command (default: docker exec -i nexxclinic-postgres psql ...)
#   PG_DUMP_CMD   backup command (default: docker exec nexxclinic-postgres pg_dump ...)
#   DB_NAME / DB_USER / CONTAINER   used to build the defaults above
#   DB_URL        standard postgres URL (Supabase DATABASE_URL) to point at the
#                 backend/production database; takes precedence over defaults
#   INSURANCE_MARKUP / PRIVATE_MARKUP  price deltas (default 100 / 300)
#   BACKUP_DIR    if set, pg_dump the affected tables there before writing
#
# Running against the backend database from this repo's docker-compose (Supabase
# postgres at host.docker.internal:55322). psql is not installed on the host so
# the local nexxclinic-postgres container is used as the SQL client:
#
#   BACKUP_DIR=. "DB_URL=postgresql://postgres.your-tenant-id:PASSWORD@host.docker.internal:55322/postgres" \
#     ./populate-insurance-prices.sh --dry-run     # preview (ROLLBACK)
#   BACKUP_DIR=. "DB_URL=postgresql://postgres.your-tenant-id:PASSWORD@host.docker.internal:55322/postgres" \
#     ./populate-insurance-prices.sh               # apply (COMMIT)
#
# SECURITY: never put the DB_URL/password into the repo. Keep it in a shell
# variable, a git-ignored .env, or a secret manager.
# ============================================================================
set -euo pipefail

DB_NAME="${DB_NAME:-nexxclinic}"
DB_USER="${DB_USER:-nexxclinic_user}"
CONTAINER="${CONTAINER:-nexxclinic-postgres}"

# Default connection: the local docker postgres. Override PG_PSQL for the
# backend/production database, e.g.  PG_PSQL="psql \"\$DATABASE_URL\""
PG_PSQL="${PG_PSQL:-docker exec -i $CONTAINER psql -U $DB_USER -d $DB_NAME}"
PG_DUMP_CMD="${PG_DUMP_CMD:-docker exec $CONTAINER pg_dump -U $DB_USER -d $DB_NAME}"

# Optional standard postgres URL (Supabase-style DATABASE_URL), e.g.
#   postgresql://postgres.your-tenant-id:PASSWORD@host.docker.internal:55322/postgres
# When set it becomes the connection used by psql / pg_dump. The local
# nexxclinic-postgres container is used as the client because the host has no
# psql installed. This URL (and its embedded password) must NOT be committed.
DB_URL="${DB_URL:-}"
if [[ -n "$DB_URL" ]]; then
  # DB_URL must not contain whitespace (standard for connection URLs).
  PG_PSQL="docker exec -i $CONTAINER psql $DB_URL"
  PG_DUMP_CMD="docker exec $CONTAINER pg_dump $DB_URL"
fi

INSURANCE_MARKUP="${INSURANCE_MARKUP:-100}"
PRIVATE_MARKUP="${PRIVATE_MARKUP:-300}"
BACKUP_DIR="${BACKUP_DIR:-}"
DRY_RUN=0

for arg in "$@"; do
  case "$arg" in
    --dry-run) DRY_RUN=1 ;;
    *) echo "Unknown argument: $arg (expected: --dry-run)" >&2; exit 2 ;;
  esac
done

# Sanity-check the numeric overrides.
for v in INSURANCE_MARKUP PRIVATE_MARKUP; do
  if [[ ! "${!v}" =~ ^[0-9]+$ ]]; then
    echo "ERROR: $v must be a non-negative integer (got '${!v}')" >&2
    exit 2
  fi
done

# ── Optional backup of the tables that will be written ───────────────────
if [[ -n "$BACKUP_DIR" ]]; then
  mkdir -p "$BACKUP_DIR"
  TS="$(date +%Y%m%d-%H%M%S)"
  echo "💾 Backing up affected tables -> $BACKUP_DIR"
  for tbl in product_insurance_coverages products insurance_providers; do
    ${PG_DUMP_CMD} --data-only --table="$tbl" > "$BACKUP_DIR/${tbl}.${TS}.sql" 2>/dev/null
  done
fi

if [[ "$DRY_RUN" == "1" ]]; then
  echo "🔬 DRY RUN — changes will be rolled back, nothing is persisted."
  END_SQL="ROLLBACK;"
else
  echo "🚀 APPLYING changes to $DB_NAME (run with --dry-run to preview)."
  END_SQL="COMMIT;"
fi

if [[ -n "$DB_URL" ]]; then
  # Print the target with the password redacted, so the console never echoes it.
  MASKED="$(printf '%s' "$DB_URL" | sed -E 's#(://[^:/]+:)[^@/]+(@)#\1********\2#')"
  echo "🎯 Target: ${MASKED}  (credentials passed via DB_URL, do not commit them)"
fi

START_TIME="$(date '+%Y-%m-%d %H:%M:%S')"

$PG_PSQL -v ON_ERROR_STOP=1 \
         -v ins_markup="$INSURANCE_MARKUP" \
         -v priv_markup="$PRIVATE_MARKUP" \
         --set=VERBOSITY=default <<SQL
BEGIN;

-- ╔══════════════════════════════════════════════════════════════════════╗
-- ║  0. Classify supported insurance providers                            ║
-- ╚══════════════════════════════════════════════════════════════════════╝
CREATE TEMP TABLE _pc_roles ON COMMIT DROP AS
SELECT id, acronym, insurance_name,
       CASE
         WHEN lower(coalesce(acronym,'')) = 'rssb'
              OR lower(insurance_name) LIKE '%rwanda social security%'
           THEN 'BASE'
         WHEN lower(coalesce(acronym,'')) = 'mmi'
              OR lower(insurance_name) LIKE '%madison%'
           THEN 'SAME'
         ELSE 'OTHER'
       END AS role
FROM insurance_providers
WHERE supported_by_clinic = true;

\echo ''
\echo '──────────────── Insurance provider roles ────────────────'
SELECT role,
       string_agg(insurance_name, ', ' ORDER BY insurance_name) AS providers,
       string_agg(coalesce(acronym,'-'), ', ' ORDER BY insurance_name) AS acronyms
FROM _pc_roles
GROUP BY role
ORDER BY role;

\echo '──────────────── Pre-run coverage counts ────────────────'
SELECT pr.acronym, count(*) AS coverages
FROM product_insurance_coverages pic
JOIN insurance_providers pr ON pr.id = pic.insurance_provider_id
GROUP BY pr.acronym ORDER BY pr.acronym;

-- ╔══════════════════════════════════════════════════════════════════════╗
-- ║  1. Compute the base price for every product                          ║
-- ╚══════════════════════════════════════════════════════════════════════╝
CREATE TEMP TABLE _pc_base ON COMMIT DROP AS
WITH rssb AS (
    SELECT pic.product_id, max(pic.cost) AS cost
    FROM product_insurance_coverages pic
    JOIN _pc_roles r ON r.id = pic.insurance_provider_id AND r.role = 'BASE'
    GROUP BY pic.product_id
),
mx AS (
    SELECT pic.product_id, max(pic.cost) AS cost
    FROM product_insurance_coverages pic
    GROUP BY pic.product_id
)
SELECT p.id AS product_id,
       p.code,
       COALESCE(
         NULLIF(rssb.cost, 0),                 -- 1. positive RSSB anchor
         mx.cost,                              -- 2. highest existing coverage cost
         p.private_rhic_price - :priv_markup,  -- 3. private minus private markup
         0                                     -- 4. no price reference -> skipped
       ) AS base
FROM products p
LEFT JOIN rssb ON rssb.product_id = p.id
LEFT JOIN mx    ON mx.product_id = p.id;

\echo '──────────────── Products in scope ────────────────'
SELECT count(*) FILTER (WHERE base > 0) AS processed,
       count(*) FILTER (WHERE base = 0) AS skipped_no_price_reference
FROM _pc_base;

-- ╔══════════════════════════════════════════════════════════════════════╗
-- ║  2. Upsert coverages per role                                          ║
-- ║     Existing rows: price updated only when it differs (policy fields   ║
-- ║     like require_medical_advisor are left untouched).                  ║
-- ║     Missing rows: created with app defaults.                          ║
-- ╚══════════════════════════════════════════════════════════════════════╝
-- BASE providers carry the anchor so re-runs are stable.
INSERT INTO product_insurance_coverages
  (id, product_id, insurance_provider_id, cost, covered, not_paid,
   require_medical_advisor, drug_administration_frequency, must_prescribed_by,
   created_at, updated_at)
SELECT gen_random_uuid(), b.product_id, r.id, b.base, (b.base > 0), false, false,
       'CUSTOM_HOURS', 'ALL', now(), now()
FROM _pc_base b
JOIN _pc_roles r ON r.role = 'BASE'
WHERE b.base > 0
ON CONFLICT (product_id, insurance_provider_id) DO UPDATE
   SET cost = EXCLUDED.cost,
       covered = EXCLUDED.cost > 0,
       updated_at = now()
   WHERE product_insurance_coverages.cost IS DISTINCT FROM EXCLUDED.cost;

-- SAME providers get the exact same amount as the anchor (e.g. MMI).
INSERT INTO product_insurance_coverages
  (id, product_id, insurance_provider_id, cost, covered, not_paid,
   require_medical_advisor, drug_administration_frequency, must_prescribed_by,
   created_at, updated_at)
SELECT gen_random_uuid(), b.product_id, r.id, b.base, (b.base > 0), false, false,
       'CUSTOM_HOURS', 'ALL', now(), now()
FROM _pc_base b
JOIN _pc_roles r ON r.role = 'SAME'
WHERE b.base > 0
ON CONFLICT (product_id, insurance_provider_id) DO UPDATE
   SET cost = EXCLUDED.cost,
       covered = EXCLUDED.cost > 0,
       updated_at = now()
   WHERE product_insurance_coverages.cost IS DISTINCT FROM EXCLUDED.cost;

-- All OTHER supported providers get base + insurance markup (+100).
INSERT INTO product_insurance_coverages
  (id, product_id, insurance_provider_id, cost, covered, not_paid,
   require_medical_advisor, drug_administration_frequency, must_prescribed_by,
   created_at, updated_at)
SELECT gen_random_uuid(), b.product_id, r.id, b.base + :ins_markup,
       ((b.base + :ins_markup) > 0), false, false,
       'CUSTOM_HOURS', 'ALL', now(), now()
FROM _pc_base b
JOIN _pc_roles r ON r.role = 'OTHER'
WHERE b.base > 0
ON CONFLICT (product_id, insurance_provider_id) DO UPDATE
   SET cost = EXCLUDED.cost,
       covered = EXCLUDED.cost > 0,
       updated_at = now()
   WHERE product_insurance_coverages.cost IS DISTINCT FROM EXCLUDED.cost;

-- ╔══════════════════════════════════════════════════════════════════════╗
-- ║  3. Private RHIC price: fill base + private markup, only when NULL     ║
-- ╚══════════════════════════════════════════════════════════════════════╝
UPDATE products p
SET private_rhic_price = b.base + :priv_markup,
    updated_at = now()
FROM _pc_base b
WHERE b.product_id = p.id
  AND b.base > 0
  AND p.private_rhic_price IS NULL;

\echo ''
\echo '──────────────── Post-run coverage counts ────────────────'
SELECT pr.acronym, count(*) AS coverages
FROM product_insurance_coverages pic
JOIN insurance_providers pr ON pr.id = pic.insurance_provider_id
GROUP BY pr.acronym ORDER BY pr.acronym;

\echo '──────────────── Products still missing any coverage ────────────────'
SELECT p.code
FROM products p
WHERE NOT EXISTS (SELECT 1 FROM product_insurance_coverages pic WHERE pic.product_id = p.id)
ORDER BY p.code;

\echo '──────────────── Products still missing private price ────────────────'
SELECT code FROM products WHERE private_rhic_price IS NULL ORDER BY code;

\echo '──────────────── Sample ladder (first 8 products) ────────────────'
SELECT p.code AS product,
       coalesce(to_char(p.private_rhic_price, 'FM999,999,990'), 'NULL') AS private,
       (SELECT string_agg(pr.acronym || '=' || pic.cost::text, ', ' ORDER BY pr.acronym)
          FROM product_insurance_coverages pic
          JOIN insurance_providers pr ON pr.id = pic.insurance_provider_id
         WHERE pic.product_id = p.id) AS coverages
FROM products p
ORDER BY p.code
LIMIT 8;

$END_SQL
SQL

if [[ "$DRY_RUN" == "1" ]]; then
  echo ""
  echo "🔬 DRY RUN complete (started $START_TIME). Nothing was persisted."
else
  echo ""
  echo "✅ Done (started $START_TIME). See the coverage counts above — every product"
  echo "   should now have a coverage for every supported insurer following the ladder."
fi