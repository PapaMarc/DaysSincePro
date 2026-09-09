[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$RepoRoot,
    [switch]$UpdateBaseline
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$includeRoots = @(
    (Join-Path -Path $RepoRoot -ChildPath "app/src/main/java"),
    (Join-Path -Path $RepoRoot -ChildPath "app/src/main/kotlin")
)

$includeExtensions = @(".java", ".kt", ".kts")

$excludePathRegexes = @(
    '[\\/]app[\\/]src[\\/]test[\\/]',
    '[\\/]app[\\/]src[\\/]androidTest[\\/]',
    '[\\/]build[\\/]'
)

# Keep this targeted to obvious UI sinks to reduce false positives.
$patterns = @(
    '\bsetText\s*\(\s*"',
    '\bsetTitle\s*\(\s*"',
    '\bsetMessage\s*\(\s*"',
    '\bsetHint\s*\(\s*"',
    '\bsetError\s*\(\s*"',
    '\bsetSummary\s*\(\s*"',
    '\bsetPositiveButton\s*\(\s*"',
    '\bsetNegativeButton\s*\(\s*"',
    '\bsetNeutralButton\s*\(\s*"',
    '\bToast\.makeText\s*\([^\)]*"',
    '\bshowToast\s*\(\s*"',
    '\bSnackbar\.make\s*\([^\)]*"'
)

$approvedNonLocalizableLineRegexes = @(
    '^\s*[\w\.]+\s*\.setText\(\s*""\s*\)\s*;?\s*$',
    '^\s*[\w\.]+\s*\.setText\(\s*"\("\s*\+\s*.+\+\s*"\)"\s*\)\s*;?\s*$'
)

$violationRecords = New-Object System.Collections.Generic.List[object]

foreach ($root in $includeRoots) {
    if (-not (Test-Path $root)) {
        continue
    }

    Get-ChildItem -Path $root -Recurse -File | Where-Object {
        $includeExtensions -contains $_.Extension
    } | ForEach-Object {
        $path = $_.FullName
        foreach ($exclude in $excludePathRegexes) {
            if ($path -match $exclude) {
                return
            }
        }

        $lineNumber = 0
        Get-Content -Path $path | ForEach-Object {
            $lineNumber++
            $line = $_

            # Skip fully commented lines.
            if ($line -match '^\s*//') {
                return
            }

            foreach ($approved in $approvedNonLocalizableLineRegexes) {
                if ($line -match $approved) {
                    return
                }
            }

            foreach ($regex in $patterns) {
                if ($line -match $regex) {
                    $normalizedPath = $path.Replace('/', '\\')
                    $normalizedRoot = $RepoRoot.Replace('/', '\\').TrimEnd('\\')
                    $displayPath = $normalizedPath
                    if ($displayPath.StartsWith($normalizedRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
                        $displayPath = $displayPath.Substring($normalizedRoot.Length)
                        if ($displayPath.StartsWith('\\') -or $displayPath.StartsWith('/')) {
                            $displayPath = $displayPath.Substring(1)
                        }
                    }
                    $displayPath = $displayPath.Replace('\\', '/')

                    $trimmedLine = $line.Trim()
                    $fingerprint = "${displayPath}|${regex}|${trimmedLine}"
                    $display = "${displayPath}:${lineNumber}: ${line}"
                    $violationRecords.Add([PSCustomObject]@{
                        Fingerprint = $fingerprint
                        Display = $display
                    })
                    break
                }
            }
        }
    }
}

$uniqueRecords = @($violationRecords | Sort-Object Fingerprint -Unique)
$baselinePath = Join-Path -Path $RepoRoot -ChildPath "scripts/no_hardcoded_ui_strings.baseline"

if ($UpdateBaseline) {
    $baselineDir = Split-Path -Path $baselinePath -Parent
    if (-not (Test-Path $baselineDir)) {
        New-Item -ItemType Directory -Path $baselineDir | Out-Null
    }

    $uniqueRecords | Select-Object -ExpandProperty Fingerprint | Set-Content -Path $baselinePath -Encoding UTF8
    Write-Host "Updated baseline with $($uniqueRecords.Count) known violations: $baselinePath" -ForegroundColor Yellow
    exit 0
}

$baselineFingerprints = @()
if (Test-Path $baselinePath) {
    $baselineFingerprints = @(Get-Content -Path $baselinePath | Where-Object {
        -not [string]::IsNullOrWhiteSpace($_) -and -not $_.TrimStart().StartsWith('#')
    })
}

$newViolations = @($uniqueRecords | Where-Object {
    $baselineFingerprints -notcontains $_.Fingerprint
})

if ($newViolations.Count -gt 0) {
    Write-Host "Hardcoded UI string guard failed. Found new potential violations:" -ForegroundColor Red
    $newViolations | ForEach-Object { Write-Host $_.Display }
    if ($baselineFingerprints.Count -eq 0) {
        Write-Host "No baseline file found at $baselinePath. Create one using -UpdateBaseline." -ForegroundColor Yellow
    }
    exit 1
}

if ($uniqueRecords.Count -gt 0) {
    Write-Host "Hardcoded UI string guard passed for new code ($($uniqueRecords.Count) known baseline violations)." -ForegroundColor Yellow
    exit 0
}

Write-Host "Hardcoded UI string guard passed." -ForegroundColor Green
exit 0
