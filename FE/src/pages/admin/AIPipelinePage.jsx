import { useState, useEffect } from 'react'
import { Brain, RefreshCw, AlertTriangle, CheckCircle, Clock, Activity } from 'lucide-react'
import StatusBadge from '../../components/admin/StatusBadge'
import { getAIPipelineEvents } from '../../features/analytics/analytics.api'
import { formatRelativeTime, formatDateTime } from '../../utils/formatDate'

export default function AIPipelinePage() {
  const [events, setEvents] = useState([])
  const [selectedEvent, setSelectedEvent] = useState(null)
  const [retryingId, setRetryingId] = useState(null)

  useEffect(() => {
    getAIPipelineEvents().then(setEvents)
  }, [])

  const retryEvent = (eventId) => {
    setRetryingId(eventId)
    setEvents(prev => prev.map(e => e.id === eventId ? { ...e, status: 'processing' } : e))
    setSelectedEvent(prev => prev && prev.id === eventId ? { ...prev, status: 'processing' } : prev)
    setTimeout(() => {
      setEvents(prev => prev.map(e => e.id === eventId ? { ...e, status: 'synced' } : e))
      setSelectedEvent(prev => prev && prev.id === eventId ? { ...prev, status: 'synced' } : prev)
      setRetryingId(null)
    }, 2000)
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-headline-md text-primary">AI Pipeline</h1>
          <p className="text-sm text-on-surface-variant mt-1">Monitor AI service health and event flow</p>
        </div>
        <button className="bg-surface-container text-on-surface px-4 py-2 rounded-lg text-sm font-medium flex items-center gap-2 hover:bg-surface-container-high">
          <RefreshCw size={14} /> Refresh
        </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="bg-surface-container-lowest rounded-xl p-5 ambient-shadow">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-2"><Activity size={16} className="text-primary" /><span className="text-sm font-medium text-primary">Vector Index</span></div>
            <StatusBadge status="synced" />
          </div>
          <p className="text-2xl font-semibold text-primary">99.98%</p>
          <p className="text-xs text-on-surface-variant mt-1">Health Score</p>
        </div>
        <div className="bg-surface-container-lowest rounded-xl p-5 ambient-shadow">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-2"><Brain size={16} className="text-primary" /><span className="text-sm font-medium text-primary">Knowledge Graph</span></div>
            <StatusBadge status="synced" />
          </div>
          <p className="text-2xl font-semibold text-primary">100%</p>
          <p className="text-xs text-on-surface-variant mt-1">Integrity Score</p>
        </div>
        <div className="bg-surface-container-lowest rounded-xl p-5 ambient-shadow">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-2"><Clock size={16} className="text-primary" /><span className="text-sm font-medium text-primary">Pipeline Latency</span></div>
            <StatusBadge status="failed" />
          </div>
          <p className="text-2xl font-semibold text-error">420ms</p>
          <p className="text-xs text-error mt-1">Above threshold (200ms)</p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        <div className="lg:col-span-8 bg-surface-container-lowest rounded-xl ambient-shadow overflow-hidden">
          <div className="p-4 border-b border-outline-variant/20">
            <h2 className="font-title-lg text-primary">Event Log</h2>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="bg-surface-container-low/50">
                  <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Timestamp</th>
                  <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Event</th>
                  <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Service</th>
                  <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-outline-variant/5">
                {events.map((event) => (
                  <tr key={event.id} className={`hover:bg-surface-container-high/30 cursor-pointer transition-colors ${selectedEvent?.id === event.id ? 'bg-surface-container-low' : ''}`} onClick={() => setSelectedEvent(event)}>
                    <td className="px-4 py-3 text-xs text-on-surface-variant">{formatRelativeTime(event.timestamp)}</td>
                    <td className="px-4 py-3 text-sm font-medium text-primary">{event.name}</td>
                    <td className="px-4 py-3 text-sm text-on-surface-variant">{event.service}</td>
                    <td className="px-4 py-3"><StatusBadge status={event.status} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        <div className="lg:col-span-4">
          <div className="bg-surface-container-lowest rounded-xl p-5 ambient-shadow sticky top-24">
            {selectedEvent ? (
              <div className="space-y-4">
                <div className="flex items-center gap-2">
                  {selectedEvent.status === 'failed' ? <AlertTriangle size={18} className="text-error" /> : <CheckCircle size={18} className="text-green-status" />}
                  <h3 className="font-title-lg text-primary">{selectedEvent.name}</h3>
                </div>
                <StatusBadge status={selectedEvent.status} />
                <div className="space-y-2 text-sm">
                  <div className="flex justify-between"><span className="text-on-surface-variant">Service</span><span className="text-primary">{selectedEvent.service}</span></div>
                  <div className="flex justify-between"><span className="text-on-surface-variant">Timestamp</span><span className="text-primary">{formatDateTime(selectedEvent.timestamp)}</span></div>
                </div>
                {selectedEvent.status === 'failed' && (
                  <div className="bg-error-container/50 rounded-lg p-4">
                    <h4 className="text-xs font-medium text-error mb-2">Error Details</h4>
                    <p className="text-xs text-on-surface-variant">Connection timeout: Service unavailable. Retry scheduled in 30s.</p>
                  </div>
                )}
                {selectedEvent.status === 'processing' && (
                  <div className="bg-primary/10 rounded-lg p-4">
                    <h4 className="text-xs font-medium text-primary mb-2">Retry In Progress</h4>
                    <div className="flex items-center gap-2">
                      <div className="w-4 h-4 border-2 border-primary border-t-transparent rounded-full animate-spin" />
                      <p className="text-xs text-on-surface-variant">Re-syncing event...</p>
                    </div>
                  </div>
                )}
                <div className="bg-surface-container-low rounded-lg p-4">
                  <h4 className="text-xs font-medium text-on-surface-variant mb-2">Payload Preview</h4>
                  <pre className="text-[10px] text-on-surface-variant overflow-x-auto">{'{' + `\n  "event": "${selectedEvent.name}",\n  "service": "${selectedEvent.service}",\n  "status": "${selectedEvent.status}"` + '\n}'}</pre>
                </div>
                <div className="flex gap-2">
                  {selectedEvent.status === 'failed' && (
                    <button onClick={() => retryEvent(selectedEvent.id)} disabled={retryingId === selectedEvent.id} className="flex-1 bg-primary text-on-primary rounded-lg py-2 text-xs font-medium hover:opacity-90 disabled:opacity-50">
                      {retryingId === selectedEvent.id ? 'Retrying...' : 'Retry'}
                    </button>
                  )}
                  <button className="flex-1 bg-surface-container text-on-surface rounded-lg py-2 text-xs font-medium hover:bg-surface-container-high">View Logs</button>
                </div>
              </div>
            ) : (
              <div className="text-center py-8 text-on-surface-variant text-sm">Select an event to view details</div>
            )}
          </div>
        </div>
      </div>

      <div className="bg-surface-container-lowest rounded-xl p-6 ambient-shadow">
        <h2 className="font-title-lg text-primary mb-4">Pipeline Health</h2>
        <div className="flex items-center justify-between">
          {['Ingest', 'Parse', 'Index', 'Embed', 'Store'].map((step, idx) => (
            <div key={step} className="flex items-center">
              <div className="flex flex-col items-center">
                <div className={`w-10 h-10 rounded-full flex items-center justify-center text-xs font-bold ${
                  idx < 4 ? 'bg-primary text-on-primary' : 'bg-surface-container-high text-on-surface-variant'
                }`}>{idx + 1}</div>
                <span className="text-xs text-on-surface-variant mt-2">{step}</span>
              </div>
              {idx < 4 && <div className={`w-16 h-0.5 mx-2 ${idx < 3 ? 'bg-primary' : 'bg-outline-variant'}`} />}
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
