# Project licence review

Date: 2026-10-25

## Evidence reviewed

The current production provenance records are:

- `LICENSE_MAP.md`
- `DEPENDENCIES_AND_PROVENANCE.md`
- `THIRD_PARTY_NOTICES.md`
- `docs/SOURCE_PROVENANCE.md`

The detailed upstream records are maintained in the separate research
workspace's provenance manifest and adaptation ledger.

## Production code boundary

The Android application, OM8 driver, Sony adapter, profile model, mapping
model, persistence, tests, and documentation are local project work unless a
file-level attribution says otherwise.

The only direct/partial production adaptation is the mapping and lifecycle
boundary derived from `ItzDFPlayer/OpenGimbal` at commit
`6b7f403265d4ef96c9238ecaeed12bf5ce9f711c`, licensed MIT, copyright Denys.
The required MIT notice is already present in `THIRD_PARTY_NOTICES.md`.

The Pixel selector class records evidence from MIT-licensed
`AgusRomeroL/video-boost-ao`; it is not a copied service. The DJI UXSDK
repositories, OpenCamera, CameraRemote, and other GPL/no-license/research
projects were not copied into production. No Insta360 implementation is in
this repository.

## Compatibility assessment

The known incorporated upstream material is compatible with permissive
distribution provided the OpenGimbal copyright and MIT notice remain with the
adapted portion. The research-only repositories do not add distribution
obligations because their source was not incorporated.

This technical assessment does not establish who owns or may license all
original project code. The repository currently has no top-level `LICENSE`,
and the Git history uses a personal author identity. Those are release-owner
decisions, not facts that should be guessed by an implementation agent.

## Recommendation

MIT is the most straightforward candidate for the project if the repository
owner confirms authority to license the original work and accepts the existing
provenance boundary. It is compatible with the direct OpenGimbal adaptation
and keeps the public project simple. Apache-2.0 is another possible choice if
the owner wants an explicit patent grant, but it would still require the same
ownership decision and a complete notice review.

No top-level `LICENSE` file is added in this task because the owner has not
selected or authorized a project licence. Publication should remain blocked
until that choice is made and the selected licence text is added at the
repository root.

## Release gate

**Licence selection: PENDING OWNER DECISION**

Do not describe the repository as open source, publish a GitHub release, or
claim a final `1.0.0` licence state until the project licence and history
publication choice are settled.
