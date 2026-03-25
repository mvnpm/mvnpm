---
layout: doc
nav:
  order: 3
  title: MCP
  mobile: false
title: "AI Agent Integration"
description: "Connect your AI coding assistant to mvnpm via MCP (Model Context Protocol). Setup guides for Claude Code, Cursor, VS Code, Windsurf, and more."
image: og-image.png
---

# AI Agent Integration

Connect your AI coding assistant to mvnpm using the **Model Context Protocol (MCP)**. Your agent can search NPM packages, get Maven coordinates, browse artifacts, and monitor sync status — all without leaving your editor.

## Endpoints

mvnpm exposes two MCP transport options:

| Transport | URL | Notes |
|-----------|-----|-------|
| **SSE** (Server-Sent Events) | `https://mvnpm.org/mcp/sse` | Widest client support |
| **Streamable HTTP** | `https://mvnpm.org/mcp` | Newer, simpler protocol |

## Available Tools

| Tool | Description |
|------|-------------|
| `search_packages` | Search NPM packages with Maven coordinate mapping |
| `get_package_info` | Full package metadata (license, deps, maintainers) |
| `list_versions` | All versions with dist-tags (latest, next) |
| `get_maven_coordinates` | NPM → Maven coordinate conversion with dependency snippets |
| `get_pom` | Generated Maven POM XML |
| `get_import_map` | ES module import map JSON |
| `browse_artifact_contents` | File listing inside JAR/TGZ archives |
| `download_jar` | Download/create JAR (triggers Central sync if needed) |
| `check_sync_status` | Maven Central sync stage and timestamps |
| `list_sync_pipeline` | Overview of sync pipeline by stage |
| `get_event_log` | Sync events and errors for a package |

All tools accept both **NPM names** (`lit`, `@hotwired/stimulus`) and **Maven coordinates** (`org.mvnpm:lit`, `org.mvnpm.at.hotwired:stimulus`).

## Setup Guides

### Claude Code

```bash
claude mcp add mvnpm --transport sse https://mvnpm.org/mcp/sse
```

That's it. The mvnpm tools are now available in your Claude Code sessions.

### Cursor

Add to `.cursor/mcp.json` in your project root (or `~/.cursor/mcp.json` globally):

```json
{
  "mcpServers": {
    "mvnpm": {
      "url": "https://mvnpm.org/mcp/sse"
    }
  }
}
```

### VS Code (GitHub Copilot)

Add to `.vscode/mcp.json` in your project root:

```json
{
  "servers": {
    "mvnpm": {
      "type": "sse",
      "url": "https://mvnpm.org/mcp/sse"
    }
  }
}
```

### Windsurf

Add to `~/.codeium/windsurf/mcp_config.json`:

```json
{
  "mcpServers": {
    "mvnpm": {
      "serverUrl": "https://mvnpm.org/mcp/sse"
    }
  }
}
```

### Generic / Other Clients

Any MCP-compatible client can connect using:

- **SSE**: `https://mvnpm.org/mcp/sse`
- **Streamable HTTP**: `https://mvnpm.org/mcp`

Refer to your client's documentation for how to add a remote MCP server.

## What Can Your Agent Do?

Once connected, your AI assistant can help you with tasks like:

- *"Find an NPM package for web components and give me the Maven dependency"*
- *"What's the latest version of lit? Show me the POM."*
- *"Is @hotwired/stimulus synced to Maven Central?"*
- *"List the files inside the htmx JAR"*
- *"Give me the import map for lit 3.2.1"*

The agent handles the NPM-to-Maven name mapping automatically — just use whichever naming convention you prefer.
