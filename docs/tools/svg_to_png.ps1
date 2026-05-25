$ErrorActionPreference = 'Continue'
$svgDir = 'D:\BetterMe\docs\exports\svg'
$pngDir = 'D:\BetterMe\docs\exports\png'
$edge = 'C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe'

if (-not (Test-Path $pngDir)) { New-Item -ItemType Directory -Path $pngDir | Out-Null }

$tmpDir = Join-Path $env:TEMP 'betterme_docs_svg2png'
if (-not (Test-Path $tmpDir)) { New-Item -ItemType Directory -Path $tmpDir | Out-Null }

$svgFiles = Get-ChildItem -Path $svgDir -Filter '*.svg' | Sort-Object Name
Write-Host "Found $($svgFiles.Count) SVG file(s)"

$css = 'html,body{margin:0;padding:0;background:#fff;}body{display:inline-block;}svg{display:block;}'

foreach ($svg in $svgFiles) {
    $name = [System.IO.Path]::GetFileNameWithoutExtension($svg.Name)
    $svgXml = Get-Content -Raw -Path $svg.FullName -Encoding UTF8

    $w = 0
    $h = 0
    if ($svgXml -match 'width="(\d+)"') { $w = [int]$Matches[1] }
    if ($svgXml -match 'height="(\d+)"') { $h = [int]$Matches[1] }
    if ($w -eq 0 -or $h -eq 0) {
        Write-Warning "Skip $name -- no width/height parsed"
        continue
    }

    $html = '<!doctype html><html><head><meta charset="utf-8"><style>' + $css + '</style></head><body>' + $svgXml + '</body></html>'

    $htmlPath = Join-Path $tmpDir ($name + '.html')
    [System.IO.File]::WriteAllText($htmlPath, $html, [System.Text.Encoding]::UTF8)

    $pngPath = Join-Path $pngDir ($name + '.png')
    $fileUrl = 'file:///' + $htmlPath.Replace('\', '/')
    $winSize = "$w,$h"
    $shotArg = '--screenshot=' + $pngPath
    $winArg  = '--window-size=' + $winSize

    $errFile = Join-Path $tmpDir ($name + '.err.log')
    & $edge --headless=new --disable-gpu --hide-scrollbars --force-device-scale-factor=2 --default-background-color=FFFFFFFF $winArg $shotArg $fileUrl 2>$errFile | Out-Null

    if (Test-Path $pngPath) {
        $size = (Get-Item $pngPath).Length
        $line = "OK  {0,-50} {1,5}x{2,-5} -> {3} KB" -f $name, $w, $h, [int]($size / 1024)
        Write-Host $line
    } else {
        Write-Warning "FAILED $name"
    }
}
Write-Host ""
Write-Host "Done. PNGs in $pngDir"
