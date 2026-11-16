# LENSORA_AI MASTER ARCHITECTURE

## 1. HARDWARE & CONSTRAINTS
- Device: Redmi Note 13 Pro 5G (8GB RAM, Android 16, HyperOS 3.0, 200MP sensor).
- Constraints: 100% offline AI, no cloud API costs, zero OOM/HAL crashes.
- Probe Audit Finding: 200MP direct capture is FAILED on this hardware. Fallback to 12.5MP capture + Real-ESRGAN AI upscale for Ultra-Res.

## 2. ARCHITECTURE (L0-L4)
- L0 Safe Core: Camera2, capture journal (.tmp rename), thermal guard, storage guard. NEVER depends on AI.
- L1 Light AI: 500ms (2 FPS) scene classification, pose detection.
- L2 Planner: Rule-based orchestrator.
- L3 Heavy AI (Serial queue, 1 at a time): w8a8 Real-ESRGAN, LaMa-Dilated, MobileSAM, BioCLIP Mobile.
- L4 Validator: Enforces Anti-AI Rulebook (no halos, preserve skin texture, max +/-15 color shift).

## 3. UI/UX (OBSIDIAN ROYAL)
- Colors: Background #0E0E0E, Surface #131313, Primary #2563EB.
- 5 Tabs: Camera, Studio, Gallery, Vibes, Settings.
- Flat UI, standard Material icons, zero gradients.

## 4. FORBIDDEN LIST
- NO CameraX.
- NO SAF for Drive (Use REST API v3).
- NO Stable Diffusion.
- NO full 200MP Bitmap loading (Use Tiled 12.5MP bursts).
