# Basic-DNS

## Overview

`Basic-DNS` is a simple Java-based DNS server that supports caching and recursive query handling. This server is designed to resolve DNS queries by forwarding requests to upstream servers when needed and caching responses to improve performance.

## Features

- **Recursive Query Handling:** Forwards queries to upstream DNS servers if the requested record is not found in the local zone file.
- **Caching:** Stores DNS responses to provide faster results for subsequent queries within the Time-To-Live (TTL) period.
- **Customizable Zone Files:** Reads and manages DNS records from a custom zone file.

## How It Works

1. **Query Handling:**
   - Receives DNS queries from clients.
   - Checks if the requested record is in the local zone file.
   - If not found, forwards the query to an upstream DNS server.
   - Receives the response, caches it, and then returns it to the client.

2. **Caching:**
   - Stores DNS records with their TTL values.
   - Removes expired records from the cache based on TTL.
   - Uses Java's `Instant` class for timestamping records.

3. **Configuration:**
   - Use the provided `Makefile` to build the application.
   - Configure the server and zone file as needed.

## Getting Started

1. **Clone the Repository:**

   ```sh
   git clone https://github.com/bprall/Basic-DNS.git
   cd Basic-DNS
    ```
2. **Configure Your DNS ZONE File:**
    Edit the `server.zone` file to include DNS records in the following format:
        
```domain  TTL IN  A   IPv4 address```
    
    Replace `domain`, `TTL`, and `IPv4 address` with your desired values. For example:
        
```example.com  3600 IN  A   192.168.1.1```

3. Build the Application:
    ```sh
    make
    ```
4. Run the DNS Server:
    In one terminal window run:
    ```sh
    sudo java dns.DNSServer server.zone
    ```

5. Test with dig:
    In another terminal window run:
    ```sh
    dig @localhost www.example.com
    ```

# Files
- Makefile: Build script for the application.
- csci3363.zone: Example DNS zone file with sample records.
- dns/: Directory containing the Java source files:
    - DNSZone.java: Manages DNS zone records.
    - DNSMessage.java: Handles DNS messages and parsing.
    - DNSRecord.java: Defines DNS records with caching support.
    - DNSServer.java: Main DNS server class handling queries and caching.
    - DNSCache.java: Implements the caching mechanism for DNS records.
