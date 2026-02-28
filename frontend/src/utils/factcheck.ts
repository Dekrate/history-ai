import type { FactCheckResult } from '../types';

export const formatStreamText = (text: string) =>
  text
    .replace(/([.,!?;:])(\S)/g, '$1 $2')
    .replace(/\s*(VERIFICATION:)/gi, '\n$1 ')
    .replace(/\s*(CONFIDENCE:)/gi, '\n$1 ')
    .replace(/\s*(EXPLANATION:)/gi, '\n$1 ')
    .replace(/\s*(SOURCE:)/gi, '\n$1 ')
    .replace(/:\s+/g, ': ')
    .replace(/\n+/g, '\n')
    .trimStart();

export const parseStructuredResult = (text: string): FactCheckResult | null => {
  const normalized = formatStreamText(text);
  const normalizedForConfidence = normalized
    .replace(/CONFIDENCE:\s*([0-9])\s*[,.]\s*([0-9]+)/i, 'CONFIDENCE: $1.$2')
    .replace(/CONFIDENCE:\s*([0-9])\s+([0-9]+)/i, 'CONFIDENCE: $1.$2');

  const verificationMatch = normalized.match(/VERIFICATION:\s*([A-Z_]+)/i);
  const confidenceMatch = normalizedForConfidence.match(/CONFIDENCE:\s*([0-9]*[.]?[0-9]+)/i);
  const explanationMatch = normalized.match(/EXPLANATION:\s*([\s\S]*?)(?:\nSOURCE:|$)/i);
  const sourceMatch = normalized.match(/SOURCE:\s*([\s\S]*?)$/i);

  if (!verificationMatch && !confidenceMatch && !explanationMatch && !sourceMatch) {
    return null;
  }

  const rawVerification = verificationMatch?.[1]?.toUpperCase() ?? 'VERIFIED';
  const mappedVerification =
    rawVerification === 'TRUE' ? 'VERIFIED' :
    rawVerification === 'FALSE' ? 'FALSE' :
    rawVerification === 'PARTIAL' ? 'PARTIAL' :
    rawVerification === 'UNVERIFIABLE' ? 'UNVERIFIABLE' :
    rawVerification;

  return {
    verification: mappedVerification as FactCheckResult['verification'],
    confidence: confidenceMatch ? Number(confidenceMatch[1]) : 0.9,
    explanation: explanationMatch?.[1]?.trim() ?? normalized,
    source: sourceMatch?.[1]?.trim() ?? 'AI',
  };
};

export const parseFinalEvent = (data: string): FactCheckResult | null => {
  try {
    const parsed = JSON.parse(data) as FactCheckResult;
    if (!parsed || !parsed.verification) {
      return null;
    }
    return parsed;
  } catch {
    return null;
  }
};
