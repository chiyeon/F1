# About
**F1** is a simple, customizable speedrunning stopwatch, developed in Java 1.8 with JNativeHook.

# Features
 - Accurate stopwatch that rolls into minutes, hours, etc
 - "One key" design (to start, pause, reset)
 - Customizable color scheme
 - Customizable global hotkeys from JNativeHook
 
# Usage
F1 can be started and stopped to keep an accurate record of time, from a few seconds to a few hours. The global hotkey for "Go" functions differently depending on the state of the clock: It can be pressed to start the timer if at "0.00", to pause the timer if active, and to reset the timer back to "0.00" if paused. F1 uses global hotkeys for the "Go" and "Reset" keys, meaning they will always activate when pressed (assuming the program is running). If the window is focused, one can press the "Quit" key twice, defaulted to "Escape", to exit the application. 

Hovering the mouse over the window reveals two buttons, one marked "X" to immediately close the application, and another marked "S" to open the settings menu. Currently, the customizable colors only accept hexademical values with a "#" in front, notated as "#ff00ff".

# Download
1. Download/clone repo into a .zip file
2. extract the release folder to target location
3. run F1-release.jar

Note: F1-release.jar will create a config.properties file in its current directory. This holds the program settings.

# Previews
![Clock Demo](https://i.imgur.com/ipsyClh.gif)

*Demo of timer in its off, on, and paused states*

<br />


![Settings](https://i.imgur.com/WWkqHSo.png)

*Change colors/keybinds/settings through the options menu*

# TODO
 - custom splits/split functionality
 - warn against incorrect hex values
 - easy copy & paste
 - clean code
 
