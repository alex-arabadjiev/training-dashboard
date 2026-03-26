# Agent Delegation Rules

## Model Routing

Always set `model:` when spawning subagents. Haiku costs 12× less than Sonnet — use it for all search and explore work.

| Model | Use for |
|-------|---------|
| `haiku` | ALL Explore agents, file search, codebase questions, simple research |
| `sonnet` | Implementation, code generation, test writing |
| `opus` | Architecture review, complex analysis, spec creation |

Never spawn an Explore or search agent without `model: haiku`.
Never spawn an agent if the task has fewer than 3 tool calls worth of work.
Never let subagents inherit your session context — construct exactly what they need in the prompt.

## Hallucination Prevention

- Never invent or guess file paths — verify with Glob/Grep before referencing
- Never assume import paths, function names, or API routes exist — read the file first
- If an agent reports a file path or symbol, verify it exists before acting on the report
