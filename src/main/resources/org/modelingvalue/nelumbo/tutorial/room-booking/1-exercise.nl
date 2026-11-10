import nelumbo.collections

// build your language here: times, slots, rooms, sentences, variables and rules



// ---- given: today's bookings, complete per room, and the answers they should give ----
fact the bookings for Blue  = {9:30 to 11:00, 13:00 to 14:30}
fact the bookings for Red   = {9:00 to 10:30, 10:00 to 11:00}
fact the bookings for Green = {}

Blue is free at 10:00                ? [][()]
Blue is free at 12:00                ? [()][]
Blue is free at 11:00                ? [()][]
Blue is free between 11:00 and 13:00 ? [()][]
Blue is free between 10:30 and 12:00 ? [][()]
Blue is free between 12:00 and 15:00 ? [][()]
Green is free at 10:00               ? [()][]
Yellow is free at 10:00              ? [..][..]
Red has clashing bookings            ? [()][]
Blue has clashing bookings           ? [][()]
