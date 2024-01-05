@echo off
Set "pw=wypo%%!@#!URjqwe3"
java -jar "serverWypozyczalnia.jar" localhost 12345 localhost 3306 wypozyczalnia wypozyczalniaServer "%pw%"
PAUSE
