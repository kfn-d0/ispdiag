# find_sdk.ps1 - Discovers Android SDK tools for manual build
# Returns a hashtable with paths to: AAPT2, JAVAC, D8, ZIPALIGN, APKSIGNER, ANDROID_JAR, PLATFORM_TOOLS

function Find-AndroidSdk {
    # 1. Find SDK root (validate by checking for build-tools subdir)
    $sdkCandidates = @(
        "$env:LOCALAPPDATA\Android\Sdk",
        $env:ANDROID_SDK_ROOT,
        $env:ANDROID_HOME,
        "C:\Android\Sdk"
    )

    $sdkRoot = $null
    foreach ($c in $sdkCandidates) {
        if ($c -and (Test-Path $c) -and (Test-Path "$c\build-tools")) {
            $sdkRoot = $c
            break
        }
    }

    if (-not $sdkRoot) {
        throw "Android SDK not found. Set ANDROID_HOME or ANDROID_SDK_ROOT."
    }
    Write-Host "[SDK] Root: $sdkRoot" -ForegroundColor Cyan

    # 2. Find latest build-tools
    $btDir = Get-ChildItem "$sdkRoot\build-tools" -Directory | Sort-Object Name -Descending | Select-Object -First 1
    if (-not $btDir) { throw "No build-tools found in $sdkRoot\build-tools" }
    $btPath = $btDir.FullName
    Write-Host "[SDK] Build-tools: $($btDir.Name)" -ForegroundColor Cyan

    # 3. Find latest platform (android.jar)
    $platDir = Get-ChildItem "$sdkRoot\platforms" -Directory | Sort-Object Name -Descending | Select-Object -First 1
    if (-not $platDir) { throw "No platforms found in $sdkRoot\platforms" }
    $androidJar = "$($platDir.FullName)\android.jar"
    if (-not (Test-Path $androidJar)) { throw "android.jar not found at $androidJar" }
    Write-Host "[SDK] Platform: $($platDir.Name)" -ForegroundColor Cyan

    # 4. AAPT2
    $aapt2 = "$btPath\aapt2.exe"
    if (-not (Test-Path $aapt2)) { throw "aapt2.exe not found at $aapt2" }

    # 5. JAVAC
    $javac = (Get-Command javac -ErrorAction SilentlyContinue).Source
    if (-not $javac) { throw "javac not found in PATH. Install JDK." }
    Write-Host "[SDK] javac: $javac" -ForegroundColor Cyan

    # 6. D8
    $d8 = "$btPath\d8.bat"
    if (-not (Test-Path $d8)) {
        # Try d8.jar
        $d8 = "$btPath\lib\d8.jar"
        if (-not (Test-Path $d8)) { throw "d8 not found in $btPath" }
    }

    # 7. ZipAlign
    $zipalign = "$btPath\zipalign.exe"
    if (-not (Test-Path $zipalign)) { throw "zipalign.exe not found at $zipalign" }

    # 8. ApkSigner
    $apksigner = "$btPath\apksigner.bat"
    if (-not (Test-Path $apksigner)) { throw "apksigner.bat not found at $apksigner" }

    return @{
        SDK_ROOT    = $sdkRoot
        AAPT2       = $aapt2
        JAVAC       = $javac
        D8          = $d8
        ZIPALIGN    = $zipalign
        APKSIGNER   = $apksigner
        ANDROID_JAR = $androidJar
        BUILD_TOOLS = $btPath
        PLATFORM    = $platDir.Name
    }
}
