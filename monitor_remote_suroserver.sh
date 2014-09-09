#!/bin/bash

TMP_FILENAME=/tmp/monitor_remote_suroserver.sh.dat
GNUPLOT=gnuplot
SLEEP_TIME=1
SURO_HOST=starzia@guestvm.thebrighttag.com
SURO_FILEQUEUE_PATH=/home/starzia/suroserver/sinkQueue
KAFKA_LOG_PATH=/mnt/kafka-logs

plot_setup_commands() {
    echo "set terminal x11"
    echo "set xlabel 'UTC time'"
    echo "set ylabel 'kB'"
    echo "set key out top"
    echo "set xdata time"
    echo "set timefmt '%s'"
    echo "set format x '%H:%M:%S'"
    echo "set xtics rotate by -90"
    echo "plot '$TMP_FILENAME' using 1:2 with lines title 'Suro KafkaSink file size', \
              '' using 1:3 with lines title 'Kafka log size'"
}

#######
# start remote data query process
ssh $SURO_HOST "
while [ 1 ]; do
    # get epoch date for x axis
    date=\`date +%s | awk '{printf \"%s\",\$1}' \`
    # get fileQueue size
    suro_size=\`du -s $SURO_FILEQUEUE_PATH | awk '{printf \"%s\",\$1}' \`
    # get fileQueue size
    kafka_size=\`du -s $KAFKA_LOG_PATH | awk '{printf \"%s\",\$1}' \`
    # add this entry to the data set
    echo \"\$date \$suro_size \$kafka_size\"
    sleep $SLEEP_TIME
done" > $TMP_FILENAME &
ssh_pid=$!

#######
# cleanup on exit
trap "{
    rm -f $TMP_FILENAME
    # we have to kill the child ssh process manually for reasons I don't understand
    kill $ssh_pid
}" EXIT

#######
# start plotting process
sleep 2
{
    # setup
    plot_setup_commands
    # update periodically
    while [ 1 ]; do
        echo "replot"
        sleep $SLEEP_TIME
    done
} | $GNUPLOT