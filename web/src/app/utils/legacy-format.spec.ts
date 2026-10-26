import { describe, expect, it } from 'vitest';
import { formatTaskLabel } from './legacy-format';

describe('formatTaskLabel', () => {
  it('formats priority and owner', () => {
    expect(formatTaskLabel({ priority: 'high', owner: 'student' })).toBe('HIGH · STUDENT');
  });

  it('returns SIN-DATOS when the task is missing', () => {
    expect(formatTaskLabel(null)).toBe('SIN-DATOS');
  });

  it('uses NORMAL and SIN-ASIGNAR defaults when priority and owner are empty', () => {
    expect(formatTaskLabel({ priority: '', owner: null })).toBe('NORMAL · SIN-ASIGNAR');
  });
});
