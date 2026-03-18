# PowerShell script to clear all admin service caches via API
# This script authenticates with the admin API and clears all caches
#
# Usage:
#   .\scripts\windows\clear-admin-cache.ps1
#   .\scripts\windows\clear-admin-cache.ps1 -Username admin -Password YOUR_PASSWORD
#   .\scripts\windows\clear-admin-cache.ps1 -BaseUrl http://localhost:28080
#   .\scripts\windows\clear-admin-cache.ps1 -CacheName analytics  # Clear specific cache

param(
    [string]$Username = $env:ADMIN_USERNAME,
    [string]$Password = $env:ADMIN_PASSWORD,
    [string]$BaseUrl = $env:ADMIN_BASE_URL,
    [string]$CacheName = "",
    [switch]$Verbose,
    [switch]$Help
)

# Get script directory and load .env file
$ScriptDir = Split-Path -Parent $PSScriptRoot
$EnvFile = Join-Path $ScriptDir ".env"

# Load environment variables from .env file
if (Test-Path $EnvFile) {
    Get-Content $EnvFile | ForEach-Object {
        if ($_ -match '^\s*([^#][^=]*)=(.*)$') {
            $key = $matches[1].Trim()
            $value = $matches[2].Trim()
            $value = $value -replace '^["''](.*)["'']$', '$1'
            [Environment]::SetEnvironmentVariable($key, $value, "Process")
        }
    }
}

# Re-read from environment after loading .env
if ([string]::IsNullOrEmpty($Username)) {
    $Username = [Environment]::GetEnvironmentVariable("ADMIN_USERNAME", "Process")
}
if ([string]::IsNullOrEmpty($Password)) {
    $Password = [Environment]::GetEnvironmentVariable("ADMIN_PASSWORD", "Process")
}
if ([string]::IsNullOrEmpty($BaseUrl)) {
    $BaseUrl = [Environment]::GetEnvironmentVariable("ADMIN_BASE_URL", "Process")
}

# Set defaults if not provided (no hardcoded passwords)
if ([string]::IsNullOrEmpty($Username)) {
    $Username = "admin"
}
if ([string]::IsNullOrEmpty($Password)) {
    $Password = ""
}
if ([string]::IsNullOrEmpty($BaseUrl)) {
    $BaseUrl = "http://localhost:28080"
}

$AdminPath = "/api/admin"

# Show help
if ($Help) {
    Write-Host "Usage: .\clear-admin-cache.ps1 [OPTIONS]" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -Username USERNAME        Admin username (default: admin)"
    Write-Host "  -Password PASSWORD        Admin password (from .env or environment)"
    Write-Host "  -BaseUrl URL              Base URL for admin API (default: http://localhost:28080)"
    Write-Host "  -CacheName NAME           Clear specific cache (e.g., 'analytics', 'userDetails')"
    Write-Host "  -Verbose                  Show verbose output"
    Write-Host "  -Help                     Show this help message"
    Write-Host ""
    Write-Host "Environment variables (or from scripts/.env):"
    Write-Host "  ADMIN_USERNAME           Admin username"
    Write-Host "  ADMIN_PASSWORD           Admin password (required)"
    Write-Host "  ADMIN_BASE_URL           Base URL for admin API"
    Write-Host ""
    Write-Host "Examples:"
    Write-Host "  .\clear-admin-cache.ps1"
    Write-Host "  .\clear-admin-cache.ps1 -Username admin -Password mypass"
    Write-Host "  .\clear-admin-cache.ps1 -CacheName analytics"
    Write-Host "  `$env:ADMIN_USERNAME='admin'; `$env:ADMIN_PASSWORD='***REMOVED***'; .\clear-admin-cache.ps1"
    Write-Host ""
    Write-Host "Note: Create scripts/.env file with ADMIN_PASSWORD (see .env.example)"
    exit 0
}

# Function to print verbose output
function Write-VerboseOutput {
    param([string]$Message)
    if ($Verbose) {
        Write-Host "[VERBOSE] $Message" -ForegroundColor Blue
    }
}

# Function to print error
function Write-ErrorOutput {
    param([string]$Message)
    Write-Host "[ERROR] $Message" -ForegroundColor Red
}

# Function to print success
function Write-SuccessOutput {
    param([string]$Message)
    Write-Host "[SUCCESS] $Message" -ForegroundColor Green
}

# Function to print warning
function Write-WarningOutput {
    param([string]$Message)
    Write-Host "[WARNING] $Message" -ForegroundColor Yellow
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Clear Admin Service Cache" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Configuration:"
Write-Host "  Base URL: $BaseUrl"
Write-Host "  Username: $Username"
if ($Verbose) {
    Write-Host "  Password: [REDACTED]"
}
if (-not [string]::IsNullOrEmpty($CacheName)) {
    Write-Host "  Cache Name: $CacheName"
}
Write-Host ""

# Validate required credentials
if ([string]::IsNullOrEmpty($Password)) {
    Write-ErrorOutput "ADMIN_PASSWORD is required but not set"
    Write-Host ""
    Write-Host "Please set ADMIN_PASSWORD in one of the following ways:"
    Write-Host "  1. Create scripts/.env file with ADMIN_PASSWORD=your_password"
    Write-Host "  2. Pass via command line: .\clear-admin-cache.ps1 -Password YOUR_PASSWORD"
    Write-Host "  3. Set environment variable: `$env:ADMIN_PASSWORD = 'your_password'"
    exit 1
}

# Step 1: Login to get JWT token
Write-VerboseOutput "Step 1: Authenticating with admin API..."
$LoginUrl = "$BaseUrl$AdminPath/auth/login"

$LoginBody = @{
    username = $Username
    password = $Password
} | ConvertTo-Json

try {
    $LoginResponse = Invoke-RestMethod -Uri $LoginUrl -Method Post -Body $LoginBody -ContentType "application/json" -ErrorAction Stop
    
    if ($null -eq $LoginResponse.data -or $null -eq $LoginResponse.data.token) {
        Write-ErrorOutput "Token not found in response"
        Write-Host ($LoginResponse | ConvertTo-Json -Depth 10)
        exit 1
    }
    
    $Token = $LoginResponse.data.token
    Write-VerboseOutput "Token retrieved successfully (length: $($Token.Length))"
    Write-SuccessOutput "Authentication successful"
} catch {
    $StatusCode = $_.Exception.Response.StatusCode.value__
    Write-ErrorOutput "Login failed with HTTP status $StatusCode"
    if ($_.ErrorDetails.Message) {
        Write-Host $_.ErrorDetails.Message
    } else {
        Write-Host $_.Exception.Message
    }
    
    if ($StatusCode -eq 400) {
        Write-Host ""
        Write-WarningOutput "Invalid credentials. Please check your username and password."
        Write-Host ""
        Write-Host "To use credentials:"
        Write-Host "  1. Create scripts/.env file with ADMIN_USERNAME and ADMIN_PASSWORD"
        Write-Host "  2. Or pass via command line: .\clear-admin-cache.ps1 -Username YOUR_USERNAME -Password YOUR_PASSWORD"
        Write-Host "  3. Or set environment variables:"
        Write-Host "     `$env:ADMIN_USERNAME = 'your_username'"
        Write-Host "     `$env:ADMIN_PASSWORD = 'your_password'"
    }
    exit 1
}

# Step 2: Clear cache
Write-Host ""
if (-not [string]::IsNullOrEmpty($CacheName)) {
    Write-VerboseOutput "Step 2: Clearing specific cache: $CacheName"
    $CacheUrl = "$BaseUrl$AdminPath/system/cache/clear/$CacheName"
} else {
    Write-VerboseOutput "Step 2: Clearing all admin caches"
    $CacheUrl = "$BaseUrl$AdminPath/system/cache/clear"
}

$Headers = @{
    "Authorization" = "Bearer $Token"
    "Content-Type" = "application/json"
}

try {
    $CacheResponse = Invoke-RestMethod -Uri $CacheUrl -Method Post -Headers $Headers -ErrorAction Stop
    
    $Message = $CacheResponse.message
    if ([string]::IsNullOrEmpty($Message)) {
        $Message = $CacheResponse.data
    }
    if ([string]::IsNullOrEmpty($Message)) {
        $Message = "Cache cleared successfully"
    }
    
    Write-SuccessOutput $Message
    
    if ($Verbose) {
        Write-Host ""
        Write-Host "Full response:"
        Write-Host ($CacheResponse | ConvertTo-Json -Depth 10)
    }
} catch {
    $StatusCode = $_.Exception.Response.StatusCode.value__
    
    if ($StatusCode -eq 401) {
        Write-ErrorOutput "Authentication failed (401 Unauthorized)"
        Write-Host "The token may have expired or is invalid"
        exit 1
    } elseif ($StatusCode -eq 403) {
        Write-ErrorOutput "Permission denied (403 Forbidden)"
        Write-Host "Your account may not have permission to clear caches"
        if ($_.ErrorDetails.Message) {
            Write-Host $_.ErrorDetails.Message
        }
        exit 1
    } else {
        Write-ErrorOutput "Cache clear failed with HTTP status $StatusCode"
        if ($_.ErrorDetails.Message) {
            Write-Host $_.ErrorDetails.Message
        } else {
            Write-Host $_.Exception.Message
        }
        exit 1
    }
}

# Step 3: Logout (optional, but good practice)
Write-VerboseOutput "Step 3: Logging out..."
$LogoutUrl = "$BaseUrl$AdminPath/auth/logout"

try {
    Invoke-RestMethod -Uri $LogoutUrl -Method Post -Headers $Headers -ErrorAction SilentlyContinue | Out-Null
    Write-VerboseOutput "Logged out"
} catch {
    # Ignore logout errors
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Operation completed successfully!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

