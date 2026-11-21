#!/usr/bin/env python3
"""
iPadOS Lockscreen Template Patcher for HyperOS MIUIAod.apk
Compatible with HyperOS 2, 3, and 4
"""

import sys
import os
import json
import zipfile
import tempfile
import shutil

IPADOS_CLOCK_CONFIG = {
    "templateRes": "kg_clock_template_all_in_one_max",
    "templateConfig": {
        "templateName": "ipados_pad_style1",
        "clockInfo": {
            "templateId": "all_in_one",
            "style": 1,
            "classicLine1": 103,
            "clockWeight": 560,
            "fontStyle": 21
        }
    }
}

IPADOS_LAYOUT_CONFIG = {
    "version": 4,
    "homeInfo": {
        "followStatus": 2,
        "wallpaperInfo": {
            "source": "sysui_editor_wallpaper_group/os3_classic/classic01.jpg",
            "magicType": 0,
            "resourceType": "image"
        }
    },
    "lockscreenInfo": {
        "clockInfo": {
            "templateId": "all_in_one",
            "clockEffect": 5,
            "isAutoPrimaryColor": True,
            "isAutoSecondaryColor": True,
            "enableDiffusion": False,
            "primaryColor": -1,
            "secondaryColor": -1,
            "style": 1,
            "hollowStyle": 1,
            "fontStyle": 21,
            "clockWeight": 560,
            "classicLine1": 103,
            "isColonShow": True,
            "glassTransparency": 0.15
        },
        "wallpaperInfo": {
            "source": "sysui_editor_wallpaper_group/os3_classic/classic01.jpg",
            "magicType": 0,
            "resourceType": "image"
        },
        "templateName": "ipados_pad_style1",
        "version": 4
    }
}


def patch_aod_apk(input_apk, output_apk):
    print(f"[*] Reading source APK: {input_apk}")
    if not os.path.isfile(input_apk):
        raise FileNotFoundError(f"Input file not found: {input_apk}")

    tmp_dir = tempfile.mkdtemp(prefix="aod_patch_")
    try:
        with zipfile.ZipFile(input_apk, "r") as zin:
            zin.extractall(tmp_dir)

        # 1. Patch preset_clock_template_config.json
        preset_path = os.path.join(tmp_dir, "assets", "preset_clock_template_config.json")
        if os.path.exists(preset_path):
            with open(preset_path, "r", encoding="utf-8") as f:
                presets = json.load(f)

            # Insert into classic category
            inserted = False
            for cat in presets:
                if cat.get("titleId") == "kg_template_title_classic":
                    templates = cat.get("templates", [])
                    # Check if already present
                    if not any(t.get("templateConfig", {}).get("templateName") == "ipados_pad_style1" for t in templates):
                        templates.insert(0, IPADOS_CLOCK_CONFIG)
                        inserted = True
                        print("[+] Injected iPadOS clock template into kg_template_title_classic")
                    break

            if inserted:
                with open(preset_path, "w", encoding="utf-8") as f:
                    json.dump(presets, f, indent=2, ensure_ascii=False)

        # 2. Inject iPadOS layout JSON
        config_dir = os.path.join(tmp_dir, "assets", "config")
        os.makedirs(config_dir, exist_ok=True)
        ipados_json_path = os.path.join(config_dir, "kg_ipados_pad_style1.json")
        with open(ipados_json_path, "w", encoding="utf-8") as f:
            json.dump(IPADOS_LAYOUT_CONFIG, f, indent=2, ensure_ascii=False)
        print("[+] Injected assets/config/kg_ipados_pad_style1.json")

        # 3. Update kg_os4_pad_a_style1.json for native tablet override
        pad_a_path = os.path.join(config_dir, "kg_os4_pad_a_style1.json")
        if os.path.exists(pad_a_path):
            with open(pad_a_path, "r", encoding="utf-8") as f:
                pad_cfg = json.load(f)
            # Update clock weight & style
            clock_info = pad_cfg.get("lockscreenInfo", {}).get("clockInfo", {})
            clock_info["clockWeight"] = 560
            clock_info["fontStyle"] = 21
            clock_info["classicLine1"] = 103
            clock_info["isColonShow"] = True
            with open(pad_a_path, "w", encoding="utf-8") as f:
                json.dump(pad_cfg, f, indent=2, ensure_ascii=False)
            print("[+] Updated assets/config/kg_os4_pad_a_style1.json with iPadOS geometry")

        # 4. Repack APK
        print(f"[*] Repackaging modified APK to: {output_apk}")
        with zipfile.ZipFile(output_apk, "w", compression=zipfile.ZIP_DEFLATED) as zout:
            for root, _, files in os.walk(tmp_dir):
                for file in files:
                    full_p = os.path.join(root, file)
                    rel_p = os.path.relpath(full_p, tmp_dir)
                    # Don't compress already compressed media or dex
                    compress = zipfile.ZIP_STORED if rel_p.endswith((".png", ".jpg", ".webp")) else zipfile.ZIP_DEFLATED
                    zout.write(full_p, rel_p, compress_type=compress)

        print("[✓] MIUIAod.apk successfully patched!")
    finally:
        shutil.rmtree(tmp_dir, ignore_errors=True)


if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: python3 patch_aod.py <input_MIUIAod.apk> <output_MIUIAod.apk>")
        sys.exit(1)
    patch_aod_apk(sys.argv[1], sys.argv[2])
