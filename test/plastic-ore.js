// Generates the same chunks of a world with a fixed seed and counts blocks, to see whether Plastic Ore appears.
// Usage: NODE_PATH=<dir with minecraft-protocol> node plastic-ore.js <server dir> <off|on>
//   off = no Plastic Ore may generate, on = it must generate. Vanilla ores must generate either way.

const L = require('./lib');

L.init(process.argv[2], 0);
const EXPECT = process.argv[3];
const { check, results, console_ } = L;
const RADIUS = 8; // (2*8+1)^2 = 289 chunks

async function count(block) {
  const re = new RegExp(`RMF count ${block} = (\\d+)`);
  const out = await console_(`rmf count ${block} 0 0 ${RADIUS}`, re, 240000);
  return +re.exec(out)[1];
}

async function main() {
  const plastic = await count('MedicineCraft:PlasticOre');
  const iron = await count('minecraft:iron_ore');
  const coal = await count('minecraft:coal_ore');
  console.log(`plastic=${plastic} iron=${iron} coal=${coal}`);
  if (EXPECT === 'off') {
    check('no Plastic Ore generated', plastic === 0, `plastic=${plastic}`);
  } else {
    check('Plastic Ore generated', plastic > 0, `plastic=${plastic}`);
  }
  check('vanilla ores still generate', iron > 0 && coal > 0, `iron=${iron} coal=${coal}`);
  const failed = results.filter((r) => !r.ok);
  console.log(`\n${results.length - failed.length}/${results.length} checks passed`);
  process.exit(failed.length ? 1 : 0);
}

main().catch((e) => {
  console.log('ERROR', e.stack || e);
  process.exit(2);
});
