// Bug: period comparison counts a year as 360 days instead of 365, so P1Y > P364D is false.
import nelumbo.datetime

P1Y > P364D ? [()][]
