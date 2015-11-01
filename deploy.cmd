set LEIN_SNAPSHOTS_IN_RELEASE=1
call lein uberjar
del /f .\cdn77purge-distribution\*.jar
copy .\target\uberjar\*.jar .\cdn77purge-distribution\
"C:\Program Files\7-Zip\7z.exe" a cdn77purge-distribution.zip ./cdn77purge-distribution/



