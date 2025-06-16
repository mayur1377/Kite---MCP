<div style="text-align: center; font-size: 24px;">
    <img src="https://registry.npmmirror.com/@lobehub/icons-static-png/1.47.0/files/dark/claude-text.png" width="350" height="100" alt="Claude AI Logo" style="display: inline-block; margin-right: 20px;">
    <br>
    <span style="font-size: 48px; font-weight: bold;">&</span>
    <br>
    <img src="https://play-lh.googleusercontent.com/wnNYBAH1m-XJMfduOHfEATQAhCwyKUYeHAD1Fi9-OjtxKyPKjFEmgWvbx-OX2dM65xjp=w600-h300-pc0xffffff-pd" width="350" height="150" alt="Zerodha Kite Logo" style="display: inline-block; font-family: 'Courier New', monospace; margin-top: 20px; margin-left: 20px;">
</div>


# Kite Trading MCP Server

A Spring Boot application that integrates with Zerodha's Kite Connect API to provide trading capabilities through Model Context Protocol (MCP). This allows AI assistants like Claude/Cursor to interact with your trading account through a standardized interface.





## Features

- **Trading Operations**
  - Place buy/sell orders with limit price // to do : check this properly
  - Get current positions with P&L analysis
  - View portfolio holdings
  - Get account margins
  - Order history analysis

- **Portfolio Management**
  - Track P&L (realized and unrealized)
  - Monitor portfolio value
  - Risk analysis with sector exposure

- **Authentication**
  - Secure login through Kite Connect
  - Session management with expiry handling
  - Token-based authentication

## Prerequisites

- Java 17 or higher
- Maven
- Zerodha Kite Connect API credentials
- Claude Desktop or compatible MCP client

## Setup

1. Clone the repository:
```bash
git clone https://github.com/mayur1377/Kite---MCP.git
cd Kite---MCP
```

2. Get your Kite Connect API credentials:
   - Visit [Kite Connect Developer Portal](https://developers.kite.trade/apps)
   - Sign in with your Zerodha account
   - Click "New App" to create a new application
   - Fill in the following details:
     - App Name: `Kite Trading MCP` (or any name you prefer)
     - Website: `https://example.com` (use any dummy website for testing)
     - Redirect URL: `https://example.com/callback` (must match your website domain)
     - Description: `MCP server for trading automation`
   - After creating the app, you'll get:
     - API Key
     - API Secret
   - Save these credentials securely

3. Configure your Kite API credentials in `src/main/resources/application.properties`:
```properties
kite.api.key=your_api_key
kite.api.secret=your_api_secret
```

4. Build the project:
```bash
./mvnw clean package
```

The JAR file will be created at `target/demo-0.0.1-SNAPSHOT.jar`

## Running the Application

### Standalone Mode
```bash
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

### MCP Integration

To integrate with Claude Desktop or other MCP clients, create a configuration file (e.g., `claude_desktop_config.json`):

```json
{
  "mcpServers": {
    "kite-trading-mcp": {
      "command": "/usr/bin/java",
      "args": [
        "-jar",
        "/path/to/your/target/demo-0.0.1-SNAPSHOT.jar"
      ]
    }
  }
}
```

Replace `/path/to/your/` with the actual path to your JAR file.

## Authentication Flow

Each time you start a new session with Claude Desktop, you need to authenticate with Kite Connect:

1. Ask Claude to use the `get_login_url` tool to get the Kite login URL
2. Open the URL in your browser and log in to your Zerodha account
3. After successful login, you'll be redirected to your configured redirect URL (e.g., `https://example.com/callback`)
4. In the URL, you'll find a `request_token` parameter, for example:
   ```
   https://example.com/callback?request_token=AbCdEfGhIjKlMnOpQrStUvWxYz123456
   ```
5. Copy the `request_token` value (in this case: `AbCdEfGhIjKlMnOpQrStUvWxYz123456`)
6. Ask Claude to use the `generate_session` tool with your request token
7. Once the session is generated, you can use all other trading tools

Note: The access token is valid for the current session only. You'll need to repeat this process each time you start a new session with Claude Desktop.

## Available MCP Tools

The server provides the following tools that can be used by AI assistants:

1. `get_login_url`: Get the Kite login URL for authorization
2. `generate_session`: Generate a Kite session using request token
3. `place_order`: Place trading orders with specified parameters (symbol, type, quantity, price)
4. `get_margins`: Get account margins and available cash
5. `get_holdings`: View current portfolio holdings with quantity and average price
6. `get_positions`: Get current positions with P&L analysis (net and day positions)
7. `get_risk_analysis`: Analyze portfolio risk metrics and sector exposure
8. `get_order_analysis`: Get order history analysis with symbol-wise statistics
