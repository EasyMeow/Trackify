import { useEffect } from 'react';
import type { ReactNode } from 'react';

import '../../shared/styles/tokens.css';
import '../../shared/styles/globals.css';

export function ThemeProvider({ children }: { children: ReactNode }) {
  useEffect(() => {
    document.documentElement.dataset.theme = 'light';
  }, []);

  return <>{children}</>;
}
