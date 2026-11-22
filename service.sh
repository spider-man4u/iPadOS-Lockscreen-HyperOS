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

    # Ensure permissions for iPadOS Lockscreen standalone app
    pm grant com.ipados.lockscreen android.permission.CAMERA 2>/dev/null
    pm grant com.ipados.lockscreen android.permission.READ_EXTERNAL_STORAGE 2>/dev/null
    pm grant com.ipados.lockscreen android.permission.SYSTEM_ALERT_WINDOW 2>/dev/null

    # Launch iPadOS Lockscreen background service
    am start-foreground-service -n com.ipados.lockscreen/.LockScreenService 2>/dev/null || \
    am startservice -n com.ipados.lockscreen/.LockScreenService 2>/dev/null
) &
