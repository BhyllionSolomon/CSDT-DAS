# Examination Management and Control — Implementation Specification

Target branch: `feature/examination-management`
Base: `render-deployment`

## Purpose

Provide a separate, auditable examination workflow for CSDT. This is not the existing teaching timetable. It covers exam-question preparation and confidential vetting, exam timetable extraction, dispatch custody, answer-script returns, lecturer collection and return of marked scripts, and student reminders.

## Roles and access

- HOD: owns the examination cycle, sets dates and deadlines, configures course rules, appoints/removes committee and dispatch members, authorises confidential access, sees all status and audit reports.
- Exam officer (if assigned by HOD): manages the department course catalogue and timetable import within permissions delegated by HOD.
- Lecturer/adjunct: sees only assigned course deadlines and their own paper workflow; uploads and revises own paper; sees own script collection/return history.
- Exam committee member: sees committee schedule and only papers assigned to the committee; records vetting outcome and correction requests.
- Dispatch team member: sees only dispatch schedule and custody tasks assigned to them; records collection from HOD's office and return of scripts.
- Student: sees only their own exam timetable, derived from registered courses, programme and level.
- Non-departmental staff who return scripts: can be recorded as the person returning a bundle without needing a CSDT-DAS account. Staff identity/name, unit and timestamp are captured by an authorised dispatch officer.

All access must be checked on the server, not merely hidden in the frontend.

## Workflow A — Examination calendar and question submission

1. HOD creates an examination cycle for academic session and semester and sets the first exam date, question submission deadline, vetting date, final correction deadline and any other milestones.
2. The system opens the lecturer task list seven weeks before the first examination date. Seven weeks is a configurable lead time, defaulting to 49 days.
3. Each allocated lecturer sees only their assigned courses, exact deadline and countdown. Reminders are sent in-app and by email where email is configured. Escalations and late status are visible to HOD.
4. Lecturer uploads the question paper. The system encrypts the original file before persistence, records its digest, uploader and timestamp, and blocks unauthorised retrieval.
5. Committee access is limited to HOD-appointed members and only after submission. Committee date and membership are visible only to HOD and committee members.
6. Committee records quality review, decision and specific correction notes. Lecturer receives the notes and can upload a revised version. Each version is retained; the previous version is not overwritten.
7. HOD sees submission, vetting, correction and resubmission status by course and lecturer. Reports show late, missing and outstanding papers.

## Workflow B — Question-paper validation

Validation is policy-driven per course, not hard-coded solely from course units.

Initial suggested defaults from the HOD's brief:
- 3-unit course: 6 questions; students answer 4; selected questions must total 70 marks.
- 2-unit course: 5 questions; students answer 3; selected questions must total 70 marks.

For each course, HOD/exam officer can set:
- number of questions;
- number students must answer;
- total examination marks;
- compulsory question numbers (if any);
- whether choice is unrestricted or restricted to defined groups;
- mark allocation and whether selected questions must total the required marks.

The validator must understand both compulsory-question papers and papers where students may choose any permitted combination. It should flag missing/incorrect institutional heading, missing or mismatched approved school logo, course code/title, unit, duration, instructions, question count, compulsory/choice instructions, question numbering, marks per question, and whether every permitted answer combination totals 70 marks.

A DOCX can be structurally inspected. A text-based PDF can be parsed. A scanned PDF needs OCR and may require human confirmation. Logo comparison and visual layout checks are advisory checks; the system must not claim perfect detection. The committee remains responsible for academic quality.

## Workflow C — Timetable import and student alerts

1. HOD/exam officer maintains the authoritative CSDT course catalogue and maps course code, title, unit, level, programme and semester.
2. Upload the school's official exam timetable as DOCX or PDF.
3. Extract timetable rows, match only department courses against the authoritative catalogue, and produce a review screen for ambiguous/unmatched rows.
4. HOD/exam officer confirms the extracted rows before publication. Never dispatch from unconfirmed OCR/parser output.
5. Publish the confirmed CSDT-only timetable to dispatch members and students.
6. Students receive their own exam dates based on registered courses, programme and level, with daily list and reminders. Do not show unrelated courses or other students' registrations.

## Workflow D — Physical dispatch and custody

For every CSDT exam sitting, maintain a custody record:
- paper/course, date, scheduled start time and venue where supplied;
- dispatch team members assigned;
- time papers were collected from HOD's office;
- staff member who physically collected/dispatched;
- time and confirmation of delivery to exam venue;
- number of answer scripts/bundles returned;
- name, unit/department and role of the person who returned scripts, including non-CSDT staff;
- time received, receiving officer and exceptions/shortages.

Every status change is timestamped and attributable. Corrections are appended as audit events rather than silently rewriting custody history.

## Workflow E — Lecturer collection and marked-script return

1. HOD sends a notice to all lecturers/adjunct lecturers to collect their answer scripts.
2. Lecturer acknowledges each course/bundle collected, with collection date/time and number of scripts.
3. Lecturer dashboard keeps a personal history of all collected courses and outstanding marking.
4. Lecturer records return of marked scripts, date/time, quantity and any outstanding scripts.
5. HOD dashboard shows totals received, collected, marked/returned and outstanding, grouped by course and lecturer.
6. Deadline reminders and escalation reports identify overdue staff and unresolved bundles. Reports should be factual, auditable and suitable for management review; the software should not automatically make salary or disciplinary decisions.

## Confidentiality and cryptographic design

- Encrypt every original and revised question-paper file before storing it in the database or object storage. Use authenticated encryption (AES-256-GCM), unique nonce per encryption, integrity verification and a separate key per examination cycle or paper.
- Use envelope encryption: a protected HOD-controlled master key wraps the data key. HOD explicitly grants named committee members access through server-side authorisation. A committee member never receives the HOD master key.
- Keep master keys outside source control and the database (Render secret/KMS or equivalent); define key rotation, backup and recovery before production use.
- Enforce view/download permissions on every request. Use time-limited access, watermark previews with user identity, disable ordinary public links, log views/downloads, and restrict exports.
- No web application can guarantee that an authorised person cannot photograph a screen or copy information manually. The system can make unauthorised access, bulk extraction and covert downloads substantially harder and provide evidence of access.
- Do not expose question-paper content, keys, or committee-only dates in student/lecturer APIs, notices, logs or error messages.

## Acceptance tests before production

- HOD-only cycle/date/policy configuration and committee membership.
- Lecturer cannot see another lecturer's paper or committee-only schedule.
- Committee member cannot see papers outside assigned committee scope.
- No plaintext paper in database, logs, API metadata or backups.
- Tampered ciphertext fails authentication.
- Seven-week opening and all reminder/escalation dates use Africa/Lagos time.
- Validation covers compulsory and unrestricted-choice combinations and confirms marks total correctly.
- DOCX/PDF timetable parsing is reviewed before publication; unmatched rows cannot silently become dispatch tasks.
- Student sees only registered courses matching programme/level/session/semester.
- Dispatch and answer-script custody cannot be silently deleted or backdated.
- Lecturer collection/return history and HOD outstanding totals reconcile.
- Email failure does not silently lose in-app reminders.
- Role-based integration tests and deployment smoke tests pass before merge.

## Delivery sequence

1. Examination cycle, course policies, secure question upload/versioning, deadline reminders, committee review and correction loop.
2. Question-paper structural validation and committee dashboard.
3. Official timetable import, CSDT course matching, confirmation and student timetable/reminders.
4. Dispatch and return custody register.
5. Lecturer script collection/marking/return register and HOD accountability reports.
6. Security review, automated tests, deployment verification and pilot feedback.

Do not merge to `render-deployment` or call the module live until the relevant acceptance tests and Render deployment checks pass.
