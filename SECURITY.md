# Security Policy

LocoStream is a self-hosted application. Since you run your own instance and control your own data, most of your security posture depends on how you deploy it, not just the code itself. This document covers both how to report a vulnerability and how to deploy safely.

## Supported versions

Only the latest `main` branch is supported with security fixes. If you're running an older version, please update before reporting an issue, it may already be fixed.

## Reporting a vulnerability

If you find a security issue, **please do not open a public GitHub issue for it**. Public issues make the vulnerability visible before a fix is available.

Instead, report it privately using [GitHub's private vulnerability reporting](../../security/advisories/new) (Security tab → Report a vulnerability). If that's not available for any reason, open a regular issue with as few technical details as possible, just enough to say "I found a security issue," and ask to be contacted privately.

Please include:
- A description of the vulnerability and its potential impact
- Steps to reproduce, or a proof of concept if possible
- The version/commit you tested against

You can expect an initial response within a few days. This is a community project maintained in spare time, so please be patient, a fix will be prioritized once confirmed.

## Deployment security notes

A few things worth knowing if you're self-hosting:

* **CORS is private-network-only by default.** The backend only trusts `localhost` and RFC-1918 private IPs (`10.x`, `172.16-31.x`, `192.168.x`) out of the box. If you expose the app through a reverse proxy on a public domain, you must explicitly add that domain via the `ALLOWED_ORIGINS` environment variable, nothing is trusted automatically. See the [README's Configuration section](./README.md#configuration).
* **No built-in authentication.** LocoStream itself does not have a login system. If you expose it beyond your local network, put it behind your own authentication layer (e.g. a reverse proxy with Basic Auth, like the Apache config in some issue reports, or a VPN/Tailscale-only setup).
* **Downloaded files and your database are stored unencrypted** in the `./data/` and `./downloads/` directories. Treat access to the host machine accordingly.
* **Environment variables containing secrets should never be committed.** Use `.env` (gitignored) rather than hardcoding anything in `docker-compose.yml` or source files.

## Scope

This policy covers the LocoStream backend, frontend, and desktop app in this repository. It does not cover [NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor) itself, report issues with that library to its own maintainers, we are not affiliated with the NewPipe team.
