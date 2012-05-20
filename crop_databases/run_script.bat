@echo off
rem Parameters:
rem %1      the port where mysqld.exe is running
rem %2      the database with which the script should be run
rem %3      the SQL script to run
@echo on
mysql -u root -P %1 %2 < %3