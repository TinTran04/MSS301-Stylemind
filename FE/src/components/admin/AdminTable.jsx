import clsx from 'clsx'
import StatusBadge from './StatusBadge'

export default function AdminTable({ columns, data, onRowClick }) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full">
        <thead>
          <tr className="bg-surface-container-low/50">
            {columns.map((col) => (
              <th
                key={col.key}
                className="text-left font-label-sm uppercase tracking-wider text-on-surface-variant px-4 py-3"
              >
                {col.label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-outline-variant/5">
          {data.map((row, idx) => (
            <tr
              key={row.id || idx}
              className={clsx(
                'hover:bg-surface-container-high/30 transition-colors',
                onRowClick && 'cursor-pointer'
              )}
              onClick={() => onRowClick?.(row)}
            >
              {columns.map((col) => (
                <td key={col.key} className="px-4 py-3 text-sm text-on-surface">
                  {col.render ? col.render(row[col.key], row) : row[col.key]}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
