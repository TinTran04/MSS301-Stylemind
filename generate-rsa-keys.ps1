# generate-rsa-keys.ps1
# Run from project root: .\generate-rsa-keys.ps1

# PREREQUISITE: If openssl is not recognized, add Git/usr/bin to PATH:
# $env:Path += ";C:\Program Files\Git\usr\bin"

$certsDir = "BE\.docker\certs"

# Create directory if it doesn't exist
if (-not (Test-Path $certsDir)) {
    New-Item -ItemType Directory -Path $certsDir | Out-Null
}

# Add Git usr/bin to PATH if needed
$opensslPath = "C:\Program Files\Git\usr\bin\openssl.exe"
if (-not (Get-Command openssl -ErrorAction SilentlyContinue)) {
    if (Test-Path $opensslPath) {
        $env:Path += ";C:\Program Files\Git\usr\bin"
    } else {
        Write-Host "ERROR: OpenSSL not found. Please install Git for Windows or add OpenSSL to PATH."
        exit 1
    }
}

# Generate RSA-2048 Private Key using modern openssl genpkey
Write-Host "Generating RSA-2048 private key..."
& openssl genpkey -algorithm RSA -out "$certsDir\private_key.pem" -pkeyopt rsa_keygen_bits:2048

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Failed to generate private key."
    exit 1
}

# Extract Public Key from Private Key
Write-Host "Extracting public key from private key..."
& openssl rsa -in "$certsDir\private_key.pem" -pubout -out "$certsDir\public_key.pem"

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Failed to extract public key."
    exit 1
}

# Set appropriate permissions
Write-Host "Setting file permissions..."
icacls "$certsDir\private_key.pem" /inheritance:r | Out-Null
icacls "$certsDir\private_key.pem" /grant:r "$($env:USERNAME):(R)" | Out-Null

Write-Host "✅ RSA-2048 key pair generated successfully:"
Write-Host "   Private Key: $certsDir\private_key.pem"
Write-Host "   Public Key:  $certsDir\public_key.pem"
