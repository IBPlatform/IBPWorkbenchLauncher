taskkill /T /F /IM ibpworkbench.exe
..\mysql\bin\mysqladmin.exe --defaults-file=..\mysql\my.ini -u root shutdown
rmdir /S /Q ..\mysql\data\ibdb_cowpea_local
mkdir ..\mysql\data\ibdb_cowpea_local
copy "dump\ibdb_cowpea_local-5-F3 Nursery-arlett" ..\mysql\data\ibdb_cowpea_local
pause