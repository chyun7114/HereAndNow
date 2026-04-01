param(
    [ValidateSet("small", "large", "both")]
    [string]$Dataset = "both"
)

$ErrorActionPreference = "Stop"

$ComposeFile = "docker/query-test/docker-compose.yml"
$DbUser = "hereandnow"
$DbName = "hereandnow_query"

$MigrationFiles = @(
    "/workspace/src/main/resources/db/migration/V1__init_schema.sql",
    "/workspace/src/main/resources/db/migration/V2_1__add_key_constraints_course_scrap.sql",
    "/workspace/src/main/resources/db/migration/V2_2__add_key_constraint_couple_course_comment.sql",
    "/workspace/src/main/resources/db/migration/V2_3__add_key_constraints_pin_image.sql",
    "/workspace/src/main/resources/db/migration/V2_4__add_key_contraints_pin_tag.sql",
    "/workspace/src/main/resources/db/migration/V2__add_key_constraint_course.sql"
)

function Invoke-Psql([string]$SqlCommand) {
    docker compose -f $ComposeFile exec -T postgis `
        psql -U $DbUser -d $DbName -v ON_ERROR_STOP=1 -c $SqlCommand | Out-Host
}

function Invoke-PsqlFile([string]$FilePath) {
    docker compose -f $ComposeFile exec -T postgis `
        psql -U $DbUser -d $DbName -v ON_ERROR_STOP=1 -f $FilePath | Out-Host
}

function Wait-PostgisReady {
    Write-Host "[1/4] PostGIS 컨테이너 상태 확인 중..."
    for ($i = 1; $i -le 40; $i++) {
        $ok = docker compose -f $ComposeFile exec -T postgis `
            pg_isready -U $DbUser -d $DbName 2>$null
        if ($LASTEXITCODE -eq 0) {
            Write-Host "PostGIS 준비 완료"
            return
        }
        Start-Sleep -Seconds 2
    }
    throw "PostGIS가 준비되지 않았습니다."
}

function Apply-Schema {
    Write-Host "[2/4] 스키마/마이그레이션 적용 중..."
    foreach ($file in $MigrationFiles) {
        Write-Host " - applying: $file"
        Invoke-PsqlFile $file
    }
}

function Seed-Dataset([string]$Name, [int]$PlaceCount, [int]$CourseCount) {
    Write-Host "[3/4] 데이터셋 생성: $Name (place=$PlaceCount, course=$CourseCount)"
    docker compose -f $ComposeFile exec -T postgis `
        psql -U $DbUser -d $DbName -v ON_ERROR_STOP=1 `
        -v dataset_prefix=$Name `
        -v place_count=$PlaceCount `
        -v course_count=$CourseCount `
        -v lat_spread=0.35 `
        -v lon_spread=0.45 `
        -f /workspace/scripts/query-test/seed_query_dataset.sql | Out-Host
}

Write-Host "[0/4] 실행 컨테이너 기동 중..."
docker compose -f $ComposeFile up -d | Out-Host

Wait-PostgisReady
Apply-Schema

switch ($Dataset) {
    "small" { Seed-Dataset -Name "ds10k" -PlaceCount 10000 -CourseCount 2800 }
    "large" { Seed-Dataset -Name "ds100k" -PlaceCount 100000 -CourseCount 28000 }
    "both" {
        Seed-Dataset -Name "ds10k" -PlaceCount 10000 -CourseCount 2800
        Seed-Dataset -Name "ds100k" -PlaceCount 100000 -CourseCount 28000
    }
}

Write-Host "[4/4] 완료"
Write-Host "PostGIS: localhost:5432 / DB=hereandnow_query / USER=hereandnow / PASSWORD=hereandnow"
Write-Host "Redis  : localhost:6379"
