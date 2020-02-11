#!/bin/bash

dev_hosts=$(cat /tmp/remote.config.js  | awk '/dev/, /ansible/' | grep host | awk '{print $2}' | sed 's/[",]//g') 
dev_ports=$(cat /tmp/remote.config.js  | awk '/dev/, /ansible/' | grep port | awk '{print $2}' | sed 's/[",]//g') 
dev_users=$(cat /tmp/remote.config.js  | awk '/dev/, /ansible/' | grep username | awk '{print $2}' | sed 's/[",]//g') 
dev_passwords=$(cat /tmp/remote.config.js  | awk '/dev/, /ansible/' | grep password | awk '{print $2}' | sed 's/[",]//g') 

echo $dev_hosts 
echo $dev_ports
echo $dev_users
echo $dev_passwords
