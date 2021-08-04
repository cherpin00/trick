(cd $TRICK_HOME/trick_sims/Ball/SIM_ball_L1 && $TRICK_HOME/bin/trick-CP && ./S_main_* RUN_test/input_for_test.py &)
count=0
max_retries=100
sleep_time=.1
until (echo "Sim Process ID: " && pgrep S_main_)
do
    if [ $count -gt $max_retries ]
    then
        break
    fi
    # current_time=$( expr $count \* $sleep_time )
    current_time=$( echo "$count*$sleep_time" | bc )
    printf "\rWaiting for sim to start... %s" $current_time
    sleep $sleep_time
    count=$( expr $count + 1 )
done
if [ $count -gt $max_retries ]
then
    echo "Server did not start."
    exit 1
fi
echo "Running tests..."
./run_tests.py tests/test_variable_server.py
pkill -9 S_main_
echo "Finished."