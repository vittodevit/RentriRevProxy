# RentriRevProxy
This software is a reverse proxy that implements internally the authentication and signing of request directed to the RENTRI api. 
The software exposes locally a REST API that does not need authentication and can be used to interact with the RENTRI API.

### Installation
- build the project with maven or use the provided jar in the release tab
- launch the jar passing the environment variables needed to configure the proxy

### Configuration
The proxy can be configured using the following environment variables:
- `SERVER_PORT`: the port that the proxy will listen to locally
- `MONGODB_URI`: the uri of the mongodb instance that the proxy will use to retrieve the user data and certificates
- `REDIS_HOST`:  the host of the redis instance that the proxy will use to store the cachd data
- `REDIS_PORT`: the port of the redis instance
- `REDIS_PASSWORD`: the password of the redis instance