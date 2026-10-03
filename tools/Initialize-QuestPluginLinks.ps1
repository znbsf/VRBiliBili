[CmdletBinding()]
param([string] $ClientRoot = (Join-Path $PSScriptRoot '../clients/piliplus'))
$ErrorActionPreference = 'Stop'
if (!$IsWindows) { return }
$ClientRoot = [IO.Path]::GetFullPath($ClientRoot)
$metadata = Get-Content -LiteralPath (Join-Path $ClientRoot '.flutter-plugins-dependencies') -Raw | ConvertFrom-Json
# Junctions need no Windows Developer Mode; only generated desktop plugin links
# are touched. Android builds do not use these desktop binaries.
foreach ($platform in @('windows', 'linux')) {
    $directory = Join-Path $ClientRoot "$platform/flutter/ephemeral/.plugin_symlinks"
    New-Item -ItemType Directory -Force -Path $directory | Out-Null
    foreach ($plugin in $metadata.plugins.$platform) {
        if ($plugin.name -notmatch '^[a-zA-Z0-9_]+$') { throw 'Invalid plugin name' }
        $link = Join-Path $directory $plugin.name
        if (!(Test-Path -LiteralPath $link)) {
            New-Item -ItemType Junction -Path $link -Target $plugin.path | Out-Null
        }
    }
}
