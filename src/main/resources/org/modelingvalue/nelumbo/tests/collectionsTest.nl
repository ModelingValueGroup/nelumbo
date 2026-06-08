
  import      nelumbo.collections
    
  List<Integer>       l
  Set<Integer>        s
  Collection<Integer> c
  Integer             i
    
  s={1,2,3} ?         [(s={1,2,3})][..]
  
  s={}      ?         [(s={})][..]
  
  {[i](|i|=10)}=s   ? [(s={-10,10})][(s={0}),..]
