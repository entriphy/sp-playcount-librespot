## Deprecation notice
Deprecated in favor of [sp-playcount](https://github.com/entriphy/sp-playcount).

# sp-playcount-librespot
`sp-playcount-librespot` is a modification of `librespot-java` that serves as an enhancement of [Spotify-PlayCount](https://github.com/evilarceus/Spotify-PlayCount).

**NOTE:** This repository will most likely be archived if I decide to rewrite this in another language.

## Disclaimer
This project is to be used for **educational purposes only**. We (librespot developers, librespot-java developers, and I) do not condone using this tool for commercial purposes. Doing so puts this project at risk and can cause legal issues (**the data is property of Spotify, not me**). Any attempt or request to pay me for anything related to this project will be denied.

## Advantages vs. [Spotify-PlayCount](https://github.com/evilarceus/Spotify-PlayCount)
* Does NOT require the Spotify desktop app (woo)
* Lower CPU and memory usage
* More information given in API response
* Provides endpoint for artist info (monthly listeners, top tracks, follower count, etc)

## Requirements
* Java 8+
* Spotify Account (recommended to create another account)

## Installation
1. Compile this repository or [download an executable JAR](https://github.com/evilarceus/sp-playcount-librespot/releases/latest).
2. Run the JAR: `java -jar sp-playcount-librespot.jar <spotify_username> <spotify_password>`
    * You only need to provide your Spotify username and password once. After `creds.json` has been generated, the username and password are not required in the launch arguments.
3. Make any appropriate configuration changes in the generated `config.toml` file (see "Configuration" section for config options).
4. Run the JAR again: `java -jar sp-playcount-librespot.jar`

## Usage
Simply make a GET request to the endpoint with the query string `albumid` set to the ID of a Spotify album (ex. if the URL is https://open.spotify.com/album/6Lq1lrCfkpxKa4jCo5gKWr or spotify:album:6Lq1lrCfkpxKa4jCo5gKWr, the string is 6Lq1lrCfkpxKa4jCo5gKWr)

Curl example: (endpoint is /albumPlayCount)
```bash
$ curl https://example.com/albumPlayCount?albumid=6Lq1lrCfkpxKa4jCo5gKWr
{"success": true, "data": {"uri":"spotify:album:6Lq1lrCfkpxKa4jCo5gKWr","name":"Good Faith","cover":{"uri":"https://i.scdn.co/image/ab67616d00001e02dc384e6d13983fe1cd415ade"},"year":2019,"track_count":10,"discs":[{"number":1 ...
```

There are also endpoints for retrieving artist info. `artistid` must be set to the ID of a Spotify artist.
* `/artistInfo?artistid=` (monthly listeners, top tracks w/ play count, follower count, albums/singles released by artist, etc)
* `/artistAbout?artistid=` (biography, images, social links, etc)
* `/artistInsights?artistid=` (monthly listeners, "Discovered On" playlists, cities w/ most listeners, etc)

## Compiling (requires Maven)
1. Clone this repository: `git clone https://github.com/evilarceus/sp-playcount-librespot && cd sp-playcount-librespot`
2. Build with Maven: `mvn clean package`
3. Run JAR file: `java -jar ./core/target/librespot-core-jar-with-dependencies.jar <spotify_username> <spotify_password>`

## Configuration
A `config.toml` file is generated after you run the JAR for the first time. This file can be used to change the settings of the server. The settings are located at the very bottom of the file under `[server]`.
To reset the configuration, simply delete the file and run the JAR again.
```
[server]
port = 8080
albumEndpoint = "/albumPlayCount"
artistEndpoint = "/artistInfo"
artistAboutEndpoint = "/artistAbout"
enableHttps = false
httpsKs = ""
httpsKsPass = ""
```
| Option                     | Description                                                                                      |
|----------------------------|--------------------------------------------------------------------------------------------------|
| `port`                     | Selects what port to listen for HTTP requests on                                                 |
| `albumEndpoint`            | Endpoint at which the user can send HTTP GET requests to the API for album info                  |
| `artistEndpoint`           | Endpoint at which the user can send HTTP GET requests to the API for artist info                 |
| `artistAboutEndpoint`      | Endpoint at which the user can send HTTP GET requests to the API for an artist's about page      |
| `artistInsightsEndpoint`   | Endpoint at which the user can send HTTP GET requests to the API for an artist's insights        |
| `enableHttps`              | If true, enables HTTPS support (requires certificate, see section below)                         |
| `httpsKs`                  | Location to keystore with HTTPS certificate and key                                              |
| `httpsKsPass`              | Password to HTTPS keystore file (if applicable)                                                  |

### HTTPS Configuration
The server can be configured to use HTTPS. If you're using LetsEncrypt, use [this guide](https://www.wissel.net/blog/2018/03/letsencrypt-java-keystore.html) to create a keystore with the certificate.

Then, edit `config.toml`:
```
[server]
...
enableHttps = true
httpsKs = "<location of keystore file>"
httpsKsPass = "<keystore password (if applicable)>"
```

## Public API
I am currently hosting this API at https://api.t4ils.dev (endpoints: /albumPlayCount, /artistInfo, /artistAbout). Please use it for **educational purposes only** (see disclaimer).

If your application previously used Spotify-PlayCount, you will need to update your application to support the new API response.
