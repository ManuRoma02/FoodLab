param (
    [string]$ApkPath = "FoodLab_Latest.apk",
    [string]$Caption = "FoodLab Latest APK Build"
)
python scripts\send_apk_telegram.py $ApkPath $Caption
