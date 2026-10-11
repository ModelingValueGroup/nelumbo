import nelumbo.logic

// Language

Root ::= transitive <Type> <NAME> <NAME> #100

{
    Type T
    NAME DN, CN

    transitive T DN CN ::> {
        Root             ::= <{T,Literal}> DN <{T,Literal}> #30
        Boolean          ::= <T> CN <T>                     #30
        private FactType ::= DN(<T>,<T>)

        T x, y, z
        x CN y <=> DN(x,y) | E[z](DN(x,z) & z CN y)

        {T,Literal} l1, l2
        l1 DN l2 ::> {
            fact DN(l1,l2)
        }
    }
}

// Model

Station :: Object
Animal  :: Object

transitive Station track reaches
transitive Animal  eats  feedsOn

// Facts

Station ::= Amsterdam, Utrecht, Arnhem, Eindhoven, Maastricht, Texel
Animal  ::= Fox, Rabbit, Carrot, Owl, Mouse, Grain

Amsterdam track Utrecht
Utrecht   track Arnhem
Utrecht   track Eindhoven
Eindhoven track Maastricht

Fox    eats Rabbit
Rabbit eats Carrot
Owl    eats Mouse
Mouse  eats Grain
Fox    eats Mouse

// Queries

Station s
Animal  a

Amsterdam reaches Maastricht ? [()][]
Maastricht reaches Amsterdam ? [][()]
Amsterdam reaches Texel      ? [][()]
Amsterdam reaches s          ? [(s=Utrecht),(s=Arnhem),(s=Eindhoven),(s=Maastricht)][..]
Fox feedsOn a                ? [(a=Rabbit),(a=Carrot),(a=Mouse),(a=Grain)][..]
a feedsOn Grain              ? [(a=Mouse),(a=Owl),(a=Fox)][..]
Owl feedsOn Carrot           ? [][()]
