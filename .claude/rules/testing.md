# Testing Rules

## Approach
Write tests alongside implementation — not as an afterthought.
When fixing a bug, write a failing test that reproduces it first, then fix the code.

## Assertions
Assert the specific value, not just that something is truthy.
Prefer `assertEquals(42, result)` over `assertNotNull(result)`.

## Edge Case Coverage
Every function must cover: empty input, null, boundary values, and error paths.
Do not only test the happy path — edge cases are where bugs live.

## No Mocks by Default
Prefer real implementations over mocks.
Only mock: external services and time-dependent behavior.
When mocking, document why a real implementation could not be used.

## Isolation
Each test must be fully independent — no shared mutable state between tests.
Reset all side effects in `@After` teardown blocks.
Tests must pass in any order and when run in isolation.

## Test Naming
Use descriptive names that explain the scenario and expected outcome:
`fun returnsNullWhenUserIdDoesNotExist()` not `fun testWorks()`.
