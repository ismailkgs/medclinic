$ErrorActionPreference = 'Stop'
Set-Location (Split-Path -Parent $PSScriptRoot)
if (-not (Test-Path "$env:JAVA_HOME/bin/java.exe")) { throw 'Укажи JAVA_HOME на JDK 21 перед запуском' }
& ./mvnw.cmd -B -DskipTests package
if ($LASTEXITCODE -ne 0) { throw 'Сборка Java завершилась ошибкой' }
docker compose --profile full up -d --build
if ($LASTEXITCODE -ne 0) { throw 'Docker Compose завершился ошибкой' }
Write-Host 'API: http://localhost:8082/api/health ; Keycloak: http://localhost:8081'
