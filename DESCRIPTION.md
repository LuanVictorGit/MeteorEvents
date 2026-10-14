<center>
<h2>MeteorEvents</h2>
<p><strong>Cinematic meteor strike events featuring real-time packet trajectories, rotating display entities, procedural craters, interactive cores, and customizable rewards.</strong></p>
<p>Author: <strong>lHawk_</strong> | Contact: <a href="https://minebuilders.com.br/discord">https://minebuilders.com.br/discord</a></p>
</center>

<hr>

<h3>1. Introduction & Overview</h3>
<p><strong>MeteorEvents</strong> introduces cinematic celestial events to Minecraft servers. Rather than instant unannounced block explosions, meteors traverse the sky along configurable atmospheric entry trajectories rendered via <strong>PacketEvents</strong>. Falling meteors feature spinning volumetric display entities (giant fire charges, snowballs, ender eyes) tumbling along multi-axis rotational curves with high-altitude supersonic trails visible up to 256+ blocks away.</p>
<p>Upon impact, the plugin creates virtual packet craters and towering plumes of smoke without griefing or corrupting server world saves. An interactive cooldown hologram protects the core during a cooling period before players can open the loot inventory. Once claimed, the destabilizing crater remnants vanish cleanly after 30 seconds.</p>

<hr>

<h3>2. Features & Detailed Functionality</h3>
<ul>
<li><strong>5-Stage Event Lifecycle:</strong> Warning omen sky tremors, atmospheric crossing, explosive shockwave impact, interactive cooldown exploration, and post-loot vanishing cleanup.</li>
<li><strong>Spinning Packet Display Entities:</strong> Meteors are rendered as client-side packet entities with dynamic scale (up to 5.5x) and tumble rotations (yaw +28°, pitch +18° per tick).</li>
<li><strong>100% Virtual Packet Craters:</strong> Impact craters and destroyed terrain are delivered through client-side packet changes. Real server blocks are never permanently modified.</li>
<li><strong>Towering Smoke Plumes:</strong> Configurable columns of campfire signal smoke billow from the crater perimeter throughout the exploration phase.</li>
<li><strong>Directional 3D Audio with Auto-Cutoff:</strong> Flight audio originates from the exact 3D coordinates of the falling meteor. Flight audio immediately halts upon impact, preventing infinite audio loops.</li>
<li><strong>Interactive Countdown Holograms:</strong> Floating ArmorStand packet holograms count down the cooling period and vanishing phases in real time.</li>
<li><strong>Loot Chest & Fragment System:</strong> Interactive core chest with custom display names, weighted item rolls, enchantments, and mineable meteorite fragments.</li>
<li><strong>Folia Multi-Threaded Region Scheduling:</strong> Native support for Folia regionized threading as well as Spigot and Paper.</li>
</ul>

<hr>

<h3>3. Requirements & Dependencies</h3>
<table>
<tr>
<th>Component</th>
<th>Requirement</th>
<th>Status</th>
</tr>
<tr>
<td>Java</td>
<td>Java 8, 11, 17, 21, or 25</td>
<td>Universal bytecode</td>
</tr>
<tr>
<td>PacketEvents</td>
<td>2.14.0 or newer</td>
<td>Required for packet entities & craters</td>
</tr>
<tr>
<td>WorldGuard</td>
<td>7.0 or newer</td>
<td>Optional soft dependency</td>
</tr>
</table>

<hr>

<h3>4. Supported Platforms & Versions</h3>
<ul>
<li><strong>Supported Minecraft Versions:</strong> 1.16.5, 1.17.x, 1.18.x, 1.19.x, 1.20.x, 1.21.x, and 26.x</li>
<li><strong>Supported Server Platforms:</strong> Paper, Spigot, Purpur, and Folia</li>
</ul>

<hr>

<h3>5. Installation</h3>
<ol>
<li>Download and install <strong>PacketEvents 2.14.0+</strong> into your server's <code>plugins/</code> folder.</li>
<li>Place <strong>MeteorEvents.jar</strong> into your <code>plugins/</code> folder.</li>
<li>Restart your server to generate the configuration files.</li>
<li>Configure your desired settings in <code>config.yml</code>, <code>meteors.yml</code>, <code>messages.yml</code>, <code>rewards.yml</code>, and <code>protections.yml</code>.</li>
<li>Run <code>/meteorevents reload</code> to apply updates without restarting.</li>
</ol>

<hr>

<h3>6. Commands & Usage Examples</h3>
<table>
<tr>
<th>Command</th>
<th>Permission</th>
<th>Description</th>
</tr>
<tr>
<td><code>/meteorevents start [type]</code></td>
<td><code>meteorevents.start</code></td>
<td>Spawns a meteor event at the player's location or random coordinates.</td>
</tr>
<tr>
<td><code>/meteorevents stop</code></td>
<td><code>meteorevents.stop</code></td>
<td>Terminates all active events and removes client packet entities immediately.</td>
</tr>
<tr>
<td><code>/meteorevents remove</code></td>
<td><code>meteorevents.remove</code></td>
<td>Removes all fallen and active meteors immediately and regenerates craters.</td>
</tr>
<tr>
<td><code>/meteorevents test [type]</code></td>
<td><code>meteorevents.test</code></td>
<td>Executes a safe visual flight test without creating crater blocks.</td>
</tr>
<tr>
<td><code>/meteorevents next</code></td>
<td><code>meteorevents.info</code></td>
<td>Shows the countdown until the next automated meteor strike attempt.</td>
</tr>
<tr>
<td><code>/meteorevents types</code></td>
<td><code>meteorevents.info</code></td>
<td>Lists all loaded meteor types defined in <code>meteors.yml</code>.</td>
</tr>
<tr>
<td><code>/meteorevents info</code></td>
<td><code>meteorevents.info</code></td>
<td>Displays real-time stage, coordinates, and timers for active meteors.</td>
</tr>
<tr>
<td><code>/meteorevents reload</code></td>
<td><code>meteorevents.reload</code></td>
<td>Reloads all configuration files and validates values.</td>
</tr>
</table>

<hr>

<h3>7. Permissions</h3>
<table>
<tr>
<th>Permission Node</th>
<th>Default</th>
<th>Description</th>
</tr>
<tr>
<td><code>meteorevents.admin</code></td>
<td>op</td>
<td>Grants access to all administrative commands and controls.</td>
</tr>
<tr>
<td><code>meteorevents.start</code></td>
<td>op</td>
<td>Allows triggering manual meteor events.</td>
</tr>
<tr>
<td><code>meteorevents.stop</code></td>
<td>op</td>
<td>Allows halting active events and clearing entities.</td>
</tr>
<tr>
<td><code>meteorevents.remove</code></td>
<td>op</td>
<td>Allows removing all fallen meteors and regenerating craters.</td>
</tr>
<tr>
<td><code>meteorevents.reload</code></td>
<td>op</td>
<td>Allows reloading configurations on the fly.</td>
</tr>
<tr>
<td><code>meteorevents.test</code></td>
<td>op</td>
<td>Allows running safe visual trajectory tests.</td>
</tr>
<tr>
<td><code>meteorevents.info</code></td>
<td>true</td>
<td>Allows viewing upcoming strike times and type lists.</td>
</tr>
</table>

<hr>

<h3>8. Configuration Options & Examples</h3>
<p>MeteorEvents features 5 organized YAML configuration files with full documentation comments:</p>
<details>
<summary><strong>config.yml (Core Settings)</strong></summary>
<pre><code># Master toggle to enable automated meteor events.
enabled: true

scheduling:
  check-interval-seconds: 60
  chance-percentage: 25.0
  min-cooldown-seconds: 600
  max-concurrent-events: 2
  min-players-online: 1
  allowed-worlds:
    - world

performance:
  view-distance: 256
  update-rate-ticks: 1
  max-particles-per-tick: 80

stages:
  omen-duration-seconds: 10
  core-lock-seconds: 30
  loot-duration-seconds: 120
  post-loot-vanish-seconds: 30
  default-exploration-seconds: 600</code></pre>
</details>

<details>
<summary><strong>meteors.yml (Meteor Customization)</strong></summary>
<pre><code>meteors:
  ember:
    display-name: "&6&lEmber Meteor"
    rarity: "COMMON"
    weight: 50
    speed: 0.85
    start-height: 160
    horizontal-offset: 120
    visual-entity: "FIRE_CHARGE"
    visual-entity-scale: 3.8
    smoke-plumes-enabled: true
    smoke-plumes-count: 10
    trail-particles:
      - "FLAME"
      - "LAVA"
      - "CAMPFIRE_COSY_SMOKE"
    crossing-sound: "ENTITY_FIREWORK_ROCKET_BLAST"
    impact-sound: "ENTITY_GENERIC_EXPLODE"
    crater-radius: 4
    crater-depth: 3
    exploration-duration-seconds: 600
    lock-duration-seconds: 30</code></pre>
</details>

<hr>

<h3>9. Integrations & Optional Dependencies</h3>
<ul>
<li><strong>WorldGuard 7+:</strong> Automatically detects protected regions, plots, and claims. Meteors will never land inside regions where block-break or explosions are restricted.</li>
<li><strong>PlaceholderAPI:</strong> Supported across message templates and broadcasts using <code>{meteor}</code>, <code>{world}</code>, <code>{x}</code>, <code>{y}</code>, <code>{z}</code>, <code>{player}</code>, and <code>{time}</code>.</li>
</ul>

<hr>

<h3>10. Performance Considerations</h3>
<ul>
<li><strong>Zero Terrain Churn:</strong> Virtual packet craters avoid writing thousands of block state updates to chunk storage or disk, avoiding save stalls.</li>
<li><strong>Pre-Calculated Flight Vectors:</strong> Particle trajectories, spiral spirals, and supersonic cones are pre-calculated once per tick rather than duplicated across player loops.</li>
<li><strong>Distance-Filtered Packet Distribution:</strong> All particle and entity packets are selectively dispatched strictly to viewers within configurable view distance radii.</li>
<li><strong>Folia Native Region Safety:</strong> Region-bound operations are dispatched directly through regional schedulers without blocking adjacent world threads.</li>
</ul>

<hr>

<h3>11. Known Limitations</h3>
<ul>
<li>Packet display scaling (3.8x to 5.5x) requires clients connecting on 1.19.4 or higher for full visual model magnification. On older clients, PacketEvents smoothly falls back to standard projectile packets.</li>
<li>Because crater blocks are client-side virtual packets, players may experience momentary client resynchronization if they attempt to mine protected non-fragment blocks.</li>
</ul>

<hr>

<h3>12. Troubleshooting & FAQ</h3>
<p><strong>Q: Why does the meteor sound continue after landing?</strong><br>
A: Make sure you are running version 1.1.0+. In this version, non-looping audio cues are utilized and <code>stopSound</code> packets are dispatched on impact.</p>
<p><strong>Q: Why does the crater disappear after 30 seconds?</strong><br>
A: The crater is a virtual packet display. You can customize this timer via <code>stages.post-loot-vanish-seconds</code> in <code>config.yml</code>.</p>
<p><strong>Q: Can players grief the crater before looting?</strong><br>
A: No. The impact area is protected against breaking, placing, bucket usage, and pistons until the core is looted.</p>

<hr>

<center>
<p><small>For support, feature requests, and updates, visit our community: <a href="https://minebuilders.com.br/discord">https://minebuilders.com.br/discord</a></small></p>
</center>
