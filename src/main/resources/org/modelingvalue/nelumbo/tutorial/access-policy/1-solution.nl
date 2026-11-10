import nelumbo.logic
Person  :: Object
Person  ::= Jan, Ann, Eva, Bob
Article :: Object
Article ::= a, an
Role    :: Object
Role    ::= manager, intern, employee, auditor
Action  :: Object
Action  ::= approve, read, sign
Item    :: Object
Item    ::= invoices, contracts
Boolean ::= <Person> may <Action> <Item> #30
private Boolean ::= <Person> has role <Role> #30
Root    ::= role <Article> <Role>             #100,
            every <Role> may <Action> <Item>  #0,
            no <Role> may <Action> <Item>     #0
{
    Article          ar
    {Role,Literal}   r, s
    {Action,Literal} x
    {Item,Literal}   i
    {Person,Literal} p
    Person           q
    role ar r ::> {
        Root ::= <{Person,Literal}> is <ar> <r>     #0
        Root ::= every <{Role,Literal}> is <ar> <r> #0
        p is ar r ::> {
            fact p has role r
        }
        every s is ar r ::> {
            q has role r <=> true if q has role s
        }
    }
    every r may x i ::> {
        q may x i <=> true if q has role r
    }
    no r may x i ::> {
        q may x i <=> false if q has role r
    }
}
Person Who

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
