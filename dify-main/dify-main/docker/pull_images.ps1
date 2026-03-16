# Dify Docker 镜像拉取脚本
# 请开启全局VPN后运行此脚本
# 用法: 右键PowerShell管理员运行，或在终端执行: powershell -ExecutionPolicy Bypass -File pull_images.ps1

# 直接从Docker Hub拉取（需要全局VPN）
# 如果VPN不稳定，可以尝试换成其他镜像源前缀：
#   docker.1ms.run/  或  dockerpull.org/  或  docker.xuanyuan.me/
$images = @(
    "langgenius/dify-api:1.13.0",
    "langgenius/dify-web:1.13.0",
    "langgenius/dify-sandbox:0.2.12",
    "langgenius/dify-plugin-daemon:0.5.3-local",
    "postgres:15-alpine",
    "redis:6-alpine",
    "nginx:latest",
    "busybox:latest",
    "ubuntu/squid:latest",
    "semitechnologies/weaviate:1.27.0"
)

$total = $images.Count
$success = 0
$failed = @()

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Dify Docker Images Pull Script" -ForegroundColor Cyan
Write-Host " Total: $total images" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

foreach ($img in $images) {
    $idx = [array]::IndexOf($images, $img) + 1
    Write-Host "[$idx/$total] Pulling: $img" -ForegroundColor Yellow
    
    $maxRetry = 3
    $pulled = $false
    
    for ($i = 1; $i -le $maxRetry; $i++) {
        docker pull $img 2>&1 | Out-Host
        if ($LASTEXITCODE -eq 0) {
            Write-Host "  OK!" -ForegroundColor Green
            $success++
            $pulled = $true
            break
        } else {
            Write-Host "  Attempt $i/$maxRetry failed." -ForegroundColor Red
            if ($i -lt $maxRetry) {
                Write-Host "  Retrying in 5 seconds..." -ForegroundColor Gray
                Start-Sleep -Seconds 5
            }
        }
    }
    
    if (-not $pulled) {
        $failed += $img
        Write-Host "  FAILED after $maxRetry attempts: $img" -ForegroundColor Red
    }
    Write-Host ""
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Result: $success/$total succeeded" -ForegroundColor $(if ($success -eq $total) { "Green" } else { "Yellow" })
if ($failed.Count -gt 0) {
    Write-Host " Failed images:" -ForegroundColor Red
    foreach ($f in $failed) { Write-Host "   - $f" -ForegroundColor Red }
}
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "All images pulled! Next step:" -ForegroundColor Green
Write-Host "  cd e:\beiruan_develop\dify-main\dify-main\docker" -ForegroundColor White
Write-Host "  docker compose up -d" -ForegroundColor White
Write-Host "  Then visit: http://localhost:8180/install" -ForegroundColor White
