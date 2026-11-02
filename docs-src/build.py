#!/usr/bin/env python3
"""Builds the SteelCodeCore documentation site.

    python3 docs-src/build.py            # pages only
    ./gradlew buildDocs                   # Javadoc + pages (see the root build.gradle)

Markdown pages in docs-src/pages are rendered into docs/ as static HTML, ready for GitHub Pages
("Deploy from a branch", folder /docs). Requires: pip install markdown pygments
"""
import html
import pathlib
import re
import shutil

import markdown
from pygments.formatters import HtmlFormatter

ROOT = pathlib.Path(__file__).resolve().parent
OUT = ROOT.parent / 'docs'
PAGES = ROOT / 'pages'

SITE = 'SteelCodeCore'
VERSION = '0.1.0'
MINECRAFT = '1.21.1'
REPO_URL = 'https://github.com/SteelCodeTeam/SteelCodeCore'

def front_matter(text):
    meta = {}
    if text.startswith('---\n'):
        head, text = text[4:].split('\n---\n', 1)
        for line in head.splitlines():
            key, _, value = line.partition(':')
            meta[key.strip()] = value.strip()
    return meta, text


def load_pages():
    pages = []
    for path in sorted(PAGES.glob('*.md')):
        meta, body = front_matter(path.read_text(encoding='utf-8'))
        pages.append({
            'slug': path.stem,
            'file': 'index.html' if path.stem == 'index' else f'{path.stem}.html',
            'title': meta.get('title', path.stem),
            'nav': meta.get('nav', meta.get('title', path.stem)),
            'order': int(meta.get('order', 99)),
            'body': body,
        })
    return sorted(pages, key=lambda p: p['order'])


def render(page):
    md = markdown.Markdown(extensions=[
        'fenced_code', 'tables', 'attr_list', 'md_in_html', 'sane_lists',
        'codehilite', 'toc',
    ], extension_configs={
        'codehilite': {'guess_lang': False, 'css_class': 'highlight'},
        'toc': {'permalink': '#', 'toc_depth': '2-3'},
    })
    content = md.convert(page['body'])
    # Open external links in a new tab.
    content = re.sub(r'<a href="(https?://[^"]+)"', r'<a href="\1" target="_blank" rel="noopener"', content)
    return content, md.toc_tokens


def toc_html(tokens):
    # toc_depth "2-3": the top-level tokens are the h2 headings, their children the h3 ones.
    items = []
    for h2 in tokens:
        items.append(f'<li><a href="#{h2["id"]}">{h2["name"]}</a></li>')
        for h3 in h2.get('children', []):
            items.append(f'<li class="sub"><a href="#{h3["id"]}">{h3["name"]}</a></li>')
    if not items:
        return ''
    return '<nav class="toc" aria-label="On this page"><p>On this page</p><ul>' + ''.join(items) + '</ul></nav>'


def nav_html(pages, current):
    links = []
    for p in pages:
        cls = ' class="active" aria-current="page"' if p is current else ''
        links.append(f'<li><a href="{p["file"]}"{cls}>{html.escape(p["nav"])}</a></li>')
    links.append('<li class="sep"><a href="javadoc/index.html">API reference ↗</a></li>')
    return '<ul>' + ''.join(links) + '</ul>'


TEMPLATE = """<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{title}</title>
<meta name="description" content="SteelCodeCore: a Fabric + NeoForge library to create Minecraft {mc} blocks, items and features from common code.">
<link rel="stylesheet" href="assets/style.css">
<link rel="stylesheet" href="assets/highlight.css">
</head>
<body>
<header class="topbar">
  <a class="brand" href="index.html"><span class="mark" aria-hidden="true"></span>{site}</a>
  <span class="badge">v{version} · MC {mc}</span>
  <nav class="toplinks">{top}</nav>
</header>
<div class="layout">
  <aside class="sidebar">
    <details open><summary>Guide</summary>{nav}</details>
  </aside>
  <main class="content">
{content}
    <footer class="pager">{pager}</footer>
  </main>
  {toc}
</div>
<script>
  // Collapse the guide menu on small screens so the page content comes first.
  if (window.matchMedia('(max-width: 760px)').matches) document.querySelector('.sidebar details').removeAttribute('open');
</script>
</body>
</html>
"""


def pager(pages, i):
    parts = []
    if i > 0:
        p = pages[i - 1]
        parts.append(f'<a class="prev" href="{p["file"]}"><small>Previous</small>{html.escape(p["nav"])}</a>')
    if i < len(pages) - 1:
        p = pages[i + 1]
        parts.append(f'<a class="next" href="{p["file"]}"><small>Next</small>{html.escape(p["nav"])}</a>')
    return ''.join(parts)


def fix_javadoc_fonts():
    """Some JDK builds (e.g. Debian's) omit the DejaVu fonts the Javadoc stylesheet imports; drop the dead import."""
    css = OUT / 'javadoc' / 'stylesheet.css'
    if css.exists() and not (OUT / 'javadoc' / 'resources' / 'fonts' / 'dejavu.css').exists():
        text = css.read_text(encoding='utf-8')
        css.write_text(re.sub(r'@import url\([\'"]?resources/fonts/dejavu\.css[\'"]?\);\s*', '', text), encoding='utf-8')


def main():
    OUT.mkdir(exist_ok=True)
    assets = OUT / 'assets'
    assets.mkdir(exist_ok=True)
    shutil.copy(ROOT / 'assets' / 'style.css', assets / 'style.css')
    (assets / 'highlight.css').write_text(HtmlFormatter(style='github-dark').get_style_defs('.highlight'),
                                         encoding='utf-8')
    (OUT / '.nojekyll').write_text('', encoding='utf-8')

    pages = load_pages()
    top = '<a href="javadoc/index.html">API reference</a>'
    if REPO_URL:
        top += f'<a href="{REPO_URL}" target="_blank" rel="noopener">Source</a>'
    for i, page in enumerate(pages):
        content, tokens = render(page)
        title = SITE if page['slug'] == 'index' else f'{page["title"]} · {SITE}'
        out = TEMPLATE.format(title=html.escape(title), site=SITE, version=VERSION, mc=MINECRAFT, top=top,
                              nav=nav_html(pages, page), content=content, toc=toc_html(tokens),
                              pager=pager(pages, i))
        (OUT / page['file']).write_text(out, encoding='utf-8')
        print('wrote', page['file'])
    fix_javadoc_fonts()
    if not (OUT / 'javadoc' / 'index.html').exists():
        print('note: docs/javadoc is missing; run ./gradlew buildDocs to generate the API reference')


if __name__ == '__main__':
    main()
