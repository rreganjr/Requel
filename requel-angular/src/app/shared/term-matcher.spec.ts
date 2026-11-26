import { findTermsUsed } from './term-matcher';
import { GlossaryTermDto } from '../models/term';

function term(id: number, name: string, text: string | null = `${name} means something.`,
    canonicalTermId: number | null = null): GlossaryTermDto {
  return { id, version: 1, name, text, createdBy: null, canonicalTermId, canonicalTermName: null,
    alternateTerms: null, referers: null };
}

const HOST = term(1, 'Host', 'The person running the room.');
const VIDEO_ROOM = term(2, 'video room', 'Where a meeting happens.');
const ROOM = term(3, 'room', 'Any space.');
const TERMS = [ROOM, HOST, VIDEO_ROOM];

const names = (found: { name: string }[]) => found.map(t => t.name);

describe('findTermsUsed (#506)', () => {
  it('matches the longest term and hides the shorter one inside it', () => {
    expect(names(findTermsUsed(TERMS, 'End a meeting', 'The Host ends the video room'))).toEqual(['Host', 'video room']);
  });

  it('still counts the shorter term where it stands alone', () => {
    expect(names(findTermsUsed(TERMS, '', 'Leave the video room, then the room.'))).toEqual(['video room', 'room']);
  });

  it('matches whole words in any case', () => {
    expect(names(findTermsUsed([ROOM], '', 'ROOM one'))).toEqual(['room']);
    expect(findTermsUsed([ROOM], '', 'two rooms, a bedroom, room2, roommate')).toEqual([]);
  });

  it('treats punctuation, apostrophes and hyphens as boundaries, as the server does', () => {
    const dryRun = term(4, 'dry-run event');
    expect(names(findTermsUsed([dryRun], '', "the Dry-run event's slides"))).toEqual(['dry-run event']);
    expect(findTermsUsed([dryRun], '', 'two dry-run events')).toEqual([]);
    expect(names(findTermsUsed([ROOM], '', '(room).'))).toEqual(['room']);
    expect(names(findTermsUsed([term(5, 'café')], '', 'Le Café!'))).toEqual(['café']);
  });

  it('lists each term once, in order of first appearance, the name before the text', () => {
    expect(names(findTermsUsed(TERMS, 'room for a Host', 'Host, room, Host'))).toEqual(['room', 'Host']);
  });

  it('never matches across the name and the text', () => {
    expect(findTermsUsed([VIDEO_ROOM], 'video', 'room')).toEqual([]);
  });

  it('takes a term name literally, regex characters included', () => {
    const cpp = term(6, 'C++');
    expect(names(findTermsUsed([cpp], '', 'Written in C++ today'))).toEqual(['C++']);
    expect(findTermsUsed([term(7, 'a.b')], '', 'axb')).toEqual([]);
  });

  it('reports an alternate with its canonical name, definition and link target', () => {
    const meeting = term(10, 'meeting', 'A scheduled session.');
    const call = term(11, 'call', 'Old word.', 10);
    const [found] = findTermsUsed([meeting, call], '', 'Join the call');
    expect(found).toEqual({ id: 11, name: 'call', targetId: 10, canonicalName: 'meeting',
      definition: 'A scheduled session.' });
  });

  it('leaves out the excluded term', () => {
    expect(names(findTermsUsed(TERMS, 'Host', 'A Host runs the room.', 1))).toEqual(['room']);
  });

  it('finds nothing in blank input, and ignores blank term names', () => {
    expect(findTermsUsed(TERMS, null, undefined)).toEqual([]);
    expect(findTermsUsed([term(8, '   ')], '', 'anything')).toEqual([]);
  });

  it('says so when a term has no definition', () => {
    expect(findTermsUsed([term(9, 'gate', null)], '', 'the gate')[0].definition).toBe('No definition yet.');
  });
});
