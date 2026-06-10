# TutorHub Secure Exam: Code Signing & SmartScreen Plan

This document outlines the strategy for digitally signing executable files for the TutorHub Secure Exam application, mitigating Windows SmartScreen warnings, and preventing false positives from Antivirus (AV) software.

## A. Target Files for Code Signing

The following executables must be digitally signed before release to production:

1. **`tutorhub_lockdown.exe` (Rust Lockdown Core)**: The core executable that performs desktop isolation and low-level API hooks. This is highly critical as it behaves similarly to malware (hooks, desktop switches) and is prone to AV flags.
2. **`TutorHubSecureExamSetup.exe` (Full Installer)**: The Inno Setup generated installer containing the complete JRE.
3. **`TutorHubSecureExamSetup-jlink.exe` (Mini Installer)**: The Inno Setup generated installer containing the jlink minimized JRE.
4. **Any future `Launcher.exe` wrapper**: If the `.bat` launcher is ever converted to an `.exe` wrapper, it must also be signed.

## B. Certificate Types & Recommendations

1. **OV (Organization Validation) Code Signing Certificate**:
   - Requires vetting of the organization's identity.
   - Eliminates "Unknown Publisher" warnings.
   - *Limitation*: Does NOT immediately bypass Windows SmartScreen. The application still needs to build "reputation" over time through downloads and usage.
2. **EV (Extended Validation) Code Signing Certificate**:
   - Requires rigorous identity and physical hardware token (USB token or Cloud HSM) validation.
   - *Advantage*: Immediately establishes a high reputation with Windows SmartScreen, effectively bypassing the "Windows protected your PC" blue screen warning from day one.
   - **Recommendation for Production**: Highly recommended for a smooth user experience.
3. **Self-Signed Certificate**:
   - For internal testing/lab only. Will be blocked or heavily flagged on end-user machines.

## C. Tooling

- **Tool**: `signtool.exe` (included in the Windows SDK).
- **Algorithm**: SHA-256 (`/fd sha256`).
- **Timestamping**: Crucial to ensure the signature remains valid even after the certificate expires. We will use a reliable public timestamp server (e.g., `http://timestamp.digicert.com`).
- **Verification**: `signtool verify /pa /v <file>`.

## D. Recommended CI/CD Signing Workflow

Because the application relies heavily on `tutorhub_lockdown.exe` operating cleanly, the signing process must happen in stages:

1. Build Rust release binary (`cargo build --release`).
2. **Sign** `tutorhub_lockdown.exe`.
3. Copy the signed Rust binary into the Maven project resources (`src/main/resources/tools/`).
4. Build the Maven fat JAR (`mvn clean install`).
5. Build the Portable Folders (`build_portable.ps1`).
6. Build the Installers (`build_installer.ps1`).
7. **Sign** the final Installer executables (`TutorHubSecureExamSetup.exe` and `TutorHubSecureExamSetup-jlink.exe`).
8. Verify all signatures before publishing.

## E. Secret Management & Security

- **Strict Rule**: NEVER commit `.pfx`, `.p12`, or `.key` files to the repository.
- **Strict Rule**: NEVER hardcode certificate passwords in build scripts.
- **Implementation**:
  - In local development, the certificate should ideally be installed into the Windows Certificate Store and referenced by its SHA-1 Thumbprint or Subject Name.
  - In automated CI/CD pipelines (e.g., GitHub Actions), the certificate should be stored as an encrypted secret, decoded at runtime, and the password passed securely via environment variables (e.g. `TSE_CERT_PASSWORD`).

## F. SmartScreen Limitations

- Code signing proves *who* published the software and that it hasn't been tampered with. It does not automatically mean Windows trusts it (unless using EV).
- With an OV certificate, users may still see the SmartScreen warning initially. Instruct users to click "More info" -> "Run anyway".

## G. Antivirus (AV) False Positives Mitigation

The `tutorhub_lockdown.exe` utilizes `SetWindowsHookEx` (keyboard hooking) and `SwitchDesktop` (screen isolation). These are classic heuristics for keyloggers and malware.
- **Mitigation 1**: Code signing is the first line of defense. AVs are less likely to flag signed binaries from known publishers.
- **Mitigation 2**: If flagged, submit the signed installer and the standalone Rust binary to Microsoft Defender Security Intelligence and other major AV vendors (Kaspersky, Bitdefender) as a "False Positive". Include the documentation explaining its educational/exam security purpose.
