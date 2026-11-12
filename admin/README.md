# Canberra Explorer Admin — preview

Separate Android application ID: `au.com.canberraexplorer.admin`.

This first build is a red-and-black UI preview only. It intentionally shows no fabricated user counts and does not expose any account controls.

## Required before live administration

1. Register the Android package `au.com.canberraexplorer.admin` in the existing Firebase project.
2. Configure Google sign-in for this package and add the generated `google-services.json` client.
3. Grant the owner's verified Firebase UID an administrator custom claim using a trusted, server-side process.
4. Deploy a protected backend using Firebase Admin SDK. Never put service-account credentials in the APK.
5. Implement user listing, active-session presence, disable/block, token revocation, immediate client-side forced sign-out checks, and an audit log.
6. Verify authorization and account actions with test accounts before enabling controls.

No billing-enabled Firebase or Google Cloud service is configured by this preview. Do not deploy paid resources without the owner's explicit approval.
