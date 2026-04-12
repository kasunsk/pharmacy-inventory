import { useMemo, useState } from 'react';
import { askAiAssistant } from '../api';
import { useAuth } from '../auth/AuthContext';

export default function AiAssistantPanel({ isOpen, onToggle, onClose }) {
  const { session } = useAuth();
  const [messages, setMessages] = useState([
    {
      role: 'assistant',
      content: 'Hi, I am your pharmacy AI assistant. Ask about billing, inventory, transactions, or profit insights.'
    }
  ]);
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const history = useMemo(
    () => messages.slice(-10).map((m) => ({ role: m.role, content: m.content })),
    [messages]
  );

  const quickPrompts = useMemo(() => {
    const roles = session?.roles || [];
    const isAdmin = roles.includes('ADMIN');
    return [
      ...(isAdmin ? ['Show profit for last 7 days'] : []),
      ...(isAdmin || roles.includes('INVENTORY') ? ['Do we have Paracetamol?'] : []),
      ...(isAdmin || roles.includes('BILLING') ? ['Start a new billing'] : []),
      ...(isAdmin || roles.includes('TRANSACTIONS') ? ["Show today's transactions"] : [])
    ];
  }, [session?.roles]);

  const showSuggestions = !query.trim() && !messages.some((message) => message.role === 'user');

  async function send(text) {
    const content = text.trim();
    if (!content) {
      return;
    }

    setMessages((prev) => [...prev, { role: 'user', content }]);
    setQuery('');
    setError('');
    setLoading(true);

    try {
      const response = await askAiAssistant({
        query: content,
        sessionId: 'default-session',
        history
      });

      const extras = response.quickActions?.length
        ? `\n\nQuick actions: ${response.quickActions.join(' | ')}`
        : '';

      setMessages((prev) => [
        ...prev,
        {
          role: 'assistant',
          content: `${response.answer}${extras}`
        }
      ]);
    } catch (e) {
      setError(e.message);
      setMessages((prev) => [
        ...prev,
        {
          role: 'assistant',
          content: "I couldn't understand that. Try asking about billing, inventory, transactions, or profit."
        }
      ]);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className={`ai-floating-widget ${isOpen ? 'open' : ''}`}>
      {isOpen && (
        <aside className="ai-floating-panel" aria-label="AI assistant">
          <div className="ai-panel-header">
            <div>
              <p className="eyebrow">AI assistant</p>
              <h2>Ask while you work</h2>
            </div>
            <button type="button" className="ghost icon-btn" onClick={onClose} aria-label="Close AI assistant">x</button>
          </div>

          {error && <p className="error">{error}</p>}

          {showSuggestions && (
            <div className="ai-quick-actions">
              {quickPrompts.map((prompt) => (
                <button key={prompt} type="button" className="ghost" onClick={() => send(prompt)} disabled={loading}>
                  {prompt}
                </button>
              ))}
            </div>
          )}

          <div className="ai-thread">
            {messages.map((message, index) => (
              <article key={index} className={`ai-msg ${message.role}`}>
                <strong>{message.role === 'assistant' ? 'Assistant' : 'You'}</strong>
                <p>{message.content}</p>
              </article>
            ))}
            {loading && <p className="muted">Assistant is thinking...</p>}
          </div>

          <form
            className="ai-input-row"
            onSubmit={(event) => {
              event.preventDefault();
              send(query);
            }}
          >
            <input
              placeholder="Ask about stock, profit, bills..."
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              disabled={loading}
            />
            <button type="submit" disabled={loading || !query.trim()}>
              Send
            </button>
          </form>
        </aside>
      )}
      <button
        type="button"
        className="ai-fab"
        onClick={onToggle}
        aria-expanded={isOpen}
        aria-label={isOpen ? 'Collapse AI assistant' : 'Open AI assistant'}
      >
        <span aria-hidden="true">{isOpen ? 'x' : 'AI'}</span>
      </button>
    </div>
  );
}
