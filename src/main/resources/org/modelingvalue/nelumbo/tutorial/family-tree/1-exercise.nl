import nelumbo.logic

// build your language here: types, people, sentences, variables and rules



// ---- given: make these facts parse and these questions give the expected answers ----
fact Juliana   is the mother of Beatrix, Juliana  is the mother of Irene, Juliana  is the mother of Margriet
fact Bernhard  is the father of Beatrix, Bernhard is the father of Irene, Bernhard is the father of Margriet
fact Beatrix   is the mother of Alexander, Beatrix is the mother of Friso
fact Claus     is the father of Alexander, Claus   is the father of Friso
fact Alexander is the father of Amalia

Who is the mother of Beatrix   ? [(Who=Juliana)][..]
Irene is the mother of Beatrix ? [][()]
Who is a parent of Beatrix     ? [(Who=Bernhard),(Who=Juliana)][..]
Who is a grandparent of Amalia ? [(Who=Beatrix),(Who=Claus),..][..]
Who is a grandchild of Juliana ? [(Who=Alexander),(Who=Friso),..][..]
Who is an ancestor of Amalia   ? [(Who=Alexander),(Who=Beatrix),(Who=Bernhard),(Who=Claus),(Who=Juliana),..][..]
Who is a sister of Beatrix     ? [(Who=Irene),(Who=Margriet),..][(Who=Beatrix),..]
Who is a brother of Alexander  ? [(Who=Friso),..][(Who=Alexander),..]
Friso is a sister of Alexander ? [][()]
Who is an aunt of Alexander    ? [(Who=Irene),(Who=Margriet),..][..]
