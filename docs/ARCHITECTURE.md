# Architecture

TouchPilot is organized around a small agent runtime and a typed Android tool
layer.

```text
User
  -> Chat UI
  -> Agent Runtime
  -> LLM Client
  -> Tool Router
  -> Android Tool Layer
  -> Accessibility / Intents / Storage / Notifications
```

## Core Modules

- `app`: Android UI, navigation, settings, permissions.
- `agent`: session loop, provider clients, prompt building, retries.
- `tools`: tool specifications, routing, validation, execution results.
- `androidcontrol`: AccessibilityService integration and action execution.
- `memory`: local sessions, tool logs, skills, and audit storage.
- `security`: approvals, policy checks, risk classification, secret storage.

## Execution Loop

1. User sends a request.
2. Agent runtime builds context from session, skills, and current policy.
3. LLM returns a message or a structured tool call.
4. Tool router validates the requested tool and arguments.
5. Active skill allowlist approves or denies the requested tool.
6. Security policy approves, denies, or asks the user.
7. Android tool layer executes the action.
8. Result is logged and fed back to the agent.

## First Runtime Target

The first implementation should support OpenAI-compatible chat completions with
structured tool calls. Local inference is intentionally deferred until the
Android control layer is reliable.
