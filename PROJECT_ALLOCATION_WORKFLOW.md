# Final-Year Project Allocation Workflow

## Roles

- **H.O.D.**: selects all project supervisors and separately marks the subset eligible to receive First Class students.
- **Project Coordinator / Assistant Project Coordinator**: generates the allocation draft, reviews and edits student-to-supervisor assignments, then publishes the approved schedule.
- **Students and staff**: can view the published schedule.

## Allocation rules

1. First Class students are assigned first, in strict round-robin order among only the supervisors marked First Class-eligible by the H.O.D.
2. Second Class Upper, Second Class Lower, Third Class and unclassified students are processed in that order.
3. Remaining students are distributed across all eligible supervisors, including First Class-eligible supervisors, by current total workload.
4. The proposal accounts for existing assignments when calculating supervisor loads.
5. If First Class students exist but the H.O.D. has not selected any First Class-eligible supervisor, generation stops with a clear error.

## Review and publication

- `Assign / Generate Draft` creates a persisted draft. It does not change the official supervision schedule.
- The Project Coordinator can move a student to another eligible supervisor. A First Class student can only be moved to a First Class-eligible supervisor.
- Each draft creation, move and publication is recorded in the allocation history.
- Publishing creates the official assignments and marks previously confirmed assignments for the same session as published.
- A published draft cannot be edited. A new draft can be generated if a fresh allocation round is required.

## Public schedule and privacy

The published schedule is available from **Published Project Allocation**. Its response contains only academic session, matriculation number, student name, programme, supervisor name and designation. It does not contain CGPA or degree classification.

The project management screen is restricted by the backend to the H.O.D, ADMIN, Project Coordinator and Assistant Project Coordinator. Student access to the public schedule is read-only.

## Database

The backend uses `spring.jpa.hibernate.ddl-auto=update`. On startup, Hibernate should create the three allocation workflow tables and add the `first_class_eligible` field to `staff_profiles`. Take a database backup before deploying schema changes to a live database.

## API routes added

- `POST /api/projects/drafts?sessionId=...`
- `GET /api/projects/drafts/latest?sessionId=...`
- `GET /api/projects/drafts/{draftId}`
- `PATCH /api/projects/drafts/{draftId}/items/{itemId}`
- `POST /api/projects/drafts/{draftId}/publish`
- `GET /api/projects/public-schedule?sessionId=...`

## Validation note

The changed frontend files pass ESLint. A full Vite build could not be completed in this Linux workspace because the uploaded `node_modules` contains Windows-only Rolldown native binaries. The Maven wrapper could not download Maven because Maven Central was unreachable from this workspace. Run the normal frontend build and Maven tests in the project's Windows/CI environment before deploying.
