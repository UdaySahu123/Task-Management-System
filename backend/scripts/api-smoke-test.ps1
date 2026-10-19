# =============================================================================
# API smoke test for the Task Management System backend.
#
# Requires the backend to be running (see README). Uses unique emails on every
# run, so it can be executed repeatedly against the same database.
#
# Usage:  powershell -ExecutionPolicy Bypass -File .\scripts\api-smoke-test.ps1
# =============================================================================
param(
    [string]$BaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"
$script:failures = 0

function Write-Check([string]$Name, [bool]$Ok, [string]$Detail = "") {
    if ($Ok) {
        Write-Host "  [PASS] $Name" -ForegroundColor Green
    } else {
        Write-Host "  [FAIL] $Name $Detail" -ForegroundColor Red
        $script:failures++
    }
}

# Performs an HTTP request and always returns { Status, Body } without throwing.
function Invoke-Api {
    param([string]$Method, [string]$Path, [string]$Token, $Body)

    $headers = @{ "Content-Type" = "application/json" }
    if ($Token) { $headers["Authorization"] = "Bearer $Token" }

    $params = @{
        Uri             = "$BaseUrl$Path"
        Method          = $Method
        Headers         = $headers
        UseBasicParsing = $true
        TimeoutSec      = 15
    }
    if ($null -ne $Body) { $params["Body"] = ($Body | ConvertTo-Json -Depth 6) }

    try {
        $resp = Invoke-WebRequest @params
        $json = $null
        if ($resp.Content) { try { $json = $resp.Content | ConvertFrom-Json } catch { $json = $resp.Content } }
        return [pscustomobject]@{ Status = [int]$resp.StatusCode; Body = $json }
    } catch [System.Net.WebException] {
        $r = $_.Exception.Response
        $status = if ($r) { [int]$r.StatusCode } else { 0 }
        $body = $null
        if ($_.ErrorDetails -and $_.ErrorDetails.Message) {
            try { $body = $_.ErrorDetails.Message | ConvertFrom-Json } catch { $body = $_.ErrorDetails.Message }
        }
        return [pscustomobject]@{ Status = $status; Body = $body }
    }
}

function Count-Of($Value) {
    if ($null -eq $Value) { return 0 }
    return @($Value).Count
}

$stamp  = Get-Date -Format "yyyyMMddHHmmss"
$emailA = "alice.$stamp@example.com"
$emailB = "bob.$stamp@example.com"
$today  = (Get-Date).ToString("yyyy-MM-dd")
$in3    = (Get-Date).AddDays(3).ToString("yyyy-MM-dd")
$in5    = (Get-Date).AddDays(5).ToString("yyyy-MM-dd")
$past   = (Get-Date).AddDays(-2).ToString("yyyy-MM-dd")

Write-Host "`n=== Auth ===" -ForegroundColor Cyan

$regA = Invoke-Api -Method POST -Path "/api/auth/register" -Body @{
    name = "Alice"; email = $emailA; password = "Passw0rd123"; confirmPassword = "Passw0rd123" }
Write-Check "register user A -> 201 + token" ($regA.Status -eq 201 -and $regA.Body.token) "got $($regA.Status)"
$tokenA = $regA.Body.token

$regB = Invoke-Api -Method POST -Path "/api/auth/register" -Body @{
    name = "Bob"; email = $emailB; password = "Passw0rd123"; confirmPassword = "Passw0rd123" }
Write-Check "register user B -> 201 + token" ($regB.Status -eq 201 -and $regB.Body.token) "got $($regB.Status)"
$tokenB = $regB.Body.token

$dup = Invoke-Api -Method POST -Path "/api/auth/register" -Body @{
    name = "Alice Again"; email = $emailA; password = "Passw0rd123"; confirmPassword = "Passw0rd123" }
Write-Check "duplicate email -> 409" ($dup.Status -eq 409) "got $($dup.Status)"

$weak = Invoke-Api -Method POST -Path "/api/auth/register" -Body @{
    name = "Weak"; email = "weak.$stamp@example.com"; password = "short"; confirmPassword = "short" }
Write-Check "weak password -> 400" ($weak.Status -eq 400) "got $($weak.Status)"

$badLogin = Invoke-Api -Method POST -Path "/api/auth/login" -Body @{ email = $emailA; password = "WrongPass123" }
Write-Check "wrong password -> 401" ($badLogin.Status -eq 401) "got $($badLogin.Status)"

$login = Invoke-Api -Method POST -Path "/api/auth/login" -Body @{ email = $emailA; password = "Passw0rd123" }
Write-Check "login -> 200 + token" ($login.Status -eq 200 -and $login.Body.token) "got $($login.Status)"

$me = Invoke-Api -Method GET -Path "/api/auth/me" -Token $tokenA
Write-Check "GET /auth/me returns profile" ($me.Status -eq 200 -and $me.Body.email -eq $emailA)
Write-Check "profile hides password hash" ($null -eq $me.Body.passwordHash)

$noAuth = Invoke-Api -Method GET -Path "/api/tasks"
Write-Check "unauthenticated /tasks -> 401" ($noAuth.Status -eq 401) "got $($noAuth.Status)"

$badToken = Invoke-Api -Method GET -Path "/api/tasks" -Token "not.a.valid.token"
Write-Check "invalid token -> 401" ($badToken.Status -eq 401) "got $($badToken.Status)"

Write-Host "`n=== Task creation + validation ===" -ForegroundColor Cyan

$t1 = Invoke-Api -Method POST -Path "/api/tasks" -Token $tokenA -Body @{
    title = "Write project report"; description = "Q4 summary"; status = "TODO"; priority = "HIGH"; dueDate = $in3 }
Write-Check "create task 1 -> 201" ($t1.Status -eq 201 -and $t1.Body.id) "got $($t1.Status)"
$id1 = $t1.Body.id

$t2 = Invoke-Api -Method POST -Path "/api/tasks" -Token $tokenA -Body @{
    title = "Refactor reporting module"; description = "cleanup"; status = "IN_PROGRESS"; priority = "MEDIUM"; dueDate = $in5 }
Write-Check "create task 2 -> 201" ($t2.Status -eq 201) "got $($t2.Status)"
$id2 = $t2.Body.id

$t3 = Invoke-Api -Method POST -Path "/api/tasks" -Token $tokenA -Body @{
    title = "Deploy release"; description = "Production deploy"; status = "COMPLETED"; priority = "LOW"; dueDate = $in5 }
Write-Check "create task 3 -> 201" ($t3.Status -eq 201) "got $($t3.Status)"
$id3 = $t3.Body.id

$t4 = Invoke-Api -Method POST -Path "/api/tasks" -Token $tokenA -Body @{
    title = "Overdue security patch"; description = "past due"; status = "TODO"; priority = "HIGH"; dueDate = $past }
Write-Check "create overdue task -> 201" ($t4.Status -eq 201) "got $($t4.Status)"
$id4 = $t4.Body.id

$blank = Invoke-Api -Method POST -Path "/api/tasks" -Token $tokenA -Body @{ title = "   "; dueDate = $today }
Write-Check "blank title -> 400" ($blank.Status -eq 400) "got $($blank.Status)"

$badEnum = Invoke-Api -Method POST -Path "/api/tasks" -Token $tokenA -Body @{ title = "Bad enum"; status = "BLOCKED"; dueDate = $today }
Write-Check "invalid status -> 400" ($badEnum.Status -eq 400) "got $($badEnum.Status)"

$missingDue = Invoke-Api -Method POST -Path "/api/tasks" -Token $tokenA -Body @{ title = "No due date" }
Write-Check "missing due date -> 400" ($missingDue.Status -eq 400) "got $($missingDue.Status)"

Write-Host "`n=== Read / search / filter / sort / paginate ===" -ForegroundColor Cyan

$list = Invoke-Api -Method GET -Path "/api/tasks" -Token $tokenA
Write-Check "list -> 200" ($list.Status -eq 200) "got $($list.Status)"
Write-Check "list contains 4 tasks" ($list.Body.totalElements -eq 4) "got $($list.Body.totalElements)"

$one = Invoke-Api -Method GET -Path "/api/tasks/$id1" -Token $tokenA
Write-Check "get by id returns the task" ($one.Status -eq 200 -and $one.Body.title -eq "Write project report")

$search = Invoke-Api -Method GET -Path "/api/tasks?search=report" -Token $tokenA
Write-Check "search 'report' matches 2 tasks" ($search.Body.totalElements -eq 2) "got $($search.Body.totalElements)"

$statusFilter = Invoke-Api -Method GET -Path "/api/tasks?status=COMPLETED" -Token $tokenA
Write-Check "filter status=COMPLETED -> 1 task" ($statusFilter.Body.totalElements -eq 1) "got $($statusFilter.Body.totalElements)"

$priorityFilter = Invoke-Api -Method GET -Path "/api/tasks?priority=HIGH" -Token $tokenA
Write-Check "filter priority=HIGH only returns HIGH" ((Count-Of ($priorityFilter.Body.content | Where-Object { $_.priority -ne 'HIGH' })) -eq 0)

$overdueFilter = Invoke-Api -Method GET -Path "/api/tasks?overdue=true" -Token $tokenA
Write-Check "overdue filter -> 1 overdue task" ($overdueFilter.Body.totalElements -eq 1) "got $($overdueFilter.Body.totalElements)"

$sorted = Invoke-Api -Method GET -Path "/api/tasks?sort=priority,asc" -Token $tokenA
$firstPriority = @($sorted.Body.content)[0].priority
Write-Check "sort priority,asc starts with HIGH" ($firstPriority -eq 'HIGH') "got $firstPriority"

$sortedDue = Invoke-Api -Method GET -Path "/api/tasks?sort=dueDate,asc" -Token $tokenA
$firstDue = @($sortedDue.Body.content)[0].title
Write-Check "sort dueDate,asc puts the overdue task first" ($firstDue -eq 'Overdue security patch') "got $firstDue"

$paged = Invoke-Api -Method GET -Path "/api/tasks?page=0&size=2" -Token $tokenA
Write-Check "pagination page0 size2 -> 2 rows, 2 pages" ((Count-Of $paged.Body.content) -eq 2 -and $paged.Body.totalPages -eq 2) "rows=$((Count-Of $paged.Body.content)) pages=$($paged.Body.totalPages)"

Write-Host "`n=== Update / patch / delete ===" -ForegroundColor Cyan

$updated = Invoke-Api -Method PUT -Path "/api/tasks/$id1" -Token $tokenA -Body @{
    title = "Renamed report"; description = "updated"; status = "IN_PROGRESS"; priority = "LOW"; dueDate = $in3 }
Write-Check "PUT updates title + status" ($updated.Status -eq 200 -and $updated.Body.title -eq "Renamed report" -and $updated.Body.status -eq "IN_PROGRESS") "got $($updated.Status)"

$patched = Invoke-Api -Method PATCH -Path "/api/tasks/$id1/status" -Token $tokenA -Body @{ status = "COMPLETED" }
Write-Check "PATCH status -> COMPLETED" ($patched.Status -eq 200 -and $patched.Body.status -eq "COMPLETED") "got $($patched.Status)"

$patchedPrio = Invoke-Api -Method PATCH -Path "/api/tasks/$id1/priority" -Token $tokenA -Body @{ priority = "HIGH" }
Write-Check "PATCH priority -> HIGH" ($patchedPrio.Status -eq 200 -and $patchedPrio.Body.priority -eq "HIGH") "got $($patchedPrio.Status)"

$badPatch = Invoke-Api -Method PATCH -Path "/api/tasks/$id1/status" -Token $tokenA -Body @{ status = "NOPE" }
Write-Check "PATCH invalid status -> 400" ($badEnum.Status -eq 400) "got $($badEnum.Status)"

$deleted = Invoke-Api -Method DELETE -Path "/api/tasks/$id2" -Token $tokenA
Write-Check "DELETE -> 204" ($deleted.Status -eq 204) "got $($deleted.Status)"

$afterDelete = Invoke-Api -Method GET -Path "/api/tasks/$id2" -Token $tokenA
Write-Check "deleted task -> 404" ($afterDelete.Status -eq 404) "got $($afterDelete.Status)"

$missing = Invoke-Api -Method GET -Path "/api/tasks/99999999" -Token $tokenA
Write-Check "unknown task id -> 404" ($missing.Status -eq 404) "got $($missing.Status)"

Write-Host "`n=== Ownership isolation ===" -ForegroundColor Cyan

$otherRead = Invoke-Api -Method GET -Path "/api/tasks/$id3" -Token $tokenB
Write-Check "user B cannot read user A's task -> 404" ($otherRead.Status -eq 404) "got $($otherRead.Status)"

$otherUpdate = Invoke-Api -Method PUT -Path "/api/tasks/$id3" -Token $tokenB -Body @{ title = "Hijacked"; dueDate = $today }
Write-Check "user B cannot update user A's task -> 404" ($otherUpdate.Status -eq 404) "got $($otherUpdate.Status)"

$otherDelete = Invoke-Api -Method DELETE -Path "/api/tasks/$id3" -Token $tokenB
Write-Check "user B cannot delete user A's task -> 404" ($otherDelete.Status -eq 404) "got $($otherDelete.Status)"

$bList = Invoke-Api -Method GET -Path "/api/tasks" -Token $tokenB
Write-Check "user B sees only their own (0) tasks" ($bList.Body.totalElements -eq 0) "got $($bList.Body.totalElements)"

Write-Host "`n=== Dashboard ===" -ForegroundColor Cyan

# A pending task with a future due date so upcomingDeadlines is non-empty.
$upcoming = Invoke-Api -Method POST -Path "/api/tasks" -Token $tokenA -Body @{
    title = "Plan next sprint"; status = "TODO"; priority = "MEDIUM"; dueDate = (Get-Date).AddDays(1).ToString("yyyy-MM-dd") }
Write-Check "create upcoming task -> 201" ($upcoming.Status -eq 201) "got $($upcoming.Status)"

$stats = Invoke-Api -Method GET -Path "/api/dashboard/stats" -Token $tokenA
Write-Check "dashboard stats -> 200" ($stats.Status -eq 200) "got $($stats.Status)"
Write-Check "totalTasks = 4" ($stats.Body.totalTasks -eq 4) "got $($stats.Body.totalTasks)"
Write-Check "pendingTasks (TODO) >= 1" ($stats.Body.pendingTasks -ge 1) "got $($stats.Body.pendingTasks)"
Write-Check "completedTasks >= 1" ($stats.Body.completedTasks -ge 1) "got $($stats.Body.completedTasks)"
Write-Check "overdueTasks = 1" ($stats.Body.overdueTasks -eq 1) "got $($stats.Body.overdueTasks)"
Write-Check "completionPercentage 0..100" ($stats.Body.completionPercentage -ge 0 -and $stats.Body.completionPercentage -le 100) "got $($stats.Body.completionPercentage)"
Write-Check "recentTasks returned" ((Count-Of $stats.Body.recentTasks) -ge 1)
Write-Check "upcomingDeadlines returned" ((Count-Of $stats.Body.upcomingDeadlines) -ge 1)

Write-Host "`n=== Profile ===" -ForegroundColor Cyan

$profilePut = Invoke-Api -Method PUT -Path "/api/users/me" -Token $tokenA -Body @{ name = "Alice Updated"; email = $emailA }
Write-Check "PUT /users/me updates name" ($profilePut.Status -eq 200 -and $profilePut.Body.name -eq "Alice Updated") "got $($profilePut.Status)"

$pwChange = Invoke-Api -Method PUT -Path "/api/users/me/password" -Token $tokenA -Body @{
    currentPassword = "Passw0rd123"; newPassword = "NewPassw0rd456" }
Write-Check "password change -> 200" ($pwChange.Status -eq 200) "got $($pwChange.Status)"

$relogin = Invoke-Api -Method POST -Path "/api/auth/login" -Body @{ email = $emailA; password = "NewPassw0rd456" }
Write-Check "login with new password -> 200" ($relogin.Status -eq 200) "got $($relogin.Status)"

$oldPw = Invoke-Api -Method POST -Path "/api/auth/login" -Body @{ email = $emailA; password = "Passw0rd123" }
Write-Check "login with old password -> 401" ($oldPw.Status -eq 401) "got $($oldPw.Status)"

Write-Host ""
if ($script:failures -eq 0) {
    Write-Host "ALL SMOKE TESTS PASSED" -ForegroundColor Green
    exit 0
} else {
    Write-Host "$script:failures SMOKE TEST(S) FAILED" -ForegroundColor Red
    exit 1
}
