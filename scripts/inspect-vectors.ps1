param(
    [string]$BaseUrl = 'http://localhost:8080',
    [switch]$Reindex,
    [string]$OutputPath
)
$ErrorActionPreference = 'Stop'
$workspace = Split-Path $PSScriptRoot -Parent
$adminSettings = @{}
$dotenv = Join-Path $workspace '.env'
if (Test-Path -LiteralPath $dotenv) {
    foreach ($line in Get-Content -LiteralPath $dotenv) {
        if ($line -match '^\s*(ADMIN_USERNAME|ADMIN_PASSWORD)\s*=(.*)$') {
            $adminSettings[$matches[1]] = $matches[2].Trim()
        }
    }
}
$username = if ($env:ADMIN_USERNAME) { $env:ADMIN_USERNAME } elseif ($adminSettings.ContainsKey('ADMIN_USERNAME')) { $adminSettings['ADMIN_USERNAME'] } else { 'admin' }
$password = if ($env:ADMIN_PASSWORD) { $env:ADMIN_PASSWORD } elseif ($adminSettings.ContainsKey('ADMIN_PASSWORD')) { $adminSettings['ADMIN_PASSWORD'] } else { 'admin1234' }
$headers = @{ Authorization = 'Basic ' + [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($username + ':' + $password)) }
if ($Reindex) {
    # Explicit opt-in: this POST reads the catalog and may call Gemini for its documents.
    $csrf = Invoke-RestMethod "$BaseUrl/api/csrf" -SessionVariable vectorSession -TimeoutSec 15
    $headers[$csrf.headerName] = $csrf.token
    $result = Invoke-RestMethod "$BaseUrl/api/admin/vector/reindex" -Method Post -Headers $headers -WebSession $vectorSession -TimeoutSec 600
    if ($result.status -ne 'SUCCESS') { throw 'Reindex did not return SUCCESS.' }
    Write-Output "Reindex SUCCESS: products=$($result.productCount), documents=$($result.documentCount)"
}
$documents = [Collections.Generic.List[object]]::new()
$offset = 0
do {
    $page = Invoke-RestMethod "$BaseUrl/api/admin/vector/documents?offset=$offset&limit=100" -Headers $headers -TimeoutSec 30
    foreach ($document in $page.documents) {
        if ($document.embedding.Count -ne $document.dimensions -or $document.dimensions -ne 768) {
            throw "Unexpected vector dimensions for document $($document.id)."
        }
        foreach ($number in $document.embedding) {
            if ([double]::IsNaN($number) -or [double]::IsInfinity($number)) { throw 'Non-finite vector value.' }
        }
        $documents.Add($document)
    }
    $offset += $page.documents.Count
} while ($page.documents.Count -gt 0 -and $offset -lt $page.total)
if ($documents.Count -ne $page.total) { throw 'Index changed during pagination; read the page again.' }
Write-Output "Inspection PASS: storage=$($page.storage), documents=$($documents.Count), dimensions=768 for every returned document"
if ($documents.Count -gt 0) {
    Write-Output "First stored vector numbers: $($documents[0].embedding[0..5] -join ', ')"
}
if ($OutputPath) {
    $result = @{ storage = $page.storage; total = $documents.Count; documents = @($documents.ToArray()) }
    $result | ConvertTo-Json -Depth 30 | Set-Content -LiteralPath $OutputPath -Encoding UTF8
    Write-Output "Saved stored documents: $OutputPath"
}
