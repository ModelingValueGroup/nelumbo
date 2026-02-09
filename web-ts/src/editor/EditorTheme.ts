/**
 * EditorTheme - Color scheme configuration for the Nelumbo editor.
 * Defines colors and styles for different token types.
 */

import { TokenType } from '../TokenType';

/**
 * Style definition for a token type.
 */
export interface ColorScheme {
  foreground: string | null;
  background: string | null;
  bold: boolean;
  italic: boolean;
  underline: boolean;
}

/**
 * Default color schemes for token types.
 */
const DEFAULT_TOKEN_COLORS: Map<string, ColorScheme> = new Map([
  ['STRING', { foreground: '#006633', background: null, bold: false, italic: false, underline: false }],
  ['DECIMAL', { foreground: '#000077', background: null, bold: false, italic: false, underline: false }],
  ['NUMBER', { foreground: '#000077', background: null, bold: false, italic: false, underline: false }],
  ['NAME', { foreground: '#0000ff', background: null, bold: false, italic: false, underline: false }],
  ['END_LINE_COMMENT', { foreground: '#999999', background: null, bold: false, italic: true, underline: false }],
  ['IN_LINE_COMMENT', { foreground: '#999999', background: null, bold: false, italic: true, underline: false }],
  ['OPERATOR', { foreground: '#333333', background: null, bold: true, italic: false, underline: false }],
  ['ERROR', { foreground: '#ff0000', background: '#ffdddd', bold: false, italic: false, underline: false }],
  ['VARIABLE', { foreground: '#339900', background: null, bold: false, italic: false, underline: false }],
  ['KEYWORD', { foreground: '#0000ff', background: null, bold: true, italic: false, underline: false }],
  ['TYPE', { foreground: '#880088', background: null, bold: false, italic: false, underline: false }],
  ['META_OPERATOR', { foreground: '#00cccc', background: '#ffffff', bold: false, italic: false, underline: false }],
]);

/**
 * Dark theme color schemes.
 */
const DARK_THEME_COLORS: Map<string, ColorScheme> = new Map([
  ['STRING', { foreground: '#a6e22e', background: null, bold: false, italic: false, underline: false }],
  ['DECIMAL', { foreground: '#ae81ff', background: null, bold: false, italic: false, underline: false }],
  ['NUMBER', { foreground: '#ae81ff', background: null, bold: false, italic: false, underline: false }],
  ['NAME', { foreground: '#66d9ef', background: null, bold: false, italic: false, underline: false }],
  ['END_LINE_COMMENT', { foreground: '#75715e', background: null, bold: false, italic: true, underline: false }],
  ['IN_LINE_COMMENT', { foreground: '#75715e', background: null, bold: false, italic: true, underline: false }],
  ['OPERATOR', { foreground: '#f92672', background: null, bold: true, italic: false, underline: false }],
  ['ERROR', { foreground: '#ff5555', background: '#442222', bold: false, italic: false, underline: false }],
  ['VARIABLE', { foreground: '#a6e22e', background: null, bold: false, italic: false, underline: false }],
  ['KEYWORD', { foreground: '#f92672', background: null, bold: true, italic: false, underline: false }],
  ['TYPE', { foreground: '#fd971f', background: null, bold: false, italic: false, underline: false }],
  ['META_OPERATOR', { foreground: '#66d9ef', background: null, bold: false, italic: false, underline: false }],
]);

/**
 * Current active color schemes (can be customized).
 */
let tokenColors: Map<string, ColorScheme> = new Map(DEFAULT_TOKEN_COLORS);
let isDarkTheme = false;

/**
 * EditorTheme - manages the color theme for the editor.
 */
export const EditorTheme = {
  /**
   * Get the color scheme for a token type.
   */
  getTokenColor(tokenType: TokenType | string): ColorScheme | null {
    const typeName = typeof tokenType === 'string' ? tokenType : tokenType.name;
    return tokenColors.get(typeName) ?? null;
  },

  /**
   * Set a custom color scheme for a token type.
   */
  setTokenColor(tokenType: string, scheme: ColorScheme): void {
    tokenColors.set(tokenType, scheme);
    saveTokenColors();
  },

  /**
   * Reset token colors to defaults.
   */
  resetTokenColors(): void {
    tokenColors = new Map(isDarkTheme ? DARK_THEME_COLORS : DEFAULT_TOKEN_COLORS);
    saveTokenColors();
  },

  /**
   * Switch to dark theme.
   */
  useDarkTheme(): void {
    isDarkTheme = true;
    tokenColors = new Map(DARK_THEME_COLORS);
    saveTokenColors();
  },

  /**
   * Switch to light theme.
   */
  useLightTheme(): void {
    isDarkTheme = false;
    tokenColors = new Map(DEFAULT_TOKEN_COLORS);
    saveTokenColors();
  },

  /**
   * Check if dark theme is active.
   */
  isDarkTheme(): boolean {
    return isDarkTheme;
  },

  /**
   * Get all token colors.
   */
  getAllTokenColors(): Map<string, ColorScheme> {
    return new Map(tokenColors);
  },

  /**
   * Generate CSS styles for a color scheme.
   */
  schemeToCSS(scheme: ColorScheme): string {
    const styles: string[] = [];
    if (scheme.foreground) {
      styles.push(`color: ${scheme.foreground}`);
    }
    if (scheme.background) {
      styles.push(`background-color: ${scheme.background}`);
    }
    if (scheme.bold) {
      styles.push('font-weight: bold');
    }
    if (scheme.italic) {
      styles.push('font-style: italic');
    }
    if (scheme.underline) {
      styles.push('text-decoration: underline');
    }
    return styles.join('; ');
  },

  /**
   * Load token colors from localStorage.
   */
  loadTokenColors(): void {
    try {
      const stored = localStorage.getItem('nelumbo.tokenColors');
      if (stored) {
        const data = JSON.parse(stored);
        isDarkTheme = data.isDarkTheme ?? false;
        if (data.colors) {
          tokenColors = new Map(Object.entries(data.colors));
        } else {
          tokenColors = new Map(isDarkTheme ? DARK_THEME_COLORS : DEFAULT_TOKEN_COLORS);
        }
      }
    } catch {
      // Use defaults on error
    }
  },
};

/**
 * Save token colors to localStorage.
 */
function saveTokenColors(): void {
  try {
    const data = {
      isDarkTheme,
      colors: Object.fromEntries(tokenColors),
    };
    localStorage.setItem('nelumbo.tokenColors', JSON.stringify(data));
  } catch {
    // Ignore storage errors
  }
}

// Load colors on module init
EditorTheme.loadTokenColors();
