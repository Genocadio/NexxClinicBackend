# Lab Record Field (labRecord)

Overview
- Purpose: capture structured laboratory results composed of multiple rows (test items) where each row contains a fixed `name` and an answer column (either `value` + `unit`, or a `result` like +ve / -ve).
- Use cases: Full blood count, CRP, Glycemia, other panel-style labs.

Field configuration (admin builder)
- `layout`: `valueUnit` or `result`
  - `valueUnit`: three columns — `Name` (fixed), `Value` (free text / number), `Unit` (dropdown or `none`).
  - `result`: two columns — `Name`, `Result` (enum / dropdown e.g. `+ve` / `-ve`). The status/unit column is hidden in this layout.
- `rows`: ordered list of row configs. Each row has:
  - `id` (string): unique identifier for the row (generated if omitted).
  - `name` (string): label shown in UI; configured in builder and stored with the form definition.
  - `unitMode` (`dropdown` | `none`): whether a unit dropdown is shown in `valueUnit` layout.
  - `unitOptions` (string[]): allowed unit values when `unitMode` is `dropdown`.
  - `defaultUnit` (string | undefined): selected by default in the dropdown.
  - `resultOptions` (string[]): options shown in `result` layout (e.g., [`+ve`, `-ve`]).

Admin builder behavior
- Adding a `labRecord` field opens a small editor where you can switch layout and add/remove rows.
- The field definition is stored through the form-management API as `type: labRecord` with a `labRecordConfig` payload.
- `valueUnit` layout shows Value + Unit columns in preview; `result` layout shows only Name + Result.
- Row `name` is part of the form definition (it is not editable by end-users during consultation).

Runtime / storage shape
- Answers are stored under the field ID as an object with `rows` keyed by row id:

```json
{
  "<fieldId>": {
    "rows": {
      "lab_row_1": { "value": "12.3", "unit": "mg/dL" },
      "lab_row_2": { "result": "+ve" }
    }
  }
}
```

- For `valueUnit` rows, `value` (string) and optional `unit` (string) are used.
- For `result` rows, `result` (string) holds the selected option.

Consultation UI notes
- `result` layout uses a compact two-column grid (Name + Result). The Unit/Status column is hidden.
- `valueUnit` layout shows Name, Value (input) and Unit (select, or `None` if `unitMode` is `none`).
- Default units are applied when available; users can change units from the dropdown.

Backend considerations
- The backend may expect different shapes; if your API requires a flattened array, map `rows` into an array before submission.
- When validating or migrating older forms, ensure `labRecordConfig` exists on `FormField` for compatibility.

Examples
- CBC (valueUnit): rows for Hemoglobin, WBC, Platelets with appropriate unitOptions.
- Rapid test (result): rows for HIV, Malaria with resultOptions [`+ve`, `-ve`].

See also
- Consultation answer guide: docs/consultation/lab-record-answer-guide.md
