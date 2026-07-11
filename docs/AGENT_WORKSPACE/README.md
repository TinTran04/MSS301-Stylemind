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
├── README.md                    # This file - workspace overview
├── ARCHITECTURE_ANALYSIS.md    # Comprehensive architecture analysis
├── SERVICE_SEPARATION_PLAN.md   # Plan for service database separation
├── IMPLEMENTATION_LOG.md        # Agent work tracking and progress
└── MEMORY/
    ├── CURRENT_STATE.md         # Current project state snapshot
    └── DECISIONS.md             # Architectural decisions and rationale
```

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
