$ErrorActionPreference = "Stop"

$failures = New-Object System.Collections.Generic.List[string]
$warnings = New-Object System.Collections.Generic.List[string]

function Pass($message) {
    Write-Host "[OK] $message"
}

function Fail($message) {
    Write-Host "[FAIL] $message"
    $script:failures.Add($message) | Out-Null
}

function Warn($message) {
    Write-Host "[WARN] $message"
    $script:warnings.Add($message) | Out-Null
}

function Require-FileContains($path, $patterns, $label) {
    if (-not (Test-Path -LiteralPath $path)) {
        Fail "$label missing: $path"
        return
    }

    $content = Get-Content -Raw -Encoding UTF8 -LiteralPath $path
    foreach ($pattern in $patterns) {
        if ($content -notmatch $pattern) {
            Fail "$label missing required pattern: $pattern"
        }
    }

    Pass "$label checked"
}

Write-Host "T00 repository baseline check"
Write-Host "Working directory: $((Get-Location).Path)"

try {
    git rev-parse --is-inside-work-tree *> $null
    if ($LASTEXITCODE -eq 0) {
        Pass "current directory is a Git repository"
    } else {
        Fail "current directory is not a Git repository"
    }
} catch {
    Fail "git is unavailable or current directory is not a Git repository"
}

$branch = (git branch --show-current 2>$null).Trim()
if ($branch -eq "main") {
    Pass "current branch is main"
} else {
    Fail "current branch must be main, actual: $branch"
}

$origin = (git remote get-url origin 2>$null).Trim()
if ($origin -eq "https://github.com/violet-hekmatyar/Tifo.git") {
    Pass "origin remote is correct"
} else {
    Fail "origin remote mismatch, actual: $origin"
}

if (Test-Path -LiteralPath "docs" -PathType Container) {
    Pass "docs directory exists"
} else {
    Fail "docs directory missing"
}

if (Test-Path -LiteralPath "tmp" -PathType Container) {
    Pass "tmp directory exists"
} else {
    Fail "tmp directory missing"
}

if (Test-Path -LiteralPath ".gitignore") {
    Pass ".gitignore exists"
    Require-FileContains ".gitignore" @(
        "(?m)^tmp/$",
        "(?m)^\.env$",
        "(?m)^\*\.local$",
        "(?m)^application-prod\.yml$",
        "(?m)^target/$",
        "(?m)^logs/$",
        "(?m)^uploads/$"
    ) ".gitignore"
} else {
    Fail ".gitignore missing"
}

if (Test-Path -LiteralPath "README.md") {
    Pass "README.md exists"
} else {
    Fail "README.md missing"
}

if (Test-Path -LiteralPath "docs/09_DEPLOYMENT_GUIDE.md") {
    $deployDoc = Get-Content -Raw -Encoding UTF8 -LiteralPath "docs/09_DEPLOYMENT_GUIDE.md"
    if (($deployDoc -match "jar") -or ($deployDoc -match "java\s+-jar")) {
        Pass "deployment guide contains jar/java -jar deployment notes"
    } else {
        Fail "deployment guide must mention jar or java -jar"
    }
} else {
    Fail "docs/09_DEPLOYMENT_GUIDE.md missing"
}

if (Test-Path -LiteralPath "docs/12_CODEX_TASK_PLAN.md") {
    $taskPlan = Get-Content -Raw -Encoding UTF8 -LiteralPath "docs/12_CODEX_TASK_PLAN.md"
    if ($taskPlan -match "T00") {
        Pass "task plan contains T00"
    } else {
        Fail "task plan must contain T00"
    }
} else {
    Fail "docs/12_CODEX_TASK_PLAN.md missing"
}

$trackedEnv = git ls-files -- ".env"
if ([string]::IsNullOrWhiteSpace($trackedEnv)) {
    Pass ".env is not tracked by Git"
} else {
    Fail ".env is tracked by Git"
}

$trackedTmp = git ls-files -- "tmp"
if ([string]::IsNullOrWhiteSpace($trackedTmp)) {
    Pass "tmp files are not tracked by Git"
} else {
    Fail "tmp files are tracked by Git: $trackedTmp"
}

if (Test-Path -LiteralPath "pom.xml") {
    Fail "pom.xml exists, but T00 must not create Maven project files"
} else {
    Pass "pom.xml not present"
}

if (Test-Path -LiteralPath "src") {
    Fail "src directory exists, but T00 must not create business code"
} else {
    Pass "src directory not present"
}

$cnPassword = ([string][char]0x5BC6) + ([string][char]0x7801)
$cnSecret = ([string][char]0x5BC6) + ([string][char]0x94A5)
$sensitivePattern = "password|secret|token|$([regex]::Escape($cnPassword))|$([regex]::Escape($cnSecret))"

$sensitiveNames = Get-ChildItem -Force -Recurse -File -ErrorAction SilentlyContinue |
    Where-Object {
        $_.FullName -notmatch "\\.git\\" -and
        ($_.Name -match $sensitivePattern)
    } |
    Select-Object -ExpandProperty FullName

if ($sensitiveNames) {
    Warn "sensitive-looking filenames found; review before committing:"
    $sensitiveNames | ForEach-Object { Write-Host "       $_" }
} else {
    Pass "no sensitive-looking filenames found"
}

if ($failures.Count -gt 0) {
    Write-Host ""
    Write-Host "T00 check failed with $($failures.Count) failure(s)."
    exit 1
}

Write-Host ""
Write-Host "T00 check passed."
if ($warnings.Count -gt 0) {
    Write-Host "Warnings: $($warnings.Count). Please review them before committing."
}
exit 0
