# Visit document storage

Generated visit invoices and visit archive folders are stored in the configured
private invoice bucket (`SUPABASE_BUCKET_INVOICES`, default `clinic-files`). In
LOCAL mode, the bucket is a directory below `LOCAL_STORAGE_PATH`.

The object-key layout is:

```text
clinic-{clinicId}/
└── patient-{patientId}_{patientName}_{phone}_{visitDate}_{visitNumber}/
    └── visit-{visitDate}_{visitId}/
        └── departments/
            └── {departmentName}-{visitDepartmentId}/
                ├── consultations/
                └── invoices/
                    └── billing-{billingId}/
                        ├── invoice-default.pdf
                        ├── invoice-patient.pdf
                        └── invoice-insurance.pdf
```

The patient visit number is stable in the order visits were created for that
patient. IDs ensure uniqueness; readable names and dates make the hierarchy
easier to browse manually. Path components are sanitized for filesystem/object
key use. The full phone number is included to match the configured naming
guideline, so this bucket must remain private and access must be restricted.

The backend creates the folder structure when a visit is finalised. Generating
an invoice also creates its parent directories. Consultations directories are
created for organization only; this change does not create or store consultation
PDFs. Local storage uses directories, while object storage uses `.keep` marker
objects to make otherwise-empty folders visible in storage browsers.

When a visit date changes, the patient and visit prefixes are renamed in storage
after the database transaction commits. The visit-date part of both folder names
is updated, while the stable IDs and visit number remain unchanged. Existing
invoice paths are updated by the existing invoice-regeneration flow.

Changing the default bucket from `data` to `clinic-files` does not migrate
existing files. Deployments with existing invoices should preserve their current
`SUPABASE_BUCKET_INVOICES` setting until the previous objects and billing
references have been migrated.

## Selecting a storage provider

The backend supports `LOCAL`, `SUPABASE`, and `MINIO` via `STORAGE_TYPE`.
For MinIO, configure:

```text
STORAGE_TYPE=MINIO
MINIO_ENDPOINT=http://minio:9000
MINIO_PUBLIC_ENDPOINT=https://files.example.org
MINIO_ACCESS_KEY=<application-access-key>
MINIO_SECRET_KEY=<application-secret-key>
MINIO_REGION=us-east-1
SUPABASE_BUCKET_INVOICES=clinic-files
```

`MINIO_ENDPOINT` is the address reachable from the backend. The public endpoint
must reach the same MinIO deployment and be reachable by browser clients because
the backend signs downloads for that host. In local Compose, start the optional
MinIO service with:

```sh
STORAGE_TYPE=MINIO docker compose --profile minio up -d
```

The local Compose credentials are development defaults only; use dedicated
least-privilege credentials and TLS in a deployed environment. The `uploads-public`
bucket is given a read-only object policy when first created. Private upload and
invoice buckets are created without public-read access; downloads use
time-limited presigned URLs.

MinIO prefix moves use server-side copy followed by deletion of the source
objects, so they avoid downloading invoice data through the backend. A failure
can still leave a partially copied prefix; preserve backups and monitor storage
errors. This provider does not automatically migrate objects from local storage
or Supabase.
