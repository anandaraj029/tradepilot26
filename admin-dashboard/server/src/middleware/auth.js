import jwt from 'jsonwebtoken';
import { Admin } from '../models/index.js';

export const JWT_SECRET = process.env.JWT_SECRET || 'tradepilot-dev-secret';

export async function requireAuth(req, res, next) {
  const header = req.headers.authorization || '';
  const token = header.startsWith('Bearer ') ? header.slice(7) : null;
  if (!token) return res.status(401).json({ error: 'Authentication required' });
  try {
    const payload = jwt.verify(token, JWT_SECRET);
    const admin = await Admin.findById(payload.sub).select('-passwordHash');
    if (!admin) return res.status(401).json({ error: 'Admin not found' });
    req.admin = admin;
    next();
  } catch {
    res.status(401).json({ error: 'Invalid or expired token' });
  }
}

/** Restrict a route to the given admin roles (SUPER_ADMIN always passes). */
export const requireRole = (...roles) => (req, res, next) => {
  if (req.admin.role === 'SUPER_ADMIN' || roles.includes(req.admin.role)) return next();
  res.status(403).json({ error: `Requires role: ${['SUPER_ADMIN', ...roles].join(' or ')}` });
};
