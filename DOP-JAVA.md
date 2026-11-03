# Data-Oriented Programming in Java: a manual for the style

Based on Chris Kiehl's book *Data-Oriented Programming in Java* (Manning MEAP v11). This early-access copy has 9 of the 11 chapters. Ch. 10 ("Data Oriented Architecture") and ch. 11 ("Testing Data Oriented Programs") aren't in it yet.

The book targets Java 21 and says every idea also works on JDK 8 (ch. 1 §1.5). This README keeps the book's ideas and updates the code to Java 25.

## How to read this

- Everything here summarizes the book unless it's marked:
  - **Java 25:** an update that uses a feature from Java 22–25. It isn't in the book.
  - **Note:** a correction or an addition of mine. It isn't in the book either.
- The code is a cleaned-up version of the book's examples, not a verbatim copy. Several listings in this early-access copy have typos.
- Snippets omit imports. On Java 25, `import module java.base;` (JEP 511) covers every JDK type they use. Static imports such as `Comparator.comparing` are omitted too.
- Code outside a sealed interface refers to the interface's nested records by their simple names. That assumes an import of the nested types, such as `import com.example.checklist.StepStatus.*;`. The rules DSL in rule 17 assumes `import static com.example.rules.Rule.*;` and `import static com.example.rules.Attribute.*;`, because static interface methods aren't inherited.
- A body written as `{ ... }` is a sketch: the signature is the point.
- Every Java snippet was compiled with `javac --release 25 -Xlint:all`, with small stub types for the names a snippet doesn't define (`Step`, `User`, `Invoice`, `USD` and so on).

## Contents

0. [The whole idea in one paragraph](#0-the-whole-idea-in-one-paragraph)
1. [The core vocabulary](#1-the-core-vocabulary)
2. [Toolkit](#2-toolkit-the-book-targets-java-21)
3. [The rules](#3-the-rules)
4. [Smell checklist](#4-smell-checklist-signs-code-isnt-data-oriented-yet)
5. [Refactoring recipe for legacy code](#5-refactoring-recipe-for-legacy-code-ch-9)
6. [Pragmatism](#6-pragmatism-the-author-is-explicitly-not-a-purist)
7. [Practice path](#7-practice-path-to-reach-the-style)
8. [Chapter map](#8-chapter-map-for-rereading)
9. [What changes on Java 25](#9-what-changes-on-java-25)
- [Appendix A. OOP vs FP vs DOP: when each fits](#appendix-a-oop-vs-fp-vs-dop-when-each-fits)
- [Appendix B. What else the book teaches](#appendix-b-what-else-the-book-teaches)

---

## 0. The whole idea in one paragraph

Before asking "what does it do?", ask "what is it?" Work out exactly what your data means, then write that meaning into types so that invalid states can't be constructed. Once the data is modeled precisely, behavior becomes functions that take data in and return data out. Objects still exist, but they sit at the edges, where they manage resources and I/O, and they pass data around. The book borrows Fred Brooks's line from *The Mythical Man-Month*: "Representation is the essence of programming."

The three-step method (ch. 1 §1.4):

1. Figure out what the data really means. This usually means asking people and reading about the domain, not the code.
2. Express that meaning in code. Use types, constructors, and sum types.
3. Let the effects spread outward. Method signatures, pipelines and object boundaries follow from the data.

---

## 1. The core vocabulary

| Concept | Meaning | In Java |
| --- | --- | --- |
| Identity | Something that stays the same thing over time while its values change | objects, non-final variables, DB entities |
| Value | Immutable, defined only by equality, no identity (4, a date, a point) | `record`, final fields, `List.copyOf` |
| Data | A value given meaning in a domain (4 → "4 days elapsed") | domain records such as `Degrees` or `SampleId` |
| Product type (AND, ×) | Has field A and field B | `record` |
| Sum type (OR, +) | Is case A or case B or case C, and nothing else | `sealed interface` + records |
| Function | Deterministic mapping: same inputs give the same outputs | `static` methods (a convention; Java doesn't enforce it) |

Key insights:

- Mutability turns a value back into an identity. A value stored in a non-final field is no longer really a value.
- Change over time is modeled as a succession of values. `haveBirthday(person)` returns a new `Person`. It doesn't mutate the old one.
- Data can model more than nouns. It can also model decisions, lifecycle states, knowledge ("this invoice is past due"), and operations (rules).

---

## 2. Toolkit (the book targets Java 21)

| Tool | Use it for | Final since |
| --- | --- | --- |
| `record` | Every piece of data | Java 16 (JEP 395) |
| Compact constructor | Validating invariants and normalizing input, once, at creation | Java 16 (part of records) |
| `List.copyOf` / `Map.copyOf` | Unmodifiable, null-rejecting copies of the collections inside records. The copy is shallow: the elements have to be values too | Java 10 |
| `Objects.requireNonNull` | Rejecting nulls at construction time instead of trusting convention | Java 7 |
| `sealed interface` | "These options and no others" | Java 17 (JEP 409) |
| `switch` + record patterns | Exhaustive handling of a sum type with no `default` | Java 21 (JEP 441, JEP 440) |
| Generics as state tags | Moving lifecycle checks from runtime to compile time (`LateFee<Draft>`) | Java 5 |
| `static` methods | Marking a method as a deterministic function | always |
| `Optional`, `BinaryOperator`, `Comparator`, `TemporalAdjuster` | Built-in building blocks for this style | Java 8 |

On older JDKs (from ch. 2 §2.6 and ch. 4 §4.7, with the exact versions added):

- **Java 8–15** have no records. Use Lombok `@Value` or Immutables for value classes. `List.copyOf` arrived in Java 10; on 8 and 9 copy into `Collections.unmodifiableList(new ArrayList<>(list))`.
- **Java 8–16** have no sealed types. Nest every implementation inside the interface; that's sealing by convention. The compiler won't stop new implementations, and it can't check that a `switch` is exhaustive.
- **Java 17–20** have records and sealed types, but no pattern matching in `switch` and no record patterns. Use `instanceof` patterns (final in Java 16) and accept that exhaustiveness isn't checked.

**Note:** The book titles §2.6 "What to do if you're on a JDK older than 17", but records are already final in Java 16. Only sealed types need 17.

**Java 25:** The toolkit doesn't change. Java 22–25 add `_` for pattern variables you don't use (rule 6), statements before `this(...)` in record constructors (rule 1), and stream gatherers (rules 16 and 20). Section 9 lists them all.

---

## 3. The rules

### Rule 1: Kill ambiguous primitives, especially String

A `String` can hold effectively infinite values, and almost none of them are valid IDs. `String id` accepts "hello", "2024-05-04" and "172.16.24.105". `UUID id` says what an ID is, and the type itself rules out everything else (ch. 1).

Wrap numbers that carry meaning in domain types:

```java
record Degrees(double value) {
    Degrees {
        if (!(value >= 0.0 && value < 360.0))   // written this way so NaN is rejected too
            throw new IllegalArgumentException("angle must be in [0, 360): " + value);
    }
}

record NonNegativeInt(int value) {
    NonNegativeInt {
        if (value < 0) throw new IllegalArgumentException("negative: " + value);
    }
}
```

Prefer structure to regex-validated strings. A `SampleId` such as "HEAT-12" is really two pieces of data:

```java
enum CuringMethod { HEAT, AIR, UV }
record SampleId(CuringMethod method, NonNegativeInt number) {}
```

**Java 25:** Flexible constructor bodies (JEP 513, final in Java 25) let a non-canonical record constructor run statements before `this(...)`. The serialized form can now be parsed on the type itself, and the canonical constructor still guards the result:

```java
record SampleId(CuringMethod method, NonNegativeInt number) {
    /// Parses the serialized form, for example `HEAT-12`.
    SampleId(String raw) {
        String[] parts = raw.split("-", -1);
        if (parts.length != 2)
            throw new IllegalArgumentException("expected {method}-{number}: " + raw);
        this(CuringMethod.valueOf(parts[0]), new NonNegativeInt(Integer.parseInt(parts[1])));
    }
}
```

On Java 21 the statements before `this(...)` don't compile. Use a static factory such as `SampleId.parse(String)` instead. That's still a good choice when you'd rather return an `Optional` than throw. The `///` comment is a Markdown documentation comment (JEP 467, final in Java 23).

### Rule 2: Validate in the constructor, once

Immutable data has exactly one state. If it's valid when it's created, it stays valid. There's nothing to re-check anywhere else.

```java
record Rational(int num, int denom) {
    Rational {
        if (denom == 0) throw new IllegalArgumentException("denominator is 0");
        if (denom < 0) {                          // keep the sign in the numerator
            num = Math.negateExact(num);
            denom = Math.negateExact(denom);
        }
        int g = gcd(Math.absExact(num), denom);   // you can also normalize here
        num /= g;
        denom /= g;
    }

    private static int gcd(int a, int b) {
        while (b != 0) {
            int t = a % b;
            a = b;
            b = t;
        }
        return a;
    }
}
```

`new Rational(2, -4)` and `new Rational(-1, 2)` are now the same value, so `equals` works. The `*Exact` methods throw instead of overflowing on `Integer.MIN_VALUE`.

> Comments, Javadoc and good names help, but they can't enforce anything. Types and constructors can.

**Java 25:** Javadoc can be written in Markdown with `///` lines (JEP 467, final in Java 23). It's nicer to write, and it still enforces nothing.

### Rule 3: Make records truly immutable

Records are only shallowly immutable. Close the gaps:

```java
record Person(String name, List<String> friends) {
    Person {
        Objects.requireNonNull(name);
        friends = List.copyOf(friends);   // defensive copy, unmodifiable, rejects null elements
    }
}
```

The copy protects the list, not what's in it. A list of mutable objects (the book uses `Date`) is still mutable through its elements: "you're only as value-like as the least value-like thing you touch" (ch. 2). The book also suggests a wrapper type such as `ImmutableList<A>` when you want the field's type, not just the constructor, to say "unmodifiable".

### Rule 4: Watch the hidden AND

Every field in a record is implicitly ANDed with the others. If a field only makes sense sometimes, you've written an AND where you meant an OR.

Smell: nullable fields whose combinations carry meaning.

```java
// "if completedBy is set then... unless skipRationale is set, then..."
record Status(Step step, boolean isCompleted, User completedBy,
              Instant completedOn, String skipRationale) {}
```

Fix: one record per meaning, joined by a sealed interface.

```java
sealed interface StepStatus {
    record NotStarted(Step step) implements StepStatus {}
    record Completed(Step step, User by, Instant on) implements StepStatus {}
    record Skipped(Step step, User by, Instant on, String rationale) implements StepStatus {}
}
```

After the split, each record's constructor body is empty, because there are no illegal combinations left to guard against.

### Rule 5: Count your states

Types are sets of values. Product types multiply and sum types add.

- `record Beverage(Drink d, Temp t)` has 2 × 2 = 4 states. Is every one of them legal?
- Two booleans and a nullable `User` (the book's `Status` once skipping was added) give 2 × 2 × (users + 1) states. Most of them are probably nonsense.
- `StepStatus` = `NotStarted` + `Completed` + `Skipped`: three cases, each with only the fields it needs.

If the type allows more states than the domain does, the type is wrong. Defensive `if` checks are a warning sign that the code's meaning has drifted from the data's meaning.

### Rule 6: switch exhaustively, with no default

`instanceof` and pattern matching aren't an OO smell here. You're inspecting data, not sending messages to objects.

```java
static String describe(StepStatus s) {
    return switch (s) {
        case NotStarted _            -> "todo";
        case Completed(_, var by, _) -> "done by " + by;
        case Skipped sk              -> "skipped: " + sk.rationale();
    };  // add a 4th case and the compiler shows you every place to update
}
```

A `default` branch would hide that check: the new case would fall into it silently.

**Java 25:** `_` (JEP 456, final in Java 22) matches without declaring a variable, so a pattern only names what it uses. On Java 21, write `case NotStarted n` and `case Completed c -> "done by " + c.by()`.

### Rule 7: Turn runtime if/else into compile-time data

Separate deciding from acting. Return the decision as data and act on it somewhere else.

```java
// Before: void reschedule() { if (...) { setScheduledAt(...); setAttempts(n + 1); } else ... }

sealed interface RetryDecision {
    record RetryImmediately(LocalDateTime at, int attempts) implements RetryDecision {}
    record ReattemptLater(LocalDateTime at) implements RetryDecision {}
    record Abandon() implements RetryDecision {}
}

static RetryDecision reschedule(FailedTask task) { ... }   // a switch that returns one of the three
```

Compare the two signatures. `void reschedule()` tells you nothing. `RetryDecision reschedule(FailedTask)` tells you everything. Also compare `task.getScheduledAt() == null` with `task.status() instanceof Abandon`.

### Rule 8: Encode the lifecycle in the type

Use a generic parameter as a state tag so illegal behaviors won't compile:

```java
sealed interface Lifecycle {
    record Draft() implements Lifecycle {}
    record Billed(InvoiceId id) implements Lifecycle {}
    record Rejected(Reason reason) implements Lifecycle {}
    record InReview(ApprovalId id) implements Lifecycle {}
}

record LateFee<S extends Lifecycle>(S state, CustomerId customer, USD total, LocalDate dueDate) {}

static LateFee<Billed> markBilled(LateFee<Draft> fee, InvoiceId id) {   // only drafts can be billed
    return new LateFee<>(new Billed(id), fee.customer(), fee.total(), fee.dueDate());
}

void save(LateFee<? extends Lifecycle> fee) { ... }                      // doesn't care about the state
```

How much type safety to use is a design decision. The higher the stakes (billing money, for example), the more invariants you should move to compile time.

Leaving the type argument off (`void save(LateFee fee)`) still compiles, which lets you add type safety to legacy code gradually. **Note:** The book says the raw type "will produce a useful warning". With default flags javac is silent; it reports a `[rawtypes]` warning only with `-Xlint:rawtypes` or `-Xlint:all`.

**Java 25:** `markBilled` is a hand-written "wither", and it stays that way. Derived record creation (`fee with { ... }`, JEP 468) isn't in Java 25. Even as proposed, it returns the origin's record type, so a state change such as `Draft` to `Billed` would still need a method like this one.

### Rule 9: Design behavior as a pipeline of types, before writing any code

Write the feature as a story of data transformations and work above the code:

```text
List<Invoice>
  -> List<PastDue>
  -> LateFee<Draft>
  -> Billable | NeedsApproval | NotBillable          (a sum type: the decision)
  -> LateFee<Billed> | LateFee<Rejected> | LateFee<InReview>
```

Each arrow becomes one method. Even I/O steps such as `submitBill` are designed as "data in, new data out". The design is done when it reads like it was written for humans, not machines. Try several factorings, because the first one is just the first.

- It's fine to add new types. As many as you need. Thin wrappers like `record PastDue(Invoice invoice)` capture something you know.
- Don't let existing entities and services infect your design. Reuse can hurt: an `Invoice` entity built for other use cases will produce illegal states in yours.

### Rule 10: Separate how you get data from what you do with it

```java
// Bad: loads AND decides
LateFee<Draft> buildDraft() { ... }   // repo calls, LocalDate.now(), logic...

// Good: pure logic over data that was passed in
static LateFee<Draft> buildDraft(LocalDate today, EnrichedCustomer c, List<PastDue> invoices) { ... }

record InvoicingData(EnrichedCustomer customer, List<Invoice> invoices, LocalDate today, Rules rules) {}
```

Load everything up front into one record, such as `InvoicingData`. Hide messy service and repository calls behind a façade or data access layer that returns well-formed, meaningful data (for example `EnrichedCustomer`). Loading becomes atomic and the failure modes get simpler.

### Rule 11: Deterministic core, non-deterministic shell

- Anything that reads the clock, a DB, an API or randomness is non-deterministic. Push it to the edges.
- Mark functions `static`. Then `this.someApi` is out of scope, so determinism is enforced by friction.
- Pass in `today`. Never call `LocalDate.now()` inside logic.

```java
public void processLateFees() {
    loadInvoicingData().forEach(data -> {                                               // SHELL: I/O in
        var pastDue  = collectPastDue(data.customer(), data.today(), data.invoices());  // CORE
        var draft    = buildDraft(data.today(), data.customer(), pastDue);              // CORE
        var reviewed = assessDraft(data.rules(), draft);                                // CORE
        LateFee<? extends Lifecycle> result = switch (reviewed) {                       // SHELL: I/O out
            case Billable b      -> submitBill(b);
            case NeedsApproval a -> startApproval(a);
            case NotBillable nb  -> markNotBilled(nb.fee(), nb.reason());
        };
        save(result);
    });
}
```

Where the code lives is a convention, not a rule. The book puts the deterministic functions in a class named `Core`, next to `Service` and `Types`, inside the feature's package (`com.dop.invoicing.latefees`), and says this isn't gospel (ch. 6 §6.4).

"Basically deterministic" is good enough. Don't chase purity.

**Java 25:** Scoped values (JEP 506, final in Java 25) replace `ThreadLocal` for context that a whole call tree needs, such as the clock for one run. They belong in the shell. Reading a scoped value inside a function is a hidden input, just like `LocalDate.now()`, so the shell reads it and passes the plain value into the core:

```java
static final ScopedValue<Clock> CLOCK = ScopedValue.newInstance();

void runNightly() {                                   // shell entry point; a test binds Clock.fixed(...)
    ScopedValue.where(CLOCK, Clock.systemUTC()).run(this::processLateFees);
}

LocalDate today() {                                   // called from loadInvoicingData(), never from the core
    return LocalDate.now(CLOCK.get());
}
```

### Rule 12: Let the types write the implementation

A function's possible implementations are limited by its signature. Narrow inputs and outputs leave only a few ways to write it, and often the obvious one is the only one that compiles.

Functions are data: you can return them and pass them around.

```java
static TemporalAdjuster gracePeriod(CustomerRating rating) {
    return switch (rating) {
        case GOOD       -> d -> d.plus(60, ChronoUnit.DAYS);
        case ACCEPTABLE -> d -> d.plus(30, ChronoUnit.DAYS);
        case POOR       -> TemporalAdjusters.lastDayOfMonth();
    };
}

static boolean isPastDue(Invoice inv, CustomerRating r, LocalDate evalDate) {
    return evalDate.isAfter(inv.dueDate().with(gracePeriod(r)));   // reads like the requirement
}
```

A function behaves like a lookup table, but be careful swapping a `switch` for a `Map`. You lose exhaustiveness and get the question "what's the `getOrDefault` value?"

### Rule 13: Name the "else"

When logic says "not those other things", give that case a name:

```java
enum Assessment { BELOW_MINIMUM, WITHIN_RANGE, ABOVE_MAXIMUM }

static Assessment assessTotal(USD total, Rules rules) {
    if (total.value().compareTo(rules.minimumFeeThreshold()) < 0) return Assessment.BELOW_MINIMUM;
    if (total.value().compareTo(rules.maximumFeeThreshold()) > 0) return Assessment.ABOVE_MAXIMUM;
    return Assessment.WITHIN_RANGE;
}
```

Then switch over it. Three cases, explicitly.

### Rule 14: Treat legacy entities as DTOs at the boundary

Don't rewrite the ORM. Map your data into the existing entity at the edge with a static function and save it as usual:

```java
static Invoice toInvoice(LateFee<? extends Lifecycle> fee) {
    Invoice invoice = new Invoice();
    invoice.setInvoiceId(switch (fee.state()) {
        case Billed(var id)                  -> id.value();
        case Draft _, Rejected _, InReview _ -> Invoice.tempId();   // no real ID until billed
    });
    // ...the rest is boring field copying
    return invoice;
}
```

The exchange points between your feature and the rest of the system are always the hardest part, so give them extra thought.

**Java 25:** The book's version ends that `switch` with `default -> Invoice.tempId()`, which gives up the check from rule 6. Since Java 22 (JEP 456) one `case` can list several patterns as long as none of them names a variable, so the remaining states can be spelled out with `_`. A fifth lifecycle state then breaks this `switch` instead of quietly getting a temporary ID.

### Rule 15: Define correctness as "for all" properties

Turn English requirements into laws: ∀ states, X holds. Look past the verbs (the actions) to the nouns (what must be true afterwards).

- Example: "move unavailable items to Saved" becomes ∀ items in active: desired ≤ available.
- Useful algebraic properties:
  - Associativity: f(a, f(b, c)) == f(f(a, b), c). This is what makes parallel `reduce` safe. Floating-point `+` is not associative.
  - Commutativity: f(a, b) == f(b, a).
  - Ordering (the `Comparator` laws): reflexive, antisymmetric, transitive.
  - Monotonicity: a1 ≤ b1 ∧ a2 ≤ b2 ⇒ f(a1, a2) ≤ f(b1, b2).
- Test them. When the state space is small, test it exhaustively:

```java
for (var a : allStates()) for (var b : allStates()) for (var c : allStates())
    assertEquals(add(a, add(b, c)), add(add(a, b), c));
```

- For large state spaces, the book points to property-based testing libraries, says the Java ones aren't very good, and leaves them for the testing chapter, which isn't in this copy. **Note:** jqwik is one option to look at; that's my suggestion, not the book's.
- Don't force a property onto the data. Clear data meaning beats an elegant algebra.

### Rule 16: Prefer standard algebraic building blocks

Most loops are "apply a binary operation over a list". Say that directly:

```java
BinaryOperator<Policy> mostFavorable =
    BinaryOperator.maxBy(comparing(Policies::impact).thenComparing(Policy::name));
```

`Optional` is a parallel universe. `Optional.of` moves values into it, and `lift` moves functions into it:

```java
static <A> BinaryOperator<Optional<A>> lift(BinaryOperator<A> f) {
    return (oa, ob) -> oa.flatMap(a -> ob.map(b -> f.apply(a, b)));
}
```

**Note:** `lift` returns empty when either side is empty. The ch. 7 merge has to replace missing values with concrete ones, so there the present side should win. The book's listing 7.59 (`withOptional`) aims at that but returns its first argument in both branches. This version does what the text describes, and it stays associative when `f` is:

```java
static <A> BinaryOperator<Optional<A>> withOptional(BinaryOperator<A> f) {
    return (oa, ob) -> lift(f).apply(oa, ob).or(() -> oa).or(() -> ob);
}
```

Skip the `Optional` holy war. Pick the usage that puts the reader's attention in the right place.

**Java 25:** Stream gatherers (JEP 485, final in Java 24) add the stages `reduce` can't express. Keep `reduce` for associative operations, because it can run in parallel. `Gatherers.scan` emits every intermediate result, and `Gatherers.fold` is the sequential reduction for an operation that isn't associative:

```java
List<USD> runningTotal = amounts.stream()
    .gather(Gatherers.scan(USD::zero, USD::add))   // one total per element, in order
    .toList();
```

### Rule 17: Business rules as data plus an interpreter

When rules multiply, stop writing `if` chains and describe the rules as data:

```java
enum Attribute { COUNTRY, REGION, SEGMENT }

sealed interface Rule {
    record Equals(Attribute attr, String value) implements Rule {}
    record And(Rule a, Rule b) implements Rule {}
    record Or(Rule a, Rule b) implements Rule {}
    record Not(Rule rule) implements Rule {}

    static Rule eq(Attribute a, String v) { return new Equals(a, v); }
    static Rule not(Rule r)               { return new Not(r); }
    default Rule and(Rule o)              { return new And(this, o); }
    default Rule or(Rule o)               { return new Or(this, o); }
}
```

```java
Rule rule = eq(REGION, "EMEA").and(eq(SEGMENT, "Public")).or(not(eq(REGION, "LATAM")));
```

```java
static boolean eval(Rule r, Account acct) {
    return switch (r) {
        case Equals(var attr, var v) -> get(acct, attr).equals(v);
        case And(var a, var b)       -> eval(a, acct) && eval(b, acct);
        case Or(var a, var b)        -> eval(a, acct) || eval(b, acct);
        case Not(var x)              -> !eval(x, acct);
    };
}

static String get(Account acct, Attribute attr) {   // from the attribute's name to the account's value
    return switch (attr) {
        case COUNTRY -> acct.country().name();
        case REGION  -> acct.region().name();
        case SEGMENT -> acct.segment().name();
    };
}
```

`Account` is a record of enums (`region`, `country`, `segment`), as in ch. 8.

What you gain:

- It decouples what from how and when.
- Rules can be stored, serialized, diffed and sent like any other data.
- Several interpreters can run over the same data: `eval`, `explain` (a `Result(matched, expected, found)` that says why a rule matched or didn't), Graphviz diagrams, generated docs.
- Composition comes free from the algebra (`And`/`Or`/`Not`).
- Add type safety later if needed. The book's version is a record `Attr<A>(Attribute attribute, Function<Account, A> getter)`, because enums can't be generic, plus `Equals<A>(Attr<A> field, A value)` and `GreaterThan` on `Comparable` values.

Small, problem-specific interpreters are fine. In this style, the line between an interpreter, a DSL and "just code" is blurry.

### Rule 18: Validation produces evidence, not error lists

`Set<Error> validate(Object)` only tells you "no errors found", and that isn't the same as "valid". It leads to shotgun parsing: decisions made on half-validated objects all over the code.

Instead, a data type is a demand for proof. Validating means constructing that proof:

```java
record ProductSeller(String productId, String sellerId) {
    ProductSeller {
        Objects.requireNonNull(productId);
        Objects.requireNonNull(sellerId);
    }
}

record Item(ProductSeller productSeller, long desiredQty, long availableQty) {   // details and prices left out
    Item withDesiredQty(long qty) { return new Item(productSeller, qty, availableQty); }
    static Item mergeQty(Item a, Item b) { return a.withDesiredQty(a.desiredQty() + b.desiredQty()); }
}

record ValidCart(Map<ProductSeller, Item> active, Map<ProductSeller, Item> saved) {
    ValidCart {
        active = Map.copyOf(active);   // copy first, so the checks run on what gets stored
        saved  = Map.copyOf(saved);
        for (Item i : active.values())
            if (i.desiredQty() <= 0 || i.desiredQty() > i.availableQty())
                throw new IllegalArgumentException("invalid active item: " + i);
        for (Item i : saved.values())
            if (i.desiredQty() <= 0)
                throw new IllegalArgumentException("invalid saved item: " + i);
    }
}

public void checkout(ValidCart cart) { ... }   // you can't call this without proof
```

- Draw boundaries with data, even halfway through a messy legacy method: `ValidCart valid = new ValidCart(...);` Nothing below that line runs on bad state.
- Model the in-between states too: `ValidCart` → refresh → `UnbalancedCart` → balance → `ValidCart`.
- "Valid" covers preconditions (the state of the system), not just the input object. A `ValidAddItemsRequest` can carry the loaded cart along with the request.
- The theory behind this (Curry–Howard): types are claims, and instances are proofs.

**Java 25:** `withDesiredQty` is another hand-written wither; see rule 8.

### Rule 19: Mutation is fine if it's local and builds data

```java
record UnbalancedCart(Map<ProductSeller, Item> active, Map<ProductSeller, Item> saved) {}   // no invariants yet

static ValidCart balance(UnbalancedCart cart) {
    Map<ProductSeller, Item> active = new HashMap<>();                 // mutable, but never escapes
    Map<ProductSeller, Item> saved  = new HashMap<>(cart.saved());
    for (var e : cart.active().entrySet()) {
        Item item = e.getValue();
        if (item.availableQty() <= 0) {                                // nothing in stock: save it all
            saved.merge(e.getKey(), item, Item::mergeQty);
        } else if (item.desiredQty() > item.availableQty()) {          // keep what's in stock, save the rest
            active.put(e.getKey(), item.withDesiredQty(item.availableQty()));
            saved.merge(e.getKey(), item.withDesiredQty(item.desiredQty() - item.availableQty()), Item::mergeQty);
        } else {
            active.put(e.getKey(), item);
        }
    }
    return new ValidCart(active, saved);                               // immutable data comes out
}
```

Don't cargo-cult. Mutation isn't the enemy in itself. Say concretely how a given mutation causes a problem before removing it.

**Note:** The book's listing 9.47 starts `saved` empty, which drops the items already saved, and computes the spill-over as `available - desired`, which is negative. The version above keeps the saved items and saves `desired - available`.

### Rule 20: Write the utilities your language is missing

When code is clumsy, say "ah, what you mean is…" and write that as a utility: `mapValues`, `intersect` (and a right-biased version), `union`, and difference (`except`) on Maps:

```java
static <K, V1, V2> Map<K, V2> mapValues(Map<K, V1> m, Function<? super V1, ? extends V2> f) {
    Map<K, V2> out = new HashMap<>();
    m.forEach((k, v) -> out.put(k, f.apply(v)));
    return Map.copyOf(out);
}

static <K, V> Map<K, V> intersect(BinaryOperator<V> op, Map<K, V> xs, Map<K, V> ys) {
    Map<K, V> out = new HashMap<>();
    xs.forEach((k, x) -> { if (ys.containsKey(k)) out.put(k, op.apply(x, ys.get(k))); });
    return Map.copyOf(out);
}

static <K, V> Map<K, V> union(BinaryOperator<V> op, Map<K, V> xs, Map<K, V> ys) {
    Map<K, V> out = new HashMap<>(xs);
    ys.forEach((k, y) -> out.merge(k, y, op));
    return Map.copyOf(out);
}

static <K, V> Map<K, V> except(Map<K, V> xs, Map<K, V> ys) {   // the keys of xs that aren't in ys
    Map<K, V> out = new HashMap<>(xs);
    out.keySet().removeAll(ys.keySet());
    return Map.copyOf(out);
}

// Right-biased versions: on a collision, the value on the right wins
static <K, V> Map<K, V> intersect(Map<K, V> xs, Map<K, V> ys) { return intersect((_, right) -> right, xs, ys); }
static <K, V> Map<K, V> union(Map<K, V> xs, Map<K, V> ys)     { return union((_, right) -> right, xs, ys); }
```

Good utilities change how you think about a problem. Adding items to a cart (requirements 3.1 to 3.3 in ch. 9) turns into set operations, with no loops and no `if`s:

```java
static UnbalancedCart addItems(UnbalancedCart cart, Map<ProductSeller, Item> incoming) {
    var promote = intersect(incoming, cart.saved());   // 3.2: saved items added again (right bias keeps the saved Item)
    var active  = union(Item::mergeQty, union(Item::mergeQty, cart.active(), incoming), promote);   // 3.1, 3.3, and 3.2's sum
    var saved   = except(cart.saved(), incoming);      // 3.2: they leave Saved
    return new UnbalancedCart(active, saved);
}
```

Balancing the cart afterwards stays a plain loop (rule 19): the book tried set operations there too and found them clumsier. Performance is rarely a problem on the JVM, and when you're unsure, benchmark.

**Note:** The book's listing 9.44 (`except`) filters with `ys.containsKey`, which keeps the shared keys instead of dropping them.

**Java 25:** The `(_, right) -> right` lambdas use an unnamed parameter (JEP 456, final in Java 22). A missing stream stage is now a utility you can write too: a `Gatherer` (JEP 485, final in Java 24) plugs into `Stream.gather`. `Gatherers` already ships `fold`, `scan`, `windowFixed`, `windowSliding` (consecutive pairs are `windowSliding(2)`) and `mapConcurrent`. A hand-written one:

```java
static <T, K> Gatherer<T, ?, T> distinctBy(Function<? super T, ? extends K> key) {   // first element per key
    return Gatherer.ofSequential(
        HashSet<K>::new,
        Gatherer.Integrator.ofGreedy((seen, element, downstream) ->
            !seen.add(key.apply(element)) || downstream.push(element)));
}
```

### Rule 21: Ask OO questions last

"Who owns this?" and "which class is responsible?" are good questions, but only after the data problems are solved. Asked too early, indirection hides problems instead of fixing them.

---

## 4. Smell checklist (signs code isn't data-oriented yet)

- [ ] `String id`, `String status`, `double percent` (stringly or primitively typed domain)
- [ ] A null that means something ("null `scheduledAt` means abandoned")
- [ ] Boolean flags plus fields that are only set in some states
- [ ] Comments explaining what field combinations mean
- [ ] `if` checks guarding against states the type shouldn't allow
- [ ] `void doThing()` with no parameters (all hidden state)
- [ ] Sequential coupling: methods that must be called in a specific order
- [ ] Partial construction: objects passed around before they're fully valid
- [ ] Hidden knowledge: a field that's secretly nullable, discovered three layers down
- [ ] `LocalDate.now()` or repository calls mixed into business logic
- [ ] Methods that both load data and decide things
- [ ] A `switch` with `default` over something that should be a closed set
- [ ] Reusing an entity built for another feature and patching around its fields
- [ ] Mutating a reference three objects away

---

## 5. Refactoring recipe for legacy code (ch. 9)

1. Analyze first. List the concrete problems: messy modeling, uncontrolled mutation, sequential coupling, hidden knowledge, partial construction, shotgun validation.
2. Write the requirements as "for all" invariants. Find the nouns behind the verbs.
3. Put the invariants into records. Constructors guard them, and nulls are forbidden explicitly.
4. Plant a data boundary in the legacy flow. Construct a `ValidX` mid-method so everything after it is protected.
5. Turn validation into building evidence types, and model the intermediate states.
6. Separate loading from logic.
7. Keep mutation local and use it to produce immutable data.
8. Extract utilities for repeated awkwardness.
9. Now ask the OO questions about boundaries and ownership.

Good refactors fix problems. Great ones make them impossible to reintroduce.

---

## 6. Pragmatism (the author is explicitly not a purist)

- DOP complements OOP and FP; it doesn't replace them. Objects are still the best tool for resources (pools, sockets, lifecycles) and for drawing system boundaries.
- There's a sweet spot for typing. Too many types is obnoxious; too few is dangerous.
- Code clarity trumps everything else.
- When types disagree with existing code, the friction is a feature. It tells you about wrong assumptions. Not every disagreement is an emergency, though; often the right call is to annotate it and move on.
- Design is iterative and messy. Resets are normal.
- Extra types are cheap on the JVM. When in doubt, benchmark.
- Adding formal algebraic interfaces (`Monoid` and similar) to your codebase is usually unnecessary. Think algebraically and code plainly.

**Java 25:** Compact object headers (JEP 519) became a product feature in Java 25. They're off by default; `-XX:+UseCompactObjectHeaders` turns them on and shrinks every object's header, which makes many small records cheaper still.

---

## 7. Practice path to reach the style

| Stage | Practice | Done when |
| --- | --- | --- |
| 1. Values | Turn DTOs and data classes into records with `List.copyOf` and `requireNonNull` | No setters on data |
| 2. Domain types | Replace every meaningful `String`/`int`/`double` with a validated record or enum | Constructors hold the validation |
| 3. Sum types | Find nullable or flag-driven fields and split them into a sealed interface | Exhaustive switches without `default` |
| 4. Decisions as data | Change `void` methods that mutate into methods that return a decision | Signatures explain behavior |
| 5. Core/shell | Make logic `static` and pass in time and loaded data | Unit tests need no mocks |
| 6. Pipelines | Sketch each new feature as `A -> B -> (C \| D)` before coding | The type story reads like the requirements |
| 7. Lifecycle types | Use generic state tags where the stakes are high | Illegal transitions don't compile |
| 8. Properties | Write "∀" invariants and test them (exhaustively or property-based) | You can state what "correct" means |
| 9. Rules as data | Turn one `if` tangle into a sealed algebra plus `eval` and `explain` | Two interpreters over one dataset |
| 10. Legacy | Plant one `ValidX` boundary in an old flow | Bugs stop recurring in that area |

**Java 25:** For stages 1 to 3, a compact source file (section 9) is the quickest place to try a model: records and a `void main()` in one file, run with `java Notes.java`.

Questions to ask on every PR:

1. What is this data? Could a newcomer tell from the type alone?
2. How many states does this type allow, and how many does the domain allow?
3. Is there an AND that should be an OR?
4. Is this function deterministic? If not, can the non-deterministic part move to the shell?
5. Does the signature tell the story, or do I have to read the body?
6. Where is the evidence that this input is valid?

---

## 8. Chapter map (for rereading)

| Ch | Title | Topic | Reread when… |
| --- | --- | --- | --- |
| 1 | Data Oriented Programming | Data as data; the `String id` and `ScheduledTask` examples | you need the "why" |
| 2 | Data, Identity, and Values | Identity vs values; records, compact constructors, immutability pitfalls; Lombok and Immutables on old JDKs | writing records |
| 3 | Data and Meaning | Meaning: types as sets; domain types; beware the String | modeling fields |
| 4 | Representation is the Essence of Programming | Checklist example: AND/OR, sealed interfaces, algebraic data types | modeling states |
| 5 | Modelling Domain Behaviors | Late-fee feature: pipelines, decisions, lifecycle generics, data loading | designing a feature |
| 6 | Implementing the Domain Model | Implementation: determinism, `static`, core/shell, `Optional`, entities as DTOs | writing the code |
| 7 | Guiding the design with properties | Algebraic properties (associativity, ordering, monotonicity), correctness, `Optional` lifting | defining "correct" and testing |
| 8 | Business Rules as Data | Business rules as data, DSLs, interpreters | rule engines or config logic |
| 9 | Refactoring towards Data | Refactoring legacy toward data; validation as evidence; utilities | working in old code |
| 10 | Data Oriented Architecture | Not in MEAP v11 | |
| 11 | Testing Data Oriented Programs | Not in MEAP v11 | |

---

## 9. What changes on Java 25

The book targets Java 21. These features became final (not preview) in Java 22 to 25. The release is where each one became final; each was checked against its JEP page and the JDK 22–25 release pages on openjdk.org.

| Feature | JEP | Final in | Where it's used here |
| --- | --- | --- | --- |
| Unnamed variables and patterns (`_`) | 456 | Java 22 | Rule 6 (`case NotStarted _`), rule 14 (several patterns in one `case`), rule 20 (`(_, right) -> right`) |
| Markdown documentation comments (`///`) | 467 | Java 23 | Rules 1 and 2 |
| Stream gatherers (`Stream.gather`, `Gatherers`) | 485 | Java 24 | Rule 16 (`scan`), rule 20 (a custom `Gatherer`) |
| Flexible constructor bodies | 513 | Java 25 | Rule 1 (`SampleId(String)` validates and parses before `this(...)`) |
| Scoped values | 506 | Java 25 | Rule 11 (shell only; never read one inside the core) |
| Module import declarations (`import module java.base;`) | 511 | Java 25 | "How to read this": one import covers the JDK types in every snippet |
| Compact source files and instance main methods | 512 | Java 25 | Practice path: a model plus `void main()` in one file |
| Compact object headers (JVM option, off by default) | 519 | Java 25 | Section 6: small records get cheaper |

Two details worth knowing:

- In a record, flexible constructor bodies only help non-canonical constructors. A canonical (or compact) constructor still can't call `this(...)`, so validation of the components stays where it was.
- A module import is the least specific kind of import. If two imported modules export the same simple name (`java.base` and `java.sql` both have a `Date`), using that name is an error until a single-type import such as `import java.sql.Date;` picks one.

A compact source file with an instance `main`. It imports `java.base` automatically and runs with `java Notes.java`, with no build step:

```java
record Degrees(double value) {
    Degrees {
        if (!(value >= 0.0 && value < 360.0))
            throw new IllegalArgumentException("angle must be in [0, 360): " + value);
    }
}

void main() {
    IO.println(new Degrees(17.4));               // java.lang.IO, new in Java 25
    IO.println(List.of(new Degrees(90), new Degrees(180)));
}
```

The explicit form of the import, for ordinary source files:

```java
import module java.base;   // java.util, java.util.function, java.util.stream, java.time, java.math, ...
```

Since Java 22, `java Main.java` can also launch a program spread over several source files (JEP 458).

Still preview in Java 25. These need `--enable-preview`, aren't used in any example here, and weren't compiled:

| Feature | JEP | Status in Java 25 | Why it would matter here |
| --- | --- | --- | --- |
| Primitive types in patterns, `instanceof` and `switch` | 507 | Third preview | `switch` and patterns over `long`, `double` and `boolean` values, for example in validation |
| Structured concurrency | 505 | Fifth preview | Fanning out the shell's service calls (`loadInvoicingData`) and failing them together |
| Stable values | 502 | Preview | Lazily initialized immutable constants |

Not in Java 25 at all:

- **Derived record creation** ("withers", `fee with { ... }`, JEP 468) is still a candidate JEP with no target release. Write `withX` methods by hand (rules 8 and 18).
- **Value classes and objects** (JEP 401), which the book mentions in ch. 2 as a future way to drop object identity from value types. On openjdk.org it's integrated as a preview feature for JDK 28.

---

## Appendix A. OOP vs FP vs DOP: when each fits

This appendix is general commentary, not a summary of the book. Where it says what the book holds, it names the chapter.

### Each style starts from a different question

| Style | First question | Basic unit |
| --- | --- | --- |
| OOP | "Who is responsible for this, and what messages do they exchange?" | An object: state and behavior together, hidden behind an interface |
| FP | "How do I compose transformations without side effects?" | A pure function: inputs map to outputs |
| DOP | "What is this, exactly, and how do I make invalid states impossible?" | Immutable data whose meaning is captured in its types |

### How they differ in practice

| | OOP | FP | DOP (Kiehl's sense) |
| --- | --- | --- | --- |
| State | Mutable, encapsulated inside objects | Immutable, passed explicitly | Immutable values; change is a series of new values |
| Where behavior lives | Methods on the object that owns the data | Free functions | Static functions separate from the data |
| Data visibility | Hidden behind getters and an interface | Open | Open on purpose: the data is the interface |
| Variation | Subclasses and interfaces (polymorphism) | Higher-order functions, sum types | Sealed types + exhaustive switch |
| Design effort goes into | Boundaries, responsibilities, collaboration | Composition, purity, abstractions | Representation, meaning, state space |
| Typical failure | Hidden state with implicit meaning; mutation tangles | Abstraction-heavy code that's hard for the team to read | Over-typing; leaking internals across a module boundary |

### The trade-off that decides most cases

This is the expression problem: which kind of change is cheap?

- OOP: adding new kinds of things is easy. Add a `PdfExporter` class and nothing else changes. Adding a new operation is expensive, because you touch every class.
- DOP and FP with sum types: adding new operations is easy. Write one more function with a `switch`. Adding a new case to a sealed type is expensive, but the compiler lists every place you need to update.

So ask: is the set of variants open or closed?

- Open, and other people extend it (plugins, drivers, UI widgets, payment providers): use OOP interfaces.
- Closed, and the domain fixes it (order states, decisions, rule types, message kinds): use sealed types (DOP). The book makes the same point in ch. 4 §4.4: openness is a property to weigh, not a universal good.

### When each one fits best

OOP: things with lifecycles, resources, and boundaries

- Managing runtime resources: connection pools, sockets, threads, caches, transactions
- Module and service boundaries ("past this line I don't care how it works")
- Extension points: plugin systems, frameworks, strategies where third parties add implementations
- Simulations where entities really do have identity and evolving state (a game entity, a UI component tree)

FP: transformation and composition

- Data pipelines, stream processing, ETL
- Concurrency and parallelism (immutability plus associative operations make parallel `reduce` safe)
- Compilers, parsers, interpreters
- Logic you want to test without mocks

DOP: domain modeling and business logic

- Business domains full of states, rules and edge cases: billing, orders, workflows, approvals, compliance
- Anywhere bugs come from "this field is null, which means…" or "you have to call these in order"
- Request validation at system edges (validated types as proof)
- Configurable rules (rules as data plus an interpreter)
- Messages between services, events, API payloads: they're data already

### They aren't rivals

The book's central position is that DOP complements the other two (ch. 1). In a well-built Java app all three show up, each in its own layer, from the outside in:

| Layer | Made of | Job |
| --- | --- | --- |
| Objects (the shell, outermost) | Controllers, services, repositories, API clients | Own resources, enforce boundaries, do I/O |
| Data (the model) | Records + sealed types | Say what things ARE |
| Functions (the core, innermost) | Static, deterministic methods | Say what happens to the data |

In short: objects at the edges, data in the middle, functions transforming it. The late-fee service in the book (ch. 5 and 6) has exactly this shape. It's a class holding repositories and APIs (OOP), it passes records and sealed types around (DOP), and its logic is static deterministic methods (FP).

DOP borrows a lot from FP (immutability, pure functions, algebraic data types). The difference is emphasis. FP puts functions and composition at the center. DOP puts the data and its meaning at the center and treats FP techniques as tools, without chasing purity ("basically deterministic is good enough", ch. 6).

### Quick decision guide

| If you're writing… | Lean toward |
| --- | --- |
| A REST controller, repository, or HTTP client | OOP |
| A plugin interface others will implement | OOP |
| Entity/status modeling, business rules, validation | DOP |
| A calculation such as pricing, fees, scoring, or eligibility | DOP data + FP functions |
| A transformation pipeline or batch job | FP |
| Concurrent or parallel aggregation | FP |
| A rules engine or configurable policies | DOP (rules as data + interpreter) |

### The same name means different things

"Data-oriented" has three common meanings, and they're often confused:

1. Kiehl's DOP (this book): model meaning precisely with types. The goal is correctness and clarity.
2. Data-Oriented Design (game engines, Mike Acton, ECS): lay data out for the CPU cache (structs of arrays, hot/cold splitting). The goal is performance, and it has almost nothing to do with the book.
3. Sharvit's DOP (*Data-Oriented Programming*, Manning, Clojure-influenced): represent data with generic maps and schemas instead of specific types. It overlaps on "separate code from data and keep data immutable", but it's nearly the opposite of Kiehl's heavy use of the type system.

The book itself notes the other flavors in ch. 1 §1.7: some chase performance from the hardware, others give up static typing. If you search for "data-oriented" and find articles about cache lines and ECS, that's meaning 2, not this book.

---

## Appendix B. What else the book teaches

Records, sealed interfaces, exhaustive switch and pattern matching are only the syntax. Most of the book is about the techniques that use them (sections 3 to 5). These are the other Java features and patterns it uses.

**Note:** The original version of this table stopped after the `Optional` row. The rows from "Switch expressions" on were added from the book text.

| Feature | What he uses it for | Ch. |
| --- | --- | --- |
| Compact constructors | Validating invariants and normalizing input (for example, reducing a `Rational` by its GCD) | 2, 3 |
| `List.copyOf` / `Map.copyOf`, `requireNonNull` | Fixing records' shallow immutability and forbidding nulls at construction | 2, 9 |
| Record deconstruction patterns | `case Billed(var id) -> ...`, pulling fields straight out in the case. **Java 25:** `_` for the components you don't need | 6, 8 |
| Guarded patterns (`when`) | `case FailedTask t when canRetry(t) -> ...` | 1 |
| Generics as state tags | `LateFee<Draft>` vs `LateFee<Billed>`, so illegal transitions don't compile | 5 |
| Wildcards and raw types | `LateFee<? extends Lifecycle>` for "any state". The raw `LateFee` still compiles, and javac flags it under `-Xlint:rawtypes`, which lets you add type safety to legacy code gradually | 5 |
| `static` methods | Marking a method as a deterministic function; instance fields such as APIs are out of reach | 6 |
| `withX(...)` methods | Modeling change as a new value (`item.withDesiredQty(n)`, `markBilled`). **Java 25:** still hand-written; JEP 468 isn't in Java 25 | 6, 9 |
| Default methods on sealed interfaces | Fluent APIs: `rule.and(other).or(...)` | 8 |
| Static factory methods | `eq(REGION, "EMEA")` instead of `new Equals(...)`; small DSLs | 8 |
| Local records | Records defined inside a method for temporary groupings | 2 |
| Nesting types in an interface | Putting a whole model in one place: `interface CardGame { enum Suit…; record Card…; }`. On old JDKs this also works as "sealing by convention" | 2, 4 |
| Enums | Closed sets of cases that carry no data (`Assessment`, `CuringMethod`) | 3, 6 |
| JDK functional types | `Function`, `BinaryOperator`, `UnaryOperator`, `BinaryOperator.maxBy`, `Comparator.comparing().thenComparing()`, `TemporalAdjuster` (functions as data) | 6, 7 |
| `Optional` | `map`/`flatMap`, plus writing a `lift` that moves functions into the `Optional` world | 6, 7 |
| Switch expressions (arrow form, `yield`) | Returning a decision as a value instead of assigning fields in each branch | 1, 6, 8 |
| Unmodifiable collection factories | `List.of`, `Set.of`, `Map.of` create collections as values; `Arrays.asList` is a trap (fixed size, but elements can be reassigned) | 2 |
| `final` | A statement about meaning ("this is a value"). The book drops it by convention once the team assumes immutability | 2 |
| Wrapper records | `ImmutableList<A>`, `NonEmptyList<A>`, `PastDue(Invoice)`: a type that says what you know | 2, 5, 7 |
| Streams and collectors | `groupingBy`, `mapping`, `reducing`, `toMap`, `reduce`; `parallel()` once the operation is associative | 3, 5, 7, 9 |
| Typed attribute keys | `Attr<A>(Attribute, Function<Account, A>)`: makes a rule's attribute and value types agree. Enums can't be generic, so a record carries the enum plus a getter as evidence | 8 |
| Bounded generic helper methods | `<A extends Comparable<A>>` to recover the type that a wildcard pattern (`GreaterThan<?>`) hides | 8 |
| Text blocks | Generating the Graphviz file for a rule | 8 |
| Annotation processors | Lombok `@Value` and Immutables for value classes on JDKs without records | 2 |
| Façade over the data access layer | A "smart" repository that vends well-formed data (`EnrichedCustomer`) and hides service boundaries | 5 |
