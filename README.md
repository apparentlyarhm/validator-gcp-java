## Introduction

This repository contains the backend service for the GCP Minecraft Control Panel. 
Built with Spring Boot, this application provides a REST API to interface with a 
Minecraft server hosted on Google Cloud Platform.

The service is designed to be hosted on Google Cloud Run. It handles everything from user 
authentication and GCP infrastructure management to direct communication with the Minecraft server itself.

## Core Features

- Google Cloud Platform Integration: Natively interacts with GCP services using the official client libraries:
    - **Compute Engine**: To fetch details and manage the state of the Minecraft server's Virtual Machine
    - **Firewall**: enabling IP whitelisting for players, as well as privatising or publicising the server
    - **GCS**: To read and generate download links for current mods. This bucket is populated using a script
        in the vm.

- Custom Minecraft Protocol Implementation: Includes from-scratch implementations of: 
  - **Query Protocol**: To fetch the Message of the Day (MOTD) and server status.
  - **RCON**: To securely execute commands on the server as an administrator.
    
  of course, these must be enabled in the `server.properties`

- Authentication & Authorization:
  - **GitHub OAuth2**: Handles user login and identity verification.
  - **JWTs**: Issued to authenticated users for securing API endpoints.
  - **Spring Security**: Comes with a lot of stuff out-of-the-box(three roles: USER, ADMIN, ANON)

- Configuration
    
    Apart from standard ENV VARS, you can also whitelist GitHub IDs of normal users and Admins. that will directly 
    affect who is allowed to do what in case of non-public apis.
- Misc
    - Docker for deployment
    - `ps1` and `sh` scripts for CI/CD.

## API Overview

The API provides a clear and logical set of endpoints for managing the server. Since Swagger is present, an auto-generated
OpenAPI spec will be ready. Below is a small overview.

### Authentication (`/api/v2/auth`)

- GET /login: Initiates the GitHub OAuth flow.
- GET /callback: The callback URI for GitHub to complete the login process and issue a JWT.

### Main Controller (`/api/v2`)

* GET /ping: A simple health-check endpoint.
* GET /machine: Retrieves details of the associated GCP Compute Engine VM.
* GET /firewall: Fetches the current firewall state
* GET /firewall/check-ip: Checks if a specific IP address is currently whitelisted.
* PATCH /firewall/add-ip: Adds the requesting user's IP address to the firewall whitelist.
* PATCH /firewall/purge: [ADMIN] Removes all IP addresses from the firewall whitelist.
* PATCH /firewall/make-public: [ADMIN] Opens the server to the public by setting the firewall rule to allow 0.0.0.0/0.
* GET /server-info: Gets the server's Message of the Day (MOTD), version, and player count.
* GET /mods: Lists all mods currently available on the server.
* GET /mods/download/{fileName}: Provides a download link or stream for a specific mod file.
* POST /execute: [AUTHENTICATED] Executes a command on the Minecraft server via RCON.

## Environment
see `env.example`
```bash

GITHUB_CLIENT_ID=value
GITHUB_CLIENT_SECRET=value
GOOGLE_CLOUD_BUCKET_NAME=value 
GOOGLE_CLOUD_FIREWALL_NAME=value
GOOGLE_CLOUD_PROJECT=value
GOOGLE_CLOUD_VM_NAME=value
GOOGLE_CLOUD_VM_ZONE=value
MINECRAFT_SERVER_PORT=value
MINECRAFT_RCON_PASS=value
MINECRAFT_RCON_PORT=value
SIGNING_SECRET=value
GOOGLE_CLOUD_VM_REGION=value
GOOGLE_CLOUD_AR_REPO_NAME=value
GOOGLE_CLOUD_CR_SERVICE_NAME=value
FE_HOST=value
GOOGLE_APPLICATION_CREDENTIALS=value
```

## Running the app

While just running the app doesn't require `gcloud cli` its needed for CI/CD stuff along with `docker`.
Just configuring the env vars with Intellij is enough to run the app. 

## See also- related repos

[the terraform-based orchaestrator](https://github.com/apparentlyarhm/minecraft-terraform)

[the nextJS frontend](https://github.com/apparentlyarhm/minecraft-vm-management-console)
