# Third-party notices

## DJI Mobile UXSDK repositories audited, not incorporated

The final source audit examined:

- <https://github.com/dji-sdk/Mobile-UXSDK-Android> at
  `6a28754964a7517a0602dfda0bad09adc938489a`. Its `LICENSE.txt` distinguishes
  the DJI UX SDK EULA from MIT-licensed sample code `(c) 2018 DJI`.
- <https://github.com/dji-sdk/Mobile-UXSDK-Beta-Android> at
  `ed5ca45d118efbada6716f372b96dc5fe11e0620`, whose `LICENSE.txt` identifies
  MIT and `(c) 2021 DJI`.

No source, binary, or runtime dependency from either repository is included in
this production project. The audit found DJI camera-command/UI paths such as
focus-ring and touch-focus setters, but no physical OM8 wheel transport. See
the research report for the file-level provenance and licence boundary.

## OpenGimbal mapping boundary

Phase 3A partially adapts the mapping/persistence boundary from:

<https://github.com/ItzDFPlayer/OpenGimbal>

Pinned upstream commit:

`6b7f403265d4ef96c9238ecaeed12bf5ce9f711c`

Copyright (c) 2026 Denys.

The upstream project is distributed under the MIT License. The following
notice is preserved for the adapted portion:

```text
MIT License

Copyright (c) 2026 Denys

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

No GPL, no-license, or other restricted upstream source was copied into the
Phase 3A production implementation.

## Phase 3B lifecycle attribution

Phase 3B independently implements the Android companion/presence and service
lifecycle boundary using the following MIT-licensed project as an attributed
design/source reference:

<https://github.com/ItzDFPlayer/OpenGimbal>

Pinned commit: `6b7f403265d4ef96c9238ecaeed12bf5ce9f711c`.

The adapted ideas are the connect-policy decision boundary, remembered-device
reconnect lifecycle, and bounded retry policy. The production implementation
is local Java code adapted to the proven DJI OM8 FFF4 notification driver. No
HJ360 wire implementation or camera-control fallback was copied.

## Pixel selector evidence

The local `PixelCameraProfileEvidence` constants are derived from the MIT-licensed
`AgusRomeroL/video-boost-ao` repository, commit
`0205b761dca8b033b755ac18421f25ad1ef2d426`, especially `Selectors.kt`. The
production class is a small attributed evidence record, not a copied service or
APK-resource generator. No Pixel camera control is enabled by this release.
