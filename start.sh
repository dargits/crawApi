#!/bin/bash

# Set Java options for low memory environment
export JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Set Spring profile
export SPRING_PROFILES_ACTIVE=production

# Start the application
java $JAVA_OPTS -jar target/genshin-coupon-scraper-0.0.1-SNAPSHOT.jar