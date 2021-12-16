# Betshop application #

 - Application shows betshops for client on map -> bet shops are located in Germany
 - If user does denies the location permission, map zooms to Munich, otherwise zooms out and shows data for whole Europe
- User can move the map and zoom-in/zoom-out, rotate it, locate self
- Every time users moves the camera, async api call is being called with screen visible region coordinates and loading indicator shows on top of the screen
- When clicking on betshop icon, detail pops out and user can see the relevant information such as opening and closing time, general information about the bet shop or call number
- When clicking on the "Route" button, user is transferred into default navigation application to navigate to betshop location

## Tech Stack ##

The app is written with Model-View-ViewModel and Kotlin as a programming language of choice. DI is handled with a Dagger Hilt and concurrency solution uses RxJava3. Composite disposable is being used to clear previous queries if the user tries to brute force them.
The code is structured in packages by feature.
All images and either vectors or webp for additional lossless quality compression
## Extra feature ##
* Clustering ->  makes map more readable, increases performance when drawing lot of markers and shows the number of betshops on the map 

## Info ##
* Some of the server items return strange symbols instead of clean strings: [Example](https://interview.superology.dev/betshops?boundingBox=50.7481463255406%2C12.610780186951159%2C50.70792419045236%2C12.559317648410799)
* Phone numbers seem to be absent from the endpoint so I hardcoded the values and made them callable on click
* Tested on Android 8 and 12
