#!/sbin/sh
# iPadOS Lockscreen Mod - HyperOS Tablet Edition
# Copyright (C) 2026 Spider, Antigravity

ui_print "
======================================================
  _ ____            _  ____   ____  
 (_|  _ \  __ _  __| |/ __ \ / ___| 
 | | |_) |/ _  |/ _  | |  | |\___ \ 
 | |  __/| (_| | (_| | |__| | ___) |
 |_|_|    \__,_|\__,_|\____/ |____/ 
                                    
    iPadOS Lockscreen - HyperOS Tablet Edition (v1.0)
======================================================
  Device: Redmi Pad Pro 5G (ruan) & HyperOS Tablets
  Target: HyperOS 3 (Native), HyperOS 2 & 4 (Cross-Compatible)
  Credits: Spider, Antigravity, Mods Center, BlassGO
======================================================
"

# --------------------------------------------------- Environment Check
check_hyperos() {
    sdk=$(getprop ro.build.version.sdk)
    case "$sdk" in ''|*[!0-9]*) sdk=0 ;; esac
    if [ "$sdk" -lt 34 ]; then
        abort "! Android $(getprop ro.build.version.release) (SDK $sdk) is too old: Requires HyperOS 1/2/3/4 (Android 14+)."
    fi

    os_ver=$(getprop ro.mi.os.version.name)
    [ -z "$os_ver" ] && os_ver=$(getprop ro.build.version.incremental)
    ui_print "- HyperOS Build: $os_ver (SDK $sdk)"

    characteristics=$(getprop ro.build.characteristics)
    case "$characteristics" in
        *tablet*) ui_print "- Form Factor: Tablet (Pad) detected" ;;
        *) ui_print "- Form Factor: Phone/Pad detected" ;;
    esac
}

check_hyperos

# --------------------------------------------------- Extract Files
ui_print "- Extracting module assets..."
[ -d "$MODPATH/system" ] || mkdir -p "$MODPATH/system"

# Extract system overlays, fonts, wallpapers, and configs
if [ -d "$ZIPFILE" ]; then
    cp -af "$ZIPFILE/system" "$MODPATH/" 2>/dev/null
else
    unzip -qo "$ZIPFILE" "system/*" -d "$MODPATH" 2>/dev/null
fi

# --------------------------------------------------- Device Features Tuning
device_codename=$(getprop ro.product.device || getprop ro.boot.product.hardware.sku || getprop ro.boot.hwname)
features_path="$MODPATH/system/product/etc/device_features"
mkdir -p "$features_path"

ui_print "- Optimizing device features for iPadOS lockscreen ($device_codename)..."
for device_features in "/product/etc/device_features/"*.xml; do
    [ -f "$device_features" ] || continue
    out="$features_path/$(basename "$device_features")"
    cp -f "$device_features" "$out"
    
    # Enable necessary AOD, ambient, and lockscreen depth capabilities
    for key in support_aod support_aod_notification_animate aod_support_keycode_goto_dismiss \
               support_screen_color_persist doze_display_state_supported doze_proximity_check_before_pulse_intent; do
        sed -i "/<bool name=\"$key\">/d" "$out"
        sed -i "/<\/features>/ i\\    <bool name=\"$key\">true</bool>" "$out"
    done

    # Tablets should not force fullscreen OLED AOD (avoids LCD backlight drain)
    sed -i "/<bool name=\"support_aod_fullscreen\">/d" "$out"
    grep -q '</features>' "$out" || { ui_print "! Warning editing $(basename "$device_features"), falling back"; rm -f "$out"; }
done

# --------------------------------------------------- Font & Layout Deployment
ui_print "- Deploying Apple SF Pro Rounded & New York typography..."
if [ -d "$MODPATH/system/product/fonts" ]; then
    chmod 0644 "$MODPATH/system/product/fonts/"*.ttf 2>/dev/null
    for font in MiHaus.ttf MiSansRCF.ttf MiSansSquareF.ttf; do
        [ -e "$MODPATH/system/product/fonts/$font" ] || ln -sf /product/fonts/MiSansVF.ttf "$MODPATH/system/product/fonts/$font"
    done
fi

ui_print "- Deploying iPadOS Lockscreen Standalone App & Customizer..."

# --------------------------------------------------- Set Permissions
ui_print "- Setting file permissions..."
set_perm_recursive "$MODPATH" 0 0 0755 0644
set_perm "$MODPATH/service.sh" 0 0 0755
set_perm "$MODPATH/post-fs-data.sh" 0 0 0755

ui_print "
======================================================
  Installation Successful!
  Reboot your device to activate the iPadOS Lockscreen.
======================================================
"
