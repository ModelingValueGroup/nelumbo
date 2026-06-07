import nelumbo.integers

DateTime :: Object
Date 	 :: Object
Time 	 :: Object
Period   :: Object

// TODO: Think about extracting Timezone part? should this be a full literal?
DateTime ::= <[> <Date> T <Time#50> <(> <(> Z <|> <(> <(> + <|> - <)> <NUMBER> : <NUMBER> <)> <)> <)?> <]> @nelumbo.datetime.NDateTime,
             <DateTime> + <Period>   #40,
             <DateTime> - <Period>   #40

// TODO: Disallow spaces, limit number length, prefix?
Date 	::= <[> <NUMBER> - <NUMBER> - <NUMBER> <]> 						                                   @nelumbo.datetime.NDate,
             <Date> + <Period>   #40,
             <Date> - <Period>   #40

// TODO: Disallow spaces, or prefix with T, decimal?
Time	::= <[> <NUMBER> : <NUMBER> <(> : <NUMBER> <(> . <NUMBER> <)?> <)?> <]>		                       @nelumbo.datetime.NTime,
             <Time> + <Period>   #40,
             <Time> - <Period>   #40

// TODO separate on multiple lines?
Period 	::= <[> P <(> <(> <NUMBER> <(> Y <|> M <|> W <|> D <)> <)+> <(> T <(> <NUMBER> <(> H <|> M <|> S <)> <)+> <)?> <|> T <(> <NUMBER> <(> H <|> M <|> S <)> <)+> <)> <]> @nelumbo.datetime.NPeriod,
             <DateTime> - <DateTime> #40,
             <Date> - <Date>         #40,
             <Time> - <Time>         #40,
             <Period> + <Period>     #40,
             <Period> - <Period>     #40,
             <Period> * <Integer>    #50

private Boolean ::= datetime_add(<DateTime>,<Period>,<DateTime>)    @nelumbo.datetime.Add,
                    date_add(<Date>,<Period>,<Date>)                @nelumbo.datetime.Add,
                    time_add(<Time>,<Period>,<Time>)                @nelumbo.datetime.Add,
                    period_add(<Period>,<Period>,<Period>)          @nelumbo.datetime.AddPeriod,
                    period_multiply(<Period>,<Integer>,<Period>)    @nelumbo.datetime.MultiplyPeriod

Boolean ::= <DateTime> ">"  <DateTime>   #30 @nelumbo.datetime.GreaterThan,
            <DateTime> "<"  <DateTime>   #30,
            <DateTime> "<=" <DateTime>   #30,
            <DateTime> ">=" <DateTime>   #30

Boolean ::= <Date> ">"  <Date>   #30 @nelumbo.datetime.GreaterThan,
            <Date> "<"  <Date>   #30,
            <Date> "<=" <Date>   #30,
            <Date> ">=" <Date>   #30

Boolean ::= <Time> ">"  <Time>   #30 @nelumbo.datetime.GreaterThan,
            <Time> "<"  <Time>   #30,
            <Time> "<=" <Time>   #30,
            <Time> ">=" <Time>   #30

Boolean ::= <Period> ">"  <Period>   #30 @nelumbo.datetime.GreaterThan,
            <Period> "<"  <Period>   #30,
            <Period> "<=" <Period>   #30,
            <Period> ">=" <Period>   #30


DateTime a, b
Date c, d
Time e, f
Period x, y, z
Integer n


a<b    <=>  b>a
a<=b   <=>  a<b | a=b
a>=b   <=>  a>b | a=b

a+x=b  <=>  datetime_add(a,x,b)
a-x=b  <=>  datetime_add(b,x,a)
a-b=x  <=>  datetime_add(b,x,a)

c<d    <=>  d>c
c<=d   <=>  c<d | c=d
c>=d   <=>  c>d | c=d

c+x=d  <=>  date_add(c,x,d)
c-x=d  <=>  date_add(d,x,c)
c-d=x  <=>  date_add(d,x,c)

e<f    <=>  f>e
e<=f   <=>  e<f | e=f
e>=f   <=>  e>f | e=f

e+x=f  <=>  time_add(e,x,f)
e-x=f  <=>  time_add(f,x,e)
e-f=x  <=>  time_add(f,x,e)

x<y    <=>  y>x
x<=y   <=>  x<y | x=y
x>=y   <=>  x>y | x=y

x+y=z  <=>  period_add(x,y,z)
x-y=z  <=>  period_add(z,y,x)
x*n=y  <=>  period_multiply(x,n,y)
