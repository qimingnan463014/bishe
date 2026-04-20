param()

$ErrorActionPreference = 'Stop'

$projectDir = Split-Path -Parent $PSScriptRoot
$loginUrl = 'http://localhost:8080/api/login.html'
$maxRetries = 90
$sleepSeconds = 2

function Test-LoginPage {
    param(
        [string]$Url
    )

    try {
        $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 2
        return $response.StatusCode -ge 200 -and $response.StatusCode -lt 500
    } catch {
        return $false
    }
}

function Assert-CommandExists {
    param(
        [string]$Name,
        [string]$Hint
    )

    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        Write-Host $Hint -ForegroundColor Red
        exit 1
    }
}

if (Test-LoginPage -Url $loginUrl) {
    Start-Process $loginUrl | Out-Null
    exit 0
}

Assert-CommandExists -Name 'java' -Hint 'Java was not found in PATH. Please install JDK/JRE first.'
Assert-CommandExists -Name 'mvn' -Hint 'Maven was not found in PATH. Please install Maven first.'

$mavenCmd = (Get-Command mvn).Source
Write-Host 'Starting salary-system backend...' -ForegroundColor Cyan
Start-Process -FilePath 'cmd.exe' -WorkingDirectory $projectDir -ArgumentList @('/k', "`"$mavenCmd`" spring-boot:run") -WindowStyle Minimized | Out-Null

for ($i = 1; $i -le $maxRetries; $i++) {
    Start-Sleep -Seconds $sleepSeconds
    Write-Host ("Waiting for login page... {0}/{1}" -f $i, $maxRetries)
    if (Test-LoginPage -Url $loginUrl) {
        Write-Host 'Login page is ready. Opening browser...' -ForegroundColor Green
        Start-Process $loginUrl | Out-Null
        exit 0
    }
}

Write-Host 'Startup timed out. Please check the new PowerShell window for backend errors.' -ForegroundColor Yellow
exit 1
