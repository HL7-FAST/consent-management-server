#!/bin/bash
while [ ! -f /tmp/server_can_shutdown.txt ]
do
  sleep 2
done
rm -f /tmp/server_can_shutdown.txt
mysqladmin shutdown
sleep 15

