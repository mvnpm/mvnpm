import { LitElement } from 'lit';
import { state } from 'lit/decorators.js';

type Constructor<T = {}> = new (...args: any[]) => T;

/**
 * Mixin that tracks the current theme (light/dark) from the document's data-theme attribute.
 * Provides a reactive `_theme` property that triggers re-renders on theme changes.
 */
export const ThemeMixin = <T extends Constructor<LitElement>>(superClass: T) => {
    class ThemeAware extends superClass {
        @state() _theme: string = 'dark';

        private __themeObserver: MutationObserver | null = null;

        connectedCallback() {
            super.connectedCallback();
            this._theme = document.documentElement.getAttribute('data-theme') || 'dark';
            this.__themeObserver = new MutationObserver(() => {
                this._theme = document.documentElement.getAttribute('data-theme') || 'dark';
            });
            this.__themeObserver.observe(document.documentElement, {
                attributes: true,
                attributeFilter: ['data-theme']
            });
        }

        disconnectedCallback() {
            super.disconnectedCallback();
            this.__themeObserver?.disconnect();
        }
    }
    return ThemeAware as Constructor<{ _theme: string }> & T;
};
