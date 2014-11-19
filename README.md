GarageOpenerAndroid - Garage Opener app
===================

This app implements a secure Garage Opener client app for the corresponding server app running on Spark Core. 

= Security =
Symmetric shared-key security is used. The client Android app must have a secret key in order to connect. 

AES-128 is used for encrypting the traffic. Challenge based, timed session token approach is used to prevent Replay Attacks.


Please see the server app for more technical info:
https://github.com/vace117/GarageOpenerSpark.git
