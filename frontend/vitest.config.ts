import { defineConfig } from 'vitest/config';

// Limita la contención de memoria/CPU en equipos de revisión y CI.
// Se conserva el límite de tiempo de cada prueba.
export default defineConfig({ test: { maxWorkers: 2 } });
