#!/system/bin/sh
# iPadOS Lockscreen Mod - Boot Service
MODDIR=${0%/*}

(
    until [ "$(getprop sys.boot_completed)" = "1" ]; do
        sleep 3
    done

    # Ensure secure settings for AOD & Lockscreen transitions
    settings put secure aod_mode 1
    settings put secure always_on_display_v2 1
    settings put secure doze_always_on 1

    cur_show_mode=$(settings get secure aod_show_mode)
    case "$cur_show_mode" in
        ""|null) settings put secure aod_show_mode 0 ;;
    esac
) &
