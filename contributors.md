# CONTRIBUTION GUIDELINES
Guidelines are loose and can be changed or exceptions made.

## Code
Code contributions can be Java, but kotlin is HIGHLY preferred.
Code must be of "okay" quality. I reserve the right to make adjustments to your code.

## Writing
I'm not asking for pristine writing, but I am asking for proficiency in english and an effort to minimize errors, an effort
for sentences to flow well, and a general attempt at quality. 
I will probably go around and do some quality passes on any writing you submit.

## Spriting
Just make the ships look good and I'll tell you what I think.

### Lore
Any writing must have a good grasp of the vanilla setting, or at least make sense within the world of starsector and WWLB.

# CURRENT FOCUSES
Current projects I am open for help on.

## IAIIC Crisis
Code path: data.scripts.campaign.magnetar.crisis

The IAIIC crisis happens after you install the fractal core from the magnetar onto a planet for more than a few months.
A colony crisis is triggered, and once it pops, a star fortress spawns in your space and begins to launch AI inspections against you.

### Design
The IAIIC crisis is meant to be an end-game crisis that challenges even modded playthroughs. There are a few main ways this is achieved.
1. Elevation - The IAIIC has an event meter that progressively launches actions against you.
    Escalation - When this bar gets high enough, "escalation" triggers. This increases the fleet size of the IAIIC, while also repairing all 
    buildings on the FOB and respawning all fleets.
    Sabotage - Occasionally, sabotage will be triggered. This causes various maluses to be applied to your markets. They are **very severe**,
    and serve as a way to slow down your own faction and force you to go defensive.
2. Politics - The FOB cannot be invaded or decivilized. Raiding it triggers sabotage.
    It is a proxy state of various factions. **The intended way to counter it is to do missions and favors for it's contributing factions.**
3. The IAIIC is meant to be a long-term threat that cannnot be instantly squashed. You are intended to struggle and have to rise to the challenge
   \- or at least spend a lot of resources and time.

The culture/history of the IAIIC has not been fully established, but there are a few tips.
1. Due to being a proxy state, it has no real culture - everyone there is there on business.
2. Individuals can be hateful towards you, indifferent, or opportunistic. Theres a ton of variety.
3. The IAIIC only exists to counter you - they have limited diplomatic or trade relation with any other faction
    Of course, due to being a proxy state, internal politics are almost entirely controlled by contributing factions.

This list is not exhaustive. Please ask me if you have any questions about it's design philosophy.

### Factions
The main modules that I need help on are the **faction questlines.**

#### Design
Due to design philosophy **3**, faction questlines are intended to be a resource investment. Because of this, a few approaches are suggested.
1. Time-gates: At certain points within the questline, it is suggested to force the player to wait a month or so.
2. Mutual exclusivity: This is likely to be implemented by me. Some way to prevent you from progressing multiple questlines at once.
3. Resource intensivity: Story points, colony items, special items, etc. Credits are not suggested as a primary drain - a end-game colony can easily get credits.
4. Challenge: Hard fights, puzzles to solve, etc.
5. Intrigue: Reputation loss for factions, or other related "soft" losses.

#### World
Factions invest in the IAIIC because they want something from you, hate you, or both.
Factions like the _hegemony_ are unlikely to capitulate if you give them a favor. The diktat, however, is more than happy to.
Factions like _tritachyon_ are happy to use this as an opportunity to punish you for being competition - ganging up on you - but they also want that core.

Ideological factions like the luddic church hate you. Their majority will not be swayed by favors.
    _However_. Minor factions within the church can and will pull weight.

Here are a few suggestions of how to make a faction's questline make sense.

1. Opportunistic executive willing to work with you to sabotage the main faction's involvement
2. A faction leader directly asking for a favor
3. A concerned aristocracy worried about the hegemony's massive investment

There are many ways to make a faction questline make sense, but you need to know the faction's place in the setting, and their internal politics.

If going for "a favor" approach, it is suggested that whatever plot you design has **lasting consequences** for the parties involved.
    This is to enrich the world, and hammer home the fact that some people are opportunistic.

**No faction will ever directly state that they are involved with the IAIIC unless they are sure the public will never hear it.**

#### TODO List

1. Hegemony (STARTED, CAN BE CLAIMED, ASK ME FOR DETAILS)
2. Luddic Church (UNCLAIMED, NOT STARTED)
3. Independent (STARTED, CAN BE CLAIMED, ASK ME FOR DETAILS)
4. Tri-Tachyon (FINISHED - NEEDS TOUCHUPS AND FURTHER RESOURCE INVESTMENT)
5. Diktat (FINISHED - NEEDS TOUCHUPS)
    Possible lore conflicts with deposing Umbra's quartermaster
   Needs to be longer
6. Luddic Path (FINISHED - NO CHANGES NEEDED)

### Alliances
A smaller submodule, related to factions. "Alliances" in the context of the IAIIC means factions sending you defense fleets, or otherwise taking action against the IAIIC.

data.scripts.campaign.magnetar.crisis.intel.support.MPC_fractalCrisisSupport

#### TODO List

1. Persean League (UNCLAIMED, NOT STARTED)
    Only obtainable if you are in the PL!
    Have to strongarm heynard (or whoever leads the league) and he gets all shifty about it
    Sends a moderate amount of defense fleets
2. Lion's Guard (UNCLAIMED, NOT STARTED)
   Talk to horacio
    Donate omega weapons, or threat weapons, or something really high tech and flashy, you knnow the LG
    Sends a number of lion's guard (SPECIFICALLY LG) fleets
3. Diktat (UNCLAIMED, NOT STARTED, **OPTIONAL**)
    Talk to Hyder
    Do something more substantial(?)
    
    Sends a number of diktat fleets (NOT LIONS GUARD)

    Potentially exclusive with the Lion's Guard(?)

### Requirements
Rules.csv knowledge is **NECESSARY**. Code capability is preferred.

## Misc
Any pull requests submitted will be reviewed. Feel free to suggest changes or fixes for other things.

# TESTING

## Magnetar
The magnetar is in the abyss, bottom left. Disable sensors to find a collection of jump points in the abyss.

## IAIIC Crisis
Run niko_MPC_prepareIAIICEvent
