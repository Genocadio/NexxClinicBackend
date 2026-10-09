# ICD-11 disease suggestions in GraphQL

The backend can proxy disease and ICD-11 code autocomplete from the NexxICD
wrapper. Configure `NEXXICD_URL` as the wrapper's base URL — `http://localhost:8082`
for a backend on the host, `http://host.docker.internal:8082` for the Docker stack
(the default in `docker-compose.yml`; the wrapper itself starts with
`docker compose -f icd11/docker-compose.yml up -d`). When it is unset, the
GraphQL query returns an explicit configuration error.

The `searchIcd11Diseases` query is available to authenticated CLINICIAN, NURSE,
CLINIC_ADMIN, and ADMIN users:

```graphql
query SearchDiseases {
  searchIcd11Diseases(query: "malaria", lang: "en", limit: 10, chapter: "01") {
    status
    message
    data {
      query
      lang
      total
      source
      results {
        id
        code
        title
        chapter
        score
      }
    }
  }
}
```

`lang` defaults to English, `limit` defaults to 10 and is capped at 50, and
`chapter` is optional. The ICD-11 wrapper may return Meilisearch-backed results
or fall back to the WHO ICD-11 API. This search result is a suggestion; the
backend's existing `addDiagnosis` mutation still accepts a name and optional
code independently and does not verify that submitted codes came from this
search.

## SNOMED CT symptom suggestions

`searchSnomedSymptoms` proxies the same wrapper's SNOMED CT endpoint
(`GET /api/v1/snomed/search`) and backs the consultation's signs-and-symptoms
field. Same roles as above:

```graphql
query SearchSymptoms {
  searchSnomedSymptoms(query: "headache", limit: 10) {
    status
    message
    data {
      query
      limit
      total
      source
      releaseVersion
      results {
        id
        preferred
        synonyms
        fsn
        type
        common
        bodySystem
        score
      }
    }
  }
}
```

`id` is the SNOMED CT concept id (SCTID) and is what `addSymptom` stores as
`sonomedId`; `preferred` is the term shown to the clinician. `type` is the
optional SNOMED provenance filter (`reported`, `physical` or `finding`) — leave
it unset for symptom search, since everyday complaints such as headache, fever
and cough are indexed as `finding`. `limit` defaults to 10 and is capped at 50.

`bodySystem` is the anatomical/physiological grouping (SNOMED ancestors directly
under *Clinical finding*), e.g. `["Neurological finding", "Head finding"]`. It is
populated for every indexed concept and is **display only**: the suggestion list
in the UI shows it instead of the SCTID, but nothing about it is written to
NexxClinic — a saved symptom keeps `symptomName`, `sonomedId` and `notes`.
`findingSite` (the precise structure, when SNOMED asserts one) is carried by the
wrapper too, but only ~0.8% of in-scope concepts have it and always as a raw
SCTID, so it is not exposed here.

Unlike the ICD-11 endpoint there is no fallback backend: when the wrapper or its
Meilisearch index is down the query answers
`Symptom search service is unavailable.` rather than silently returning nothing.
