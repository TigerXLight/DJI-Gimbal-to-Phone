# Public release sanitisation review

Date: 2026-10-25

## Scope

This review covers every tracked file in the Android-DJI-OM-Bridge production
repository at the current release-review HEAD and the complete reachable Git
history. It is a source-publication review, not a
hardware or penetration test. Ignored local capture directories were checked
for tracking status but are not part of the public source tree.

## Findings and fixes

The working tree contained no OM8 device address as device data, credentials,
access tokens,
raw bugreports, raw HCI archives, private project-memory files, or private
research binaries in tracked files. The observed hardware address was not
found in the current tree or any reachable commit. The synthetic
`AA:BB:CC:DD:EE:FF` value in a unit-test fixture is test data, not a device
identifier.

Four public-documentation references exposed local Windows paths. A fifth
project-state line named a private historical freeze. They were replaced with
repository-relative wording and a generic description of the separate
research/freeze material:

- `LICENSE_MAP.md`
- `DEPENDENCIES_AND_PROVENANCE.md`
- `docs/SOURCE_PROVENANCE.md`
- `docs/TESTING.md`
- `PROJECT_STATE.md`

The ignored `HCI_Captures/` and `PC_OM8_Captures/` directories remain local
diagnostic material and are not tracked. They must stay excluded from any
manual upload or release archive.

## History review

All reachable commits currently use the author identity
`TigerXLight <fahad.malik11@outlook.com>`. This is not a source secret, but it
is personal metadata that should not be published without the owner's
decision. The history also contains earlier versions of the now-corrected
private documentation paths. No history rewrite was performed.

Before public GitHub publication, choose one of these owner-approved options:

1. publish a new sanitized history with an intentional public author identity;
2. keep the existing history private and publish a clean public repository
   snapshot; or
3. explicitly accept the existing metadata and path history.

The first two options are preferable for a public release. A history rewrite
must be performed deliberately outside this working task and reviewed before
publication.

## Remaining publication concerns

- The repository does not yet contain a top-level project `LICENSE`.
- The project licence has not been selected by the repository owner.
- OpenGimbal MIT notices are present for the adapted mapping/lifecycle boundary;
  no GPL or no-license source was copied into production.
- The two DJI UXSDK repositories and the broader research catalogue are
  research-only and are not runtime dependencies.
- A public release must be assembled from tracked files only, excluding local
  build output, `local.properties`, IDE state, ignored captures, and research
  workspaces.

## Recommendation

Working-tree source sanitisation: **PASS**, after the path-reference fixes in
this change.

Public publication readiness: **FAIL / NOT READY**. The remaining blockers are
owner approval of the project licence and an explicit decision about the
personal Git history identity/path history. Hardware acceptance is also still
pending, although it is a separate product-validation gate.

## Recheck commands

Run from the repository root before creating a public archive:

```powershell
git ls-files
rg -n -i -S 'C:\\Users|\.codex-remote-attachments|PROJECT_MEMORY|FROZEN_REFERENCE' (git ls-files)
git status --short --ignored
git log --all --format='%H %an <%ae> %s'
```

The audit document intentionally contains generic search-pattern terms. It does
not contain the observed hardware address or a concrete private filesystem
path. The command should still be reviewed rather than blindly treated as a
zero-match rule: research/freeze terms may be appropriate in sanitized
historical documentation, while personal filesystem paths and device
identifiers are not.
