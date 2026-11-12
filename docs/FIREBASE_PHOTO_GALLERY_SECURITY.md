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
4. Verify Storage and Firestore rules together. The development Storage rule now checks the matching Firestore photo record: approved photos are readable by signed-in users, pending photos only by their uploader or a trusted moderator. Publishing cross-service rules requires enabling the Cloud Storage service account's Firestore read permission in Firebase. Test this in the Emulator Suite before production.
5. Deploy with Firebase CLI only after the tests pass and the project owner approves.
6. Do not add the upload UI or announce shared galleries as live until deployment and two-account device tests pass.

## Development implementation status

The Android development branch now includes a native Firebase bridge, location-specific gallery UI, upload with pending status, uploader attribution, reporting, and moderator-only approval/removal. The app does not bypass Firebase Authentication. The client code and rules have not been emulator-tested together.

**Not deployed; not production-ready.** Rules must be reviewed and deployed by the Firebase project owner, the moderator custom claim must be assigned through a trusted admin process, and cross-account upload/read/report/moderation tests must pass before the gallery is treated as live.
