# Script to run all seed data files
# Run this after pasting AI-generated content into the seed files

$ErrorActionPreference = "Stop"

Write-Host "=== Running Seed Data for StyleMind Backend ===" -ForegroundColor Cyan

# Function to run SQL file in container
function Run-SqlFile {
    param(
        [string]$ContainerName,
        [string]$DatabaseName,
        [string]$SqlFile
    )
    
    Write-Host "Copying $SqlFile to $ContainerName..." -ForegroundColor Yellow
    docker cp $SqlFile "$ContainerName`:/tmp/seed.sql"
    
    Write-Host "Executing seed data for $DatabaseName..." -ForegroundColor Yellow
    docker compose exec $ContainerName psql -U postgres -d $DatabaseName -f /tmp/seed.sql
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Seed data for $DatabaseName completed successfully" -ForegroundColor Green
    } else {
        Write-Host "✗ Seed data for $DatabaseName failed" -ForegroundColor Red
    }
}

# Check if seed files exist
$seedFiles = @(
    "init-scripts/02-user-db-seed.sql",
    "init-scripts/05-cart-db-seed.sql",
    "init-scripts/06-order-db-seed.sql",
    "init-scripts/07-payment-db-seed.sql",
    "init-scripts/09-notification-db-seed.sql",
    "init-scripts/08-ai-db-seed.sql"
)

foreach ($file in $seedFiles) {
    if (-not (Test-Path $file)) {
        Write-Host "✗ File not found: $file" -ForegroundColor Red
        Write-Host "Please paste AI-generated content into all seed files first." -ForegroundColor Yellow
        exit 1
    }
}

# Run seed data for each database
Run-SqlFile "postgres-user" "user_db" "init-scripts/02-user-db-seed.sql"
Run-SqlFile "postgres-cart" "cart_db" "init-scripts/05-cart-db-seed.sql"
Run-SqlFile "postgres-order" "order_db" "init-scripts/06-order-db-seed.sql"
Run-SqlFile "postgres-payment" "payment_db" "init-scripts/07-payment-db-seed.sql"
Run-SqlFile "postgres-notification" "notification_db" "init-scripts/09-notification-db-seed.sql"
Run-SqlFile "postgres-ai" "ai_db" "init-scripts/08-ai-db-seed.sql"

Write-Host ""
Write-Host "=== All seed data completed ===" -ForegroundColor Green
