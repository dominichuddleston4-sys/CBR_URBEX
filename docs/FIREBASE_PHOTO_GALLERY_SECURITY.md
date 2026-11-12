# Firebase photo gallery backend — deployment gate

These rules are a starting security boundary for the planned shared location galleries. They are committed for review only; committing rules does **not** deploy them to the Firebase project.

## Data model

- Metadata: `locations/{locationId}/photos/{photoId}`
- Storage object: `locationPhotos/{locationId}/{uploaderUid}/{fileName}`
- New uploads must be created with `status: "pending"`; only a trusted moderator may approve/update or delete metadata.
- Approved metadata is visible to signed-in users. Pending metadata is visible only to its uploader and moderators.
- Reports: `locations/{locationId}/photoReports/{reportId}`
- Moderators require a trusted Firebase Authentication custom claim `moderator: true`, assigned only by a trusted admin environment.

## Before deployment

1. Confirm the Firebase project and default Firestore database are provisioned.
2. Review these rules against the final client data model.
3. Add Firebase Emulator Suite tests for unauthenticated reads/writes, cross-user writes, invalid MIME types, files over 10 MiB, pending/approved visibility, reports, and moderator-only actions.
4. Verify Storage and Firestore rules together. The current Storage rule allows authenticated users to read image objects; the app must only expose object URLs for approved photo metadata. If strict object-level pending-image privacy is required, add and test a Firestore-backed Storage rule check before production.
5. Deploy with Firebase CLI only after the tests pass and the project owner approves.
6. Do not add the upload UI or announce shared galleries as live until deployment and two-account device tests pass.

## Status

**Not deployed; not production-ready.** These rules are not a substitute for emulator tests, a moderation workflow, or verification in the Firebase console.
