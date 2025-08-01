@echo off
setlocal enabledelayedexpansion

:: ========================================================
:: Edge-Fog Task Offloading Simulation Runner Script (Windows)
:: ========================================================
::
:: This script runs the Multi-UAV Edge Computing simulation and displays
:: the simulation results in a clean, organized format.
::
:: Author: Cascade AI
:: Last updated: August 1, 2025
:: ========================================================

echo Running Edge-Fog Task Offloading Simulation...

:: Check if Maven is installed
where mvn >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo Error: Maven is not installed or not in PATH. Please install Maven.
    goto end
)

:: Run the simulation and capture output directly
echo Running simulation, please wait...

:: Create a temporary file for the output
del simulation_output.txt 2>nul

:: Run the Maven command and display the output directly
call mvn exec:java -Dexec.mainClass="org.edgefog.simulation.SimulationRunner"

echo.
echo Simulation completed. Press any key to exit...
pause > nul

:end
