# Spec-Driven Development

Specs are structured task plans created before coding. They live in `specs/` and follow a simple template.

## When to Create a Spec

**Create a spec when:**
- Changes touch 3+ files
- Adding a new feature or module
- Requirements are ambiguous or complex
- Architectural decisions are involved

**Skip specs for:**
- Single-file fixes (typos, CSS tweaks, config changes)
- Bug fixes with obvious root cause
- Documentation-only changes

## Naming Convention

```
NNN-short-description.md
```

Examples: `001-add-user-authentication.md`, `002-refactor-api-layer.md`

## Workflow

### 1. Create a spec

```
/spec "add dark mode support"
```

This runs an integrated workflow:
1. **Challenge phase** — validates the idea, questions assumptions, surfaces edge cases
2. **Spec generation** — creates `specs/NNN-add-dark-mode-support.md` with steps, criteria, and files to modify

### 2. Review and refine

Read the generated spec. Edit steps, acceptance criteria, or files to modify as needed.

### 3. Execute one spec

```
/spec-work NNN
```

Executes the spec step by step, checking off each step as it completes. Sets status to `in-review` when done.

### 4. Execute all specs in parallel

```
/spec-work-all
```

Discovers all draft specs in `specs/`, detects dependencies between them, then executes in parallel waves using **isolated Git worktrees** — one branch per spec, no merge conflicts. Independent specs run simultaneously; dependent specs wait for their dependencies.

### 5. Review and create PR

```
/spec-review NNN
```

Opus reviews all code changes against the spec's acceptance criteria. Three possible verdicts:
- **APPROVED** — spec completed, moved to `specs/completed/`, PR draft prepared
- **CHANGES REQUESTED** — feedback written to spec, status back to `in-progress` for another `/spec-work` pass
- **REJECTED** — spec blocked with explanation

### 6. View the board

```
/spec-board
```

Kanban-style overview of all specs grouped by status with step-level progress (`[3/8]`) and branch info.

## Spec Status Lifecycle

```
draft → in-progress → in-review → completed
                ↑          |
                └──────────┘  (changes requested)
                
Any status → blocked
```

| Status | Meaning |
|---|---|
| `draft` | Planned, not started |
| `in-progress` | Agent is working on it |
| `in-review` | Work done, awaiting review |
| `blocked` | Blocked by dependency or review rejection |
| `completed` | Reviewed and done, moved to `specs/completed/` |

## Directory Structure

```
specs/
  README.md           # This file
  TEMPLATE.md         # Blank spec template
  001-feature-name.md # Draft spec (status: draft)
  002-other-task.md   # In-progress spec (status: in-progress, branch: spec/002-other-task)
  completed/
    000-old-spec.md   # Completed spec
```
