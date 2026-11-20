# Examination security operations runbook

## Initial activation (required before any question-paper upload)

1. Generate a cryptographically random 32-byte key on a trusted machine:
   `node -e "console.log(require('crypto').randomBytes(32).toString('base64'))"`
2. Give custody of the generated value to the HOD. Keep an offline recovery copy in an institution-approved secure vault. Do not send it by email, WhatsApp, source control, or this chat.
3. In the Render backend service, add environment variable `APP_EXAM_MASTER_KEY` with the Base64 value. Never put the value in `application.properties` or Git.
4. Restart/redeploy the backend and verify the service starts. The application deliberately rejects question-paper uploads if the key is missing or malformed.
5. Limit Render environment-variable access to the small set of trusted platform administrators. The HOD controls committee appointments and per-paper grants, but a Render administrator with environment access is technically able to obtain the server-held key. This is not a hardware-backed HSM or zero-knowledge design.
6. Confirm that encrypted paper data is stored only in the database; do not enable public object links or public backup exports. Protect database backups with the institution's normal encryption and access policy.

## Committee access

- HOD appoints committee members for the examination cycle.
- Appointment alone does not decrypt a question paper.
- HOD must grant access to each individual paper and set an expiry.
- HOD can revoke an individual grant or remove a member from the cycle.
- Every grant, revocation, view, download, submission and review is audited.
- The download is stamped with the authorised user's identity and timestamp. This discourages casual redistribution; it cannot stop screenshots, photographs, or manual copying.

## Master-key rotation

The application supports a staged rewrap so the encrypted paper files do not need to be decrypted and re-encrypted.

1. Generate a new random Base64 32-byte key using the command above. Preserve the old key in the approved offline vault.
2. In Render, set `APP_EXAM_PREVIOUS_MASTER_KEY` to the old key and replace `APP_EXAM_MASTER_KEY` with the new key.
3. Restart/redeploy the backend. The service encrypts new uploads with the new key and can temporarily unwrap existing paper keys using the previous key.
4. Sign in as HOD and call the HOD-only **Rewrap all paper keys** action once. It rewraps every stored per-paper data key with the new active master key in one database transaction. Check the returned paper count.
5. Only after successful rewrap, remove `APP_EXAM_PREVIOUS_MASTER_KEY` from Render and restart the backend.
6. Verify that representative old and new papers can be opened. Retain the old key offline only according to the institution's approved recovery/retention policy.

If rewrap fails, do not remove the previous key. Restore the original active/previous environment values and investigate. Never delete either key while encrypted papers still depend on it.

## Limitations and production gate

- The HOD controls application authorisation; Render administrators remain trusted infrastructure operators.
- A browser cannot prevent an authorised person from photographing or manually copying a displayed paper.
- DOCX/PDF extraction checks are advisory. Scanned PDFs need OCR; logo, layout and academic quality require human review.
- Timetable extraction is provisional until HOD confirms each row.
- Configure Render backups and restrict access before real exam papers are uploaded.
- Test restoration of both the database and the matching master key before the first live examination cycle.
