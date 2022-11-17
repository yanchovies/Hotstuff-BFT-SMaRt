for /L %%i in (0,1,40) do start "server" cmd /k call smartrun.bat bftsmart.demo.counter.CounterServer %%i

exit