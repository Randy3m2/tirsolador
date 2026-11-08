# Native execution fixtures

The production APK contains no ROMs. `make_console_fixtures.py` creates original
CC0 diagnostics for NES, SNES, Mega Drive, GB, GBC and GBA. Nintendo DS uses the
existing original diagnostic in `make_test_rom.py`.

`n64-systemtest.zip` contains the MIT homebrew ROM built by the upstream author:
https://github.com/thelemmy/nemu64-test/actions/runs/34256763312
Source revision: `9a8b9f7d94ee2f6f57d7feed70c98c22cdc30e6c`.
Artifact `n64-systemtest-base-z64`, id 10068208947. The source README documents
the open-source IPL3; no Nintendo BIOS dump is needed. License: N64-LICENSE.txt.
Tests check frontend video/input/state integration, not correctness of every
low-level test inside the diagnostic or compatibility of every commercial ROM.

The MAME test downloads Gridlee from https://www.mamedev.org/roms/gridlee/gridlee.zip
at CI test time, with SHA-256 verification. MAMEdev permits distribution on its
site only. This ROM is neither committed nor shipped in production/release
artifacts. Test ROM assets and the instrumentation APK are not uploaded.
