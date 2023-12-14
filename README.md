# Interesium
Interesium optimizes Mojang's PointOfInterests(to be simple, POI) system. The optimization concept comes from [Lithium](https://github.com/CaffeineMC/lithium-fabric/tree/develop/src/main/java/me/jellysquid/mods/lithium/mixin/ai/poi) but hugely extended in execution.

Interesium is not simply a 1.16.5 Forge port of Lithium POI optimiztion. In fact, only [a very small part of the code for this project](https://github.com/MCTeamPotato/Interesium/blob/1165/src/main/java/com/teampotato/interesium/mixin/vanilla/SecondaryPoiSensorMixin.java) comes from Lithium, everything else is original.
# Details
POI is widely used in entities ai goals and dimensional teleportation. 

However, Mojang's POI manager is using [Stream API](https://www.baeldung.com/java-8-streams-introduction) heavily, which introduces too much overhead in their iteration process.

So, Interesium goes back to basics and rewrites [a new POI manager](https://github.com/MCTeamPotato/Interesium/blob/1165/src/main/java/com/teampotato/interesium/api/InteresiumPoiManager.java) using the traditional [Iterator](https://www.baeldung.com/java-iterator), which reduces the overhead effectively.

# RoadRunner
So why don't you use RoadRunner? Doesn't it also contain POI optimization?

In March 2023, I had a 1.16.5 Forge Minecraft server with RoadRunner installed. But during 2 months long gameplay, it caused countless tiny compatibility issues with mods which is quite annoying, so I didn't want to use it.

You can try this mod with [embeddedt's RoadRunner fork](https://github.com/embeddedt/roadrunner) though, which disables its semi-broken POI optimization.

# Expectation
So what do you hope to get from Interesium?

This mod may not improve TPS very much, but MSPT should be decreased when dimensional teleportation/certain entities goals process take place in your world.

Benchmark has been roughly done about [nether portal teleportation](https://github.com/MCTeamPotato/Interesium/blob/1165/src/main/java/com/teampotato/interesium/mixin/vanilla/PortalForcerMixin.java), in which I've observed about 2 to 3 times findPortalAround execution speed increasing averagely.
# Issue Report
https://github.com/MCTeamPotato/Kasualix-Issue-Tracker
# Mods Integration
Interesium has internal integration with these mods and will optimize their POI usages automatically:
- Atum
- Yung's Better Portals
- Blue Skies
- Forbidden Arcanus
- Gaia Dimension
- Minecraft Comes Alive Reborn
- Phi
- Vampirism
