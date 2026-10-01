import { Router } from 'express';
import { requireRole } from '../middleware/auth.js';
import { logAdminAction } from './audit.js';

const escape = (s) => s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');

/**
 * Builds a paginated list/detail/create/update/delete router for a model.
 * Query params: page, limit, q (search), sort (e.g. "-createdAt"), from/to (createdAt range),
 * plus any field listed in `filters` (exact match; comma-separated for $in).
 */
export function crudRouter(Model, {
  searchFields = [],
  filters = [],
  populate = null,
  defaultSort = '-createdAt',
  writable = true,
  writeRoles = ['RISK_OFFICER'],
  name = Model.modelName,
} = {}) {
  const router = Router();

  router.get('/', async (req, res) => {
    const page = Math.max(1, parseInt(req.query.page) || 1);
    const limit = Math.min(500, Math.max(1, parseInt(req.query.limit) || 20));
    const query = {};
    if (req.query.q && searchFields.length) {
      const rx = new RegExp(escape(req.query.q), 'i');
      query.$or = searchFields.map((f) => ({ [f]: rx }));
    }
    for (const f of filters) {
      const v = req.query[f];
      if (v === undefined || v === '' || v === 'ALL') continue;
      if (v === 'true' || v === 'false') query[f] = v === 'true';
      else query[f] = v.includes(',') ? { $in: v.split(',') } : v;
    }
    if (req.query.from || req.query.to) {
      query.createdAt = {};
      if (req.query.from) query.createdAt.$gte = new Date(req.query.from);
      if (req.query.to) query.createdAt.$lte = new Date(req.query.to);
    }
    let cursor = Model.find(query).sort(req.query.sort || defaultSort).skip((page - 1) * limit).limit(limit);
    if (populate) cursor = cursor.populate(populate, 'name email');
    const [items, total] = await Promise.all([cursor.lean(), Model.countDocuments(query)]);
    res.json({ items, total, page, limit, pages: Math.ceil(total / limit) });
  });

  router.get('/:id', async (req, res) => {
    let q = Model.findById(req.params.id);
    if (populate) q = q.populate(populate, 'name email');
    const doc = await q.lean();
    if (!doc) return res.status(404).json({ error: `${name} not found` });
    res.json(doc);
  });

  if (!writable) return router;

  router.post('/', requireRole(...writeRoles), async (req, res) => {
    const doc = await Model.create(req.body);
    await logAdminAction(req, `ADMIN_${name.toUpperCase()}_CREATED`, { details: `Created ${name} ${doc._id}` });
    res.status(201).json(doc);
  });

  router.patch('/:id', requireRole(...writeRoles), async (req, res) => {
    const { _id, createdAt, updatedAt, ...body } = req.body;
    const doc = await Model.findByIdAndUpdate(req.params.id, body, { new: true, runValidators: true });
    if (!doc) return res.status(404).json({ error: `${name} not found` });
    await logAdminAction(req, `ADMIN_${name.toUpperCase()}_UPDATED`, {
      user: doc.user,
      details: `Updated ${name} ${doc._id}: ${Object.keys(body).join(', ')}`,
    });
    res.json(doc);
  });

  router.delete('/:id', requireRole(...writeRoles), async (req, res) => {
    const doc = await Model.findByIdAndDelete(req.params.id);
    if (!doc) return res.status(404).json({ error: `${name} not found` });
    await logAdminAction(req, `ADMIN_${name.toUpperCase()}_DELETED`, {
      user: doc.user,
      severity: 'WARNING',
      details: `Deleted ${name} ${doc._id}`,
    });
    res.json({ ok: true });
  });

  return router;
}
