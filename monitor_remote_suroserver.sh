#!/bin/bash

TMP_FILENAME=/tmp/monitor_remote_suroserver.sh.dat
GNUPLOT=gnuplot
SLEEP_TIME=1
SURO_HOST=starzia@guestvm.thebrighttag.com
SURO_FILEQUEUE_PATH=/home/starzia/suroserver/sinkQueue


plot_setup_commands() {
    echo "set terminal x11"
    echo "set xlabel 'epoch seconds'"
    echo "set ylabel 'kB'"
    echo "plot '$TMP_FILENAME' using 1:2 with lines title 'KafkaSink file size'"
}

# cleanup on exit
trap "{
    rm -f $TMP_FILENAME
}" EXIT

#######
# start remote data query process
ssh $SURO_HOST "
while [ 1 ]; do
    # get epoch date for x axis
    date=\`date +%s | awk '{printf \"%s\",\$1}' \`
    # get fileQueue size
    size=\`du -s $SURO_FILEQUEUE_PATH | awk '{printf \"%s\",\$1}' \`
    # add this entry to the data set
    echo \"\$date \$size\"
    sleep $SLEEP_TIME
done" > $TMP_FILENAME &


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
