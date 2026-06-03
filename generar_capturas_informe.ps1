# ============================================================
# generar_capturas_informe.ps1
# Ejecuta los tests y guarda las capturas del informe de caja blanca
# Ejecutar desde la raíz del proyecto: .\generar_capturas_informe.ps1
# ============================================================

$projectDir = "C:\Users\Napero\Desktop\7test-hosteado"
$capturesDir = "$projectDir\capturas_informe_v2"
New-Item -ItemType Directory -Force -Path $capturesDir | Out-Null

# ── Función para tomar captura de pantalla completa ──────────
function Capturar($nombre) {
    Add-Type -AssemblyName System.Drawing
    Add-Type -AssemblyName System.Windows.Forms
    $pantalla = [System.Windows.Forms.Screen]::PrimaryScreen.Bounds
    $bmp = New-Object System.Drawing.Bitmap($pantalla.Width, $pantalla.Height)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.CopyFromScreen($pantalla.Location, [System.Drawing.Point]::Empty, $pantalla.Size)
    $ruta = "$capturesDir\$nombre.png"
    $bmp.Save($ruta)
    $g.Dispose(); $bmp.Dispose()
    Write-Host "  [OK] Guardada: $nombre.png" -ForegroundColor Green
}

# ── Función para abrir archivo en VS Code y esperar ──────────
function AbrirEnVSCode($archivo, $pausa = 3) {
    code $archivo
    Start-Sleep -Seconds $pausa
}

Set-Location $projectDir

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  Generador de capturas — Informe Caja Blanca" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# ── JAVA HOME ─────────────────────────────────────────────────
$intellijJbr = "C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.2\jbr"
if (Test-Path $intellijJbr) {
    $env:JAVA_HOME = $intellijJbr
    $env:Path = "$env:JAVA_HOME\bin;$env:Path"
    Write-Host "[1/9] Java configurado desde IntelliJ JBR" -ForegroundColor Yellow
} else {
    Write-Host "[1/9] Usando Java del PATH del sistema" -ForegroundColor Yellow
}

# ── EJECUTAR TESTS ────────────────────────────────────────────
Write-Host "[2/9] Ejecutando tests (puede tardar 30-60 segundos)..." -ForegroundColor Yellow
$outputLines = & .\mvnw.cmd test 2>&1
$outputText = $outputLines -join "`n"

# Guardar output completo como txt (para incluir en el informe si hace falta)
$outputText | Out-File "$capturesDir\maven_output_completo.txt" -Encoding utf8

# Mostrar output en consola
$outputLines | ForEach-Object { Write-Host $_ }

Start-Sleep -Seconds 1

# ── CAPTURA 1: BUILD SUCCESS (terminal) ───────────────────────
Write-Host ""
Write-Host "[3/9] Tomando captura del resultado final (BUILD SUCCESS)..." -ForegroundColor Yellow
Start-Sleep -Seconds 1
Capturar "captura_01_build_success"

# ── VERIFICAR RESULTADO ───────────────────────────────────────
$buildSuccess = $outputText -match "BUILD SUCCESS"
if (-not $buildSuccess) {
    Write-Host ""
    Write-Host "  ATENCION: El build NO termino en BUILD SUCCESS." -ForegroundColor Red
    Write-Host "  Revisa los errores antes de continuar con el informe." -ForegroundColor Red
    Write-Host ""
}

# ── CAPTURA 9: CARPETA SUREFIRE-REPORTS ──────────────────────
Write-Host "[4/9] Abriendo carpeta surefire-reports..." -ForegroundColor Yellow
$surefireDir = "$projectDir\target\surefire-reports"
if (Test-Path $surefireDir) {
    Start-Process explorer.exe $surefireDir
    Start-Sleep -Seconds 2
    Capturar "captura_09_surefire_reports"
} else {
    Write-Host "  carpeta surefire-reports no encontrada (ejecutá los tests primero)" -ForegroundColor Red
}

# ── CAPTURAS DE CÓDIGO FUENTE (VS Code) ──────────────────────
Write-Host "[5/9] Abriendo archivos de codigo fuente en VS Code..." -ForegroundColor Yellow

# Captura 2: Configuración Mockito en ExamServiceTest
$examServiceTest = "$projectDir\src\test\java\com\seventest\application\service\ExamServiceTest.java"
AbrirEnVSCode $examServiceTest 4
Capturar "captura_02_mockito_config_ExamServiceTest"

# Captura 10: Rama negativa publicación
Capturar "captura_10_rama_negativa_publicacion"

# Captura 11: Rama positiva publicación
Capturar "captura_11_rama_positiva_publicacion"

# Captura 12: ExamSubmissionServiceTest
$submissionTest = "$projectDir\src\test\java\com\seventest\application\service\ExamSubmissionServiceTest.java"
AbrirEnVSCode $submissionTest 4
Capturar "captura_12_ExamSubmissionServiceTest"

# Captura 13: validateReadyToPublish en ExamService.java
$examService = "$projectDir\src\main\java\com\seventest\application\service\ExamService.java"
AbrirEnVSCode $examService 4
Capturar "captura_13_validateReadyToPublish"

# Captura 6: Corrección del color en ExamService.java
Capturar "captura_06_color_corregido_1956D8"

# Captura 8: application.yaml de test
$testYaml = "$projectDir\src\test\resources\application.yaml"
AbrirEnVSCode $testYaml 3
Capturar "captura_08_test_application_yaml"

# ── CAPTURAS DE SUREFIRE TXT ──────────────────────────────────
Write-Host "[6/9] Abriendo reportes Surefire individuales..." -ForegroundColor Yellow

$examServiceTxt = "$surefireDir\com.seventest.application.service.ExamServiceTest.txt"
if (Test-Path $examServiceTxt) {
    AbrirEnVSCode $examServiceTxt 3
    Capturar "captura_03_surefire_ExamServiceTest"
}

$submissionTxt = "$surefireDir\com.seventest.application.service.ExamSubmissionServiceTest.txt"
if (Test-Path $submissionTxt) {
    AbrirEnVSCode $submissionTxt 3
    Capturar "captura_04_surefire_ExamSubmissionServiceTest"
}

# ── RESUMEN FINAL ─────────────────────────────────────────────
Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  Capturas guardadas en:" -ForegroundColor Cyan
Write-Host "  $capturesDir" -ForegroundColor White
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Capturas generadas:" -ForegroundColor Yellow
Get-ChildItem "$capturesDir\*.png" | ForEach-Object {
    Write-Host "  $($_.Name)" -ForegroundColor White
}
Write-Host ""
Write-Host "Capturas que debes tomar MANUALMENTE:" -ForegroundColor Magenta
Write-Host "  captura_05_error_color_bug.png   -> log de GitHub Actions con el error #2563EB" -ForegroundColor White
Write-Host "  captura_07_error_contextloads.png -> log de GitHub Actions con el error de Spring context" -ForegroundColor White
Write-Host "  captura_14_salida_final_maven.png  -> ya esta cubierta por captura_01" -ForegroundColor White
Write-Host ""

# Abrir la carpeta de capturas al terminar
Start-Process explorer.exe $capturesDir
