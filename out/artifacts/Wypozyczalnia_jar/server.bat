@echo off
Set "pw=wypo%%!@#!URjqwe3"
java -jar "Wypozyczalnia.jar" server localhost 12345 localhost 3306 wypozyczalnia wypozyczalniaServer "%pw%"
PAUSE
