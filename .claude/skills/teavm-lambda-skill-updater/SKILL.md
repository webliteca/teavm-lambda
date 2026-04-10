---
name: teavm-lambda-skill-updater
description: "Use this skill whenever someone asks to update, refresh, regenerate, or sync the teavm-lambda skill, or after significant changes to the library that the skill should reflect. Triggers on: update the teavm-lambda skill, sync the skill, regenerate the skill, refresh skill content, skill is out of date."
---

# teavm-lambda Skill Updater

Updates `skills/teavm-lambda/` to reflect changes in the teavm-lambda library source code. This skill only reads the library source and writes to the skill directory — it never modifies library code.

## Workflow

### Step 1: Read state

Load `state/last-updated.json` from this skill's directory. It contains the last synced commit, version, and date.

```bash
cat .claude/skills/teavm-lambda-skill-updater/state/last-updated.json
```

If the file doesn't exist, treat as first-time sync — look at the current state of the library and verify the skill content matches.

### Step 2: Diff the library

Find what changed since the last sync:

```bash
# Summary of changes
git log --oneline <last_synced_commit>..HEAD -- \
    docs/ \
    "*/src/main/java/ca/weblite/teavmlambda/api/**" \
    "*/pom.xml" \
    "*/src/main/java/**/processor/**" \
    "*/META-INF/services/**"

# Detailed diff
git diff <last_synced_commit>..HEAD -- \
    docs/ \
    "*/src/main/java/ca/weblite/teavmlambda/api/**" \
    "*/pom.xml"
```

If the diff is huge (>100 commits), recommend the user do a manual review first and run the updater on smaller chunks.

### Step 3: Categorize changes

Sort each change into one of these categories:

| Category | Action |
|----------|--------|
| **Breaking API** (renamed/removed/signature-changed public methods) | Update SKILL.md gotchas, api-signatures.md, affected reference files |
| **New public API** | Add to api-signatures.md and relevant reference file |
| **New modules** | Update references/ and possibly pom-templates.md |
| **New deployment target/profile** | Create new references/deployment-*.md |
| **Documentation-only** | May update reference files, not API signatures |
| **Internal/impl changes** | Usually no skill update needed — note and skip |
| **New gotchas** (e.g., bug fix revealing a footgun) | Update gotchas.md and top gotchas in SKILL.md |

### Step 4: Regenerate api-signatures.md

If the script exists and works:

```bash
./skills/teavm-lambda/scripts/generate-api-signatures.sh
```

If the script fails, manually update `skills/teavm-lambda/references/api-signatures.md` based on the diff. The hand-curated version in the repo is the authoritative fallback.

### Step 5: Propose changes

Before editing any skill files, summarize what was found and what will change. For each proposed edit, show:
- The file to modify
- The rationale
- A brief preview of the change

Wait for the user's confirmation before applying. For large updates, batch by category and confirm each batch.

### Step 6: Apply edits

After confirmation, edit files in `skills/teavm-lambda/`. Preserve the overall structure — don't rewrite from scratch unless the user explicitly asks.

**Validation checklist after edits:**
- [ ] `skills/teavm-lambda/SKILL.md` is under ~500 lines
- [ ] Every `references/*.md` file referenced by SKILL.md exists
- [ ] `references/api-signatures.md` has no obviously broken entries
- [ ] If example projects reference changed APIs, update them too
- [ ] If version changed in pom.xml, update version references in SKILL.md and pom-templates.md

### Step 7: Update state

Write the new HEAD commit, version, and timestamp into `state/last-updated.json`:

```bash
git rev-parse HEAD  # new commit
grep '<version>' pom.xml | head -1  # version from root pom
```

```json
{
  "last_synced_commit": "<new HEAD>",
  "last_synced_version": "<version from pom.xml>",
  "last_synced_date": "<ISO 8601 timestamp>",
  "skill_path": "skills/teavm-lambda"
}
```

Commit the state file alongside the skill changes. Suggest commit message:

```
Update teavm-lambda skill to <version> (<short description of changes>)
```

### Step 8: Suggest next steps

- If the version changed (e.g., 0.1.0 → 0.2.0), suggest tagging a skill release or noting it in CHANGELOG.
- If new modules were added, suggest creating additional reference files or examples.
- If breaking changes occurred, suggest reviewing downstream projects that use the skill.

## Relevant source paths

These are the paths to monitor for changes that affect the skill:

- `docs/` — developer guide source
- `teavm-lambda-core/src/main/java/ca/weblite/teavmlambda/api/` — core public API
- `teavm-lambda-db-api/src/main/java/` — database API
- `teavm-lambda-nosqldb/src/main/java/` — NoSQL API
- `teavm-lambda-objectstore/src/main/java/` — object storage API
- `teavm-lambda-messagequeue/src/main/java/` — message queue API
- `teavm-lambda-adapter-war/src/main/java/` — WarServlet
- `teavm-lambda-image-api/src/main/java/` — image processing API
- `teavm-lambda-processor/src/main/java/` — annotation processor (affects generated code)
- `*/pom.xml` — dependency or profile changes
- `*/META-INF/services/**` — SPI registrations

## Constraints

- Never edit library source code — only read the library, write to `skills/teavm-lambda/`.
- Never silently overwrite hand-written content. When in doubt, propose and ask.
- Keep api-signatures.md focused on public API surface only — skip internal/impl classes.
