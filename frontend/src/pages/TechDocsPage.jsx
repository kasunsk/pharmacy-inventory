import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';

/**
 * TechDocsPage - Displays technical documentation for developers
 * Accessible via /tech-docs route
 */
export default function TechDocsPage() {
  const { doc } = useParams();
  const [content, setContent] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [docsList, setDocsList] = useState([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState([]);

  const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

  // Fetch documentation list on mount
  useEffect(() => {
    loadDocumentsList();
  }, []);

  // Fetch specific document when doc param changes
  useEffect(() => {
    if (doc) {
      loadDocument(doc);
    }
  }, [doc]);

  async function loadDocumentsList() {
    try {
      const response = await fetch(`${API_BASE}/tech/docs/list`);
      if (!response.ok) throw new Error('Failed to load docs list');
      const data = await response.json();
      setDocsList(data.files || []);
    } catch (e) {
      console.error('Error loading docs list:', e);
    }
  }

  async function loadDocument(docPath) {
    setLoading(true);
    setError('');
    setContent('');
    try {
      const response = await fetch(`${API_BASE}/tech/docs/${docPath}?format=text`);
      if (!response.ok) throw new Error('Document not found');
      const text = await response.text();
      setContent(text);
    } catch (e) {
      setError(`Failed to load: ${e.message}`);
    } finally {
      setLoading(false);
    }
  }

  async function handleSearch() {
    if (!searchQuery.trim()) return;

    try {
      const response = await fetch(
        `${API_BASE}/tech/docs/search?keyword=${encodeURIComponent(searchQuery)}`
      );
      if (!response.ok) throw new Error('Search failed');
      const data = await response.json();
      setSearchResults(data.results || []);
    } catch (e) {
      console.error('Search error:', e);
    }
  }

  // Render markdown content as formatted text
  const renderContent = (text) => {
    if (!text) return null;

    const lines = text.split('\n');
    return lines.map((line, idx) => {
      // Headers
      if (line.startsWith('# ')) {
        return <h1 key={idx} style={{ marginTop: '24px', marginBottom: '12px' }}>{line.substring(2)}</h1>;
      }
      if (line.startsWith('## ')) {
        return <h2 key={idx} style={{ marginTop: '20px', marginBottom: '10px' }}>{line.substring(3)}</h2>;
      }
      if (line.startsWith('### ')) {
        return <h3 key={idx} style={{ marginTop: '16px', marginBottom: '8px' }}>{line.substring(4)}</h3>;
      }

      // Bold text
      line = line.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
      // Code blocks
      if (line.startsWith('```')) {
        return <code key={idx} style={{ display: 'block', padding: '8px', background: '#f5f5f5', margin: '8px 0' }} />;
      }

      // Lists
      if (line.startsWith('- ')) {
        return <li key={idx} style={{ marginLeft: '20px' }}>{line.substring(2)}</li>;
      }

      // Empty lines
      if (!line.trim()) {
        return <br key={idx} />;
      }

      return <p key={idx} style={{ marginBottom: '8px', lineHeight: '1.6' }}>{line}</p>;
    });
  };

  return (
    <section>
      <div className="page-title-row">
        <div>
          <h2>Technical Documentation</h2>
          <p>Developer resources, guides, and technical references</p>
        </div>
      </div>

      <div className="tech-docs-layout">
        {/* Sidebar Navigation */}
        <aside className="tech-docs-sidebar">
          <div className="sidebar-search">
            <input
              type="text"
              placeholder="Search documentation..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
            />
            <button type="button" onClick={handleSearch}>Search</button>
          </div>

          {/* Main Documentation */}
          <div className="docs-section">
            <h4>Main Documentation</h4>
            <ul className="docs-list">
              {docsList
                .filter(d => d.category === 'main')
                .map((d) => (
                  <li key={d.path}>
                    <button
                      type="button"
                      className={`ghost ${doc === d.path.replace('.md', '') ? 'active' : ''}`}
                      onClick={() => window.location.hash = `/tech-docs/${d.path.replace('.md', '')}`}
                    >
                      {d.path.split('/').pop()}
                    </button>
                    <small className="muted">{d.description}</small>
                  </li>
                ))}
            </ul>
          </div>

          {/* Fixes Documentation */}
          <div className="docs-section">
            <h4>Fixes</h4>
            <ul className="docs-list">
              {docsList
                .filter(d => d.category === 'fixes')
                .map((d) => (
                  <li key={d.path}>
                    <button
                      type="button"
                      className={`ghost ${doc === d.path.replace('.md', '') ? 'active' : ''}`}
                      onClick={() => window.location.hash = `/tech-docs/${d.path.replace('.md', '')}`}
                    >
                      {d.path.split('/').pop()}
                    </button>
                    <small className="muted">{d.description}</small>
                  </li>
                ))}
            </ul>
          </div>

          {/* Improvements Documentation */}
          <div className="docs-section">
            <h4>Improvements</h4>
            <ul className="docs-list">
              {docsList
                .filter(d => d.category === 'improvements')
                .map((d) => (
                  <li key={d.path}>
                    <button
                      type="button"
                      className={`ghost ${doc === d.path.replace('.md', '') ? 'active' : ''}`}
                      onClick={() => window.location.hash = `/tech-docs/${d.path.replace('.md', '')}`}
                    >
                      {d.path.split('/').pop()}
                    </button>
                    <small className="muted">{d.description}</small>
                  </li>
                ))}
            </ul>
          </div>
        </aside>

        {/* Main Content Area */}
        <main className="tech-docs-content">
          {loading && <p>Loading documentation...</p>}
          {error && <p className="error">{error}</p>}

          {searchResults.length > 0 && (
            <div className="search-results">
              <h3>Search Results</h3>
              {searchResults.map((result, idx) => (
                <div key={idx} className="search-result-item">
                  <h4>{result.file}</h4>
                  <p><em>{result.snippet}</em></p>
                  <small className="muted">Matches: {result.matches}</small>
                </div>
              ))}
            </div>
          )}

          {content && (
            <div className="markdown-content">
              {renderContent(content)}
            </div>
          )}

          {!doc && !loading && !error && (
            <div className="docs-welcome">
              <h3>Welcome to Technical Documentation</h3>
              <p>Select a document from the sidebar to view its contents.</p>
              <div className="docs-info">
                <p><strong>Available Sections:</strong></p>
                <ul>
                  <li><strong>Main Documentation:</strong> Code review and development plan</li>
                  <li><strong>Fixes:</strong> Bug fixes and corrections</li>
                  <li><strong>Improvements:</strong> UI/UX enhancements and optimizations</li>
                </ul>
              </div>
            </div>
          )}
        </main>
      </div>
    </section>
  );
}

