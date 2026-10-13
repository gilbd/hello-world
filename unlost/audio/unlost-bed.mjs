// "Unlost" bed: 12 bars of 4/4 at 96 bpm (one bar = 2.5 s), mapped to the film.
// bar 0 playful | bar 1 the turn: the music stops on "then she wasn't" | bars 2-3 alone (strings, low heartbeat)
// bar 4 the scan: a rising music-box arpeggio | bars 5-7 the call, building | bars 8-10 reunion + range: the theme returns | bar 11 home
import { line } from "/root/.claude/skills/take-one/scripts/music/plan.mjs";
const BPB = 4;
const ch = { C: ["C3", "G3", "E4"], F: ["F2", "C3", "A3"], G: ["G2", "D3", "B3"], Am: ["A2", "E3", "C4"] };
const arp = (c) => { const [a, b, d] = ch[c]; return `${a}:.5 ${b}:.5 ${d}:.5 ${b}:.5 ${a}:.5 ${b}:.5 ${d}:.5 ${b}:.5`; };
export default () => {
  const melody = line(0, [
    "E5:.5 G5:.5 C6:.5 G5:.5 A5:1 r:1", // 0 stops on "then she wasn't" (1.9 s)
    "r:4", "r:4", "r:4", //            1-3 the turn, then silence above the strings
    "C5:.5 E5:.5 G5:.5 C6:.5 E5:.5 G5:.5 C6:.5 E6:.5", // 4 the scan
    "D6:2 r:2", "r:4", //                   5-6
    "G5:.5 A5:.5 B5:.5 D6:.5 G5:2", //      7 relief
    "E5:.5 G5:.5 C6:.5 G5:.5 A5:1 G5:1", // 8 theme back
    "F5:.5 A5:.5 C6:.5 A5:.5 G5:2", //      9
    "E5:.5 G5:.5 C6:.5 G5:.5 A5:.5 C6:.5 D6:1", // 10
    "C6:4", //                              11
  ].join(" | "), { role: "melody", v: 0.72, bpb: BPB });
  const harp = line(0, ["C3:.5 G3:.5 E4:.5 G3:.5 C3:.5 G3:.5 r:1", "r:4", "r:4", "r:4", arp("C"), arp("F"), arp("G"), arp("G"), arp("C"), arp("F"), arp("G"), "[C3 G3 E4]:4"].join(" | "), { role: "accomp", v: 0.6, bpb: BPB, roll: 0.03 });
  const bass = line(0, ["C2:3 r:1", "A1:1 r:1 A1:1 r:1", "A1:1 r:1 A1:1 r:1", "F1:1 r:1 F1:1 r:1", "C2:4", "F2:4", "G2:4", "G2:4", "C2:4", "F2:4", "G2:4", "C2:4"].join(" | "), { role: "bass", v: 0.6, bpb: BPB });
  const pad = line(4, ["[A3 C4 E4]:4", "[A3 C4 E4]:4", "[F3 A3 C4]:4", "[G3 C4 E4]:4", "[A3 C4 F4]:4", "[B3 D4 G4]:4", "[B3 D4 F4]:4"].join(" | "), { role: "inner", v: 0.42, bpb: BPB, roll: 0.02 });
  const harmony = ["C", "C", "Am", "F", "C", "F", "G", "G", "C", "F", "G", "C"].map((name, i) => ({ t: i * 4, name }));
  return {
    title: "Unlost bed", seed: 7311, tail: 0.5, harmony,
    plan: { style: "folk", tempo: 96, meter: "4/4", phraseBars: 4, sections: [
      { id: "play", bars: 2, mood: "playful", key: "C", mode: "major", melody: ["hook"], dyn: [0.55, 0.5] },
      { id: "lost", bars: 2, mood: "wistful", key: "C", mode: "major", melody: ["drone"], dyn: [0.35, 0.35] },
      { id: "scan", bars: 4, mood: "curious", key: "C", mode: "major", melody: ["arpeggio"], dyn: [0.45, 0.7] },
      { id: "home", bars: 4, mood: "joy", key: "C", mode: "major", melody: ["hook"], dyn: [0.75, 0.6], ending: "tail" },
    ] },
    parts: [
      { id: "box", inst: "musicBox", role: "melody", notes: melody },
      { id: "harp", inst: "harp", role: "accomp", notes: harp, gainDb: -3 },
      { id: "bass", inst: "bass", role: "bass", notes: bass, gainDb: -6 },
      { id: "str", inst: "strings", role: "inner", notes: pad, gainDb: -9, opts: { attack: 0.7, release: 1.2, bright: 0.5, width: 0.6 }, send: 1.1 },
    ],
  };
};
