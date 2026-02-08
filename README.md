# Aegis: Your Autonomous AI Security Companion

Aegis is an autonomous AI-powered security companion designed to shield users from the growing epidemic of social engineering scams. Unlike traditional security tools that focus on malware or network filtering, Aegis operates at the **human interaction layer**. 

By leveraging Android's Accessibility Services and Gemini's multimodal reasoning, Aegis provides a "Zero-Trust" monitoring environment that analyzes real-time chat patterns and video call streams to detect psychological manipulation and visual deception in sophisticated scams like **Digital Arrest**, **Sextortion**, and **Financial Fraud**.

## 🛡️ Core Defense Pillars

Aegis provides three layers of real-time protection:

### 1. Autonomous Video Monitoring (Multimodal Vision)
Aegis utilizes **Gemini 3 Flash** to analyze live video call frames. It specifically identifies visual indicators of scams, such as:
- Fake police uniforms, caps, or official-looking badges.
- Backgrounds simulating police stations, flags, or official government seals.
- Visual inconsistencies suggestive of deepfakes or looped video feeds.

### 2. Grounded Chat Analysis (Deep Reasoning)
When a user interacts with unknown numbers or suspicious messages, Aegis employs **Gemini 3 Pro** with **Google Search Grounding** to:
- Verify official protocols (e.g., "Do banks use personal WhatsApp numbers for bail?").
- Detect psychological hooks (urgency, secrecy, authority).
- Identify links to known malicious domains or unofficial portals.

### 3. Phishing Link Protection
Aegis intercepts suspicious URLs in real-time, generating forensic takedown reports. It cross-references current events and brand protocols using real-time search results to distinguish between legitimate communications and targeted phishing.

## ⚙️ Technical Architecture

Aegis uses a sophisticated state-based logic to balance security with performance:

- **State Deduplication**: Uses the Jaccard Index to compare screen states and suppress redundant AI analysis:
  $$J(A, B) = \frac{|A \cap B|}{|A \cup B|}$$
- **Zero-Trust Monitoring**: Operates as an Android Accessibility Service to provide persistent background protection.
- **BYOK (Bring Your Own Key)**: Includes a fail-safe mechanism allowing users to provide their own Gemini API key in case of public rate limiting.

## 🚀 Getting Started

### Prerequisites
- Android 9.0+
- Accessibility Permissions enabled for Aegis
- Gemini API Key (Optional: Can be provided in Settings -> Advanced)

### Installation
1. Install the Aegis APK.
2. Launch the app and follow the onboarding to enable Accessibility Services.
3. Aegis will run silently in the background, only intervening when a threat is detected.

## 🏗️ Technology Stack
- **Framework**: Compose Multiplatform (KMP)
- **AI Engine**: Google Gemini 3 (Pro & Flash)
- **Tools**: Google Search Grounding, Structured JSON Output
- **Language**: Kotlin

