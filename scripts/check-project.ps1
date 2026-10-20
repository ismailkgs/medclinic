param([string]$BaseUrl = 'http://localhost:8082', [string]$KeycloakUrl = 'http://localhost:8081')
$ErrorActionPreference = 'Stop'
$script:checks = 0
function Assert-Check([bool]$Condition,[string]$Label) {
    if (-not $Condition) { throw "FAIL: $Label" }
    $script:checks++
    Write-Host "PASS: $Label"
}
function Get-Token([string]$Username,[string]$Password) {
    $form = @{ grant_type='password'; client_id='medical-cli'; username=$Username; password=$Password }
    return (Invoke-RestMethod -Method Post -Uri "$KeycloakUrl/realms/medical/protocol/openid-connect/token" -ContentType 'application/x-www-form-urlencoded' -Body $form).access_token
}
function Call-Api([string]$Method,[string]$Path,[string]$Token,$Data) {
    $parameters = @{ Method=$Method; Uri="$BaseUrl$Path"; UseBasicParsing=$true; TimeoutSec=30 }
    if ($Token) { $parameters.Headers=@{ Authorization="Bearer $Token" } }
    if ($null -ne $Data) { $parameters.ContentType='application/json; charset=utf-8'; $parameters.Body=[System.Text.Encoding]::UTF8.GetBytes((ConvertTo-Json -InputObject $Data -Depth 8)) }
    try {
        $response=Invoke-WebRequest @parameters
        $status=[int]$response.StatusCode
        $content=$response.Content
    } catch {
        if ($null -eq $_.Exception.Response) { throw }
        $status=[int]$_.Exception.Response.StatusCode
        $content=$_.ErrorDetails.Message
    }
    $parsed=$null
    if ($content) { try { $parsed=$content | ConvertFrom-Json } catch { $parsed=$content } }
    return @{ status=$status; data=$parsed }
}
function New-TestSlot([long]$DoctorId,[string]$Token,[bool]$Near) {
    for ($attempt=0; $attempt -lt 10; $attempt++) {
        if ($Near) { $start=[DateTimeOffset]::UtcNow.AddMinutes((Get-Random -Minimum 60 -Maximum 240)) } else { $start=[DateTimeOffset]::UtcNow.AddDays(10).AddMinutes((Get-Random -Minimum 0 -Maximum 10000)) }
        $body=@{ startsAt=$start.ToString('o'); endsAt=$start.AddMinutes(20).ToString('o') }
        $result=Call-Api 'Post' "/api/doctors/$DoctorId/slots" $Token $body
        if ($result.status -eq 201) { return @{ slot=$result.data; body=$body } }
        if ($result.status -ne 409) { throw "Slot creation returned HTTP $($result.status)" }
    }
    throw 'Не удалось подобрать свободный тестовый интервал'
}
$health=Call-Api 'Get' '/api/health' '' $null
Assert-Check ($health.status -eq 200) 'Public health returns 200'
$patientToken=Get-Token 'patient.demo' 'Patient_demo_2026!'
$doctorToken=Get-Token 'doctor.demo' 'Doctor_demo_2026!'
$adminToken=Get-Token 'admin.demo' 'Admin_demo_2026!'
$patientMe=Call-Api 'Get' '/api/me' $patientToken $null
$doctorMe=Call-Api 'Get' '/api/me' $doctorToken $null
Assert-Check ($patientMe.status -eq 200 -and @($patientMe.data.patientIds).Count -gt 0) 'Patient identity is linked'
Assert-Check ($doctorMe.status -eq 200 -and @($doctorMe.data.doctorIds).Count -gt 0) 'Doctor identity is linked'
$patientId=$patientMe.data.patientIds[0]
$doctorId=$doctorMe.data.doctorIds[0]
Assert-Check ((Call-Api 'Get' "/api/patients/$patientId/appointments" '' $null).status -eq 401) 'Anonymous history is rejected'
Assert-Check ((Call-Api 'Get' "/api/doctors/$doctorId/appointments" $patientToken $null).status -eq 403) 'Patient cannot read doctor history'
$other=Call-Api 'Post' '/api/patients' $adminToken @{ lastName='Проверочный'; firstName='Другой'; phone='+12025550124' }
Assert-Check ($other.status -eq 201) 'Admin creates a patient profile'
Assert-Check ((Call-Api 'Get' "/api/patients/$($other.data.id)/appointments" $patientToken $null).status -eq 403) 'Patient cannot access another profile'
$fixture=New-TestSlot $doctorId $doctorToken $false
$slotId=$fixture.slot.id
$book=Call-Api 'Post' "/api/patients/$patientId/appointments" $patientToken @{ slotId=$slotId }
Assert-Check ($book.status -eq 201 -and $book.data.status -eq 'CREATED') 'Appointment is created'
Assert-Check ((Call-Api 'Post' "/api/patients/$patientId/appointments" $patientToken @{ slotId=$slotId }).status -eq 409) 'Duplicate booking is rejected'
Assert-Check ((Call-Api 'Delete' "/api/doctors/$doctorId/slots/$slotId" $doctorToken $null).status -eq 409) 'Occupied slot cannot be deleted'
$cancel=Call-Api 'Post' "/api/patients/$patientId/appointments/$($book.data.id)/cancel" $patientToken $null
Assert-Check ($cancel.status -eq 200 -and $cancel.data.status -eq 'CANCELED') 'Cancellation preserves the record'
$shift=[DateTimeOffset]::Parse($fixture.body.startsAt).AddHours(2)
$updated=Call-Api 'Put' "/api/doctors/$doctorId/slots/$slotId" $doctorToken @{ startsAt=$shift.ToString('o'); endsAt=$shift.AddMinutes(20).ToString('o') }
Assert-Check ($updated.status -eq 200) 'Free slot can be rescheduled'
Assert-Check ((Call-Api 'Delete' "/api/doctors/$doctorId/slots/$slotId" $doctorToken $null).status -eq 204) 'Free slot is removed from schedule'
$lab=Call-Api 'Post' "/api/doctors/$doctorId/lab-requests" $doctorToken @{ patientId=$patientId; testCode='BLOOD_COUNT' }
Assert-Check ($lab.status -eq 201 -and $lab.data.status -eq 'REQUESTED') 'Doctor creates a laboratory request'
$labBooked=Call-Api 'Post' "/api/patients/$patientId/lab-requests/$($lab.data.id)/booking" $patientToken @{ scheduledAt=[DateTimeOffset]::UtcNow.AddDays(3).ToString('o') }
Assert-Check ($labBooked.status -eq 200 -and $labBooked.data.status -eq 'BOOKED') 'Patient books the laboratory test'
$delivered=$false
for ($attempt=0; $attempt -lt 30; $attempt++) {
    $labList=Call-Api 'Get' "/api/patients/$patientId/lab-requests" $patientToken $null
    $match=@($labList.data | Where-Object { $_.id -eq $lab.data.id -and $null -ne $_.dispatchedAt })
    if ($match.Count -gt 0) { $delivered=$true; break }
    Start-Sleep -Seconds 1
}
Assert-Check $delivered 'Laboratory event passed through Kafka'
$year=(Get-Date).Year-1
$report=Call-Api 'Post' "/api/doctors/$doctorId/reports/$year" $doctorToken $null
Assert-Check ($report.status -eq 200) 'Annual report request is accepted'
$ready=$false
for ($attempt=0; $attempt -lt 30; $attempt++) {
    $report=Call-Api 'Get' "/api/doctors/$doctorId/reports/$year" $doctorToken $null
    if ($report.status -eq 200 -and $report.data.status -eq 'READY') { $ready=$true; break }
    Start-Sleep -Seconds 1
}
Assert-Check $ready 'Annual report is ready'
$runtime=Call-Api 'Get' '/api/admin/runtime' $adminToken $null
if ($runtime.data.smsMode -eq 'log') {
    $near=New-TestSlot $doctorId $doctorToken $true
    $nearBook=Call-Api 'Post' "/api/patients/$patientId/appointments" $patientToken @{ slotId=$near.slot.id }
    Assert-Check ($nearBook.status -eq 201) 'Reminder fixture appointment is created'
    $run=Call-Api 'Post' '/api/admin/reminders/run' $adminToken $null
    Assert-Check ($run.status -eq 200) 'Reminder queue is processed'
    $sms=Call-Api 'Get' '/api/admin/sms' $adminToken $null
    Assert-Check (@($sms.data | Where-Object { $_.appointment_id -eq $nearBook.data.id -and $_.state -eq 'SIMULATED' }).Count -eq 1) 'SMS is honestly marked SIMULATED'
    $cleanup=Call-Api 'Post' "/api/patients/$patientId/appointments/$($nearBook.data.id)/cancel" $patientToken $null
    Assert-Check ($cleanup.status -eq 200) 'Reminder fixture is canceled'
    $cleanupSlot=Call-Api 'Delete' "/api/doctors/$doctorId/slots/$($near.slot.id)" $doctorToken $null
    Assert-Check ($cleanupSlot.status -eq 204) 'Reminder fixture slot is hidden'
} else { Write-Host 'SKIP: SMS fixture requires log mode; no real SMS requested by this script.' }
Write-Host "SUCCESS: $script:checks checks passed. Base URL: $BaseUrl"
