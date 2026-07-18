# Agent Workspace

**Purpose:** Centralized documentation for AI agents working on StyleMind project  
**Created:** 2026-07-11  
**Agent:** Cascade

---

## Overview

This workspace contains all documentation needed for agents to understand the StyleMind project architecture, track work progress, and avoid redundant source code scanning. This reduces token usage and improves agent efficiency.

---

## Folder Structure

```
AGENT_WORKSPACE/
├── README.md                        # This file - workspace overview
├── ARCHITECTURE_ANALYSIS.md         # Comprehensive architecture analysis
├── ENVIRONMENT_MATRIX.md            # Docker service URL + internal-token binding matrix
├── DOCKER_AND_DATABASE_RUNBOOK.md   # Safe Docker/DB troubleshooting commands
├── JWT_AUTHENTICATION.md            # Current Gateway/Auth public-path guidance
├── KNOWN_ISSUES.md                  # Verified incidents and follow-up findings
├── CODEGRAPH.md                     # CodeGraph index status and non-destructive workflow
├── SERVICE_SEPARATION_PLAN.md       # Plan for service database separation
├── SOURCE_CODE_ISSUES.md            # Historical bug log - see staleness note below
├── IMPLEMENTATION_LOG.md            # Agent work tracking and progress (also serves as changelog)
└── MEMORY/
    ├── CURRENT_STATE.md             # Current project state snapshot
    └── DECISIONS.md                 # Architectural decisions and rationale
```

> **Staleness note:** `SOURCE_CODE_ISSUES.md` describes an older architecture state (symmetric
> JWT, single shared PostgreSQL instance, a since-reverted `origin/dev` merge) that predates the
> asymmetric-JWT and per-service-database work reflected in `MEMORY/CURRENT_STATE.md` and
> `ARCHITECTURE_ANALYSIS.md`. It was left as a historical record rather than rewritten (out of
> this task's scope); do not treat its "STILL EXISTS" labels as current without re-checking
> source. Prefer `MEMORY/CURRENT_STATE.md` and `KNOWN_ISSUES.md` for current status.

---

## Quick Start for New Agents

### 1. Read Architecture Analysis
Start with `ARCHITECTURE_ANALYSIS.md` to understand:
- Current system architecture
- Infrastructure components
- Microservices and their roles
- Database structure
- Communication patterns
- Authentication implementation
- Known bugs and issues

### 2. Check Current State
Read `MEMORY/CURRENT_STATE.md` to understand:
- Current project status
- Known issues and their status
- Planned changes
- Documentation status
- Next steps

### 3. Review Implementation Log
Check `IMPLEMENTATION_LOG.md` to see:
- What work has been completed
- What tasks are pending
- Decisions made
- Session history

### 4. Understand Decisions
Read `MEMORY/DECISIONS.md` to understand:
- Architectural decisions made
- Rationale behind decisions
- Trade-offs considered
- Pending decisions

### 5. Check Service Separation Plan
If working on database separation, read `SERVICE_SEPARATION_PLAN.md` for:
- Detailed implementation plan
- Phase-by-phase approach
- Testing checklist
- Rollback procedures

### 6. Before changing Gateway authentication
Read `JWT_AUTHENTICATION.md` and `KNOWN_ISSUES.md` before changing public paths or JWT filters. Read `MEMORY/CURRENT_STATE.md` before debugging registration or OTP flows.

> A route being matched by Spring Cloud Gateway does not prove the request reached the downstream service. A custom `GlobalFilter` can return a response before `NettyRoutingFilter` forwards the request.

---

## File Descriptions

### ARCHITECTURE_ANALYSIS.md
Comprehensive analysis of the current StyleMind architecture including:
- System overview and components
- Infrastructure configuration
- Database architecture
- Service communication patterns
- Authentication and security
- Payment service status
- Configuration management
- Docker configuration
- Known bugs
- Async/reactive patterns
- Database migration approach
- Summary of strengths and weaknesses

### SERVICE_SEPARATION_PLAN.md
Detailed plan to separate each microservice into individual Docker containers with dedicated PostgreSQL instances:
- Objective and current state analysis
- Target architecture design
- Implementation steps (5 phases)
- Application.yml updates
- Network and volume configuration
- Health checks
- Rollback plan
- Testing checklist
- Benefits and risks
- Estimated timeline

### IMPLEMENTATION_LOG.md
Tracking log for agent work:
- Session history with completed tasks
- Known bugs and their status
- Pending tasks
- Decisions made
- Notes and observations
- Next session goals

### MEMORY/CURRENT_STATE.md
Snapshot of current project state:
- Project information
- Current architecture status
- Known issues
- Service communication
- Authentication status
- Configuration management
- Docker status
- Planned changes
- Documentation status
- Next steps

### MEMORY/DECISIONS.md
Record of architectural decisions:
- Decision log with dates and rationale
- Pending decisions with options
- Rejected decisions with rationale
- Decision criteria

---

## Usage Guidelines

### For New Agents
1. Always start by reading this README
2. Read files in order: README → ARCHITECTURE_ANALYSIS → CURRENT_STATE → IMPLEMENTATION_LOG
3. Check DECISIONS.md before making new architectural decisions
4. Update IMPLEMENTATION_LOG.md when completing tasks
5. Update CURRENT_STATE.md when project state changes
6. Add new decisions to DECISIONS.md

### For Continuing Work
1. Check IMPLEMENTATION_LOG.md for last session's progress
2. Review CURRENT_STATE.md for any changes
3. Check DECISIONS.md for any new decisions
4. Update IMPLEMENTATION_LOG.md with new work
5. Update relevant files if project state changes

### When Making Changes
1. Update IMPLEMENTATION_LOG.md with task completion
2. Update CURRENT_STATE.md if project state changes
3. Add new decisions to DECISIONS.md
4. Update ARCHITECTURE_ANALYSIS.md if architecture changes
5. Update SERVICE_SEPARATION_PLAN.md if plan changes

---

## Benefits

### Token Efficiency
- Avoid scanning source code multiple times
- Single source of truth for project knowledge
- Quick reference for common questions

### Agent Collaboration
- Clear history of work done
- Easy handoff between agents
- Consistent understanding of project state

### Decision Tracking
- Rationale for architectural decisions
- Trade-offs considered
- Pending decisions with options

### Progress Tracking
- Clear view of what's been done
- What's pending
- What's planned

---

## Important Notes

- This workspace is meant to be **read-only** for most agents
- Only update files when making actual changes to the project
- Keep documentation up-to-date with project changes
- Be thorough in documenting decisions and rationale
- This workspace complements, not replaces, the original AGENTS.md

## Guidance for future agents (added 2026-07-19)

1. Read this workspace before changing the project, but do not treat it as the final word - it can
   lag the actual repository state, especially for anything dated more than a few days ago.
2. Before acting on a documented fact, cross-check it against current Git history and current
   source. A memory or doc entry that names a specific file, property, or config value is a claim
   about what was true when it was written, not a guarantee about now.
3. Do not rely only on old issue reports to decide whether something is fixed. A fix recorded for
   one service pair (e.g. `order-service ↔ auth-service`) does not imply every other pair using the
   same shared mechanism was updated - verify each pair independently (see ENVIRONMENT_MATRIX.md
   for an example of this class of problem with `internal.token`).
4. Use Docker service DNS names and internal ports for container-to-container traffic
   (`http://order-service:8087`, never `http://localhost:8087` from inside another container).
   `localhost` is only valid for host-side/browser calls to published ports, same-container
   healthchecks, and non-Docker local-process runs.
5. Environment variables in `.env` are not automatically available inside a container - Compose
   must explicitly map each one under that service's `environment:` block, and the service's own
   `application.yml` must bind to the same variable name Compose actually injects. Verify both ends
   before assuming a variable is "set".
6. Do not mark anything `RESOLVED` without fresh, reproducible evidence (a passing test, a
   read-only runtime probe, or direct log/database evidence). A configuration change plus a
   successful build is `IMPLEMENTED - RUNTIME VERIFICATION PENDING`, not `RESOLVED`, until the
   actual runtime behavior has been observed.
7. Never place secrets (JWT tokens, `INTERNAL_TOKEN`/`X_INTERNAL_TOKEN` values, SePay/webhook API
   keys, database passwords, SMTP credentials, RSA private keys) in this workspace, even when
   discovered in logs or other files during investigation. Reference where a secret lives and how
   to check it safely (see DOCKER_AND_DATABASE_RUNBOOK.md for examples), never the value itself.

---

## References

- Original AGENTS.md - Agent blueprint and developer rules
- docs/ folder - Additional project documentation
- BE/ folder - Backend source code
- FE/ folder - Frontend source code

---

## Contact/Questions

If you have questions about the workspace structure or need clarification on any documentation:
1. Check the relevant file for detailed information
2. Review IMPLEMENTATION_LOG.md for context
3. Check DECISIONS.md for architectural rationale
4. Refer to ARCHITECTURE_ANALYSIS.md for technical details
