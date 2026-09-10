Agent Instructions — Project conventions and rules

Purpose

This file defines the rules an automated agent (or contributor) must follow when generating or modifying code in this repository. The goal is consistent, maintainable Java code aligned with the project's architecture.

Core constraints

- Technology: Java only. Do not add other programming languages or transpiled artifacts.
- Architecture: Follow Clean Architecture principles: distinct layers (domain, use-cases/interactors, adapters/drivers, frameworks), dependency rules (outer layers depend on inner layers only), and clear separation of responsibilities.
- Simplicity: Prefer clear, minimal implementations over clever or highly abstracted solutions. Avoid unnecessary design patterns and abstractions.
- Readability & Maintainability: Write easy-to-read code, descriptive names, and small focused methods and classes.

Coding guidelines

- Design: Keep domain logic pure and free from framework concerns. Use interfaces for boundaries and keep implementations in outer layers.
- Tests: Provide unit tests for new or changed domain/use-case logic. Prefer simple test fixtures and avoid fragile setups.
- Error handling: Use explicit exceptions and meaningful messages. Do not swallow exceptions silently.
- Dependencies: Minimize external dependencies. Add libraries only when justified (performance or safety) and approved by maintainers.
- Formatting: Follow project formatter (if present). Keep style consistent with existing code.

Commits, branches, and PRs

- Branch naming: issue-<number>-short-description (e.g., issue-1-agent-instructions).
- Commits: Small, single-purpose commits with imperative messages. Include context in the commit body when needed.
- PRs: Provide a clear summary, list of changed files, rationale for design choices, and any follow-up tasks. Link the original issue.
- Do not push work until it is ready for public review if instructed otherwise. (Local commits are acceptable.)

Agent behavior expectations

- Minimal changes: Modify only the files necessary to implement the task.
- Explain decisions: In the PR description or issue comment, summarize non-obvious decisions and trade-offs.
- Preserve tests: Do not remove or break existing tests; update them as needed.
- Review-ready: When marking "in review", ensure code builds locally and unit tests pass.

Documentation

- Document public-facing APIs and complex business rules in code comments or README where it helps future maintainers.

If anything in this file is unclear or needs to be extended, open a follow-up issue referencing this file.
