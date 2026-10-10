[CmdletBinding()]
param(
    [ValidateSet('debug', 'release')][string] $Mode = 'debug',
    [ValidateSet('android-arm64', 'android-x64')][string] $TargetPlatform = 'android-arm64',
    [string] $BuildName = '0.1.0',
    [int] $BuildNumber = 7,
    [string] $FlutterSdk,
    [string] $AndroidSdk = "$env:LOCALAPPDATA/Android/Sdk",
    [string] $Jdk,
    [string] $ProxyHost,
    [int] $ProxyPort = 10646
)
$ErrorActionPreference = 'Stop'
$root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
if (!$FlutterSdk) { $FlutterSdk = Join-Path $root '.tools/flutter-quest' }
if (!$Jdk) {
    $Jdk = Get-ChildItem (Join-Path $root '.tools/jdk17') -Directory |
        Select-Object -First 1 -ExpandProperty FullName
}
foreach ($file in @("$FlutterSdk/bin/flutter.bat", "$Jdk/bin/java.exe", "$AndroidSdk/platform-tools/adb.exe")) {
    if (!(Test-Path -LiteralPath $file)) { throw "Required tool missing: $file" }
}
$sourceCommit = (& git -C $root rev-parse HEAD).Trim()
if ($LASTEXITCODE) { throw 'Cannot determine source commit' }
if ($Mode -eq 'release' -and (& git -C $root status --porcelain)) {
    throw 'Commit the intended source before release building; dirty trees are not release identities.'
}
$before = @{}
foreach ($name in @('JAVA_HOME','ANDROID_HOME','ANDROID_SDK_ROOT','FLUTTER_ROOT','JAVA_TOOL_OPTIONS','Path')) {
    $before[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}
try {
    $env:JAVA_HOME = $Jdk
    $env:ANDROID_HOME = $AndroidSdk
    $env:ANDROID_SDK_ROOT = $AndroidSdk
    $env:FLUTTER_ROOT = $FlutterSdk
    $env:Path = "$FlutterSdk/bin;$Jdk/bin;$env:Path"
    if ($ProxyHost) {
        if ($ProxyHost -notmatch '^[a-zA-Z0-9.:-]+$') { throw 'ProxyHost must be a hostname or IP without credentials' }
        $env:JAVA_TOOL_OPTIONS = "$env:JAVA_TOOL_OPTIONS -Dhttps.proxyHost=$ProxyHost -Dhttps.proxyPort=$ProxyPort -Dhttp.proxyHost=$ProxyHost -Dhttp.proxyPort=$ProxyPort"
    }
    Push-Location (Join-Path $root 'clients/piliplus')
    try {
        $pubOutput = & "$FlutterSdk/bin/flutter.bat" pub get --enforce-lockfile 2>&1
        $pubExit = $LASTEXITCODE
        $pubOutput | Write-Output
        if ($pubExit -and ($pubOutput -join "`n") -match 'requires symlink support') {
            & (Join-Path $PSScriptRoot 'Initialize-QuestPluginLinks.ps1')
            & "$FlutterSdk/bin/flutter.bat" pub get --enforce-lockfile
            $pubExit = $LASTEXITCODE
        }
        if ($pubExit) { throw 'Dependency preparation failed' }
        & "$FlutterSdk/bin/flutter.bat" build apk "--$Mode" --target-platform $TargetPlatform --build-name $BuildName --build-number $BuildNumber --no-pub "--dart-define=pili.hash=$sourceCommit" "--dart-define=pili.name=$BuildName" "--dart-define=pili.code=$BuildNumber" "--dart-define=vr.patch=clean" "--dart-define=vr.label=ordinary-panel"
        if ($LASTEXITCODE) { throw 'APK build failed' }
        Get-FileHash "build/app/outputs/flutter-apk/app-$Mode.apk" -Algorithm SHA256
    } finally { Pop-Location }
} finally {
    foreach ($name in $before.Keys) { [Environment]::SetEnvironmentVariable($name, $before[$name], 'Process') }
}
