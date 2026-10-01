import { Router } from 'express';
import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';
import { Admin } from '../models/index.js';
import { JWT_SECRET, requireAuth, requireRole } from '../middleware/auth.js';
import { logAdminAction } from '../utils/audit.js';

const router = Router();

router.post('/login', async (req, res) => {
  const { email, password } = req.body || {};
  const admin = await Admin.findOne({ email: String(email || '').toLowerCase() });
  if (!admin || !(await bcrypt.compare(String(password || ''), admin.passwordHash))) {
    return res.status(401).json({ error: 'Invalid email or password' });
  }
  admin.lastLoginAt = new Date();
  await admin.save();
  const token = jwt.sign({ sub: admin._id.toString(), role: admin.role }, JWT_SECRET, { expiresIn: '12h' });
  res.json({ token, admin: { _id: admin._id, name: admin.name, email: admin.email, role: admin.role } });
});

router.get('/me', requireAuth, (req, res) => res.json(req.admin));

// Admin team management
router.get('/admins', requireAuth, async (req, res) => {
  res.json(await Admin.find().select('-passwordHash').sort('createdAt').lean());
});

router.post('/admins', requireAuth, requireRole(), async (req, res) => {
  const { name, email, password, role } = req.body;
  if (!name || !email || !password || password.length < 8) {
    return res.status(400).json({ error: 'Name, email and a password of at least 8 characters are required' });
  }
  const admin = await Admin.create({ name, email, role, passwordHash: await bcrypt.hash(password, 10) });
  await logAdminAction(req, 'ADMIN_TEAM_MEMBER_ADDED', { details: `${email} added as ${role}` });
  res.status(201).json({ _id: admin._id, name, email: admin.email, role: admin.role });
});

router.delete('/admins/:id', requireAuth, requireRole(), async (req, res) => {
  if (req.params.id === req.admin._id.toString()) return res.status(400).json({ error: 'You cannot remove yourself' });
  const admin = await Admin.findByIdAndDelete(req.params.id);
  if (!admin) return res.status(404).json({ error: 'Admin not found' });
  await logAdminAction(req, 'ADMIN_TEAM_MEMBER_REMOVED', { severity: 'WARNING', details: admin.email });
  res.json({ ok: true });
});

export default router;
