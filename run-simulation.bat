@echo off
setlocal enabledelayedexpansion

:: ========================================================
:: Edge-Fog Task Offloading Simulation Runner Script (Windows)
:: ========================================================
::
:: This script runs the Multi-UAV Edge Computing simulation with clean output,
:: suppressing all debug and info logs. It shows only the essential simulation
:: statistics such as task completion, energy consumption, latency, device and
:: UAV statistics for easier analysis.
::
:: The script works by creating a temporary logback configuration file that
:: routes all logs to a file, setting appropriate log levels, and then
:: extracting only the relevant statistics sections for display.
::
:: Output sections include:
:: - Task statistics (completion, failures)
:: - Deadline statistics
:: - Latency statistics
:: - Energy consumption
:: - Network data transfer
:: - Offloading decisions (UAV vs local)
:: - Resource utilization
:: - Device statistics (first 10 devices)
:: - UAV statistics
::
:: Author: Cascade AI
:: Last updated: August 1, 2025
:: ========================================================

echo Running Edge-Fog Task Offloading Simulation...

rem Create a temporary logback configuration file to redirect output to file only
(echo ^<?xml version="1.0" encoding="UTF-8"?^>
 echo ^<configuration^>
 echo     ^<appender name="FILE" class="ch.qos.logback.core.FileAppender"^>
 echo         ^<file^>simulation_output.txt^</file^>
 echo         ^<append^>false^</append^>
 echo         ^<encoder^>
 echo             ^<pattern^>%%msg%%n^</pattern^>
 echo         ^</encoder^>
 echo     ^</appender^>
 echo.
 echo     ^<!-- Set org.edgefog package to INFO level to capture statistics --^>
 echo     ^<logger name="org.edgefog" level="INFO" /^>
 echo.
 echo     ^<!-- Silence other packages --^>
 echo     ^<logger name="org.cloudbus" level="ERROR" /^>
 echo     ^<logger name="org.nd4j" level="ERROR" /^>
 echo     ^<logger name="org.deeplearning4j" level="ERROR" /^>
 echo     ^<logger name="org.apache" level="ERROR" /^>
 echo.
 echo     ^<root level="ERROR"^>
 echo         ^<appender-ref ref="FILE" /^>
 echo     ^</root^>
 echo ^</configuration^>
) > temp-logback.xml

rem Run the simulation with our configured logging - redirect all output
echo Running simulation, please wait...
call mvn -q exec:java -Dexec.mainClass="org.edgefog.simulation.SimulationRunner" -Dlogback.configurationFile=./temp-logback.xml >nul 2>&1

echo.
echo === SIMULATION SUMMARY ===
echo.

rem Check if output file exists
if exist simulation_output.txt (
    rem Extract and show statistics
    echo --- TASK STATISTICS ---
    findstr /C:"TASK STATISTICS:" simulation_output.txt
    findstr /C:"  Total Tasks:" /C:"  Completed Tasks:" /C:"  Failed Tasks:" /C:"  Dropped Tasks:" simulation_output.txt
    
    echo.
    echo --- DEADLINE STATISTICS ---
    findstr /C:"DEADLINE STATISTICS:" simulation_output.txt
    findstr /C:"  Deadlines Met:" /C:"  Deadlines Missed:" simulation_output.txt
    
    echo.
    echo --- LATENCY STATISTICS ---
    findstr /C:"LATENCY STATISTICS:" simulation_output.txt
    findstr /C:"  Average Latency:" /C:"  Minimum Latency:" /C:"  Maximum Latency:" simulation_output.txt
    
    echo.
    echo --- ENERGY STATISTICS ---
    findstr /C:"ENERGY STATISTICS:" simulation_output.txt
    findstr /C:"  Total Energy Consumption:" /C:"  Average Energy per Task:" /C:"  Device Energy Consumption:" /C:"  UAV Energy Consumption:" simulation_output.txt
    
    echo.
    echo --- NETWORK STATISTICS ---
    findstr /C:"NETWORK STATISTICS:" simulation_output.txt
    findstr /C:"  Total Data Transferred:" /C:"  Average Data per Task:" simulation_output.txt
    
    echo.
    echo --- OFFLOADING STATISTICS ---
    findstr /C:"OFFLOADING STATISTICS:" simulation_output.txt
    findstr /C:"  uav:" /C:"  local:" simulation_output.txt
    
    echo.
    echo --- RESOURCE UTILIZATION ---
    findstr /C:"RESOURCE UTILIZATION:" simulation_output.txt
    findstr /C:"  Average UAV Utilization:" simulation_output.txt
    
    rem Extract device statistics (first 10 only)
    echo.
    echo --- DEVICE STATISTICS (First 10) ---
    findstr /B /C:"Device device-" simulation_output.txt > temp_devices.txt
    set COUNT=0
    for /f "tokens=*" %%a in (temp_devices.txt) do (
        if !COUNT! LSS 10 (
            echo %%a
            set /a COUNT+=1
        )
    )
    echo ... (showing first 10 devices only)
    
    rem Extract UAV statistics
    echo.
    echo --- UAV STATISTICS ---
    findstr /B /C:"UAV uav-" simulation_output.txt
) else (
    echo Error: Simulation output file not found. The simulation may have failed.
)

rem Clean up temporary files
del temp-logback.xml simulation_output.txt temp_devices.txt 2>nul

echo.
echo Simulation completed successfully. Press any key to exit...
pause > nul
