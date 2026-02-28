import { describe, expect, it } from 'vitest';
import { formatStreamText, parseFinalEvent, parseStructuredResult } from './factcheck';

describe('formatStreamText', () => {
  it('inserts line breaks for labels and preserves spacing', () => {
    const input = 'VERIFICATION: TRUE CONFIDENCE: 0.95 EXPLANATION: Ok SOURCE: X';
    const result = formatStreamText(input);
    expect(result).toContain('VERIFICATION: TRUE');
    expect(result).toMatch(/CONFIDENCE:\s*0\.\s*95/);
    expect(result).toContain('EXPLANATION: Ok');
    expect(result).toContain('SOURCE: X');
  });
});

describe('parseStructuredResult', () => {
  it('maps TRUE to VERIFIED and parses confidence', () => {
    const input = 'VERIFICATION: TRUE\nCONFIDENCE: 0.95\nEXPLANATION: Ok\nSOURCE: Wiki';
    const result = parseStructuredResult(input);
    expect(result?.verification).toBe('VERIFIED');
    expect(result?.confidence).toBe(0.95);
    expect(result?.source).toBe('Wiki');
  });

  it('parses spaced confidence formats', () => {
    const input = 'VERIFICATION: TRUE\nCONFIDENCE: 0. 95\nEXPLANATION: Ok';
    const result = parseStructuredResult(input);
    expect(result?.confidence).toBe(0.95);
  });

  it('returns null when no fields are found', () => {
    const result = parseStructuredResult('Just text');
    expect(result).toBeNull();
  });
});

describe('parseFinalEvent', () => {
  it('parses valid JSON payloads', () => {
    const payload = JSON.stringify({
      verification: 'VERIFIED',
      confidence: 0.8,
      explanation: 'Ok',
      source: 'Wiki',
    });
    const result = parseFinalEvent(payload);
    expect(result?.verification).toBe('VERIFIED');
    expect(result?.confidence).toBe(0.8);
  });

  it('returns null for invalid JSON', () => {
    const result = parseFinalEvent('not-json');
    expect(result).toBeNull();
  });
});
