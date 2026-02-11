#!/bin/bash

# Compilation
javac -d target/classes src/main/java/com/wifimanager/dashboard/DashboardServer.java

# Execution
java -cp target/classes com.wifimanager.dashboard.DashboardServer