# Lab Record — Answer Entry Guide

Who is this for
- Clinicians and data entry staff using the consultation UI to record lab results.

Quick summary
- If a lab field is configured with `valueUnit`, enter the numeric/text value in the `Value` column and choose the appropriate `Unit` from the dropdown (or `None` if the row is configured with `unitMode: none`).
- If a lab field is configured with `result`, select the result value (e.g. `+ve` / `-ve`) from the dropdown in the `Result` column. The UI hides the unit/status column in this mode for clarity.

Step-by-step
1. Locate the lab panel (labeled by the field name) in the consultation form.
2. For each row (test name):
   - `valueUnit` layout: type the measured value (numbers or text) into the `Value` field; then pick a unit if required.
   - `result` layout: pick from the result options (e.g., `+ve`, `-ve`).
3. Changes are autosaved as part of the consultation clinical snapshot — you do not need to press a separate save for individual rows.

Tips
- Use the `result` layout for rapid/qualitative tests (positive/negative) to save screen space and reduce errors.
- If the unit dropdown is configured, preferred units will appear first (the `defaultUnit`). If the unit is not applicable, the row can be configured with `unitMode: none`.

Data quality
- Avoid free-text units; prefer selecting from `unitOptions` to ensure consistent data for reporting.
- For numeric values, use the unit that matches your reporting conventions to avoid downstream conversion errors.

Known behavior
- `result` layout renders two columns only (Name + Result); the status/unit column is intentionally hidden.
- Answers are stored under the form field id as a `rows` object with row ids as keys.

If something looks wrong
- If a row's name is incorrect, edit the form definition in the admin Form Builder — row names are part of the saved form and not editable within a consultation.
- If units are missing, ask an administrator to update `unitOptions` and `defaultUnit` in the admin builder.

Contact
- For policy or backend mapping questions, see docs/department-forms-backend-integration.md or contact the platform admin.
