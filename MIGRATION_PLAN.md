# OFBiz → abacus-jdbc Migration Plan

Status: Proposal — pending Phase 0 sign-off
Target: Replace the OFBiz entity engine with abacus-jdbc, collapsed into a single Spring Boot application

---

## 1. Decisions (locked)

| # | Decision | Rationale |
|---|---|---|
| D1 | **Single Boot app.** All `*-abacus` modules collapse into one `ofbiz-abacus` Spring Boot application (per-domain Java packages, one main class, one HikariCP `DataSource`). | The 13 current `@SpringBootApplication` mains were prototypes. One process = one transaction manager = one bean graph. |
| D2 | **Full replace.** `framework/entity` (the legacy entity engine) and every direct caller of `Delegator`/`GenericValue` will be rewritten or removed. No long-term coexistence, no `Delegator` SPI adapter. | Cleanest end state; avoids dual maintenance of two stacks against the same DB. |
| D3 | **`SQLBuilder.PSC`** is the standard. 67/67 active DAOs already use it. | Entity-abacus's `PSC→NSC` instruction is the outlier; will be reverted and its rewrite docs/scripts deleted. |
| D4 | **abacus-common 7.1.2 / abacus-jdbc 4.1.2 / abacus-query 4.1.2**, Spring Boot 4.0.0, Java 25 (matches 12 of the 13 modules today). entity-abacus will be bumped. | One toolchain across the project. |

---

## 2. Current state inventory

### Legacy engine (to be removed)
- `framework/entity/src/main/java/org/apache/ofbiz/entity/` — **149 Java files**: `Delegator`, `DelegatorFactory`, `GenericDelegator`, `GenericValue`, `GenericEntity`, `GenericPK`, and subpackages `cache/ condition/ config/ connection/ datasource/ eca/ finder/ jdbc/ model/ serialize/ test/ testtools/ transaction/ util/`.
- `framework/entityext/src/` — entity sync, group, tenant extensions.
- **389 Java files** outside `*-abacus/` import `GenericValue` / `Delegator` / `GenericDelegator` / `GenericEntity`. Modules: `framework/{base, catalina, common, entityext, minilang, security, service, testtools, webapp, webtools, widget}` and `applications/{accounting, content, humanres, manufacturing, marketing, order, party, product, securityext, workeffort}`.
- Entity definitions in XML across `applications/datamodel/entitydef/*.xml`, `framework/*/entitydef/entitymodel.xml`, `applications/*/entitydef/entitymodel_view.xml`.
- Service definitions: **143 `services*.xml` files** (~1000+ `<service>` entries) wired through `LocalDispatcher` + `GenericDelegator`.

### Abacus stack (in place, partial)
13 standalone Spring Boot modules — to be merged into one:

| Module | Java files | DAOs | Entities | Services |
|---|---:|---:|---:|---:|
| framework/entity-abacus | 29 | 5 | 6 (db) + 11 (model) | 2 |
| framework/common-abacus | 36 | 6 | 13 | 6 |
| framework/webtools-abacus | 33 | 5 | 12 | 6 |
| applications/party-abacus | 24 | 6 | 11 | 2 |
| applications/securityext-abacus | 29 | 5 | 11 | 4 |
| applications/content-abacus | 11 | 2 | 4 | 1 |
| applications/workeffort-abacus | 21 | 4 | 9 | 2 |
| applications/product-abacus | 32 | 6 | 14 | 4 |
| applications/manufacturing-abacus | 24 | 4 | 10 | 3 |
| applications/accounting-abacus | 47 | 10 | 22 | 5 |
| applications/humanres-abacus | 25 | 5 | 10 | 3 |
| applications/order-abacus | 26 | 5 | 11 | 3 |
| applications/marketing-abacus | 30 | 5 | 13 | 4 |

**Coverage gap (entities defined in XML vs. ported to abacus POJOs):**

| Domain | XML `<entity>` | XML `<view-entity>` | XML `<extend-entity>` | Abacus POJOs | Gap |
|---|---:|---:|---:|---:|---:|
| party | 82 | 41 | 8 | 11 | ~120 |
| product | 169 | 39 + 4 | 1 | 14 | ~199 |
| accounting | 160 | 37 + 3 | 0 | 22 | ~178 |
| order | 112 | 2 + 53 | 0 | 11 | ~156 |
| content | 64 | 13 | 2 | 4 | ~75 |
| humanres | 41 | 5 | 0 | 10 | ~36 |
| marketing | 32 | 10 | 0 | 13 | ~29 |
| workeffort | 40 | 31 | 0 | 9 | ~62 |
| manufacturing | 7 (datamodel) | 1 | 0 | 10 | covers most |
| **shipment** | 39 | 12 | 0 | **0** | **51** — no abacus module exists |
| common | 46 | 11 | 1 | 13 | ~45 |
| security | 11 | 3 | 0 | 11 | 3 (views only) |
| webapp/catalina/service | 1 + 14 + 8 | 0 + 0 + 1 | 0 | (in webtools-abacus) | ~24 |
| **commonext** | 0 | 0 | 1 | **0** | 1 — no module |
| **entityext** | 7 | 1 | 0 | **0** | 8 — no module |

Roughly **~1,100 of ~1,250 logical entities are unported**. Generator-driven, not hand-written.

### Known inconsistencies to clean up first
- `framework/entity-abacus/build.gradle` is still Spring Boot 3.2.0 / Java 17. Bump to 4.0.0 / 25.
- 5 entity-abacus DAOs use `SQLBuilder.PSC` (correct per D3), but the module ships `update-entity-abacus.sh/bat`, `apply-changes.ps1`, `README_REWRITE.md`, `REWRITE_SUMMARY.md`, `REWRITE_INSTRUCTIONS.md` instructing PSC→NSC. **Delete all 5 + 2 scripts.**
- Every `-abacus` module has its own duplicated `DaoConfig`, `IdGeneratorConfig`, `GlobalExceptionHandler`. These collapse into shared infrastructure under D1.
- `*-abacus` modules are not registered in `framework/component-load.xml` or `applications/component-load.xml`, so they are invisible to the existing Gradle build (`settings.gradle` walks those files).

---

## 3. Target architecture

```
ofbiz-abacus/                                  (single Boot app, replaces the 13 modules)
├── build.gradle                               Boot 4.0.0, Java 25
├── src/main/java/org/apache/ofbiz/abacus/
│   ├── OfbizAbacusApplication.java            single @SpringBootApplication
│   ├── infrastructure/
│   │   ├── DaoAutoConfiguration.java          @AutoConfiguration scanning @AbacusDao
│   │   ├── DataSourceConfig.java              HikariCP, multi-tenant aware
│   │   ├── TransactionConfig.java             single PlatformTransactionManager
│   │   ├── IdGenerator.java                   replaces SequenceUtil / DelegatorImpl id gen
│   │   ├── EecaInterceptor.java               replaces EntityEcaUtil hooks (AOP around DAOs)
│   │   ├── AuditingInterceptor.java           created/modified stamping, change history
│   │   ├── CryptoInterceptor.java             field-level encryption (was EntityCrypto)
│   │   └── exception/GlobalExceptionHandler.java
│   ├── party/{entity,dao,service,controller}/
│   ├── securityext/...
│   ├── content/...
│   ├── workeffort/...
│   ├── product/...
│   ├── shipment/...                           NEW — no abacus module today
│   ├── manufacturing/...
│   ├── accounting/...
│   ├── humanres/...
│   ├── order/...
│   ├── marketing/...
│   ├── common/...
│   ├── entityext/...                          NEW — sync/group/tenant
│   ├── webtools/...
│   └── entity/                                meta-model (definition/field/index/relation DAOs)
└── src/main/resources/
    ├── application.yml
    └── db/migration/                          Flyway scripts derived from current entitymodel
```

Per-domain convention (unchanged from current style):
- `entity/Foo.java` — Lombok `@Data @Builder @NoArgsConstructor @AllArgsConstructor`, Abacus `@Table("foo") @Id @Column(...)`.
- `dao/FooDao.java` — `interface FooDao extends CrudDao<Foo, IdType, SQLBuilder.PSC, FooDao>` with `@Query` methods; `@Transactional` only on writes.
- `service/FooService.java` — Spring `@Service`, constructor-injected DAOs, business logic moved here from `applications/<dom>/src/.../*Services.java`.
- `controller/FooController.java` — REST endpoints (where existing servlets / events become HTTP).

---

## 4. Phased plan

### Phase 0 — Sign-off (done)
Decisions D1–D4 above. Already locked.

**Exit:** This document approved.

---

### Phase 1 — Consolidate the 13 modules into `ofbiz-abacus` (cleanup, no behavior change)

**Scope:** mechanical move + dedup. No new features, no new entities.

Steps:
1. Create `ofbiz-abacus/` at repo root with the layout in §3.
2. Move every package from each `*-abacus/src/main/java/org/apache/ofbiz/<dom>/` into `ofbiz-abacus/src/main/java/org/apache/ofbiz/abacus/<dom>/`. Rewrite imports.
3. Promote one `DaoConfig` to `infrastructure/DaoAutoConfiguration.java`; delete the 13 per-module copies. Same for `IdGeneratorConfig`, `GlobalExceptionHandler`.
4. Delete all 13 `@SpringBootApplication` main classes; keep one in `OfbizAbacusApplication`.
5. Bump entity-abacus's previously-stuck dependencies to Boot 4.0.0 / Java 25 (already merged into the single build.gradle, so this falls out naturally).
6. Delete obsolete artifacts: `framework/entity-abacus/{README_REWRITE.md, REWRITE_SUMMARY.md, REWRITE_INSTRUCTIONS.md, update-entity-abacus.sh, update-entity-abacus.bat, apply-changes.ps1}`.
7. Register `ofbiz-abacus` in `settings.gradle` (it doesn't go through `component-load.xml`).
8. Delete the 13 old `*-abacus/` directories.

**Acceptance:**
- `gradle :ofbiz-abacus:build` green.
- `gradle :ofbiz-abacus:bootRun` starts; all existing REST endpoints (currently scattered across 13 modules) reachable on one port.
- No PSC→NSC text remains in the repo (sanity grep).
- Diff: ~67 DAOs and ~145 entities/services moved; no behavior change.

**Estimate:** 2–3 days. Pure refactor; deferred work shouldn't sneak in.

---

### Phase 2 — Entity coverage to 100% via generator

**Scope:** generate the ~1,100 missing POJOs and DAO skeletons from XML.

Steps:
1. Build a generator (extend the existing `update_entities.py` at the repo root; it already parses entitymodel XML). Inputs:
   - `applications/datamodel/entitydef/*.xml`
   - `framework/*/entitydef/entitymodel.xml`
   - `applications/*/entitydef/entitymodel_view.xml`
   - `applications/*/entitydef/entitymodel_reports.xml`

   Outputs into `ofbiz-abacus/src/main/java/org/apache/ofbiz/abacus/<domain>/entity/` and `.../dao/`:
   - POJO with Lombok + Abacus annotations (template = `applications/party-abacus/.../entity/Party.java`).
   - `CrudDao<E, IdType, SQLBuilder.PSC, EDao>` with no `@Query` methods (those get added as services need them).
2. `<extend-entity>` → merge fields into the base POJO during generation (don't model as runtime inheritance).
3. `<view-entity>` → emit a read-only POJO + a hand-written `@Query` join in the DAO. Calc-fields / dynamic-view-entity / complex aliases get a follow-up issue per entity; default policy is "skip generation, flag manually." There are ~250 `<view-entity>`s total; expect ~30–50 needing custom work.
4. Generate Flyway migrations from the entitymodel field/index/relation defs (Phase 4 will switch the DB lifecycle to Flyway; for now the legacy `EntityDataLoader` still owns DDL — these scripts only need to *exist*, not run).
5. Generation order (matches `applications/component-load.xml` dependency order): `common → security → party → securityext → content → workeffort → product → shipment → manufacturing → accounting → humanres → order → marketing → entityext → commonext → webapp/catalina/service residue`.
6. Create *new* package trees for **shipment**, **entityext**, **commonext** — no `-abacus` module exists for these today.

**Acceptance:**
- One POJO + one DAO per `<entity>` and `<extend-entity>` and (where automatable) `<view-entity>`.
- A coverage report (`scripts/coverage.py` or similar) shows ≥98% of XML entities mapped; remainder listed with reasons (e.g., dynamic-view-entity, calc fields).
- `gradle :ofbiz-abacus:compileJava` green.

**Estimate:** generator: ~1 week. Generation pass + view-entity hand-work: ~2 weeks.

---

### Phase 3 — Port services + call sites

This is the largest chunk. Touches the 389 legacy call sites and ~1000 service implementations.

**Approach:** domain-by-domain, in the same order as Phase 2. For each domain:

1. **Service rewrite.** For each `services*.xml` in `applications/<dom>/servicedef/`, locate the Java impl class (typically `applications/<dom>/src/main/java/org/apache/ofbiz/<dom>/.../*Services.java`). Rewrite it to call the new Spring `*Service` instead of `dispatcher.runSync(...)` / `delegator.findOne(...)`.
2. **Direct call sites.** For each file in that domain importing `GenericValue`/`Delegator`, swap to abacus DAOs/POJOs.
3. **Minilang.** `applications/<dom>/.../*.xml` minilang scripts that touch entities → either (a) rewrite as Spring `@Service` methods or (b) keep XML and bridge through a thin `MinilangBridge` that calls abacus services. Pick per-file based on complexity; default to (a) for anything non-trivial.
4. **Widget / forms.** `widget/*-form.xml` and `widget/*-screen.xml` reference entities by name and read fields off `GenericValue`. Three sub-options:
   - **(i)** Rewrite forms to bind to POJO field names (preferred for new screens).
   - **(ii)** Keep a minimal `GenericValue` shim that wraps a POJO via reflection — *temporary*, removed in Phase 4.
   - **(iii)** Migrate the screen entirely to a REST + frontend rewrite (out of scope here).
   Default to (ii) during the domain pass, schedule (i) per-screen in a follow-up.
5. **EECA / SECA hooks.** For every `eecas.xml` / `secas.xml` in the domain, port to `EecaInterceptor` / `@EventListener` (the infrastructure landed in Phase 1).
6. **Tests.** Per-domain test suite must pass against the new stack. Where tests use `EntityTestSuite`, port to Spring Boot `@DataJdbcTest` with the abacus DAOs.

**Per-domain exit criteria:**
- Zero `import org.apache.ofbiz.entity.GenericValue` (and friends) in the domain's source tree.
- Domain's REST endpoints + screens render the same data as before (verified manually on a seeded H2 DB).
- Domain's existing JUnit suite green against abacus.

**Ordering:** same as Phase 2. Critical-path domains first (party, securityext) so downstream domains can depend on their Spring services.

**Estimate:** 1–2 weeks per domain × 13 domains. Expect 4–6 months elapsed with parallelism.

**Risks called out now:**
- Multi-datasource / per-tenant `Delegator` swap (used in `OFBizRealm`, tenant code in `entityext`) needs a tenant-aware `DataSource` routing strategy. Design before starting `securityext` / `entityext` phases.
- Cross-DAO transactions: `@Transactional` on `@Query` is per-method. Service-level `@Transactional` on the new `*Service` classes is the right boundary — make sure the Spring TX manager wraps Abacus's connection handling correctly (verify in Phase 1 with a smoke test).
- `EntityCrypto` field-level encryption: the abacus side has no replacement today. Build `CryptoInterceptor` in Phase 1 infrastructure.

---

### Phase 4 — Decommission the legacy engine

Only after all 13 domains pass Phase 3.

Steps:
1. Delete `framework/entity/src/`. Delete `framework/entityext/src/`. Delete `framework/common/src/main/java/.../GenericDelegator`-touching code that remains.
2. Remove `framework/entity` and `framework/entityext` from `framework/component-load.xml`.
3. Drop `applications/datamodel/entitydef/*.xml`, `framework/*/entitydef/entitymodel*.xml`, `applications/*/entitydef/entitymodel*.xml`. (Keep `entitygroup.xml` only if multi-tenant routing still needs it; otherwise also drop.)
4. Drop `applications/*/servicedef/services*.xml` once their consumers are migrated — keep the ones still referenced by minilang/widget if any remain (track in a "screen rewrite" backlog).
5. Remove any temporary `GenericValue` shim from Phase 3.4.ii.
6. Switch DB lifecycle from `EntityDataLoader` to Flyway (migrations generated in Phase 2.4).
7. Final `gradle build` + full smoke test on seeded H2 and one prod-shape Postgres.

**Acceptance:**
- `git grep "GenericValue\|GenericDelegator\|import org.apache.ofbiz.entity\."` returns 0 hits (outside docs).
- `framework/entity` and `framework/entityext` directories gone.
- App starts, all REST endpoints + remaining screens functional.

**Estimate:** 1 week, mostly deletion + smoke tests.

---

## 5. Milestones (rough timeline assuming 1 FTE)

| M | Phase | Output | Elapsed |
|---|---|---|---|
| M0 | Phase 0 | This doc signed off | — |
| M1 | Phase 1 | `ofbiz-abacus` single Boot app builds & runs with current coverage | +1 week |
| M2 | Phase 2 | Generator + ≥98% entity coverage (POJOs + DAO skeletons) | +4 weeks |
| M3 | Phase 3 (pilot: party + securityext) | Two domains fully ported, tests green | +7 weeks |
| M4 | Phase 3 (all remaining domains) | All 13 domains ported | +6 months |
| M5 | Phase 4 | Legacy engine deleted | +6 months + 1 week |

Multiplier for ≥2 engineers in parallel during Phase 3: ~0.5–0.6× elapsed.

---

## 6. Risks & open questions

| # | Risk | Mitigation |
|---|---|---|
| R1 | Widget/minilang screens are tightly coupled to `GenericValue` field-by-name access. Full screen rewrite is out of scope. | Phase 3.4 ships a temporary `GenericValue` reflection shim over POJOs. Removed in Phase 4. Screen rewrites tracked separately. |
| R2 | View entities with `<calculated-field>`, dynamic-view-entity, or function aliases don't generate. | Coverage report flags them; hand-write per entity (~30–50 expected). |
| R3 | EECA / SECA hook surface isn't 1:1 with Spring AOP. | Phase 1 `EecaInterceptor` validated with a representative hook (e.g., Party audit) before broader use. |
| R4 | Multi-tenant Delegator (`DelegatorFactory.getDelegator(tenantId)`) has no Spring-native equivalent. | Design tenant-routing `DataSource` in Phase 1; pilot in `securityext`/`entityext` phase. |
| R5 | `EntityCrypto` field-level encryption is silently missing today. | Add `CryptoInterceptor` infra in Phase 1; gate any domain with encrypted fields (e.g., `UserLogin` password reset tokens) on its existence. |
| R6 | Cross-domain transactions across the new Spring services must still atomically wrap multi-DAO writes. | Service-level `@Transactional` + single `PlatformTransactionManager` in Phase 1; smoke test with a known multi-DAO write (`PartyService.createPerson` already inserts party + person). |
| R7 | Build invisibility: `settings.gradle` walks `component-load.xml`, which doesn't know about abacus. | Phase 1 step 7 adds explicit include. |
| R8 | 389 call-site sweep may surface dead code or undocumented behavior. | Per-domain checkpoint before deletion; commit small, reviewable PRs (≤500 LOC where feasible). |

**Open questions to revisit at M2:**
- Does any consumer still need OFBiz's `EntityListIterator` streaming semantics over large result sets? Map to `DataSet`/`Stream<T>` in abacus if so.
- Should generated Flyway migrations be the new DDL source of truth (and entitymodel XML deleted now), or kept as documentation until Phase 4?
- Plugin compatibility: the `plugins/` dir is loaded by `component-load.xml`. Any plugin using `GenericValue` blocks deletion of the legacy engine. Audit at M3.

---

## 7. Out of scope (explicitly)

- Frontend / JS / widget-screen rewrite into React or similar.
- Replacing the `service` engine (`LocalDispatcher`) with something other than Spring `@Service` — Spring is the replacement.
- DB engine change (PostgreSQL, etc.); the migration is JDBC-level only.
- Performance tuning beyond like-for-like.
- Security hardening beyond preserving existing behavior.
