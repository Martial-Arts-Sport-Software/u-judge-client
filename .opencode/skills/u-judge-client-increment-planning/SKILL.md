---
name: u-judge-client-increment-planning
description: Use when choosing the next U'Judge client task, scoping or splitting work, creating an issue or branch, or preparing and describing a PR.
---

# U'Judge Client Increment Planning

The unit of planning and review is a shared increment (`I1`...`I8`) that spans the client and `u-judge-server`, not a
single DTO, reducer, control or fake-server contract. Follow `AGENTS.md`; this skill turns it into steps.

## 1. Choose the increment

1. Open "Инкременты поставки" in `docs/ROADMAP.md` and in the server roadmap; take the first increment with status `[ ]`.
2. Write the client scenario as one demo script: what the judge does on the device and what the operator sees on desktop.
3. List the `CLI-*` IDs and gate items it will close. If none can be closed, the scope is too small.
4. Check "Известные расхождения с server": the increment must remove the divergences assigned to it in both repositories.

## 2. Size check

- Target 3-7 working days and one issue per repository. Up to 2-3 sequential PRs under one issue are allowed when each
  leaves `main` working; only the last one marks statuses.
- Too big: split by scenario (for example "Tanbon" and "technical Send"), never by layer.
- Too small: a PR adding one DTO, reducer, control or fake-server test belongs as a commit inside the increment.
- A local scenario without the server (offline, formulas, localization) may be its own increment.

## 3. Issue, branch, commits

- Create the issue first: title `<Type> | <Imperative Description>`, parent increment, linked server issue, requirement
  IDs, scope, demo script as acceptance criteria, known limitations. Set assignee `TheGeniusOfEternity`, the gate
  milestone, one `type: *` label, relevant `area: *` and `priority: *` labels, and `pilot`. Add it to the GitHub Project,
  or ask the user which Project to use.
- Branch from local `main` as `<type>/<short-kebab-scope>`; do not fetch or pull without an explicit request.
- Commit in small Conventional Commits (`<type>(<scope>): <imperative description>`), each building on its own.

## 4. Definition of done

- The demo script runs on an Android emulator or device against `./gradlew :desktop:run` from `u-judge-server`; physical
  Android/iPhone runs are recorded when the increment requires them.
- Message types and fields match the server contract; fake-server tests use the same shapes.
- Android build, host tests, iOS compilation and `git diff --check` pass.
- The increment row, the evidence table and `REQUIREMENTS.md` statuses are updated once; requirement wording is not
  rewritten. Mark `[x]` only for closed requirements or gate items with evidence.

## 5. Pull request

- Title `<Type> | <Imperative Description>`; same metadata as the issue; link the server PR.
- Body sections: Goal (increment and scenario), Changes, Evidence (commands and results, emulator/device, server commit),
  Requirements closed, Known limitations, `Closes #<issue>`.
