#!/bin/bash

TMP_FILENAME=/tmp/monitor_remote_suroserver.sh.dat
GNUPLOT="gnuplot -background gray"
SLEEP_TIME=1
SURO_HOST=starzia@guestvm.thebrighttag.com
SURO_SINKQUEUE_PATH=/home/starzia/suroserver/sinkQueue
SURO_SERVERQUEUE_PATH=/home/starzia/suroserver/serverQueue
KAFKA_LOG_PATH=/mnt/kafka-logs

plot_setup_commands() {
    echo "set terminal x11"
    echo "set xlabel 'UTC time'"
    echo "set ylabel 'MB'"
    echo "set key out top"
    echo "set xdata time"
    echo "set timefmt '%s'"
    echo "set format x '%H:%M:%S'"
    echo "set xtics rotate by -90"
    echo "plot '$TMP_FILENAME' using 1:(\$2/1024) with lines title 'Kafka log files' \
             , '' using 1:(\$3/1024) with lines title 'disk usage (normalized)' \
             , '' using 1:(\$4/1024) with lines title 'Suro Sink queue' \
             , '' using 1:(\$5/1024) with lines title 'Suro Server queue' \
             , '' using 1:(\$6/1024) with lines title 'Suro resident memory' \
             , '' using 1:(\$7/1024) with lines title 'Suro virtual memory' \
         "
}

#######
# start remote data query process
ssh $SURO_HOST "
init_disk_usage=\$(df|awk '/sda1/{printf \"%s\",\$3}')

while [ 1 ]; do
    # get epoch date for x axis
    date=\`date +%s | awk '{printf \"%s\",\$1}' \`
    # get Suro queue sizes
    sink_q_size=\`du -s $SURO_SINKQUEUE_PATH | awk '{printf \"%s\",\$1}' \`
    server_q_size=\`du -s $SURO_SERVERQUEUE_PATH | awk '{printf \"%s\",\$1}' \`
    # get fileQueue size
    kafka_size=\`du -s $KAFKA_LOG_PATH | awk '{printf \"%s\",\$1}' \`
    # get Suro PID
    suro_pid=\$(ps -eaf|grep 'SuroServer'|grep -v 'grep'|awk '{print \$2}')
    # get Suro RSS size
    suro_rss=\`cat /proc/\$suro_pid/status |awk '/VmRSS/{print \$2}' \`
    # get Suro Virtual Memory size
    suro_vm=\`cat /proc/\$suro_pid/status |awk '/VmSize/{print \$2}' \`
    # calculate disk usage, relative to initial disk usage
    let disk_usage=\$(df|awk '/sda1/{printf \"%s\",\$3}')-\$init_disk_usage

    # add this entry to the data set
    echo \"\$date \$kafka_size \$disk_usage \$sink_q_size \$server_q_size \$suro_rss \$suro_vm \"
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
