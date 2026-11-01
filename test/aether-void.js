// End-to-end test of falling out of the Aether on a real Forge 1.7.10 server with the real Aether jar.
// Usage: NODE_PATH=<dir with minecraft-protocol> node aether-void.js <server dir> <port> <void|overworld>
//   void      = Aether Void is active: players and mobs must fall into the void
//   overworld = original behaviour expected (mod not installed, or switched off in its config)

const L = require('./lib');

L.init(process.argv[2], parseInt(process.argv[3], 10));
const EXPECT = process.argv[4];
const { check, results, cmd, logSize, logSince, waitFor, console_, where, sleep, Bot, tp } = L;

async function moveTo(bot, dim, x, y, z) {
  const teleports = bot.teleports;
  cmd(`rmf move ${bot.name} ${dim} ${x} ${y} ${z}`);
  await waitFor(() => bot.dim === dim && bot.teleports > teleports, 60000, `${bot.name} moved to ${dim}`);
  await sleep(1000);
}

async function tracked() {
  const out = await console_('rmf tracked', /RMF tracked dim=(-?\d+) y=(-?[\d.]+) dead=(\w+)/);
  const m = /RMF tracked dim=(-?\d+) y=(-?[\d.]+) dead=(\w+)/.exec(out);
  return { dim: +m[1], y: +m[2], dead: m[3] === 'true' };
}

async function main() {
  const modsOut = await console_('rmf mods', /RMF mods (\S+)/);
  const mods = /RMF mods (\S+)/.exec(modsOut)[1].split('|').filter(Boolean).map((m) => m.split('='));
  const dimsOut = await console_('rmf dims', /RMF dim -?\d+ the_aether/);
  const aether = +/RMF dim (-?\d+) the_aether/.exec(dimsOut)[1];
  console.log(`Aether is dimension ${aether}, expecting: ${EXPECT}`);

  const alice = new Bot('Alice', mods);
  await alice.ready();
  const bob = new Bot('Bob', mods);
  await bob.ready();

  // A cleared shaft, so nothing can stop a fall. Bob stays above it on a platform and keeps the chunks loaded.
  const X = 200;
  const Z = 200;
  await console_(`rmf shaft ${aether} ${X} ${Z}`, /RMF shaft cleared/, 60000);
  await console_(`rmf place ${aether} ${X + 12} 69 ${Z} minecraft:obsidian`, /RMF placed/);
  await moveTo(bob, aether, X + 12.5, 70, Z + 0.5);
  await moveTo(alice, aether, X + 0.5, 70, Z + 0.5);
  check('both players are in the Aether', alice.dim === aether && bob.dim === aether);

  // ---- Player falls ----
  let mark = logSize();
  const respawns = alice.respawns.length;
  alice.falling = true;
  if (EXPECT === 'void') {
    await waitFor(() => alice.pos.y < -30, 15000, 'Alice below y=-30');
    const mid = await where('Alice');
    check('player: still in the Aether at y=-30 (no teleport at y=-12)', mid.dim === aether && mid.y < -12 && alice.respawns.length === respawns, `dim=${mid.dim} y=${mid.y} hp=${mid.hp}`);
    await waitFor(() => /Alice fell out of the world/.test(logSince(mark)), 15000, 'Alice dying in the void');
    check('player: killed by the void', true, logSince(mark).split('\n').filter((l) => /fell out of the world/.test(l))[0].replace(/^\[.*?\]: /, ''));
    await waitFor(() => alice.dim === 0, 10000, 'Alice respawning');
    await sleep(1000);
    const after = await where('Alice');
    check('player: respawns in the Overworld with full health', after.dim === 0 && after.hp === 20, `dim=${after.dim} hp=${after.hp}`);
  } else {
    await waitFor(() => alice.dim === 0, 15000, 'Alice teleported to the Overworld');
    await sleep(1000);
    const after = await where('Alice');
    check('player: teleported to the Overworld alive', after.dim === 0 && after.hp > 0 && !/fell out of the world/.test(logSince(mark)), `dim=${after.dim} hp=${after.hp}`);
  }

  // ---- Mob falls ----
  await console_(`rmf spawn pig ${aether} ${X + 0.5} 40 ${Z + 0.5}`, /RMF spawned pig/);
  await sleep(9000);
  let t = await tracked();
  if (EXPECT === 'void') {
    check('mob: died in the void, not moved', t.dim === aether && t.dead, JSON.stringify(t));
  } else {
    check('mob: moved to the Overworld alive', t.dim === 0 && !t.dead, JSON.stringify(t));
  }

  // ---- Dropped item falls (the Aether never moved items) ----
  await console_(`rmf spawn item ${aether} ${X + 0.5} 40 ${Z + 0.5}`, /RMF spawned item/);
  await sleep(9000);
  t = await tracked();
  check('item: destroyed by the void, not moved', t.dim === aether && t.dead, JSON.stringify(t));

  // ---- Mount with a rider falls (only meaningful when nothing teleports: the rider stays seated) ----
  if (EXPECT === 'void') {
    await console_(`rmf spawn pig ${aether} ${X + 0.5} 40 ${Z + 0.5}`, /RMF spawned pig/);
    await moveTo(bob, aether, X + 0.5, 41, Z + 0.5);
    mark = logSize();
    await console_('rmf mount Bob', /RMF mounted Bob/);
    await waitFor(() => /Bob fell out of the world/.test(logSince(mark)), 25000, 'Bob dying in the void on the mount');
    t = await tracked();
    check('mount: rider and mount both fall into the void', t.dim === aether, JSON.stringify(t));
    await waitFor(() => bob.dim === 0, 10000, 'Bob respawning');
    check('mount: rider respawns in the Overworld', bob.dim === 0);
  }

  // ---- The portal itself must keep working in both directions ----
  const g = Math.round(alice.pos.y);
  const sx = Math.floor(alice.pos.x) + 6;
  const sz = Math.floor(alice.pos.z);
  await console_(`rmf place 0 ${sx} ${g} ${sz} aether_legacy:aether_portal`, /RMF placed/);
  await console_(`rmf place 0 ${sx} ${g + 1} ${sz} aether_legacy:aether_portal`, /RMF placed/);
  await sleep(3000);
  await tp(alice, sx + 0.5, g, sz + 0.5);
  await waitFor(() => alice.dim === aether, 30000, 'Alice through the portal to the Aether');
  await where('Alice', 120000);
  check('portal: Overworld to Aether works', alice.dim === aether);

  const px = X + 40;
  await console_(`rmf shaft ${aether} ${px} ${Z}`, /RMF shaft cleared/, 60000);
  await console_(`rmf place ${aether} ${px} 69 ${Z} minecraft:obsidian`, /RMF placed/);
  await console_(`rmf place ${aether} ${px} 70 ${Z} aether_legacy:aether_portal`, /RMF placed/);
  await console_(`rmf place ${aether} ${px} 71 ${Z} aether_legacy:aether_portal`, /RMF placed/);
  await console_(`rmf place ${aether} ${px + 2} 69 ${Z} minecraft:obsidian`, /RMF placed/);
  await moveTo(alice, aether, px + 2.5, 70, Z + 0.5);
  await sleep(3000);
  await tp(alice, px + 0.5, 70, Z + 0.5);
  await waitFor(() => alice.dim === 0, 30000, 'Alice through the portal to the Overworld');
  check('portal: Aether to Overworld works', alice.dim === 0);

  check('no bot was kicked', !alice.kicked && !bob.kicked, `${alice.kicked || ''} ${bob.kicked || ''}`);
  alice.quit();
  bob.quit();
  await sleep(500);
  const failed = results.filter((r) => !r.ok);
  console.log(`\n${results.length - failed.length}/${results.length} checks passed`);
  process.exit(failed.length ? 1 : 0);
}

main().catch((e) => {
  console.log('ERROR', e.stack || e);
  process.exit(2);
});
