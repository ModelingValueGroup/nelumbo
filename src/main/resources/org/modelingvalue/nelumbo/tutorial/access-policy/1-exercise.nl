import nelumbo.logic

// build your language here: types, the dictionary, statements, questions and transformations



// ---- given: the dictionary, the policy, the staff, and the answers they should give ----
role a manager
role an intern
role an employee
role an auditor

every manager is an employee
every intern is an employee
every employee may read contracts
every manager may approve invoices
no intern may approve invoices
no intern may sign contracts
every auditor may read invoices

Jan is a manager
Ann is an intern
Eva is an auditor

Jan may approve invoices ? [()][]
Ann may approve invoices ? [][()]
Eva may approve invoices ? [..][..]
Ann may read contracts   ? [()][]
Eva may read contracts   ? [..][..]
Ann may sign contracts   ? [][()]
Jan may sign contracts   ? [..][..]
Who may approve invoices ? [(Who=Jan),..][(Who=Ann),..]
Who may read contracts   ? [(Who=Ann),(Who=Jan),..][..]
