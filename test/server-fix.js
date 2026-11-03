// Glacia and the other client-only calls on a real dedicated server.
// Usage: NODE_PATH=<dir with minecraft-protocol> node server-fix.js <server dir> <port> <fixed|baseline>
//   baseline = DedicatedServerFix is not installed, the server must crash; fixed = it is, the server must survive.
// The server needs difficulty above peaceful (Glacia's portal only misbehaves then) and the rmf helper mod.

const L = require('./lib');

L.init(process.argv[2], parseInt(process.argv[3], 10));
const EXPECT = process.argv[4];
const { check, results, cmd, logSize, logSince, waitFor, console_, where, sleep, Bot } = L;

async function moveTo(bot, dim, x, y, z) {
  const teleports = bot.teleports;
  cmd(`rmf move ${bot.name} ${dim} ${x} ${y} ${z}`);
  await waitFor(() => bot.dim === dim && bot.teleports > teleports, 60000, `${bot.name} moved to ${dim}`);
  await sleep(1000);
}

async function main() {
  const start = logSize();
  const modsOut = await console_('rmf mods', /RMF mods (\S+)/);
  const mods = /RMF mods (\S+)/.exec(modsOut)[1].split('|').filter(Boolean).map((m) => m.split('='));
  const dimsOut = await console_('rmf dims', /RMF dim -?\d+ Glacia/);
  const glacia = +/RMF dim (-?\d+) Glacia/.exec(dimsOut)[1];
  let bob = new Bot('Bob', mods);
  await bob.ready();
  console.log(`Glacia is dimension ${glacia}, expecting: ${EXPECT}`);

  const side = await console_('rmf sidecheck Bob', /RMF sidecheck \w+/);
  const sideLine = side.split('\n').filter((l) => /RMF sidecheck/.test(l))[0].replace(/^\[.*?\]: /, '');
  if (EXPECT === 'baseline') {
    check('without the fix the method is missing', /RMF sidecheck missing/.test(side), sideLine);
    cmd(`rmf move Bob ${glacia} 100.5 100 100.5`);
    await waitFor(() => /NoSuchMethodError: net\.minecraft\.block\.Block\.func_149637_q/.test(logSince(start)), 60000, 'the crash');
    check('entering Glacia crashes the server', true);
    bob.quit();
  } else {
    check('restored methods answer correctly', /stone=true glass=false duration=0 swing=0\.0 food=7 cloud=128\.0/.test(side), sideLine);

    // A player inside Glacia, its crystals ticking
    await moveTo(bob, glacia, 100.5, 100, 100.5);
    await sleep(60000);
    let w = await where('Bob');
    check('60 s inside Glacia without a crash', w.dim === glacia && !/NoSuchMethodError/.test(logSince(start)), `dim=${w.dim}`);

    // Log in again inside Glacia, which crashed the server for good before
    bob.quit();
    await sleep(2000);
    bob = new Bot('Bob', mods);
    await bob.ready();
    await sleep(20000);
    w = await where('Bob');
    check('logging in again inside Glacia is fine', w.dim === glacia && !/NoSuchMethodError/.test(logSince(start)), `dim=${w.dim}`);

    // A world save used to log thousands of "missing a mapping" errors
    await console_('save-all', /Saved the world/, 60000);
    await sleep(2000);
    check('world save logs no missing mapping', !/missing a mapping/.test(logSince(start)));

    // Placing a Glacia door
    const itemsOut = await console_('rmf items door', /RMF item|^$/, 3000).catch(() => '');
    await sleep(500);
    const door = (/RMF item (Glacia:\S*)/i.exec(logSince(start)) || [])[1];
    console.log('door item:', door);
    await console_(`rmf place ${glacia} 100 99 100 minecraft:stone`, /RMF placed/);
    const used = await console_(`rmf useitem Bob ${door} 100 99 100`, /RMF used|Couldn't process/);
    check('placing a Glacia door works', /RMF used .* result=true/.test(used) && !/NoSuchMethodError/.test(logSince(start)), used.split('\n').filter((l) => /RMF used|Couldn't/.test(l)).join(' ').replace(/^\[.*?\]: /, ''));

    // A lit portal in the Overworld with mob spawning on: rare random ticks, so force many
    await console_('rmf place 0 120 70 120 Glacia:Glacialportal', /RMF placed/);
    await console_('rmf tickblock 0 120 70 120 30000', /RMF ticked/, 60000);
    await sleep(1000);
    w = await where('Bob');
    check('a lit Glacia portal does not crash the server', !/NoSuchMethodError/.test(logSince(start)), `alive, Bob dim=${w.dim}`);
    bob.quit();
  }
  await sleep(500);
  const failed = results.filter((r) => !r.ok);
  console.log(`\n${results.length - failed.length}/${results.length} checks passed`);
  process.exit(failed.length ? 1 : 0);
}

main().catch((e) => {
  console.log('ERROR', e.stack || e);
  process.exit(2);
});
