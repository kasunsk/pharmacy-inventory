import { useMemo, useState } from 'react';
import { askAiAssistant } from '../api';

const QUICK_PROMPTS = [
  'Show profit for last 7 days',
  'Do we have Paracetamol?',
  'Start a new billing',
  "Show today's transactions"
];

export default function AiAssistantPage() {
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
          content: "I couldn't understand that, try asking about billing, inventory, transactions, or profit."
        }
      ]);
    } finally {
      setLoading(false);
    }
  }

  return (
    <section>
      <div className="page-title-row">
        <div>
          <h2>AI Assistant</h2>
          <p>Natural language support for pharmacy operations and insights.</p>
        </div>
      </div>

      {error && <p className="error">{error}</p>}

      <div className="panel ai-chat-shell">
        <div className="ai-quick-actions">
          {QUICK_PROMPTS.map((prompt) => (
            <button key={prompt} type="button" className="ghost" onClick={() => send(prompt)} disabled={loading}>
              {prompt}
            </button>
          ))}
        </div>

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
            placeholder="Ask anything about billing, inventory, profit, or transactions..."
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            disabled={loading}
          />
          <button type="submit" disabled={loading || !query.trim()}>
            Send
          </button>
        </form>
      </div>
    </section>
  );
}

