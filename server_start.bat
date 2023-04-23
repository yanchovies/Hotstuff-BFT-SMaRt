for /L %%i in (0,1,3) do start "server",%%i cmd /k call smartrun.bat hotstuff.benchmark.ThroughputLatencyServer %%i

exit