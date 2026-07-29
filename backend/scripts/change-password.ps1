param(
    [string]$ApiBaseUrl = "http://localhost:8081",
    [string]$Email
)

$ErrorActionPreference = "Stop"
$ApiBaseUrl = $ApiBaseUrl.TrimEnd("/")

if (-not $Email) {
    $Email = Read-Host "Account email"
}

$uri = [Uri]$ApiBaseUrl
if ($uri.Scheme -ne "https" -and $uri.Host -notin @("localhost", "127.0.0.1")) {
    throw "Password changes over HTTP are allowed only for localhost. Use an HTTPS production URL."
}

function Read-PlainTextSecret([string]$Prompt) {
    $secure = Read-Host $Prompt -AsSecureString
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try {
        [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
        $secure.Dispose()
    }
}

$currentPassword = Read-PlainTextSecret "Current password"
$newPassword = Read-PlainTextSecret "New password"
$confirmation = Read-PlainTextSecret "Confirm new password"

try {
    if ($newPassword -cne $confirmation) {
        throw "The new password and confirmation do not match."
    }

    $loginBody = @{
        email = $Email
        password = $currentPassword
    } | ConvertTo-Json

    $session = Invoke-RestMethod `
        -Method Post `
        -Uri "$ApiBaseUrl/api/auth/login" `
        -ContentType "application/json" `
        -Body $loginBody

    $changeBody = @{
        currentPassword = $currentPassword
        newPassword = $newPassword
    } | ConvertTo-Json

    $result = Invoke-RestMethod `
        -Method Put `
        -Uri "$ApiBaseUrl/api/users/me/password" `
        -ContentType "application/json" `
        -Headers @{ Authorization = "Bearer $($session.accessToken)" } `
        -Body $changeBody

    Write-Host $result.message
} finally {
    $currentPassword = $null
    $newPassword = $null
    $confirmation = $null
    $loginBody = $null
    $changeBody = $null
    $session = $null
}
