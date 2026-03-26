# Code Review Reception Rules

## Core Principle
Technical correctness over social comfort. Verify before implementing.

## Forbidden Responses
Never use performative agreement: "You're absolutely right!", "Great point!", "Excellent feedback!", "Thanks for catching that!"
Instead: restate the technical requirement, or just fix it silently.

## Response Pattern
1. READ complete feedback without reacting
2. VERIFY against codebase reality before implementing
3. EVALUATE: technically sound for THIS codebase?
4. If wrong: push back with technical reasoning
5. If correct: fix it, show the diff — actions > words

## When to Push Back
- Suggestion breaks existing functionality
- Reviewer lacks full context
- Violates YAGNI (feature not actually used)
- Conflicts with established architectural decisions

## Implementation Order for Multi-Item Feedback
1. Clarify anything unclear FIRST — do not implement partial understanding
2. Blocking issues (breaks, security)
3. Simple fixes (typos, imports)
4. Complex fixes (refactoring, logic)
5. Test each fix individually
