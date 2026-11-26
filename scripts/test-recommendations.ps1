param(
    [ValidateSet('Local', 'Supabase', 'LiveEmbedding')]
    [string]$Mode = 'Local'
)
$ErrorActionPreference = 'Stop'
$workspace = Split-Path $PSScriptRoot -Parent
Push-Location $workspace
try {
    # Tests load credentials internally; no secrets are passed as CLI arguments.
    $maven = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    $executable = if ($maven) { $maven.Source } else { Join-Path $workspace 'mvnw.cmd' }
    $arguments = @('-B', 'test', '-Drecommendation.supabase=false', '-Drecommendation.live=false')
    if ($Mode -eq 'Local') {
        $arguments += '-Dtest=RecommendationServiceTest,RecommendationControllerTest,SemanticIndexServiceTest,SemanticRecommendationConfigurationTest,InclusiveCosinePgVectorStoreTest,ProductViewHistoryTest'
    } else {
        $arguments = @('-B', 'test', '-Dtest=SupabaseRecommendationIntegrationTest', '-Drecommendation.supabase=true')
        $arguments += if ($Mode -eq 'LiveEmbedding') { '-Drecommendation.live=true' } else { '-Drecommendation.live=false' }
    }
    & $executable @arguments
    if ($LASTEXITCODE -ne 0) { throw "Recommendation checks failed (exit code $LASTEXITCODE)." }
} finally { Pop-Location }
