import { AuditLog } from '../models/index.js';

/** Records an admin action in the immutable audit trail. */
export function logAdminAction(req, action, { user, details, severity = 'INFO', symbol, market } = {}) {
  return AuditLog.create({
    user,
    actorType: 'ADMIN',
    actorName: `${req.admin.name} <${req.admin.email}>`,
    action,
    symbol,
    market,
    severity,
    details,
    riskEngineStatus: 'N/A',
    aiConsensusVerdict: 'N/A',
  });
}
