param([int]$Port = 8080)
$ErrorActionPreference = "Stop"
$base="http://127.0.0.1:$Port"

function Invoke-Json($Method,$Path,$Body=$null,$Token=$null){
    $p=@{Uri="$base$Path";Method=$Method;TimeoutSec=15;Headers=@{}}
    if($Token){$p.Headers.Authorization="Bearer $Token"}
    if($null -ne $Body){$p.ContentType="application/json";$p.Body=$Body|ConvertTo-Json -Depth 10}
    try{Invoke-RestMethod @p}catch{if($_.ErrorDetails.Message){$_.ErrorDetails.Message|ConvertFrom-Json}else{throw}}
}
function Ok($r,$label){if($r.code -ne 0){throw "$label failed: $($r|ConvertTo-Json -Depth 10)"};Write-Host "[OK] $label";return $r.data}
function Require($c,$m){if(-not $c){throw $m}}
function New-User($prefix,$phonePrefix){
    $suffix=[DateTimeOffset]::Now.ToUnixTimeMilliseconds();Start-Sleep -Milliseconds 3
    $name="$prefix`_$suffix";$phone="$phonePrefix$($suffix.ToString().Substring($suffix.ToString().Length-8))";$password="Tifo123456"
    Ok (Invoke-Json POST "/api/auth/register" @{username=$name;phone=$phone;password=$password}) "register $prefix"|Out-Null
    $login=Ok (Invoke-Json POST "/api/auth/login" @{username=$name;password=$password}) "login $prefix"
    return @{Token=$login.accessToken;UserId=$login.user.id}
}
function Notifications($user,$type=$null,$read=$null,$page=1,$size=20){
    $query="pageNum=$page&pageSize=$size";if($type){$query+="&type=$type"};if($read){$query+="&readStatus=$read"}
    return Ok (Invoke-Json GET "/api/app/notifications?$query" $null $user.Token) "notifications $type $read"
}

Write-Host "T21 notification center smoke on port $Port"
$a=New-User "notify_a" "135";$b=New-User "notify_b" "137"
$post=Ok (Invoke-Json POST "/api/app/contents/posts" @{title="T21 notification smoke";body="notification contract"} $b.Token) "B creates content";$contentId=$post.contentId
Ok (Invoke-Json POST "/api/app/likes/toggle" @{targetType="CONTENT";targetId=$contentId} $a.Token) "A likes B content"|Out-Null
$root=Ok (Invoke-Json POST "/api/app/comments" @{contentId=$contentId;content="A root comment"} $a.Token) "A comments B content";$rootId=$root.commentId
$reply=Ok (Invoke-Json POST "/api/app/comments" @{contentId=$contentId;parentId=$rootId;replyToUserId=$a.UserId;content="B replies A"} $b.Token) "B replies A";$replyId=$reply.commentId
Ok (Invoke-Json POST "/api/app/comments/$replyId/likes/toggle" $null $a.Token) "A likes B comment"|Out-Null
Ok (Invoke-Json POST "/api/app/users/$($b.UserId)/follow" $null $a.Token) "A follows B"|Out-Null

$bList=Notifications $b
$bTypes=@($bList.records|ForEach-Object notificationType)
foreach($type in @("CONTENT_LIKED","CONTENT_COMMENTED","COMMENT_LIKED","USER_FOLLOWED")){Require ($type -in $bTypes) "B missing $type"}
$aList=Notifications $a "COMMENT_REPLIED"
Require (@($aList.records).Count -eq 1) "A reply notification missing"
Require ($aList.records[0].targetPreview.commentExcerpt -eq "B replies A") "comment preview missing"
Require ($bList.records|Where-Object{$_.targetType -eq 'CONTENT' -and $_.targetPreview.contentTitle -eq 'T21 notification smoke'}) "content preview missing"

$before=@($bList.records).Count
Ok (Invoke-Json POST "/api/app/likes/toggle" @{targetType="CONTENT";targetId=$contentId} $b.Token) "self like"|Out-Null
$afterSelf=@((Notifications $b).records).Count;Require ($afterSelf -eq $before) "self notification was created"

Ok (Invoke-Json POST "/api/app/likes/toggle" @{targetType="CONTENT";targetId=$contentId} $a.Token) "A cancels like"|Out-Null
Ok (Invoke-Json POST "/api/app/likes/toggle" @{targetType="CONTENT";targetId=$contentId} $a.Token) "A re-likes"|Out-Null
Require (@((Notifications $b "CONTENT_LIKED").records).Count -eq 1) "content like dedup failed"
Ok (Invoke-Json DELETE "/api/app/users/$($b.UserId)/follow" $null $a.Token) "A unfollows B"|Out-Null
Ok (Invoke-Json POST "/api/app/users/$($b.UserId)/follow" $null $a.Token) "A re-follows B"|Out-Null
Require (@((Notifications $b "USER_FOLLOWED").records).Count -eq 1) "follow dedup failed"

$unread=Ok (Invoke-Json GET "/api/app/notifications/unread-count" $null $b.Token) "unread count";Require ($unread.total -ge 4) "unread total too small"
$first=(Notifications $b "CONTENT_LIKED" "UNREAD").records[0]
Ok (Invoke-Json POST "/api/app/notifications/$($first.notificationId)/read" $null $b.Token) "single read"|Out-Null
Ok (Invoke-Json POST "/api/app/notifications/$($first.notificationId)/read" $null $b.Token) "single read idempotent"|Out-Null
$afterRead=Ok (Invoke-Json GET "/api/app/notifications/unread-count" $null $b.Token) "unread count after read";Require ($afterRead.total -eq ($unread.total - 1)) "single read did not decrement"
$otherRead=Invoke-Json POST "/api/app/notifications/$($first.notificationId)/read" $null $a.Token;Require ($otherRead.code -eq 0) "other notification ownership leaked"
Ok (Invoke-Json POST "/api/app/notifications/read-all" $null $b.Token) "read all"|Out-Null
Require ((Ok (Invoke-Json GET "/api/app/notifications/unread-count" $null $b.Token) "zero unread").total -eq 0) "read all failed"
$page1=Notifications $b $null $null 1 2;$page2=Notifications $b $null $null 2 2
Require ((@($page1.records).Count -eq 2) -and (@($page2.records).Count -ge 1)) "database pagination failed: total=$($page1.total), pages=$($page1.pages), page1=$(@($page1.records).Count), page2=$(@($page2.records).Count)"
$unauthorized=Invoke-Json GET "/api/app/notifications";Require ($unauthorized.code -eq 40101) "notifications must require login"
Write-Host "T21 notification center smoke passed"
