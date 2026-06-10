@ECHO OFF
title TutorHub Vault
if EXIST "Control Panel.{21EC2020-3AEA-1069-A2DD-08002B30309D}" goto UNLOCK
if NOT EXIST TutorHub_Vault goto MDLOCKER

:CONFIRM
echo Ban co muon an thu muc TutorHub_Vault khong? (Y/N)
set/p "cho=>"
if %cho%==Y goto LOCK
if %cho%==y goto LOCK
if %cho%==n goto END
if %cho%==N goto END
echo Lua chon khong hop le.
goto CONFIRM

:LOCK
ren TutorHub_Vault "Control Panel.{21EC2020-3AEA-1069-A2DD-08002B30309D}"
attrib +h +s "Control Panel.{21EC2020-3AEA-1069-A2DD-08002B30309D}"
echo Thu muc da duoc an va khoa thanh cong!
goto End

:UNLOCK
echo Nhap mat khau de mo khoa (Mat khau mac dinh la: tutorhub):
set/p "pass=>"
if NOT %pass%==tutorhub goto FAIL
attrib -h -s "Control Panel.{21EC2020-3AEA-1069-A2DD-08002B30309D}"
ren "Control Panel.{21EC2020-3AEA-1069-A2DD-08002B30309D}" TutorHub_Vault
echo Thu muc da hien thi thanh cong!
goto End

:FAIL
echo Mat khau sai!
pause
goto end

:MDLOCKER
md TutorHub_Vault
echo Da tao thu muc TutorHub_Vault! 
echo Bay gio Sep hay copy tat ca code vao trong thu muc nay, sau do chay lai file nay de An no di nhe.
pause
goto End

:End
