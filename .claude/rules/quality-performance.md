# Quality: Performance Principles

## Database
- No N+1 queries: do not run individual DB calls inside loops — batch or join
- Queries on large tables must use indexed columns
- Always add LIMIT to queries that could return unbounded result sets

## I/O
- No synchronous I/O on the main thread — use suspend functions or background dispatchers
