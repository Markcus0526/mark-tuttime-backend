# Build and Push Docker Images - Windows PowerShell Script
# Usage: .\build-images.ps1 [service-name|gateway-name|all|main] [-NoCache] [-Dev] [-Staging] [-Version <tag>]
# Examples:
#   .\build-images.ps1 user-service
#   .\build-images.ps1 user-service -Version 1.2.0
#   .\build-images.ps1 all -Version 1.2.0
#   .\build-images.ps1 admin-service -NoCache
#   .\build-images.ps1 all -NoCache -Version 2.0.0
#   .\build-images.ps1 user-service -Staging
#   .\build-images.ps1 user-service -Dev
#
# Images are tagged as both "latest" and "<version>" when -Version is provided, so you can pull by :latest or roll back to :<version>.
# Options: -NoCache (build without Docker cache), -Dev (k3s dev cluster tags), -Staging (push to boostx/staging instead of boostx/prod), -Version (tag for rollback, e.g. 1.2.0)

param(
    [Parameter(Position=0)]
    [string]$Service = "all",
    [switch]$NoCache,
    [switch]$Dev,
    [switch]$Staging,
    [Parameter(Mandatory=$false)]
    [string]$Version = ""
)

$RootDir = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$EnvFile = Join-Path (Split-Path -Parent $PSScriptRoot) ".env"

$Registry = $null
$Username = $null
$Password = $null

if (Test-Path $EnvFile) {
    Get-Content $EnvFile | ForEach-Object {
        if ($_ -match '^DOCKER_REGISTRY=(.+)$') {
            $Registry = $matches[1].Trim()
        }
        if ($_ -match '^DOCKER_USERNAME=(.+)$') {
            $Username = $matches[1].Trim()
        }
        if ($_ -match '^DOCKER_PASSWORD=***REMOVED***') {
            $Password = $matches[1].Trim()
        }
    }
}

if (-not $Registry -or -not $Username -or -not $Password) {
    Write-Host "Error: Missing Docker registry credentials in scripts/.env" -ForegroundColor Red
    Write-Host "Required: DOCKER_REGISTRY, DOCKER_USERNAME, DOCKER_PASSWORD" -ForegroundColor Yellow
    exit 1
}

# Image path: boostx/prod (default) or boostx/staging (with -Staging)
# Note: -Dev option adds "-dev" suffix to image name, not to the path
$BasePath = if ($Staging) { "boostx/staging" } else { "boostx/prod" }
$RegistryPath = "$Registry/$BasePath"

# Enable BuildKit
$env:DOCKER_BUILDKIT = "1"
$env:COMPOSE_DOCKER_CLI_BUILD = "1"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Docker Image Build Script (Windows)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
if ($Staging) {
    Write-Host "Environment: STAGING (boostx/staging)" -ForegroundColor Yellow
} else {
    Write-Host "Environment: PRODUCTION (boostx/prod)" -ForegroundColor Green
}
if ($Dev) {
    Write-Host "Mode: DEV (k3s dev cluster)" -ForegroundColor Yellow
} else {
    Write-Host "Mode: PROD (k8s prod cluster)" -ForegroundColor Green
}
if ($Version) {
    Write-Host "Version tag: $Version (images will be tagged as 'latest' and '$Version')" -ForegroundColor Cyan
}
Write-Host ""

Write-Host "Logging in to container registry ($Registry)..." -ForegroundColor Yellow
$loginOutput = echo $Password | docker login $Registry -u $Username --password-stdin 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "Login failed!" -ForegroundColor Red
    Write-Host $loginOutput
    exit 1
}
Write-Host "Login successful!" -ForegroundColor Green
Write-Host "Registry: $Registry" -ForegroundColor Cyan
Write-Host "Registry path: $RegistryPath" -ForegroundColor Cyan
Write-Host ""

function Get-ImageName {
    param(
        [string]$BaseImageName
    )
    # BaseImageName is like registry/path/apart-token-user-service:latest
    $base = $BaseImageName -replace ':latest$', ''
    if ($Dev) {
        return "${base}-dev:latest"
    }
    return "${base}:latest"
}

function Build-Service {
    param(
        [string]$ServiceName,
        [string]$ServiceDir,
        [string]$ImageName,
        [string]$ComposeServiceName,
        [switch]$NoCache,
        [string]$VersionTag = ""
    )
    
    Write-Host "Building $ServiceName..." -ForegroundColor Cyan
    if ($NoCache) {
        Write-Host "  Building without cache..." -ForegroundColor Yellow
    }
    Write-Host "  Service directory: $ServiceDir" -ForegroundColor Gray
    Write-Host "  Image: $ImageName" -ForegroundColor Gray
    
    $ComposeFile = Join-Path $ServiceDir "docker-compose.yml"
    $EnvDockerFile = Join-Path $ServiceDir ".env.docker"
    
    if (-not (Test-Path $ComposeFile)) {
        Write-Host "Error: docker-compose.yml not found at $ComposeFile" -ForegroundColor Red
        return $false
    }
    
    Push-Location $ServiceDir
    try {
        $buildArgs = @($ComposeServiceName)
        if ($NoCache) {
            $buildArgs += "--no-cache"
        }
        # Add progress and verbose output
        $buildArgs += "--progress=plain"
        
        Write-Host "  Executing docker compose build..." -ForegroundColor Gray
        if (Test-Path $EnvDockerFile) {
            docker compose --env-file .env.docker build $buildArgs 2>&1 | ForEach-Object {
                Write-Host $_ -ForegroundColor Gray
            }
        } else {
            docker compose build $buildArgs 2>&1 | ForEach-Object {
                Write-Host $_ -ForegroundColor Gray
            }
        }
        
        if ($LASTEXITCODE -ne 0) {
            Write-Host "Build failed for $ServiceName!" -ForegroundColor Red
            Write-Host "  Check the build output above for details." -ForegroundColor Yellow
            return $false
        }
        
        $ComposeProjectName = Split-Path -Leaf $ServiceDir
        $LocalImageName = "${ComposeProjectName}-${ComposeServiceName}:latest"
        docker tag $LocalImageName $ImageName
        
        if ($LASTEXITCODE -ne 0) {
            Write-Host "Tag failed for $ServiceName! Trying alternative name..." -ForegroundColor Yellow
            $AltImageName = "${ComposeServiceName}-${ComposeServiceName}:latest"
            docker tag $AltImageName $ImageName
            if ($LASTEXITCODE -ne 0) {
                Write-Host "Tag failed for $ServiceName!" -ForegroundColor Red
                return $false
            }
        }
        
        # Without -Version: push only "latest". With -Version: push both "latest" and "<version>".
        $imagesToPush = @($ImageName)
        if ($VersionTag) {
            $ImageNameVersion = $ImageName -replace ':latest$', ":$VersionTag"
            docker tag $LocalImageName $ImageNameVersion
            if ($LASTEXITCODE -ne 0) {
                Write-Host "Tag failed for $ServiceName (version $VersionTag)!" -ForegroundColor Red
                return $false
            }
            $imagesToPush += $ImageNameVersion
            Write-Host "  Tags: latest, $VersionTag" -ForegroundColor Gray
        } else {
            Write-Host "  Tags: latest only" -ForegroundColor Gray
        }
        
        Write-Host "Build successful!" -ForegroundColor Green
        
        foreach ($img in $imagesToPush) {
            Write-Host "Pushing $ServiceName ($img)..." -ForegroundColor Yellow
            Write-Host "  Full image path: $img" -ForegroundColor Gray
            
            # Verify image exists locally before pushing
            $imageExists = docker images $img --format "{{.Repository}}:{{.Tag}}" 2>&1
            if ($LASTEXITCODE -ne 0 -or -not $imageExists) {
                Write-Host "  Warning: Image $img not found locally. Attempting to find it..." -ForegroundColor Yellow
                docker images | Select-String -Pattern $ServiceName | ForEach-Object { Write-Host "  $_" -ForegroundColor Gray }
            }
            
            $pushOutput = docker push $img 2>&1 | Tee-Object -Variable pushResult
            $pushOutput | ForEach-Object {
                if ($_ -match "pushed|Pushed|digest|layer|unauthorized|denied|404|403") {
                    if ($_ -match "unauthorized|denied|404|403") {
                        Write-Host $_ -ForegroundColor Red
                    } else {
                        Write-Host $_ -ForegroundColor Cyan
                    }
                } else {
                    Write-Host $_ -ForegroundColor Gray
                }
            }
            
            if ($LASTEXITCODE -ne 0) {
                Write-Host "Push failed for $ServiceName ($img)!" -ForegroundColor Red
                Write-Host "  Exit code: $LASTEXITCODE" -ForegroundColor Yellow
                Write-Host "  Possible causes:" -ForegroundColor Yellow
                Write-Host "    1. Repository does not exist in registry (404)" -ForegroundColor Yellow
                Write-Host "       - In Selectel, repositories are auto-created on first push" -ForegroundColor Gray
                Write-Host "       - If 404 persists, verify the path structure in Selectel control panel" -ForegroundColor Gray
                Write-Host "       - Expected format: cr.selcloud.ru/<registry-name>/<image-name>" -ForegroundColor Gray
                Write-Host "    2. Authentication token expired or invalid" -ForegroundColor Yellow
                Write-Host "       - Regenerate token in Selectel control panel with read-write permissions" -ForegroundColor Gray
                Write-Host "    3. Insufficient permissions to push to this repository" -ForegroundColor Yellow
                Write-Host "       - Verify token has 'push' permissions for this repository" -ForegroundColor Gray
                Write-Host "    4. Incorrect registry path structure" -ForegroundColor Yellow
                Write-Host "       - Current path: $img" -ForegroundColor Gray
                Write-Host "       - Verify DOCKER_PROJECT_PATH in scripts/.env matches your Selectel registry name" -ForegroundColor Gray
                Write-Host "  Check the push output above for details." -ForegroundColor Yellow
                Write-Host "  Verify in Selectel control panel: Container Registry -> Repositories" -ForegroundColor Yellow
                return $false
            }
        }
        
        Write-Host "Push successful!" -ForegroundColor Green
        Write-Host ""
        return $true
    } finally {
        Pop-Location
    }
}

# Build services based on parameter
$success = $true

switch ($Service.ToLower()) {
    # Services
    "admin-service" {
        $ServiceDir = Join-Path $RootDir "services\admin-service"
        $ImageName = Get-ImageName "$RegistryPath/apart-token-admin-service:latest"
        $success = Build-Service `
            -ServiceName "admin-service" `
            -ServiceDir $ServiceDir `
            -ImageName $ImageName `
            -ComposeServiceName "admin-service" `
            -NoCache:$NoCache `
            -VersionTag $Version
    }
    "booking-integration-service" {
        $ServiceDir = Join-Path $RootDir "services\booking-integration-service"
        $ImageName = Get-ImageName "$RegistryPath/apart-token-booking-integration-service:latest"
        $success = Build-Service `
            -ServiceName "booking-integration-service" `
            -ServiceDir $ServiceDir `
            -ImageName $ImageName `
            -ComposeServiceName "booking-integration-service" `
            -NoCache:$NoCache `
            -VersionTag $Version
    }
    "content-service" {
        $ServiceDir = Join-Path $RootDir "services\content-service"
        $ImageName = Get-ImageName "$RegistryPath/apart-token-content-service:latest"
        $success = Build-Service `
            -ServiceName "content-service" `
            -ServiceDir $ServiceDir `
            -ImageName $ImageName `
            -ComposeServiceName "content-service" `
            -NoCache:$NoCache `
            -VersionTag $Version
    }
    "cdn-proxy-service" {
        $ServiceDir = Join-Path $RootDir "services\cdn-proxy-service"
        $ImageName = Get-ImageName "$RegistryPath/apart-token-cdn-proxy-service:latest"
        $success = Build-Service `
            -ServiceName "cdn-proxy-service" `
            -ServiceDir $ServiceDir `
            -ImageName $ImageName `
            -ComposeServiceName "cdn-proxy-service" `
            -NoCache:$NoCache `
            -VersionTag $Version
    }
    "conversion-service" {
        $ServiceDir = Join-Path $RootDir "services\conversion-service"
        $ImageName = Get-ImageName "$RegistryPath/apart-token-conversion-service:latest"
        $success = Build-Service `
            -ServiceName "conversion-service" `
            -ServiceDir $ServiceDir `
            -ImageName $ImageName `
            -ComposeServiceName "conversion-service" `
            -NoCache:$NoCache `
            -VersionTag $Version
    }
    "mining-service" {
        $ServiceDir = Join-Path $RootDir "services\mining-service"
        $ImageName = Get-ImageName "$RegistryPath/apart-token-mining-service:latest"
        $success = Build-Service `
            -ServiceName "mining-service" `
            -ServiceDir $ServiceDir `
            -ImageName $ImageName `
            -ComposeServiceName "mining-service" `
            -NoCache:$NoCache `
            -VersionTag $Version
    }
    "notification-service" {
        $ServiceDir = Join-Path $RootDir "services\notification-service"
        $ImageName = Get-ImageName "$RegistryPath/apart-token-notification-service:latest"
        $success = Build-Service `
            -ServiceName "notification-service" `
            -ServiceDir $ServiceDir `
            -ImageName $ImageName `
            -ComposeServiceName "notification-service" `
            -NoCache:$NoCache `
            -VersionTag $Version
    }
    "messaging-service" {
        $ServiceDir = Join-Path $RootDir "services\messaging-service"
        $ImageName = Get-ImageName "$RegistryPath/apart-token-messaging-service:latest"
        $success = Build-Service `
            -ServiceName "messaging-service" `
            -ServiceDir $ServiceDir `
            -ImageName $ImageName `
            -ComposeServiceName "messaging-service" `
            -NoCache:$NoCache `
            -VersionTag $Version
    }
    "rewards-service" {
        $ServiceDir = Join-Path $RootDir "services\rewards-service"
        $ImageName = Get-ImageName "$RegistryPath/apart-token-rewards-service:latest"
        $success = Build-Service `
            -ServiceName "rewards-service" `
            -ServiceDir $ServiceDir `
            -ImageName $ImageName `
            -ComposeServiceName "rewards-service" `
            -NoCache:$NoCache `
            -VersionTag $Version
    }
    "staking-service" {
        $ServiceDir = Join-Path $RootDir "services\staking-service"
        $ImageName = Get-ImageName "$RegistryPath/apart-token-staking-service:latest"
        $success = Build-Service `
            -ServiceName "staking-service" `
            -ServiceDir $ServiceDir `
            -ImageName $ImageName `
            -ComposeServiceName "staking-service" `
            -NoCache:$NoCache `
            -VersionTag $Version
    }
    "token-service" {
        $ServiceDir = Join-Path $RootDir "services\token-service"
        $ImageName = Get-ImageName "$RegistryPath/apart-token-token-service:latest"
        $success = Build-Service `
            -ServiceName "token-service" `
            -ServiceDir $ServiceDir `
            -ImageName $ImageName `
            -ComposeServiceName "token-service" `
            -NoCache:$NoCache `
            -VersionTag $Version
    }
    "user-service" {
        $ServiceDir = Join-Path $RootDir "services\user-service"
        $ImageName = Get-ImageName "$RegistryPath/apart-token-user-service:latest"
        $success = Build-Service `
            -ServiceName "user-service" `
            -ServiceDir $ServiceDir `
            -ImageName $ImageName `
            -ComposeServiceName "user-service" `
            -NoCache:$NoCache `
            -VersionTag $Version
    }
    "social-service" {
        $ServiceDir = Join-Path $RootDir "services\social-service"
        $ImageName = Get-ImageName "$RegistryPath/apart-token-social-service:latest"
        $success = Build-Service `
            -ServiceName "social-service" `
            -ServiceDir $ServiceDir `
            -ImageName $ImageName `
            -ComposeServiceName "social-service" `
            -NoCache:$NoCache `
            -VersionTag $Version
    }
    # Gateways
    "admin-gateway" {
        $ServiceDir = Join-Path $RootDir "admin-gateway"
        $ImageName = Get-ImageName "$RegistryPath/apart-token-admin-gateway:latest"
        $success = Build-Service `
            -ServiceName "admin-gateway" `
            -ServiceDir $ServiceDir `
            -ImageName $ImageName `
            -ComposeServiceName "admin-gateway" `
            -NoCache:$NoCache `
            -VersionTag $Version
    }
    "api-gateway" {
        $ServiceDir = Join-Path $RootDir "api-gateway"
        $ImageName = Get-ImageName "$RegistryPath/apart-token-api-gateway:latest"
        $success = Build-Service `
            -ServiceName "api-gateway" `
            -ServiceDir $ServiceDir `
            -ImageName $ImageName `
            -ComposeServiceName "api-gateway" `
            -NoCache:$NoCache `
            -VersionTag $Version
    }
    "main" {
        Write-Host "Building main services and gateways..." -ForegroundColor Cyan
        Write-Host ""
        
        # Build main services (user-specified list)
        $services = @(
            @{Name="user-service"; Dir="services\user-service"; Image="apart-token-user-service"},
            @{Name="rewards-service"; Dir="services\rewards-service"; Image="apart-token-rewards-service"},
            @{Name="notification-service"; Dir="services\notification-service"; Image="apart-token-notification-service"},
            @{Name="messaging-service"; Dir="services\messaging-service"; Image="apart-token-messaging-service"},
            @{Name="mining-service"; Dir="services\mining-service"; Image="apart-token-mining-service"},
            @{Name="content-service"; Dir="services\content-service"; Image="apart-token-content-service"},
            @{Name="social-service"; Dir="services\social-service"; Image="apart-token-social-service"},
            @{Name="cdn-proxy-service"; Dir="services\cdn-proxy-service"; Image="apart-token-cdn-proxy-service"},
            @{Name="admin-service"; Dir="services\admin-service"; Image="apart-token-admin-service"}
        )
        
        # Build main gateways
        $gateways = @(
            @{Name="api-gateway"; Dir="api-gateway"; Image="apart-token-api-gateway"},
            @{Name="admin-gateway"; Dir="admin-gateway"; Image="apart-token-admin-gateway"}
        )
        
        foreach ($svc in $services) {
            if (-not $success) { break }
            $ServiceDir = Join-Path $RootDir $svc.Dir
            $ImageName = Get-ImageName "$RegistryPath/$($svc.Image):latest"
            $success = Build-Service `
                -ServiceName $svc.Name `
                -ServiceDir $ServiceDir `
                -ImageName $ImageName `
                -ComposeServiceName $svc.Name `
                -NoCache:$NoCache `
                -VersionTag $Version
        }
        
        foreach ($gw in $gateways) {
            if (-not $success) { break }
            $ServiceDir = Join-Path $RootDir $gw.Dir
            $ImageName = Get-ImageName "$RegistryPath/$($gw.Image):latest"
            $success = Build-Service `
                -ServiceName $gw.Name `
                -ServiceDir $ServiceDir `
                -ImageName $ImageName `
                -ComposeServiceName $gw.Name `
                -NoCache:$NoCache `
                -VersionTag $Version
        }
    }
    "all" {
        Write-Host "Building all services and gateways..." -ForegroundColor Cyan
        Write-Host ""
        
        # Build all services
        $services = @(
            @{Name="user-service"; Dir="services\user-service"; Image="apart-token-user-service"},
            @{Name="notification-service"; Dir="services\notification-service"; Image="apart-token-notification-service"},
            @{Name="messaging-service"; Dir="services\messaging-service"; Image="apart-token-messaging-service"},
            @{Name="token-service"; Dir="services\token-service"; Image="apart-token-token-service"},
            @{Name="staking-service"; Dir="services\staking-service"; Image="apart-token-staking-service"},
            @{Name="rewards-service"; Dir="services\rewards-service"; Image="apart-token-rewards-service"},
            @{Name="social-service"; Dir="services\social-service"; Image="apart-token-social-service"},
            @{Name="booking-integration-service"; Dir="services\booking-integration-service"; Image="apart-token-booking-integration-service"},
            @{Name="mining-service"; Dir="services\mining-service"; Image="apart-token-mining-service"},
            @{Name="conversion-service"; Dir="services\conversion-service"; Image="apart-token-conversion-service"},
            @{Name="content-service"; Dir="services\content-service"; Image="apart-token-content-service"},
            @{Name="cdn-proxy-service"; Dir="services\cdn-proxy-service"; Image="apart-token-cdn-proxy-service"},
            @{Name="admin-service"; Dir="services\admin-service"; Image="apart-token-admin-service"}
        )
        
        # Build all gateways
        $gateways = @(
            @{Name="admin-gateway"; Dir="admin-gateway"; Image="apart-token-admin-gateway"},
            @{Name="api-gateway"; Dir="api-gateway"; Image="apart-token-api-gateway"}
        )
        
        foreach ($svc in $services) {
            if (-not $success) { break }
            $ServiceDir = Join-Path $RootDir $svc.Dir
            $ImageName = Get-ImageName "$RegistryPath/$($svc.Image):latest"
            $success = Build-Service `
                -ServiceName $svc.Name `
                -ServiceDir $ServiceDir `
                -ImageName $ImageName `
                -ComposeServiceName $svc.Name `
                -NoCache:$NoCache `
                -VersionTag $Version
        }
        
        foreach ($gw in $gateways) {
            if (-not $success) { break }
            $ServiceDir = Join-Path $RootDir $gw.Dir
            $ImageName = Get-ImageName "$RegistryPath/$($gw.Image):latest"
            $success = Build-Service `
                -ServiceName $gw.Name `
                -ServiceDir $ServiceDir `
                -ImageName $ImageName `
                -ComposeServiceName $gw.Name `
                -NoCache:$NoCache `
                -VersionTag $Version
        }
    }
    default {
        Write-Host "Unknown service: $Service" -ForegroundColor Red
        Write-Host "Available services: admin-service, booking-integration-service, content-service, conversion-service, cdn-proxy-service, messaging-service, mining-service, notification-service, rewards-service, social-service, staking-service, token-service, user-service" -ForegroundColor Yellow
        Write-Host "Available gateways: admin-gateway, api-gateway" -ForegroundColor Yellow
        Write-Host "Special options: all (builds all services and gateways), main (builds main services: user-service, rewards-service, notification-service, messaging-service, mining-service, content-service, booking-integration-service, api-gateway, admin-service, admin-gateway)" -ForegroundColor Yellow
        Write-Host "Options: -NoCache, -Dev, -Staging, -Version <tag> (e.g. 1.2.0 for rollback)" -ForegroundColor Yellow
        Write-Host "Usage: .\build-images.ps1 [service-name|gateway-name|all|main] [-NoCache] [-Dev] [-Staging] [-Version <tag>]" -ForegroundColor Yellow
        exit 1
    }
}

if ($success) {
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "All builds completed successfully!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    exit 0
} else {
    Write-Host "========================================" -ForegroundColor Red
    Write-Host "Build failed!" -ForegroundColor Red
    Write-Host "========================================" -ForegroundColor Red
    exit 1
}

