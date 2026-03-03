# build.ps1 - Manual Android build (no Gradle)
# Builds ISP Access Diagnostic APK
param(
    [switch]$Clean,
    [switch]$Install
)

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path $PSScriptRoot -Parent
$AppDir = "$ProjectRoot\app"
$BuildDir = "$ProjectRoot\build"
$OutputAPK = "$ProjectRoot\ISPDiagnostic.apk"

# Load SDK tools
. "$PSScriptRoot\find_sdk.ps1"
$sdk = Find-AndroidSdk

Write-Host ""
Write-Host "========================================" -ForegroundColor Yellow
Write-Host "  ISP Access Diagnostic - Build" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Yellow
Write-Host ""

# Clean
if ($Clean -or -not (Test-Path $BuildDir)) {
    Write-Host "[BUILD] Cleaning..." -ForegroundColor Magenta
    if (Test-Path $BuildDir) { Remove-Item $BuildDir -Recurse -Force }
}
New-Item -ItemType Directory -Path $BuildDir -Force | Out-Null
New-Item -ItemType Directory -Path "$BuildDir\compiled_res" -Force | Out-Null
New-Item -ItemType Directory -Path "$BuildDir\classes" -Force | Out-Null
New-Item -ItemType Directory -Path "$BuildDir\dex" -Force | Out-Null
New-Item -ItemType Directory -Path "$BuildDir\apk_unsigned" -Force | Out-Null

# ----------------------------------------------------------------
# Step 1: Compile Resources with AAPT2
# ----------------------------------------------------------------
Write-Host "[1/7] Compiling resources..." -ForegroundColor Green

$resDir = "$AppDir\res"
$resFolders = Get-ChildItem $resDir -Directory
foreach ($folder in $resFolders) {
    $files = Get-ChildItem $folder.FullName -File
    foreach ($f in $files) {
        & $sdk.AAPT2 compile -o "$BuildDir\compiled_res" $f.FullName
        if ($LASTEXITCODE -ne 0) { throw "AAPT2 compile failed for $($f.Name)" }
    }
}
Write-Host "   Resources compiled." -ForegroundColor DarkGreen

# ----------------------------------------------------------------
# Step 2: Link Resources (generates R.java + base APK)
# ----------------------------------------------------------------
Write-Host "[2/7] Linking resources..." -ForegroundColor Green

$flatFiles = Get-ChildItem "$BuildDir\compiled_res" -Filter "*.flat" | ForEach-Object { $_.FullName }
$manifest = "$AppDir\AndroidManifest.xml"
$rJavaDir = "$BuildDir\r_java"
New-Item -ItemType Directory -Path $rJavaDir -Force | Out-Null

$linkArgs = @(
    "link",
    "--auto-add-overlay",
    "-I", $sdk.ANDROID_JAR,
    "--manifest", $manifest,
    "--java", $rJavaDir,
    "-o", "$BuildDir\apk_unsigned\base.apk"
)
$linkArgs += $flatFiles

& $sdk.AAPT2 @linkArgs
if ($LASTEXITCODE -ne 0) { throw "AAPT2 link failed" }
Write-Host "   R.java generated." -ForegroundColor DarkGreen

# ----------------------------------------------------------------
# Step 3: Compile Java sources
# ----------------------------------------------------------------
Write-Host "[3/7] Compiling Java sources..." -ForegroundColor Green

$javaSources = @()
# Collect R.java
$javaSources += (Get-ChildItem $rJavaDir -Recurse -Filter "*.java" | ForEach-Object { $_.FullName })
# Collect app sources
$javaSources += (Get-ChildItem "$AppDir\src" -Recurse -Filter "*.java" | ForEach-Object { $_.FullName })

# Write sources list without BOM (use .NET to avoid PowerShell BOM issues)
$sourcesFile = "$BuildDir\sources.txt"
[System.IO.File]::WriteAllLines($sourcesFile, $javaSources, (New-Object System.Text.UTF8Encoding $false))

& $sdk.JAVAC -source 1.8 -target 1.8 `
    -encoding UTF-8 `
    -classpath $sdk.ANDROID_JAR `
    -d "$BuildDir\classes" `
    "@$sourcesFile"
if ($LASTEXITCODE -ne 0) { throw "javac compilation failed" }
Write-Host "   Java compiled ($($javaSources.Count) files)." -ForegroundColor DarkGreen

# ----------------------------------------------------------------
# Step 4: Convert to DEX with D8
# ----------------------------------------------------------------
Write-Host "[4/7] Converting to DEX..." -ForegroundColor Green

$classFiles = Get-ChildItem "$BuildDir\classes" -Recurse -Filter "*.class" | ForEach-Object { $_.FullName }
$classListFile = "$BuildDir\classfiles.txt"
$classFiles | Out-File -FilePath $classListFile -Encoding UTF8

if ($sdk.D8 -like "*.bat") {
    & $sdk.D8 --output "$BuildDir\dex" --lib $sdk.ANDROID_JAR @classFiles
}
else {
    & java -jar $sdk.D8 --output "$BuildDir\dex" --lib $sdk.ANDROID_JAR @classFiles
}
if ($LASTEXITCODE -ne 0) { throw "D8 failed" }
Write-Host "   DEX created." -ForegroundColor DarkGreen

# ----------------------------------------------------------------
# Step 5: Package APK
# ----------------------------------------------------------------
Write-Host "[5/7] Packaging APK..." -ForegroundColor Green

$unsignedApk = "$BuildDir\apk_unsigned\base.apk"
# Add DEX to APK
Copy-Item $unsignedApk "$BuildDir\app_unsigned.apk" -Force

# Use jar to add classes.dex
Push-Location "$BuildDir\dex"
& jar -uf "$BuildDir\app_unsigned.apk" "classes.dex"
Pop-Location
if ($LASTEXITCODE -ne 0) { throw "Failed to add DEX to APK" }
Write-Host "   APK packaged." -ForegroundColor DarkGreen

# ----------------------------------------------------------------
# Step 6: ZipAlign
# ----------------------------------------------------------------
Write-Host "[6/7] Aligning APK..." -ForegroundColor Green

$alignedApk = "$BuildDir\app_aligned.apk"
& $sdk.ZIPALIGN -f 4 "$BuildDir\app_unsigned.apk" $alignedApk
if ($LASTEXITCODE -ne 0) { throw "ZipAlign failed" }
Write-Host "   APK aligned." -ForegroundColor DarkGreen

# ----------------------------------------------------------------
# Step 7: Sign APK
# ----------------------------------------------------------------
Write-Host "[7/7] Signing APK..." -ForegroundColor Green

$keystore = "$ProjectRoot\debug.keystore"
if (-not (Test-Path $keystore)) {
    Write-Host "   Generating debug keystore..." -ForegroundColor DarkYellow
    & keytool -genkeypair -v `
        -keystore $keystore `
        -alias androiddebugkey `
        -keyalg RSA -keysize 2048 `
        -validity 10000 `
        -storepass android `
        -keypass android `
        -dname "CN=Debug,O=ISPDiag,C=BR"
    if ($LASTEXITCODE -ne 0) { throw "Keystore generation failed" }
}

& $sdk.APKSIGNER sign `
    --ks $keystore `
    --ks-key-alias androiddebugkey `
    --ks-pass "pass:android" `
    --key-pass "pass:android" `
    --out $OutputAPK `
    $alignedApk
if ($LASTEXITCODE -ne 0) { throw "APK signing failed" }

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  BUILD SUCCESS!" -ForegroundColor Green
Write-Host "  APK: $OutputAPK" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green

# Optional: Install
if ($Install) {
    $adb = "$($sdk.SDK_ROOT)\platform-tools\adb.exe"
    if (Test-Path $adb) {
        Write-Host ""
        Write-Host "[INSTALL] Installing to device..." -ForegroundColor Cyan
        & $adb install -r $OutputAPK
        if ($LASTEXITCODE -ne 0) { Write-Host "[INSTALL] Failed." -ForegroundColor Red }
        else { Write-Host "[INSTALL] Success!" -ForegroundColor Green }
    }
    else {
        Write-Host "[INSTALL] adb not found." -ForegroundColor Red
    }
}
