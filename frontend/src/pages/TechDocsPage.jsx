import { useEffect, useMemo, useState } from 'react';
import ReactMarkdown from 'react-markdown';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import remarkGfm from 'remark-gfm';
import { fetchDocContent, fetchDocsCatalog, searchDocs } from '../api';

const TECH_CATEGORIES = ['architecture', 'api', 'code-reviews', 'fixes', 'improvements', 'general'];

export default function TechDocsPage() {
  const params = useParams();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const wildcardPath = useMemo(() => (params['*'] || '').replace(/^\/+/, ''), [params]);
  const [catalog, setCatalog] = useState([]);
  const [content, setContent] = useState('');
  const [loadingDoc, setLoadingDoc] = useState(false);
  const [loadingCatalog, setLoadingCatalog] = useState(false);
  const [error, setError] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [collapsedCategories, setCollapsedCategories] = useState({});
  const [docCache, setDocCache] = useState({});

  const selectedDomain = (searchParams.get('domain') || 'technical').toLowerCase();
  const selectedCategory = (searchParams.get('category') || '').toLowerCase();

  useEffect(() => {
    loadCatalog();
  }, []);

  useEffect(() => {
    if (!wildcardPath) {
      setContent('');
      setError('');
      return;
    }
    loadDocument(wildcardPath, selectedDomain);
  }, [wildcardPath, selectedDomain]);

  async function loadCatalog() {
    setLoadingCatalog(true);
    setError('');
    try {
      const data = await fetchDocsCatalog();
      setCatalog(Array.isArray(data?.files) ? data.files : []);
    } catch (e) {
      console.error('Docs catalog fetch failed', e);
      setError(e.message || 'Unable to load documentation catalog.');
    } finally {
      setLoadingCatalog(false);
    }
  }

  async function loadDocument(path, domain) {
    const cacheKey = `${domain}:${path}`;
    if (docCache[cacheKey]) {
      setContent(docCache[cacheKey]);
      setError('');
      return;
    }

    setLoadingDoc(true);
    setError('');
    try {
      const text = await fetchDocContent(path, { domain, format: 'text' });
      setContent(text);
      setDocCache((current) => ({ ...current, [cacheKey]: text }));
    } catch (e) {
      console.error(`Doc fetch failed path=${path} domain=${domain}`, e);
      setContent('');
      setError(e.message || 'Document loading failed.');
    } finally {
      setLoadingDoc(false);
    }
  }

  async function runSearch() {
    const keyword = searchQuery.trim();
    if (!keyword) {
      setSearchResults([]);
      return;
    }
    try {
      const payload = await searchDocs(keyword, {
        domain: selectedDomain,
        category: selectedCategory || undefined
      });
      setSearchResults(payload.results || []);
    } catch (e) {
      console.error('Docs search failed', e);
      setError(e.message || 'Search failed.');
    }
  }

  function updateRoute(nextPath, options = {}) {
    const next = new URLSearchParams(searchParams);
    if (options.domain) {
      next.set('domain', options.domain);
    }
    if (options.category) {
      next.set('category', options.category);
    } else {
      next.delete('category');
    }
    const query = next.toString();
    navigate(`/docs/tech/${nextPath}${query ? `?${query}` : ''}`);
  }

  function handleDomainChange(domain) {
    const paramsCopy = new URLSearchParams(searchParams);
    paramsCopy.set('domain', domain);
    if (domain !== 'technical') {
      paramsCopy.delete('category');
    }
    setSearchParams(paramsCopy);
    if (wildcardPath) {
      updateRoute(wildcardPath, { domain, category: domain === 'technical' ? selectedCategory : '' });
    }
  }

  function toggleCategory(category) {
    setCollapsedCategories((current) => ({ ...current, [category]: !current[category] }));
  }

  function selectCategory(category) {
    const paramsCopy = new URLSearchParams(searchParams);
    paramsCopy.set('domain', 'technical');
    paramsCopy.set('category', category);
    setSearchParams(paramsCopy);
  }

  const visibleDocs = useMemo(() => {
    return catalog
      .filter((doc) => (doc.domain || 'technical') === selectedDomain)
      .filter((doc) => !selectedCategory || selectedDomain !== 'technical' || doc.category === selectedCategory)
      .filter((doc) => {
        if (!searchQuery.trim()) {
          return true;
        }
        const query = searchQuery.toLowerCase();
        const title = (doc.title || doc.path || '').toLowerCase();
        const description = (doc.description || '').toLowerCase();
        return title.includes(query) || description.includes(query);
      });
  }, [catalog, selectedDomain, selectedCategory, searchQuery]);

  const docsByCategory = useMemo(() => {
    const groups = {};
    visibleDocs.forEach((doc) => {
      const key = doc.category || 'general';
      if (!groups[key]) {
        groups[key] = [];
      }
      groups[key].push(doc);
    });
    return groups;
  }, [visibleDocs]);

  const activeDocLabel = useMemo(() => {
    const match = catalog.find((doc) => {
      const pathWithoutExtension = doc.pathWithoutExtension || (doc.path || '').replace(/\.md$/, '');
      return pathWithoutExtension === wildcardPath;
    });
    return match?.title || wildcardPath || 'Documentation Home';
  }, [catalog, wildcardPath]);

  return (
    <section>
      <div className="page-title-row">
        <div>
          <h2>Documentation Workspace</h2>
          <p>Structured technical and business documents with fast inline reading.</p>
        </div>
      </div>

      <div className="docs-shell panel">
        <div className="docs-header-row">
          <div className="docs-domain-tabs" role="tablist" aria-label="Document domains">
            <button
              type="button"
              className={`ghost ${selectedDomain === 'technical' ? 'active' : ''}`}
              onClick={() => handleDomainChange('technical')}
            >
              Technical
            </button>
            <button
              type="button"
              className={`ghost ${selectedDomain === 'business' ? 'active' : ''}`}
              onClick={() => handleDomainChange('business')}
            >
              Business
            </button>
          </div>

          <div className="docs-inline-search">
            <input
              type="text"
              placeholder="Filter docs in sidebar or run keyword search"
              value={searchQuery}
              onChange={(event) => setSearchQuery(event.target.value)}
              onKeyDown={(event) => event.key === 'Enter' && runSearch()}
            />
            <button type="button" onClick={runSearch}>Search</button>
          </div>
        </div>

        <div className="docs-breadcrumbs muted">
          {selectedDomain} / {selectedCategory || 'all'} / {activeDocLabel}
        </div>

        <div className="docs-layout-modern">
          <aside className="docs-sidebar-modern">
            {loadingCatalog && <p className="muted">Loading navigation...</p>}

            {selectedDomain === 'technical' && TECH_CATEGORIES.map((category) => {
              const items = docsByCategory[category] || [];
              const collapsed = collapsedCategories[category];
              return (
                <div className="docs-nav-group" key={category}>
                  <div className="docs-nav-group-header">
                    <button type="button" className="ghost" onClick={() => toggleCategory(category)}>
                      {collapsed ? '+' : '-'} {category}
                    </button>
                    <button
                      type="button"
                      className={`ghost docs-chip ${selectedCategory === category ? 'active' : ''}`}
                      onClick={() => selectCategory(category)}
                    >
                      {items.length}
                    </button>
                  </div>
                  {!collapsed && (
                    <ul className="docs-list modern">
                      {items.map((doc) => {
                        const pathWithoutExtension = doc.pathWithoutExtension || doc.path.replace(/\.md$/, '');
                        return (
                          <li key={`${doc.domain}:${doc.path}`}>
                            <button
                              type="button"
                              className={`ghost ${wildcardPath === pathWithoutExtension ? 'active' : ''}`}
                              onClick={() => updateRoute(pathWithoutExtension, { domain: 'technical', category })}
                              title={doc.description || doc.path}
                            >
                              {doc.title || doc.path}
                            </button>
                          </li>
                        );
                      })}
                      {!items.length && <li className="muted">No documents</li>}
                    </ul>
                  )}
                </div>
              );
            })}

            {selectedDomain === 'business' && (
              <div className="docs-nav-group">
                <div className="docs-nav-group-header">
                  <h4>Business</h4>
                </div>
                <ul className="docs-list modern">
                  {visibleDocs.map((doc) => {
                    const pathWithoutExtension = doc.pathWithoutExtension || doc.path.replace(/\.md$/, '');
                    return (
                      <li key={`${doc.domain}:${doc.path}`}>
                        <button
                          type="button"
                          className={`ghost ${wildcardPath === pathWithoutExtension ? 'active' : ''}`}
                          onClick={() => updateRoute(pathWithoutExtension, { domain: 'business' })}
                        >
                          {doc.title || doc.path}
                        </button>
                      </li>
                    );
                  })}
                  {!visibleDocs.length && <li className="muted">No business documents available.</li>}
                </ul>
              </div>
            )}
          </aside>

          <main className="docs-reader">
            {error && (
              <div className="error docs-error-box">
                <strong>Unable to load documentation.</strong>
                <p>{error}</p>
                <button type="button" className="ghost" onClick={() => wildcardPath ? loadDocument(wildcardPath, selectedDomain) : loadCatalog()}>
                  Retry
                </button>
              </div>
            )}

            {searchResults.length > 0 && (
              <section className="search-results">
                <h3>Keyword matches</h3>
                {searchResults.map((result) => (
                  <div key={`${result.domain}-${result.file}`} className="search-result-item">
                    <h4>{result.file}</h4>
                    <p><em>{result.snippet}</em></p>
                    <small className="muted">{result.domain} / {result.category} / matches: {result.matches}</small>
                  </div>
                ))}
              </section>
            )}

            {!wildcardPath && !loadingDoc && !error && (
              <div className="docs-welcome">
                <h3>Welcome to the Documentation Workspace</h3>
                <p>Select a document from the left panel for an inline preview.</p>
              </div>
            )}

            {loadingDoc && <p className="muted">Loading document...</p>}

            {!loadingDoc && content && (
              <article className="markdown-content">
                <ReactMarkdown remarkPlugins={[remarkGfm]}>
                  {content}
                </ReactMarkdown>
              </article>
            )}
          </main>
        </div>
      </div>
    </section>
  );
}

