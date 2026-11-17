# Third party notices

CleanYourTV by Gathin and contributors: MIT license, retained in LICENSE.
Source: https://github.com/Gathin23/cleanyourtv/tree/ddc3b2cdce299a5f30aa2da2437edc40f3cf6f2e/launcher
Imported source hashes are in docs/upstream/provenance.json.

The imported launcher includes the Outfit font and its SIL Open Font License at
launcher/FONT-LICENSE-OFL.txt. AndroidX dependencies retain their Apache 2.0 terms.
No Luncher code has been copied.

Projectivy wallpaper-provider API by Mickael Despesse (Spocky) and contributors:
Apache License 2.0, retained in launcher/projectivy-api/LICENSE and packaged as
assets/licenses/projectivy-api-apache-2.0.txt. Source revision and SHA-256 hashes
are in launcher/projectivy-api/provenance.json. Original AIDL and parcel classes
are unchanged. This is an independent compatible host, without Projectivy branding
or launcher implementation code.
Source: https://github.com/spocky/projectivy-plugin-wallpaper-provider/tree/60ec7b4306ab16dac83ea10ed9b81f5670b51ee3

Lottie Android by Airbnb and contributors: Apache License 2.0.
Source: https://github.com/airbnb/lottie-android/tree/v6.6.2

## About source QR assets and verification

The four offline source-link QR images are generated from public URLs with Python
qrcode 8.2. The encoder is not included in the APK. Asset URLs, checksums and
settings are recorded in `dev/about-qr-provenance.json`.
ZXing Core 3.5.4 (Apache-2.0) generates temporary phone-pairing QR codes in the
APK and decodes QR assets in JVM tests: https://github.com/zxing/zxing/releases/tag/zxing-3.5.4.

## Phone widget editor

NanoHTTPD 2.3.1: BSD 3-Clause / Modified BSD. License is retained and packaged as
`assets/licenses/nanohttpd-bsd-3-clause.txt`. The embedded core serves only bundled
editor assets and an authenticated configuration API; it is not the project's
file server or a device-administration interface.
Source: https://github.com/NanoHttpd/nanohttpd/tree/nanohttpd-project-2.3.1

ZXing Core 3.5.4: Apache License 2.0. The Apache 2.0 text is packaged under
`assets/licenses/projectivy-api-apache-2.0.txt`; upstream copyright notices remain
in the dependency. Source: https://github.com/zxing/zxing/tree/zxing-3.5.4
