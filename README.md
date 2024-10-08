What is implemented using redis:

Message login: Redis’s session

Leaderboard: List, SortedSet

Friend subscribe: Set

User Daily Badge-in: Bitmap

Lighting Deal: Redis counter, Lua, distributed Lock, mq

Store search cache: also deal with avalanche, penetration issues.

Nearby shops: GeoHash

UV Stats: HyperLogLog
