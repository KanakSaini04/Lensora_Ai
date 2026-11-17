[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$Step,

    [Parameter(Mandatory = $false)]
    [string]$Base = "dev",

    [Parameter(Mandatory = $false)]
    [string[]]$Extra = @()
)

$ErrorActionPreference = "Continue"
Add-Type -AssemblyName System.Drawing

# 1. SAFETY: Resolve repo root via git
$rawRoot = (git rev-parse --show-toplevel 2>$null)
if (-not $rawRoot) {
    Write-Error "Failed to resolve git repository root!"
    exit 1
}
$rawRoot = $rawRoot.Trim()

$root = [System.IO.Path]::GetFullPath($rawRoot)
$claudeDir = [System.IO.Path]::GetFullPath((Join-Path $root "Claude"))
$zipPath = [System.IO.Path]::GetFullPath((Join-Path $root "Claude_review.zip"))

# Safety check: target MUST be exactly <root>\Claude
if ($claudeDir -ne (Join-Path $root "Claude")) {
    Write-Error "Safety check failed! Target dir is not exactly $root\Claude"
    exit 1
}

# Wipe existing Claude\ and Claude_review.zip
if (Test-Path $claudeDir) {
    Remove-Item -Path $claudeDir -Recurse -Force
}
if (Test-Path $zipPath) {
    Remove-Item -Path $zipPath -Force
}

New-Item -ItemType Directory -Path $claudeDir -Force | Out-Null

$problems = New-Object System.Collections.Generic.List[string]

# Create subdirectories
$gitDir = Join-Path $claudeDir "01_git"
$docsDir = Join-Path $claudeDir "02_docs"
$srcDir = Join-Path $claudeDir "03_changed_source"
$testsDir = Join-Path $claudeDir "04_tests"
$buildDir = Join-Path $claudeDir "05_build"
$deviceDir = Join-Path $claudeDir "06_device"
$shotsDir = Join-Path $claudeDir "07_screenshots"
$extraDir = Join-Path $claudeDir "08_extra"

foreach ($dir in @($gitDir, $docsDir, $srcDir, $testsDir, $buildDir, $deviceDir, $shotsDir, $extraDir)) {
    New-Item -ItemType Directory -Path $dir -Force | Out-Null
}

Write-Host "Prepared Claude review workspace at: $claudeDir" -ForegroundColor Cyan

# 2. FILL 01_git
try {
    (git log -15 --oneline --decorate) | Out-File (Join-Path $gitDir "log.txt") -Encoding utf8
    (git status) | Out-File (Join-Path $gitDir "status.txt") -Encoding utf8
    (git branch -a) | Out-File (Join-Path $gitDir "branch.txt") -Encoding utf8
    (git diff "$Base..HEAD" --stat) | Out-File (Join-Path $gitDir "diff_stat.txt") -Encoding utf8
    (git diff "$Base..HEAD") | Out-File (Join-Path $gitDir "diff_full.patch") -Encoding utf8
    (git branch -a --merged $Base) | Out-File (Join-Path $gitDir "merged.txt") -Encoding utf8

    if (Get-Command gh -ErrorAction SilentlyContinue) {
        (gh pr list --state all --limit 15) | Out-File (Join-Path $gitDir "prs.txt") -Encoding utf8
    } else {
        "gh not installed" | Out-File (Join-Path $gitDir "prs.txt") -Encoding utf8
    }
} catch {
    $problems.Add("Git export error: $_")
}

# 3. FILL 02_docs
$topDocs = @("README.md", "agent.md", "progress.md", "CHANGELOG.md", "report.md")
foreach ($docName in $topDocs) {
    $filePath = Join-Path $root $docName
    if (Test-Path $filePath) {
        Copy-Item -Path $filePath -Destination (Join-Path $docsDir $docName) -Force
    } else {
        $problems.Add("Doc missing: $docName")
    }
}

$repoDocsPath = Join-Path $root "docs"
if (Test-Path $repoDocsPath) {
    Copy-Item -Path $repoDocsPath -Destination $docsDir -Recurse -Force
} else {
    $problems.Add("Docs directory missing: docs\")
}

# 4. FILL 03_changed_source
try {
    $changedFiles = (git diff "$Base..HEAD" --name-only)
    foreach ($relPath in $changedFiles) {
        if ([string]::IsNullOrWhiteSpace($relPath)) { continue }
        $fullPath = Join-Path $root $relPath
        $fileName = [System.IO.Path]::GetFileName($relPath)

        # Ignore secrets
        if ($fileName -match "^(local\.properties|.*\.jks|.*\.keystore|google-services\.json|.*\.env)$") {
            continue
        }

        if (Test-Path $fullPath -PathType Leaf) {
            $destPath = Join-Path $srcDir $relPath
            $destParent = [System.IO.Path]::GetDirectoryName($destPath)
            if (-not (Test-Path $destParent)) {
                New-Item -ItemType Directory -Path $destParent -Force | Out-Null
            }
            Copy-Item -Path $fullPath -Destination $destPath -Force
        }
    }
} catch {
    $problems.Add("Error copying changed source files: $_")
}

# 5. FILL 04_tests
try {
    $testOutput = (.\gradlew.bat testDebugUnitTest 2>&1 | Out-String)
    $testOutput | Out-File (Join-Path $testsDir "test_output.txt") -Encoding utf8
    if ($LASTEXITCODE -ne 0) {
        $problems.Add("Unit tests failed with exit code $LASTEXITCODE")
    }
} catch {
    $problems.Add("Failed to execute unit tests: $_")
}

# Parse XML test results
try {
    $xmlFiles = Get-ChildItem -Path (Join-Path $root "app\build\test-results\testDebugUnitTest") -Filter "*.xml" -ErrorAction SilentlyContinue
    $summaryLines = New-Object System.Collections.Generic.List[string]

    foreach ($xmlFile in $xmlFiles) {
        [xml]$xmlContent = Get-Content $xmlFile.FullName
        $testSuite = $xmlContent.testsuite
        if ($testSuite) {
            $name = $testSuite.name
            $tests = $testSuite.tests
            $failures = $testSuite.failures
            $skipped = $testSuite.skipped
            $summaryLines.Add("[Class] $name - Total: $tests, Failures: $failures, Skipped: $skipped")
        }
    }
    $summaryLines | Out-File (Join-Path $testsDir "test_summary.txt") -Encoding utf8
} catch {
    $problems.Add("Failed to parse test XML results: $_")
}

# 6. FILL 05_build
try {
    $buildOutput = (.\gradlew.bat assembleDebug 2>&1 | Out-String)
    $lines = $buildOutput -split "`r`n|`n"
    $tailLines = $lines | Select-Object -Last 60
    $tailLines | Out-File (Join-Path $buildDir "build_output.txt") -Encoding utf8

    if ($LASTEXITCODE -ne 0) {
        $problems.Add("Build assembleDebug failed with exit code $LASTEXITCODE")
    }

    $apkPath = Join-Path $root "app\build\outputs\apk\debug\app-debug.apk"
    if (Test-Path $apkPath) {
        $apkSize = (Get-Item $apkPath).Length
        "app-debug.apk size: $apkSize bytes ($([Math]::Round($apkSize / 1MB, 2)) MB)" | Out-File (Join-Path $buildDir "apk_info.txt") -Encoding utf8
    } else {
        "app-debug.apk missing" | Out-File (Join-Path $buildDir "apk_info.txt") -Encoding utf8
        $problems.Add("Debug APK not found after assembleDebug")
    }
} catch {
    $problems.Add("Failed assembleDebug build step: $_")
}

# 7. FILL 06_device
try {
    $adbCmd = Get-Command adb -ErrorAction SilentlyContinue
    if (-not $adbCmd) {
        $adbPath = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
        if (Test-Path $adbPath) { $adbCmd = $adbPath }
    }

    if ($adbCmd) {
        $devices = (& $adbCmd devices) | Out-String
        if ($devices -match "device\s*$") {
            $devices | Out-File (Join-Path $deviceDir "devices.txt") -Encoding utf8
            (& $adbCmd shell getprop ro.product.model) | Out-File (Join-Path $deviceDir "model.txt") -Encoding utf8
            (& $adbCmd shell logcat -d -s AndroidRuntime:E) | Out-File (Join-Path $deviceDir "logcat_crashes.txt") -Encoding utf8
        } else {
            "no device" | Out-File (Join-Path $deviceDir "info.txt") -Encoding utf8
            $problems.Add("No ADB device connected")
        }
    } else {
        "no adb" | Out-File (Join-Path $deviceDir "info.txt") -Encoding utf8
        $problems.Add("ADB tool not found")
    }
} catch {
    "device check error" | Out-File (Join-Path $deviceDir "info.txt") -Encoding utf8
    $problems.Add("Device check exception: $_")
}

# 8. FILL 07_screenshots & VALIDATE
$hashes = @{}
function Test-ScreenshotValid {
    param([string]$filePath)
    try {
        $bmp = New-Object System.Drawing.Bitmap($filePath)
        $w = $bmp.Width
        $h = $bmp.Height
        if ($w -le 0 -or $h -le 0) {
            $bmp.Dispose()
            return "Zero dimension bitmap"
        }

        $allBlack = $true
        $allWhite = $true

        for ($x = 0; $x -lt $w; $x += [Math]::Max(1, [int]($w / 10))) {
            for ($y = 0; $y -lt $h; $y += [Math]::Max(1, [int]($h / 10))) {
                $pixel = $bmp.GetPixel($x, $y)
                if ($pixel.R -ne 0 -or $pixel.G -ne 0 -or $pixel.B -ne 0) {
                    $allBlack = $false
                }
                if ($pixel.R -ne 255 -or $pixel.G -ne 255 -or $pixel.B -ne 255) {
                    $allWhite = $false
                }
            }
        }
        $bmp.Dispose()

        if ($allBlack) { return "Fully Black Image" }
        if ($allWhite) { return "Fully White Image" }
        return $null
    } catch {
        return "Failed to parse PNG: $_"
    }
}

$recentPngs = Get-ChildItem -Path (Join-Path $root "docs\screenshots") -Filter "*.png" -Recurse -ErrorAction SilentlyContinue |
    Where-Object { $_.LastWriteTime -ge (Get-Date).AddHours(-24) }

foreach ($png in $recentPngs) {
    Copy-Item -Path $png.FullName -Destination (Join-Path $shotsDir $png.Name) -Force
}

foreach ($extraItem in $Extra) {
    if (Test-Path $extraItem) {
        $itemPath = Get-Item $extraItem
        if ($itemPath.Extension -eq ".png") {
            Copy-Item -Path $itemPath.FullName -Destination (Join-Path $shotsDir $itemPath.Name) -Force
        }
    }
}

# Validate screenshots in 07_screenshots
$copiedShots = Get-ChildItem -Path $shotsDir -Filter "*.png" -ErrorAction SilentlyContinue
foreach ($shot in $copiedShots) {
    $err = Test-ScreenshotValid -filePath $shot.FullName
    if ($err) {
        $problems.Add("Screenshot $($shot.Name) is INVALID ($err)")
    } else {
        $hash = (Get-FileHash -Path $shot.FullName -Algorithm SHA256).Hash
        if ($hashes.ContainsKey($hash)) {
            $problems.Add("Screenshot $($shot.Name) is INVALID (Duplicate of $($hashes[$hash]))")
        } else {
            $hashes[$hash] = $shot.Name
        }
    }
}

# 9. FILL 08_extra
foreach ($extraItem in $Extra) {
    if (Test-Path $extraItem) {
        Copy-Item -Path $extraItem -Destination $extraDir -Recurse -Force
    } else {
        $problems.Add("Extra item not found: $extraItem")
    }
}

# 10. SECRETS SCAN
$secretPatterns = @("client_secret", "api_key", "AIza", "BEGIN PRIVATE KEY")
$allClaudeFiles = Get-ChildItem -Path $claudeDir -Recurse -File

foreach ($cFile in $allClaudeFiles) {
    if ($cFile.Extension -match "\.(png|jpg|jpeg|tflite|zip|jar|apk)$") { continue }
    try {
        $content = Get-Content $cFile.FullName -Raw -ErrorAction SilentlyContinue
        if ($content) {
            foreach ($pattern in $secretPatterns) {
                if ($content -match $pattern) {
                    Remove-Item -Path $cFile.FullName -Force
                    $problems.Add("Secret detected ($pattern) and file deleted: $($cFile.FullName)")
                    break
                }
            }
        }
    } catch {}
}

# 11. GENERATE 00_README_FIRST.md
$currentBranch = (git rev-parse --abbrev-ref HEAD 2>$null)
if ($currentBranch) { $currentBranch = $currentBranch.Trim() }
$headHash = (git rev-parse HEAD 2>$null)
if ($headHash) { $headHash = $headHash.Trim() }
$nowStr = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")

$readmeContent = New-Object System.Text.StringBuilder
[void]$readmeContent.AppendLine("# Claude Review Package - $Step")
[void]$readmeContent.AppendLine("")
[void]$readmeContent.AppendLine("**Generated**: $nowStr")
[void]$readmeContent.AppendLine("**Branch**: $currentBranch")
[void]$readmeContent.AppendLine("**HEAD Commit**: $headHash")
[void]$readmeContent.AppendLine("**Base Branch**: $Base")
[void]$readmeContent.AppendLine("")
[void]$readmeContent.AppendLine("---")
[void]$readmeContent.AppendLine("")
[void]$readmeContent.AppendLine("## PROBLEMS")
[void]$readmeContent.AppendLine("")

if ($problems.Count -gt 0) {
    foreach ($prob in $problems) {
        [void]$readmeContent.AppendLine("- ⚠️ $prob")
    }
} else {
    [void]$readmeContent.AppendLine("None (All steps completed cleanly).")
}

[void]$readmeContent.AppendLine("")
[void]$readmeContent.AppendLine("---")
[void]$readmeContent.AppendLine("")
[void]$readmeContent.AppendLine("## INCLUDED FILES")
[void]$readmeContent.AppendLine("")

$finalClaudeFiles = Get-ChildItem -Path $claudeDir -Recurse -File
foreach ($file in $finalClaudeFiles) {
    $relPath = $file.FullName.Substring($claudeDir.Length + 1).Replace("\", "/")
    $sizeKb = [Math]::Round($file.Length / 1KB, 2)
    [void]$readmeContent.AppendLine("- `"$relPath`" ($sizeKb KB)")
}

$readmeContent.ToString() | Out-File (Join-Path $claudeDir "00_README_FIRST.md") -Encoding utf8

# 12. CREATE ZIP ARCHIVE
try {
    Compress-Archive -Path "$claudeDir\*" -DestinationPath $zipPath -Force
    $zipItem = Get-Item $zipPath
    $zipMb = [Math]::Round($zipItem.Length / 1MB, 2)
    Write-Host ""
    Write-Host "✅ Created Claude_review.zip at: $zipPath ($zipMb MB)" -ForegroundColor Green
} catch {
    Write-Error "Failed to create Claude_review.zip: $_"
}
