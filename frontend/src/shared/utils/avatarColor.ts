const PASTEL_PAIRS: Array<{ bg: string; fg: string }> = [
  { bg: '#d8ebde', fg: '#4a8b6f' }, // green
  { bg: '#fbeed1', fg: '#c6913a' }, // amber
  { bg: '#fbe1de', fg: '#c46f6a' }, // rose
  { bg: '#dfe9f1', fg: '#5a87a3' }, // blue
  { bg: '#e8e0f7', fg: '#7c5cbf' }, // purple
];

export function getAvatarColors(userId: string): { bg: string; fg: string } {
  let hash = 0;
  for (let i = 0; i < userId.length; i++) {
    hash = (Math.imul(hash, 31) + userId.charCodeAt(i)) | 0;
  }
  return PASTEL_PAIRS[Math.abs(hash) % PASTEL_PAIRS.length];
}
