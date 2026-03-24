import hljs from 'highlight.js/lib/core';
import xml from 'highlight.js/lib/languages/xml';
import groovy from 'highlight.js/lib/languages/groovy';

hljs.registerLanguage('xml', xml);
hljs.registerLanguage('groovy', groovy);

hljs.highlightAll();
