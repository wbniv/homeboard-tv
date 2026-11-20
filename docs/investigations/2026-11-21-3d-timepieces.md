# 3D timepieces: 30 ideas for a clock worth watching

**Brainstorm, 2026‑11‑21.** Nothing here is built or prototyped. Will asked for "50 ideas
for 3d timepieces (sure, some watches/clock mechanisms, but can use other things like
water, pendulums, dominoes (lol) )", then "is 50 too many? how about 30?" and "include
diagram/visalization for each idea". So this is 30 ideas, each with a sketch, plus a
one‑line [bench of 20 more](#bench-20-more-not-sketched) that would take it to 50.

I have read these as candidates for 3D screensaver scenes beside the Clock scene in the
[screensaver plan](../plans/2026-11-20-screensavers.md). That is my assumption, and
nothing below depends on it.

**About the sketches.** They are schematics, not renders: one small isometric drawing
kit in [`build.py`](2026-11-21-3d-timepieces/build.py) draws all 30, so they can be
regenerated or extended with `python3 build.py`. Each one shows 10:08:22 unless its
caption says otherwise, so the readings can be compared.

## What makes one of these work on a TV

1. **The scene must be a function of the clock.** A screensaver starts at an arbitrary
   moment, so every pose has to be computable from the current time alone. A domino's
   angle depends only on how long ago its second passed; a bucket is full if its bit is
   set. All 30 are meant as choreography that looks like physics, not as a simulation
   that would have to be replayed from midnight.
2. **It has to say how it is read.** Some of these read at a glance from the sofa, some
   are counted and some are decoded. The table below says which. A decoded one probably
   wants the plan's small "Show clock" overlay switched on.
3. **Nothing bright should sit still.** A dial in a fixed place is a burn‑in risk. A
   slow camera orbit fixes that for nearly all of these, and it is also what makes them
   3D rather than a flat picture.
4. **One small event a minute, one big event an hour.** The ones that hold attention
   have a reason to look up at :00.

## At a glance

"Reads" is my judgement from the idea alone; none of it has been tested on a screen.

| # | Idea | Driven by | Reads | Moves |
|---|---|---|---|---|
| 1 | [Exploded skeleton movement](#1-exploded-skeleton-movement) | Gear train | At a glance | Continuously |
| 2 | [Triple-axis tourbillon](#2-triple-axis-tourbillon) | Escapement in cages | At a glance (sub-dial) | Continuously |
| 3 | [Planetary ring-gear clock](#3-planetary-ring-gear-clock) | Epicyclic gears | With practice | Continuously |
| 4 | [Verge and foliot tower clock](#4-verge-and-foliot-tower-clock) | Falling weight | Hour only | Each tick |
| 5 | [Pendulum wave](#5-pendulum-wave) | 12 pendulums | Count | Continuously |
| 6 | [Foucault pendulum and pegs](#6-foucault-pendulum-and-pegs) | One long pendulum | Count | Each minute |
| 7 | [Conical pendulum](#7-conical-pendulum) | Circling ball | At a glance | Continuously |
| 8 | [Float clepsydra](#8-float-clepsydra) | Dripping water | At a glance | Slowly |
| 9 | [Siphon time-flow clock](#9-siphon-time-flow-clock) | Water and siphons | Count | Each swing |
| 10 | [Tipping-bucket binary cascade](#10-tipping-bucket-binary-cascade) | Water | Decode | Each minute |
| 11 | [Hourglass carousel](#11-hourglass-carousel) | Sand | Count | Continuously |
| 12 | [Sand table that writes the time](#12-sand-table-that-writes-the-time) | Ball and magnet | At a glance | Each minute |
| 13 | [Water-curtain digits](#13-water-curtain-digits) | Falling drops | At a glance, briefly | Continuously |
| 14 | [Congreve rolling-ball clock](#14-congreve-rolling-ball-clock) | Rolling ball | At a glance | Continuously |
| 15 | [Rolling-ball rail clock](#15-rolling-ball-rail-clock) | Balls on rails | Count | Each minute |
| 16 | [Domino ring](#16-domino-ring) | Dominoes | At a glance | Each second |
| 17 | [Sixty-second chain reaction](#17-sixty-second-chain-reaction) | Chain reaction | At a glance | Continuously |
| 18 | [Marble pixel board](#18-marble-pixel-board) | Marbles | At a glance | Each minute |
| 19 | [Sundial under a real sun](#19-sundial-under-a-real-sun) | Sunlight | With practice | Slowly |
| 20 | [Shadow-digit sculpture](#20-shadow-digit-sculpture) | A lamp | At a glance | Each minute |
| 21 | [Anamorphic shards](#21-anamorphic-shards) | The camera | Only at alignment | Continuously |
| 22 | [Candle clock with dropping balls](#22-candle-clock-with-dropping-balls) | Flame | Count | Slowly |
| 23 | [Orrery geared to the clock](#23-orrery-geared-to-the-clock) | Orbits | At a glance | Continuously |
| 24 | [Globe in a 24-hour sun ring](#24-globe-in-a-24-hour-sun-ring) | Earth's rotation | With practice | Slowly |
| 25 | [Star dome with the Plough as hand](#25-star-dome-with-the-plough-as-hand) | The night sky | With practice | Slowly |
| 26 | [Split-flap wall](#26-split-flap-wall) | Flaps on drums | At a glance | Each minute |
| 27 | [Pin-field relief](#27-pin-field-relief) | Rising pins | At a glance | Each minute |
| 28 | [Construction crew](#28-construction-crew) | Tiny workers | At a glance | Continuously |
| 29 | [Train loops](#29-train-loops) | Model trains | At a glance | Continuously |
| 30 | [Hour tree](#30-hour-tree) | Growth | Count | Slowly |

## Where I would start

These are opinions, not findings.

- **Domino ring (16).** It reads like a dial, something happens every second, and each
  domino's pose is a one‑line function of the time. It is also the one you laughed at.
- **Rolling-ball rail clock (15).** The best hourly event of the 30: at 12:59 every rail
  dumps at once.
- **Anamorphic shards (21).** The only idea here that cannot exist as a flat clock, so
  it earns the 3D most.
- **Pendulum wave (5).** The most beautiful continuous motion, and the maths is exact.
- **Split-flap wall (26).** The safe one: legible from anywhere, cheap to build, and a
  good first scene for testing the plumbing.

## Gears and escapements

### 1. Exploded skeleton movement

![Exploded movement: barrel, going train, balance and dial pulled apart along one axis](2026-11-21-3d-timepieces/01-exploded-movement.svg)

A real going train, modelled tooth for tooth, with its layers pulled apart along the
centre arbor: barrel at the bottom, then the train, the balance and the dial. The camera
orbits, and the layers drift apart and close up again on the hour.

**Reads:** an ordinary dial on top. **Look for:** the balance flicking back and forth
while the barrel below it barely creeps.

### 2. Triple-axis tourbillon

![Three nested cages on three axes around a balance wheel, with a small hour and minute dial](2026-11-21-3d-timepieces/02-gimbal-tourbillon.svg)

The escapement sits inside three nested cages, each turning on a different axis at its
own rate. Watchmakers build these to average out gravity's pull on the balance. A
simulation has no gravity to average, which is the joke, but the motion is hypnotic.

**Reads:** a small sub‑dial for hours and minutes; the bead on the outer cage laps once
a minute. **Look for:** the three cages coming back into line.

### 3. Planetary ring-gear clock

![A fixed ring gear with a sun gear and three planets; the red planet marks the hour](2026-11-21-3d-timepieces/03-planetary-ring.svg)

No hands. A sun gear turns once an hour and its dot is the minute hand. Three planets
roll around inside a fixed ring gear, carried round once in 12 h, and the red one is
the hour hand. A single stage needs a ring with 11 times the sun's teeth to give 12:1,
so a real build would use a compound stage; the sketch is not to ratio.

**Reads:** like a dial once you know which planet is red. **Look for:** the planets
spinning backwards as they walk forwards.

### 4. Verge and foliot tower clock

![A wooden frame with a crown wheel, a vertical verge, a weighted foliot bar and a one-handed dial](2026-11-21-3d-timepieces/04-verge-foliot.svg)

The oldest mechanical escapement. A falling weight turns a crown wheel; two pallets on a
vertical rod catch it one tooth at a time and fling a weighted bar, the foliot, back and
forth. It has one hand because the real thing drifted by many minutes a day.

**Reads:** the hour only, which is honest about what such a clock knew. **Look for:**
the lurching tick, and the weight being wound back up at midnight.

## Pendulums

### 5. Pendulum wave

![Twelve pendulums of graded length hanging from one beam, frozen mid-wave](2026-11-21-3d-timepieces/05-pendulum-wave.svg)

Twelve pendulums of graded length, tuned so each makes one more swing a minute than its
neighbour. They start in a line, smear into a travelling wave, split into interleaved
groups and snap back into line exactly every 60 s.

**Reads:** the shape is the seconds (in line at :00, two alternating groups at :30), lit
bobs count the hour, and beads on the beam count the minutes. **Look for:** the snap
back into line.

### 6. Foucault pendulum and pegs

![A long pendulum over a ring of 60 pegs, a few knocked flat at each end of the swing](2026-11-21-3d-timepieces/06-foucault-pegs.svg)

The museum set piece: a long pendulum whose swing plane slowly turns, toppling a ring of
pegs. A real one turns with the Earth, once in 24 h divided by the sine of the latitude,
which is about 101 h in Bangkok and 37 h at 40° N. This one is geared to half a turn an
hour, so each end of the swing topples 30 pegs.

**Reads:** pegs down are the minutes, lamps around the edge are the hour. **Look for:**
all 60 pegs standing back up on the hour.

### 7. Conical pendulum

![A ball on a wire circling under a tripod, casting a shadow on a dial below](2026-11-21-3d-timepieces/07-conical-pendulum.svg)

A ball on a wire that circles instead of swinging, one lap a minute, with no tick at
all. French makers sold clocks like this in the 19th century. Its shadow on the dial
below is the seconds hand, and two beads on rings carry the minutes and hours.

**Reads:** like a dial. **Look for:** nothing in particular; this is the calmest of the
30 and suits a room where people are talking.

## Water and sand

### 8. Float clepsydra

![A raised reservoir dripping into a glass cylinder; a float carries a pointer up an hour column](2026-11-21-3d-timepieces/08-clepsydra.svg)

The ancient Greek design credited to Ctesibius. A reservoir kept brim‑full drips at a
constant rate into a cylinder; a float rises with the water and carries a pointer up an
hour column.

**Reads:** the pointer's height. **Look for:** 12 o'clock, when the cylinder siphons
empty in one gulp and the pointer drops back to the bottom.

### 9. Siphon time-flow clock

![A frame holding a reservoir, a pendulum scoop, a column of hour bulbs and vessels for minutes](2026-11-21-3d-timepieces/09-time-flow.svg)

Loosely after Bernard Gitton's glass water clocks. A pendulum meters coloured water into
glass vessels: 12 hour bulbs, five ten‑minute vessels and a tube for single minutes.
Each one fills, and a siphon empties it the moment it is full.

**Reads:** count the full vessels (the sketch shows 10:38). **Look for:** the hour,
when the minute vessels all drain into one hour bulb.

### 10. Tipping-bucket binary cascade

![Two staircases of buckets, each labelled with a power of two; the full ones add up to the time](2026-11-21-3d-timepieces/10-binary-buckets.svg)

Each bucket tips on its second fill and pours into the next one down, so a staircase of
them is a binary counter made of water. A full bucket is a 1. Six buckets count the
minutes and pass a fill to a row of four for the hours.

**Reads:** add up the full buckets, so this is one to decode. **Look for:** minute 31
to 32, when five buckets tip in a row.

### 11. Hourglass carousel

![A wheel carrying twelve hourglasses; ten are spent and the eleventh is running](2026-11-21-3d-timepieces/11-hourglass-carousel.svg)

Twelve one‑hour glasses on a wheel. On the hour the wheel steps round and the next glass
turns over.

**Reads:** spent glasses are the hour, and the sand level in the running glass is the
minutes. **Look for:** noon and midnight, when the whole wheel inverts and all twelve
start again.

### 12. Sand table that writes the time

![A sandbox with 10:08 ploughed into the sand by a steel ball, and a wiper bar at one end](2026-11-21-3d-timepieces/12-sand-table.svg)

A steel ball, dragged by a magnet under the table, ploughs the digits into fine sand,
in the manner of kinetic sand tables. A wiper bar levels a digit before it is rewritten.
Low raking light does the rest.

**Reads:** the digits, directly. **Look for:** the ball taking most of the minute to
write the minute.

### 13. Water-curtain digits

![A bar of nozzles releasing drops that form 10:08 in mid-air above a pool](2026-11-21-3d-timepieces/13-water-curtain.svg)

A bar of 19 valves releases drops row by row, so the digits form in mid‑air and fall as
a sheet, like the graphic waterfalls built in some stations and malls. Real drops fall
1.4 m in about half a second, so the scene would slow gravity to let each image hang.

**Reads:** directly, but only while the image is falling. **Look for:** two minutes on
screen at once, the old one hitting the pool as the new one forms.

## Balls and chain reactions

### 14. Congreve rolling-ball clock

![A tilting brass table with a zigzag groove and a ball, under three small dials](2026-11-21-3d-timepieces/14-congreve.svg)

William Congreve's 1808 design. A ball takes 30 s to zigzag down a tilted brass table.
At the end it trips a latch, the table tilts the other way and the hands step. The real
ones keep poor time because dust slows the ball; a simulated one has no dust.

**Reads:** three small dials. **Look for:** the tip at each half minute.

### 15. Rolling-ball rail clock

![Three sloping rails holding balls for minutes, five-minutes and hours, fed by a lifting arm](2026-11-21-3d-timepieces/15-ball-rails.svg)

The 1970s desk toy. An arm lifts one ball a minute onto the top rail. When a rail is
full it tips: one ball drops to the rail below and the rest run home.

**Reads:** add the rails, hours plus five‑minute balls plus single minutes. **Look
for:** 12:59 to 1:00, when every rail dumps at once.

### 16. Domino ring

![A ring of 60 dominoes, half fallen, with a reset arm, a ring of minute tiles and 12 hour blocks](2026-11-21-3d-timepieces/16-domino-ring.svg)

Sixty dominoes in a ring fall one a second. A reset arm follows half a lap behind and
stands them back up, so the wave never stops. Inside are a ring of 60 minute tiles and
12 hour blocks, one of each raised.

**Reads:** like a dial: the falling front is the seconds hand. **Look for:** the hour,
when the inner rings could topple too.

### 17. Sixty-second chain reaction

![A ball on a ramp, a row of dominoes, a seesaw, a bucket on a string and a flap display](2026-11-21-3d-timepieces/17-minute-machine.svg)

A Rube Goldberg machine timed to take exactly one minute: ramp, dominoes, seesaw, bucket
and string. Its last act is to drop the minute flap, and then every piece re‑arms.
Several machines could take turns, so no two minutes look alike.

**Reads:** the flap display, and how far along the chain the action is. **Look for:**
the last five seconds.

### 18. Marble pixel board

![A board of pockets with marbles spelling 10:08, a hopper above and a return tray below](2026-11-21-3d-timepieces/18-marble-pixels.svg)

Marbles drop from a hopper through gates into a grid of 19 by 5 pockets and spell the
time. On the minute, trapdoors release only the digits that changed and a screw lift
carries the marbles back up.

**Reads:** the digits, directly. **Look for:** the hour, when all four digits empty and
refill.

## Light, shadow and fire

### 19. Sundial under a real sun

![A stone sundial with a brass gnomon, its shadow lying between the hour lines, and the sun's path above](2026-11-21-3d-timepieces/19-sundial.svg)

An ordinary horizontal sundial, lit by a sun placed where it really is for the date, the
time and the home's latitude. Day length and the seasons come for free. At night the
moon casts a fainter shadow, or the numerals glow.

**Reads:** the shadow on the hour lines. A true sundial differs from clock time by up to
about 16 min over the year, so the scene either shows that honestly or nudges its sun.
**Look for:** sunrise and sunset.

### 20. Shadow-digit sculpture

![A floating cluster of blocks under a lamp, casting a shadow that reads 10:08](2026-11-21-3d-timepieces/20-shadow-digits.svg)

A cloud of blocks that looks like noise from the room, but whose shadow under one lamp
reads the time. Digital sundials use the same trick. Each minute the blocks reshuffle
into a different cloud with the next shadow.

**Reads:** only in the shadow, never in the object. **Look for:** the reshuffle, when
the shadow dissolves and reforms.

### 21. Anamorphic shards

![Fragments hanging at different depths that line up into 10:08, with an inset of the same fragments scattered](2026-11-21-3d-timepieces/21-anamorphic.svg)

Fragments hang at different depths like a mobile. From one viewpoint they line up into
the digits; from anywhere else they are debris. The camera drifts on a loop that reaches
the sweet spot once a minute and holds it for a few seconds.

**Reads:** only at alignment, so it wants the overlay clock. **Look for:** the moment
it resolves. This idea cannot exist as a flat clock.

### 22. Candle clock with dropping balls

![A burning candle with pins holding metal balls, a brass dish of fallen balls, and a ghost outline of the wax already burnt](2026-11-21-3d-timepieces/22-candle.svg)

A marked candle burns one band an hour. A pin at each mark holds a metal ball; when the
wax melts past the pin, the ball drops into a brass dish. A new candle is lit at 00:00
and at 12:00.

**Reads:** balls in the dish are the hour, and the wax level within its band is the
minutes. **Look for:** a ball dropping on the hour, and the room dimming as the candle
shortens.

## Sky

### 23. Orrery geared to the clock

![A brass orrery with three bodies on arms at the hour, minute and second positions](2026-11-21-3d-timepieces/23-orrery.svg)

A brass orrery whose three bodies lap in 12 h, 1 h and 60 s instead of years. A second
mode could put the real planets where they are tonight and show the time on the Earth's
own rotation.

**Reads:** like a dial with planets for hands. **Look for:** the comet's lap each
minute, and the three lining up at 12:00.

### 24. Globe in a 24-hour sun ring

![The Earth, half in night, inside a fixed ring marked 0 to 24, with a pin at home](2026-11-21-3d-timepieces/24-globe.svg)

The Earth turning once a day inside a fixed ring marked 0 to 24, with 12 always facing
the sun. Home's meridian meets the ring at the local time, and the day and night line
shows who else is awake.

**Reads:** follow the home pin to the ring; every other city reads the same way. **Look
for:** city lights coming on as places cross into night.

### 25. Star dome with the Plough as hand

![A night sky dome with the Plough, its two pointer stars aimed at Polaris, and a 24-hour ring](2026-11-21-3d-timepieces/25-star-dome.svg)

The night sky over the house. The line from Merak through Dubhe to Polaris sweeps
anticlockwise once per sidereal day, which is how a 16th‑century nocturnal told the
time. The stars gain about 4 min a day on the clock, so the ring is turned for the date.

**Reads:** the pointer on a 24 h ring (the sketch shows 22:08). It works only at night,
so it pairs with the sundial for the day. **Look for:** satellites and the moon.

## Digit machines

### 26. Split-flap wall

![A split-flap display reading 10:08 with one flap mid-fall, and a side view of a flap drum](2026-11-21-3d-timepieces/26-split-flap.svg)

A station departure board, modelled with every flap on its drum and the camera close
enough to see the flaps blur. The clatter is half the charm, and the plan keeps
screensaver sound off, so this is the silent version.

**Reads:** directly, from anywhere in the room. **Look for:** the hour, when all four
drums spin.

### 27. Pin-field relief

![A bed of square pins, some raised to spell 10:08 in relief](2026-11-21-3d-timepieces/27-pin-field.svg)

A bed of square pins that rise to form the digits in relief, like a pin‑art toy or MIT's
inFORM table. A low raking light gives all the contrast, so nothing on screen is bright.
Changes sweep across the bed as a wave.

**Reads:** directly. **Look for:** the wave. This is the kindest of the 30 to an OLED
panel.

### 28. Construction crew

![Plank digits on scaffolding reading 10:0 and a half-built 8, with a crane and two tiny workers](2026-11-21-3d-timepieces/28-construction-crew.svg)

After Maarten Baas's Real Time films. A tiny crew with a crane takes down and rebuilds
the digits out of planks, and each minute takes the whole minute to build.

**Reads:** directly, though the last digit is always in progress. **Look for:** the
hour, when all four digits come down and the crew has to hurry.

## Living things and toys

### 29. Train loops

![Three concentric model railway loops with a short hour train, a longer minute train and one express car](2026-11-21-3d-timepieces/29-train-loops.svg)

Three concentric model railway loops with a station at each of the 12 positions. A short
hour train runs the inner loop, a longer minute train the middle, and a single express
car laps the outer loop once a minute.

**Reads:** like a dial with trains for hands. **Look for:** the trains passing each
other at 12:00.

### 30. Hour tree

![A tree with ten leafy branches and an eleventh just sprouting a leaf and three buds](2026-11-21-3d-timepieces/30-hour-tree.svg)

A tree that grows a branch every hour. The newest branch grows a leaf every five minutes
and a bud for each minute between. At 00:00 and 12:00 the leaves fall and it starts
again. The season could follow the date.

**Reads:** count branches, then leaves and buds, so it is slow to read. **Look for:**
the leaf fall. This is the most ambient of the 30.

## Bench: 20 more, not sketched

These would take the list to the original 50. They have no diagrams yet; `build.py`
makes adding them cheap.

| # | Idea | In one line |
|---|---|---|
| 31 | Newton's cradle | Clicks the seconds; the number of balls lifted counts tens of seconds. |
| 32 | Metronome forest | Sixty metronomes drift out of step and lock back together each minute. |
| 33 | Galton board | One ball a second builds a bell curve, and the board flips on the minute. |
| 34 | Marble spiral tower | Three spiral tracks of different lengths; a marble's height on each is a hand. |
| 35 | Incense seal clock | A smouldering line follows a maze of ash past an hour marker. |
| 36 | Melting ice block | Hour tokens frozen into the block drop out one at a time. |
| 37 | Lava-lamp digits | Rising blobs merge into the digits and then sink apart. |
| 38 | Ferrofluid digits | Magnets behind a glass tank pull black fluid into numerals. |
| 39 | Bamboo tipper | A shishi-odoshi fills, tips and knocks once a minute over a pond of hour stones. |
| 40 | Tide harbour | A model harbour whose water level and moored boats follow the real tide and time. |
| 41 | Armillary sphere | Brass rings with a sun bead that travels the hour ring. |
| 42 | Moon lantern | A lit moon at the true phase, hung over a moondial. |
| 43 | Lighthouse | The beam sweeps a ring of 12 rocks once a minute; lit windows count the hour. |
| 44 | Mirror and haze | Rotating mirrors sweep three light beams through haze as hands. |
| 45 | Flip-dot wall | Thousands of two-coloured discs flip to redraw the digits. |
| 46 | Twist tower | Four stacked rings of numerals that rotate into line. |
| 47 | Abacus | Beads slide by themselves, one a minute, with carries at ten. |
| 48 | Cuckoo village | A glockenspiel town whose figures parade on the hour. |
| 49 | Knitting clock | A machine knits one stitch a minute; the scarf's length is the day so far. |
| 50 | Window city | A night city block whose lit windows spell the time across the buildings. |
