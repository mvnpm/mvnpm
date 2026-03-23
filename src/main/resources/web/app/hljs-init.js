import hljs from 'highlight.js/lib/core';
import xml from 'highlight.js/lib/languages/xml';
import groovy from 'highlight.js/lib/languages/groovy';
import 'highlight.js/styles/github-dark.css';

hljs.registerLanguage('xml', xml);
hljs.registerLanguage('groovy', groovy);

// Override hljs background to use our theme variables
const style = document.createElement('style');
style.textContent = '.hljs { background: var(--mvnpm-code-bg) !important; }';
document.head.appendChild(style);

hljs.highlightAll();
