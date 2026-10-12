<center>
<h2>MeteorEvents</h2>
<p><strong>Cinematic meteor strike events featuring real-time packet trajectories, procedural craters, interactive cores, and customizable rewards.</strong></p>
<p>Author: <strong>lHawk_</strong> | Contact: <a href="https://minebuilders.com.br">minebuilders.com.br</a></p>
</center>

<hr>

<h3>Overview</h3>
<p><strong>MeteorEvents</strong> brings celestial excitement to survival and RPG servers. Rather than instantaneous explosions, meteors traverse the sky along configurable atmospheric trajectories using <strong>PacketEvents</strong>, creating immersive visuals from heights of over 200 blocks visible from far across the world. Impact craters are rendered purely client-side via fake packet blocks, guaranteeing that real world blocks are never damaged. An interactive cooldown hologram requires players to wait before looting the core, and once claimed, the fake destroyed scenery destabilizes and cleanly vanishes after 30 seconds.</p>

<hr>

<h3>Key Features</h3>
<ul>
<li><strong>Cinematic 5-Stage Lifecycle:</strong> Omen announcements, atmospheric crossing, explosive impact, interactive exploration with cooldown, and vanishing fake scenery.</li>
<li><strong>100% Virtual Packet Craters:</strong> Impact craters and destroyed terrain are sent as client-side fake blocks via packets. Real server blocks are never altered, eliminating map corruption and griefing risks.</li>
<li><strong>Interactive Cooldown Hologram:</strong> Meteor cores remain locked during an initial cooling period (default 30 seconds), displaying a live packet countdown hologram that updates every second before unlocking.</li>
<li><strong>30s Post-Loot Vanishing Scenery:</strong> After looting the core, the unstable fake crater scenery cleanly destabilizes and vanishes after 30 seconds, restoring the original landscape via packets.</li>
<li><strong>High-Altitude Cinematic Trajectories:</strong> Multi-layer particle streams (dense core, rotating double-helix trail, sonic smoke cone, and sparks) visible up to 256+ blocks away from altitudes of 160 to 240 blocks.</li>
<li><strong>3D Localized Spatial Audio:</strong> Sounds originate realistically from the meteor's exact 3D coordinates in the sky and on the ground with natural distance attenuation, rather than artificial global playback.</li>
<li><strong>Custom Meteor Types:</strong> Built-in configurations for Ember, Frostfall, Voidfall, and Worldbreaker, plus full support for custom YAML definitions.</li>
<li><strong>Extensive Loot System:</strong> Chests with weighted item rolls, enchantments, custom names, lore, console commands, and collectible fragments.</li>
<li><strong>Folia & Paper Native:</strong> Built from the ground up to respect Folia threaded region scheduling alongside Spigot and Paper.</li>
<li><strong>WorldGuard Support:</strong> Automatically checks regions to prevent meteor strikes in protected plots and towns.</li>
</ul>

<hr>

<h3>Meteor Types</h3>
<table>
<tr>
<th>Meteor</th>
<th>Rarity</th>
<th>Visual Model</th>
<th>Crater Materials</th>
<th>Highlights</th>
</tr>
<tr>
<td><strong>Ember</strong></td>
<td>Common</td>
<td>Fireball</td>
<td>Magma, Basalt, Obsidian</td>
<td>Frequent strikes, basic ores, ember shards.</td>
</tr>
<tr>
<td><strong>Frostfall</strong></td>
<td>Rare</td>
<td>End Crystal</td>
<td>Blue Ice, Packed Ice, Snow</td>
<td>Glacial trail, cold bow weaponry, frost crystals.</td>
</tr>
<tr>
<td><strong>Voidfall</strong></td>
<td>Epic</td>
<td>Dragon Fireball</td>
<td>Crying Obsidian, End Stone</td>
<td>Dragon breath trail, netherite scrap, void essence.</td>
</tr>
<tr>
<td><strong>Worldbreaker</strong></td>
<td>Legendary</td>
<td>Fireball Shockwave</td>
<td>Ancient Debris, Obsidian</td>
<td>Massive blast, ancient debris, star-forged pickaxe.</td>
</tr>
</table>

<hr>

<h3>Commands & Permissions</h3>
<table>
<tr>
<th>Command</th>
<th>Permission</th>
<th>Description</th>
</tr>
<tr>
<td><code>/meteorevents start [type]</code></td>
<td><code>meteorevents.start</code></td>
<td>Starts a meteor event at player or random location.</td>
</tr>
<tr>
<td><code>/meteorevents stop</code></td>
<td><code>meteorevents.stop</code></td>
<td>Terminates all active meteor strikes and cleans up entities.</td>
</tr>
<tr>
<td><code>/meteorevents next</code></td>
<td><code>meteorevents.info</code></td>
<td>Displays estimated time until next automatic event check.</td>
</tr>
<tr>
<td><code>/meteorevents types</code></td>
<td><code>meteorevents.info</code></td>
<td>Lists all available meteor types.</td>
</tr>
<tr>
<td><code>/meteorevents test [type]</code></td>
<td><code>meteorevents.test</code></td>
<td>Runs a safe visual test without modifying terrain.</td>
</tr>
<tr>
<td><code>/meteorevents info</code></td>
<td><code>meteorevents.info</code></td>
<td>Shows active event stages, locations, and time remaining.</td>
</tr>
<tr>
<td><code>/meteorevents reload</code></td>
<td><code>meteorevents.reload</code></td>
<td>Reloads all configuration files safely.</td>
</tr>
</table>

<p><em>Global administrator permission:</em> <code>meteorevents.admin</code></p>

<hr>

<h3>Requirements & Compatibility</h3>
<ul>
<li><strong>Minecraft Versions:</strong> 1.16.5 through 26.x</li>
<li><strong>Server Software:</strong> Paper, Spigot, Folia</li>
<li><strong>Java:</strong> Java 8, 11, 17, 21, or 25</li>
<li><strong>Dependencies:</strong> PacketEvents 2.14.0+ (Required for packet visual effects), WorldGuard 7+ (Optional)</li>
</ul>

<hr>

<h3>Installation</h3>
<ol>
<li>Install <strong>PacketEvents</strong> in your server <code>plugins/</code> folder.</li>
<li>Place <strong>MeteorEvents.jar</strong> into your <code>plugins/</code> folder.</li>
<li>Restart your server to generate the configuration files.</li>
<li>Customize <code>config.yml</code>, <code>meteors.yml</code>, <code>rewards.yml</code>, <code>messages.yml</code>, and <code>protections.yml</code> to your server's needs.</li>
<li>Run <code>/meteorevents reload</code> to apply your changes.</li>
</ol>
