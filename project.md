Summary

Our Clinical Decision Modeling (CDM) workbench, built on the DMN standard, offers clear advantages over other knowledge modeling languages. However, its current implementation with JetBrains MPS and Dclare introduces several challenges. MPS is a heavyweight tool with a steep learning curve, primarily designed for software engineering rather than modeling. Its projectional editing system is also difficult to integrate with modern technologies such as Large Language Models (LLMs). Dclare, while instrumental in CDM's development, faces performance and consistency issues that could become bottlenecks. Additionally, MPS and Dclare do not integrate seamlessly.

To address these limitations, we are developing Nelumbo: a lightweight, high-performance, and reliable platform designed to replace both MPS and Dclare. Nelumbo will enable the creation of declarative modeling languages using a logical programming paradigm, ensuring consistency, scalability, and broader execution scenarios. CDM will be reimplemented on Nelumbo, with full backward compatibility to ensure a seamless transition for our customers.


Addressing Dclare's Limitations

Dclare is not a language but an engine, offering flexibility for Java integration at the expense of transparency. Because the engine cannot fully interpret the intent of a rule, it must execute the rule to determine its read/write effects, which limits scalability. While incremental reactive update semantics are consistent, they do not scale well. The on-demand functional execution mode offers scalability but introduces consistency issues, especially with recursive functions and bidirectional relations.

Nelumbo will be both a language and an engine based on the logical programming paradigm. Early experiments indicate that this approach can resolve the scalability and consistency issues present in Dclare.


Addressing Shortcomings of MPS

JetBrains MPS, while powerful for language engineering, has several limitations in the context of knowledge modeling and modern integration:

* Heavyweight and complex: MPS is a large, complex platform primarily designed for language engineering and software development, making it less suitable for lightweight modeling tasks.
* Steep learning curve: The projectional editing paradigm and overall architecture require significant time and expertise to master, which can be a barrier for new users or domain experts.
* Limited collaboration: MPS's projectional editor is not as intuitive or collaborative as traditional text-based editors, making distributed or team-based modeling more challenging.
* Integration challenges: Integrating MPS with modern tools and technologies, such as LLMs, web-based editors, or cloud-based workflows, is difficult due to its architecture and reliance on a custom editor.
* Performance overhead: The platform can be resource-intensive, leading to slower performance, especially for large models or when running on less powerful hardware.
* Poor web and IDE support: MPS is primarily a desktop application and does not natively support browser-based editing or seamless integration with popular IDEs outside the JetBrains ecosystem.

Nelumbo will be a meta-language capable of defining custom syntaxes and semantics for any (textual) declarative language. As a language development tool, it will offer full support for textual syntaxes that can be integrated into existing IDEs. Nelumbo will not be an IDE itself, but will integrate with all major IDEs via the Language Server Protocol (LSP), providing a modern, flexible alternative to MPS for language development and modeling.


Requirements for Nelumbo

Nelumbo is envisioned as a next-generation platform for declarative modeling and language engineering. Its requirements are structured into four key categories:

1. Language Definition and Meta-Modeling
   * Provide a meta-language for defining new declarative languages, including both syntax and semantics.
   * Support modular language composition and extension.
   * Enable facilities for language testing, validation, and evolution.

2. Logical and Declarative Foundations
   * Base the platform on a logical programming paradigm, supporting classical logical operators (and, or, not).
   * Ensure a purely declarative approach, with clear separation between logic and execution.
   * Offer configurable reasoning over extensions to guarantee performance and correctness.
   * Allow configurable primitive relations for seamless database integration and proprietary function support.
   * Guarantee proven consistency in language execution and reasoning.

3. Execution and Performance
   * Deliver high performance and scalability for both small and large models.
   * Support on-demand execution with functional semantics.
   * Provide incremental, reactive execution with update semantics for dynamic scenarios.
   * Ensure a lightweight design suitable for integration in diverse environments.

4. Tooling, Integration, and Accessibility
   * Offer a language workbench accessible in major IDEs and in the browser.
   * Integrate with IDEs via the Language Server Protocol (LSP) for a consistent editing experience.
   * Provide facilities for defining, testing, and executing both syntax and semantics within the workbench.
   * Enable seamless integration with Large Language Models (LLMs) and other modern tooling.
   * Support collaborative and distributed modeling workflows.

These requirements position Nelumbo as a flexible, extensible, and future-proof platform for declarative modeling and language development.