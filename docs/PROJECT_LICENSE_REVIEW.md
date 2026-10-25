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

For this publication request, the project owner has selected MIT for the
project-owned/original production code. The top-level `LICENSE` applies only to
that project-owned scope; it does not relicense incorporated third-party code.
The GitHub publication mirror will sanitize the personal development email from
public commit metadata without changing the canonical local history.

## Recommendation

MIT is the selected project licence because the audited project-owned code can
be distributed under MIT and the only incorporated source boundary is also
MIT-compatible OpenGimbal material. Apache-2.0 code is not incorporated. The
top-level `LICENSE` has been added for the project-owned scope.

## Release gate

**Licence selection: MIT — RESOLVED FOR PUBLICATION**

The public repository may describe the project-owned scope as MIT-licensed while
retaining the separate OpenGimbal MIT attribution and all file/component-specific
notices. Publication still requires the sanitized mirror and the normal public
tree/privacy checks.
