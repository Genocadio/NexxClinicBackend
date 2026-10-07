# ICD-11 disease suggestions in GraphQL

The backend can proxy disease and ICD-11 code autocomplete from the NexxICD
wrapper. Configure `NEXXICD_URL` as the wrapper's base URL (for example,
`http://localhost:8082`). When it is unset, the GraphQL query returns an explicit
configuration error.

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
