# Face detection server for android.

This is a simple TCP server for performing face detection using Google MLkit.
Clients need to first send image data size in 4 bytes (big endian), and then send the image data (such as in JPEG). The server will return the recognition results in JSON.

When running on Android studio emulators, port mapping needs to be set up.

1. Check TCP port number of emulator by running `adb devices`:   
   `> adb devices`  
   `List of devices attached`  
   `emulator-5554   device`

    Here, "5554" is the port number. You can also see this on the emulator's window title.

1. telnet to the emulator.  
   ` telnet 127.0.0.1 <port number>`

1. If authentication is required, check `~/.emulator_console_auth_token` for auth token, and enter `auth <token>`

1. enter the following:  
   `redir add tcp:<port number in the PC>:<port number in the emulator>`

    Example:  
    `redir add tcp:5050:8080`  
    This will map the 8080 port in the emulator to 5050 port in the PC.

To change the port number used, change the 'port' definition in `Processor.java`.
