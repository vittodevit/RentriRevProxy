# RentriRevProxy
This software is a reverse proxy that implements internally the authentication and signing of request directed to the RENTRI api. 
The software exposes locally a REST API does not need authentication and can be used to interact with the RENTRI API.

### Installation
- build the project with maven or use the provided jar in the release tab
- launch the jar passing the environment variables needed to configure the proxy

### Configuration
The proxy can be configured using the following environment variables:
- `SERVER_PORT`: the port that the proxy will listen to locally
- `BUNDLE_PATH`: the path of the .p12 file donwloaded from the RENTRI portal
- `BUNDLE_PASSWORD_PATH`: the path of a text file containing the password of the .p12 file